package com.hearinguru.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import com.hearinguru.app.data.UserProfileStore
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt


// ─── Data models ─────────────────────────────────────────────────────────────

enum class TestOutcome { PASS, REFER, INCONCLUSIVE }

private data class ToneResponse(
    val ear: String,            // "LEFT" | "RIGHT"
    val frequencyHz: Int,
    val heard: Boolean,
    val responseTimeMs: Long    // time from tone start to tap; 0 = auto-timed-out
)

private enum class TestStep {
    INTRO, CONSENT, HISTORY,
    URGENT_REFERRAL, MEDICAL_REFERRAL,
    ENVIRONMENT, PRACTICE,
    TEST_ACTIVE, RESULT
}

private enum class CheckStatus { PENDING, PASSED, FAILED, SKIPPED, UNAVAILABLE }

private data class SessionState(

    val step: TestStep = TestStep.INTRO,

    // Language preference
    val selectedLanguage: AppLanguage = SUPPORTED_LANGUAGES.first(),

    // Consent + demographics
    val ageGroup: String = "",
    val consentGiven: Boolean = false,

    // History / triage
    val hasSuddenLoss: Boolean = false,
    val hasEarPain: Boolean = false,
    val hasSignificantDizziness: Boolean = false,
    val hasTinnitus: Boolean = false,
    val hasNoiseExposure: Boolean = false,

    // Environment checks
    val headphoneCheckStatus: CheckStatus = CheckStatus.PENDING,
    val noiseCheckStatus: CheckStatus = CheckStatus.PENDING,
    val measuredAmbientDb: Float? = null,
    val checksSkipped: Boolean = false,

    // Practice
    val practiceCompleted: Boolean = false,

    // Test
    val responses: List<ToneResponse> = emptyList(),
    val currentToneIndex: Int = 0,
    val retryCount: Int = 0,
    val isRetry: Boolean = false,

    // Result
    val outcome: TestOutcome? = null,
    val leftMissed: Int = 0,
    val rightMissed: Int = 0
)

// Screening protocol: 1000, 2000, 3000, 4000, 8000 Hz each ear
// 3000 Hz catches early noise-induced notch; 8000 Hz catches presbycusis
private val TEST_TONES = listOf(
    Pair("LEFT",  1000), Pair("LEFT",  2000), Pair("LEFT",  3000), Pair("LEFT",  4000), Pair("LEFT",  8000),
    Pair("RIGHT", 1000), Pair("RIGHT", 2000), Pair("RIGHT", 3000), Pair("RIGHT", 4000), Pair("RIGHT", 8000)
)

private const val TONE_DURATION_MS      = 2000
private const val TONE_WINDOW_MS        = 4000   // auto-advance after this if no response
private const val MIN_VALID_RESPONSE_MS = 200L   // <200ms = likely false positive per tone

private val AGE_GROUPS = listOf("Under 18", "18–40", "41–60", "61–80", "Over 80")


// ─── Language support (placeholder — translations not yet implemented) ─────────

private data class AppLanguage(
    val code: String,       // BCP-47 code
    val name: String,       // English name
    val nativeName: String, // Name in the script of that language
    val flag: String        // Emoji flag
)

private val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en",    "English",   "English",    "🇬🇧"),
    AppLanguage("hi",    "Hindi",     "हिंदी",       "🇮🇳"),
    AppLanguage("es",    "Spanish",   "Español",     "🇪🇸"),
    AppLanguage("de",    "German",    "Deutsch",     "🇩🇪"),
    AppLanguage("ta",    "Tamil",     "தமிழ்",       "🇮🇳"),
    AppLanguage("te",    "Telugu",    "తెలుగు",      "🇮🇳"),
    AppLanguage("mr",    "Marathi",   "मराठी",       "🇮🇳"),
    AppLanguage("bn",    "Bengali",   "বাংলা",       "🇮🇳"),
    AppLanguage("kn",    "Kannada",   "ಕನ್ನಡ",       "🇮🇳"),
    AppLanguage("ml",    "Malayalam", "മലയാളം",      "🇮🇳"),
    AppLanguage("gu",    "Gujarati",  "ગુજરાતી",     "🇮🇳"),
    AppLanguage("pa",    "Punjabi",   "ਪੰਜਾਬੀ",      "🇮🇳"),
    AppLanguage("or",    "Odia",      "ଓଡ଼ିଆ",       "🇮🇳"),
    AppLanguage("ur",    "Urdu",      "اردو",        "🇵🇰"),
    AppLanguage("as",    "Assamese",  "অসমীয়া",     "🇮🇳")
)


// ─── Entry point ─────────────────────────────────────────────────────────────

@Composable
fun HearingTestScreen(
    onNavigateToDoctor: () -> Unit = {},
    onNavigateToHearingAids: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Load persisted consent + age group — skip ConsentStep if already saved
    val savedAgeGroup    = remember { UserProfileStore.getAgeGroup(context) }
    val isAlreadyConsented = remember { UserProfileStore.isConsented(context) }

    var session by remember {
        mutableStateOf(
            if (isAlreadyConsented) {
                SessionState(
                    consentGiven = true,
                    ageGroup     = savedAgeGroup,
                    step         = TestStep.HISTORY
                )
            } else {
                SessionState()
            }
        )
    }

    AnimatedContent(targetState = session.step, label = "step") { step ->
        when (step) {
            TestStep.INTRO -> IntroStep(
                session  = session,
                onChange = { session = it },
                onNext   = { session = session.copy(step = TestStep.CONSENT) }
            )
            TestStep.CONSENT -> ConsentStep(
                session  = session,
                onChange = { session = it },
                onNext   = {
                    // Persist so future test runs skip this step
                    UserProfileStore.saveConsent(context, session.ageGroup)
                    session = session.copy(step = TestStep.HISTORY)
                },
                onBack   = { session = session.copy(step = TestStep.INTRO) }
            )
            TestStep.HISTORY -> HistoryStep(
                session  = session,
                onChange = { session = it },
                onNext   = {
                    val isUrgent  = session.hasSuddenLoss
                    val isMedical = session.hasEarPain || session.hasSignificantDizziness
                    session = session.copy(
                        step = when {
                            isUrgent  -> TestStep.URGENT_REFERRAL
                            isMedical -> TestStep.MEDICAL_REFERRAL
                            else      -> TestStep.ENVIRONMENT
                        }
                    )
                },
                onBack = { session = session.copy(step = TestStep.CONSENT) }
            )
            TestStep.URGENT_REFERRAL -> UrgentReferralStep(
                onSeeDoctor = onNavigateToDoctor,
                onRestart   = { session = SessionState() }
            )
            TestStep.MEDICAL_REFERRAL -> MedicalReferralStep(
                onSeeDoctor  = onNavigateToDoctor,
                onContinue   = { session = session.copy(step = TestStep.ENVIRONMENT) },
                onRestart    = { session = SessionState() }
            )
            TestStep.ENVIRONMENT -> EnvironmentStep(
                session  = session,
                onChange = { session = it },
                onNext   = { session = session.copy(step = TestStep.PRACTICE) },
                onBack   = { session = session.copy(step = TestStep.HISTORY) }
            )
            TestStep.PRACTICE -> PracticeStep(
                onReady   = { session = session.copy(step = TestStep.TEST_ACTIVE, practiceCompleted = true) },
                onBack    = { session = session.copy(step = TestStep.ENVIRONMENT) }
            )
            TestStep.TEST_ACTIVE -> TestActiveStep(
                session  = session,
                isRetry  = session.isRetry,
                onResult = { responses ->
                    val outcome = classifyOutcome(responses, session.retryCount)
                    val leftMissed  = responses.count { it.ear == "LEFT"  && !it.heard }
                    val rightMissed = responses.count { it.ear == "RIGHT" && !it.heard }

                    // If inconclusive and first attempt → offer retry
                    val isFirstInconclusive = outcome == TestOutcome.INCONCLUSIVE && session.retryCount == 0
                    if (isFirstInconclusive) {
                        session = session.copy(
                            responses   = responses,
                            retryCount  = 1,
                            isRetry     = true,
                            step        = TestStep.TEST_ACTIVE,
                            currentToneIndex = 0
                        )
                    } else {
                        session = session.copy(
                            responses   = responses,
                            leftMissed  = leftMissed,
                            rightMissed = rightMissed,
                            outcome     = outcome,
                            step        = TestStep.RESULT
                        )
                    }
                }
            )
            TestStep.RESULT -> ResultStep(
                session             = session,
                onBookConsult       = onNavigateToDoctor,
                onBrowseHAs         = onNavigateToHearingAids,
                onRetake            = { session = SessionState() }
            )
        }
    }
}


// ─── Step 1: Intro ────────────────────────────────────────────────────────────

@Composable
private fun IntroStep(
    session: SessionState,
    onChange: (SessionState) -> Unit,
    onNext: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text("Select language", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // Coming soon notice
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                            Text("Translations coming soon. English is the only active language.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, lineHeight = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isSelected = session.selectedLanguage.code == lang.code
                        val isAvailable = lang.code == "en"
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .selectable(selected = isSelected, onClick = {
                                    onChange(session.copy(selectedLanguage = lang))
                                    showLanguageDialog = false
                                })
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.flag, fontSize = 22.sp)
                            Column(Modifier.weight(1f)) {
                                Text(lang.nativeName, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                Text(lang.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!isAvailable) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Text("Soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Language selector bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
                onClick = { showLanguageDialog = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Text(session.selectedLanguage.flag, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(session.selectedLanguage.nativeName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Hearing Screening", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "A fast 3–5 minute self-administered screening that checks for potential hearing difficulties and guides you towards the right care.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(8.dp))
        DisclaimerBox("This is a hearing screening — not a clinical diagnosis. Results should always be confirmed by a qualified audiologist.")
        Spacer(Modifier.height(20.dp))

        Text("WHAT HAPPENS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(12.dp))
        listOf(
            Triple(Icons.Default.AssignmentInd, "Quick health questions", "Age, concerns and safety triage to keep you safe"),
            Triple(Icons.Default.Headphones,    "Equipment check",        "Quiet room and headphone confirmation"),
            Triple(Icons.Default.HearingDisabled,"Tone screening",        "Six calibration tones — three per ear"),
            Triple(Icons.Default.AssignmentTurnedIn,"Your result",        "PASS, REFER or INCONCLUSIVE with clear next steps")
        ).forEach { (icon, title, sub) ->
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp).padding(top = 2.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(50)) {
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Begin screening", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}


// ─── Step 2: Consent + demographics ──────────────────────────────────────────

@Composable
private fun ConsentStep(
    session: SessionState,
    onChange: (SessionState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val canProceed = session.consentGiven && session.ageGroup.isNotBlank()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        StepHeader(step = 1, total = 5, title = "Consent & Details")
        Spacer(Modifier.height(16.dp))

        Text("Your age group", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        AGE_GROUPS.forEach { group ->
            val selected = session.ageGroup == group
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                    .selectable(selected = selected, onClick = { onChange(session.copy(ageGroup = group)) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(group, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Consent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = session.consentGiven, onClick = { onChange(session.copy(consentGiven = !session.consentGiven)) })
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(checked = session.consentGiven, onCheckedChange = { onChange(session.copy(consentGiven = it)) })
                Text(
                    "I understand this is a hearing screening for informational purposes only. It is not a clinical diagnosis and does not replace professional audiological evaluation.",
                    style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text("Back")
            }
            Button(onClick = onNext, enabled = canProceed, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}


// ─── Step 3: Clinical history / safety triage ────────────────────────────────

@Composable
private fun HistoryStep(
    session: SessionState,
    onChange: (SessionState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showOptional by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        StepHeader(step = 2, total = 5, title = "Safety Check")
        Spacer(Modifier.height(8.dp))
        Text(
            "Two quick safety questions before we start. Answer carefully — your answers may change the recommended path.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        // Urgent — sudden loss
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                Text("Answer carefully", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        YesNoQuestion(
            question = "Did your hearing loss start suddenly in the last 72 hours?",
            value    = session.hasSuddenLoss,
            onChange = { onChange(session.copy(hasSuddenLoss = it)) },
            isUrgent = true
        )
        Spacer(Modifier.height(12.dp))
        YesNoQuestion(
            question = "Do you currently have significant ear pain, discharge, or severe dizziness?",
            value    = session.hasEarPain || session.hasSignificantDizziness,
            onChange = { v -> onChange(session.copy(hasEarPain = v, hasSignificantDizziness = v)) },
            isUrgent = false
        )

        Spacer(Modifier.height(20.dp))

        // Optional history — collapsible
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(selected = showOptional, onClick = { showOptional = !showOptional })
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Additional history (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(if (showOptional) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(visible = showOptional) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text("These answers are optional and help your audiologist if you proceed to a consultation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        YesNoQuestion("Do you hear ringing, buzzing or other sounds in your ears (tinnitus)?", session.hasTinnitus, { onChange(session.copy(hasTinnitus = it)) })
                        Spacer(Modifier.height(10.dp))
                        YesNoQuestion("Have you had significant noise exposure at work or regularly?", session.hasNoiseExposure, { onChange(session.copy(hasNoiseExposure = it)) })
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}


// ─── Urgent referral ─────────────────────────────────────────────────────────

@Composable
private fun UrgentReferralStep(onSeeDoctor: () -> Unit, onRestart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Emergency, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Urgent Medical Evaluation Needed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Sudden hearing loss is a medical emergency. You need to see a doctor or attend an emergency clinic within 24–48 hours — early treatment significantly improves outcomes.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 24.sp
        )
        Spacer(Modifier.height(20.dp))
        DisclaimerBox("Do not attempt the hearing screening. Seek professional medical care immediately.")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSeeDoctor, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.LocalHospital, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Contact a doctor now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRestart) { Text("Start over") }
    }
}


// ─── Medical referral ────────────────────────────────────────────────────────

@Composable
private fun MedicalReferralStep(
    onSeeDoctor: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Medical Evaluation Recommended", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "You reported ear pain, discharge or significant dizziness. These symptoms should be assessed by a doctor before a routine hearing screening.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp
        )
        Spacer(Modifier.height(16.dp))
        NoticeBox(Icons.Default.Info, "You may still proceed with the screening, but your results may be less reliable until the underlying symptoms are treated.", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onSeeDoctor, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) {
            Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("See a doctor first", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) {
            Text("Continue screening anyway")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Start over") }
    }
}


// ─── Step 4: Environment + equipment ─────────────────────────────────────────

@Composable
private fun EnvironmentStep(
    session: SessionState,
    onChange: (SessionState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showSkipWarning by remember { mutableStateOf(false) }

    // Headphone channel test state: idle | playing_left | playing_right | ask | done_pass | done_fail
    var headphoneTestState by remember { mutableStateOf("idle") }
    var headphoneAnswer by remember { mutableStateOf("") }

    // Noise check state: idle | checking | done
    var noiseCheckState by remember { mutableStateOf("idle") }

    // Runtime mic permission launcher — triggers noise measurement after grant
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            noiseCheckState = "checking"
            scope.launch {
                val db     = withContext(Dispatchers.IO) { measureAmbientDb() }
                val status = when {
                    db == null     -> CheckStatus.UNAVAILABLE
                    db < 60f       -> CheckStatus.PASSED
                    else           -> CheckStatus.FAILED
                }
                onChange(session.copy(noiseCheckStatus = status, measuredAmbientDb = db))
                noiseCheckState = "done"
            }
        } else {
            onChange(session.copy(noiseCheckStatus = CheckStatus.UNAVAILABLE, measuredAmbientDb = null))
            noiseCheckState = "done"
        }
    }

    // Skip-checks warning dialog
    if (showSkipWarning) {
        AlertDialog(
            onDismissRequest = { showSkipWarning = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Checks not completed", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Skipping environment checks means your result will be labelled Unverified Screening. It cannot be treated as clinically reliable.\n\nProceed only if checks are truly unavailable on this device.",
                    style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChange(session.copy(checksSkipped = true, headphoneCheckStatus = CheckStatus.SKIPPED, noiseCheckStatus = CheckStatus.SKIPPED))
                        showSkipWarning = false
                        onNext()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Proceed unverified") }
            },
            dismissButton = {
                TextButton(onClick = { showSkipWarning = false }) { Text("Go back") }
            }
        )
    }

    val checksReady = session.headphoneCheckStatus == CheckStatus.PASSED &&
                      session.noiseCheckStatus in listOf(CheckStatus.PASSED, CheckStatus.FAILED, CheckStatus.UNAVAILABLE)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        StepHeader(step = 3, total = 5, title = "Environment Check")
        Spacer(Modifier.height(8.dp))
        Text("We run two quick checks before the test. Both can be skipped if unavailable, but skipping reduces result reliability.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))


        // ── Check 1: Headphone channel test ──────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (session.headphoneCheckStatus) {
                CheckStatus.PASSED  -> MaterialTheme.colorScheme.primaryContainer
                CheckStatus.FAILED  -> MaterialTheme.colorScheme.errorContainer
                CheckStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceContainerHigh
                else                -> MaterialTheme.colorScheme.surfaceContainerLow
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Headphones, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Headphone channel check", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    CheckStatusBadge(session.headphoneCheckStatus)
                }
                Spacer(Modifier.height(6.dp))
                Text("Put your headphones on. We play a tone in your left ear — tap to confirm which side you hear it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))

                when {
                    session.headphoneCheckStatus == CheckStatus.PASSED ||
                    session.headphoneCheckStatus == CheckStatus.FAILED  -> { /* show status via badge above */ }

                    headphoneTestState == "idle" -> {
                        OutlinedButton(
                            onClick = {
                                headphoneTestState = "playing_left"
                                scope.launch {
                                    withContext(Dispatchers.IO) { playTone(1000.0, 1000, "LEFT", 0.6) }
                                    headphoneTestState = "ask"
                                }
                            },
                            shape = RoundedCornerShape(50), modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play test tone", fontSize = 13.sp)
                        }
                    }

                    headphoneTestState == "playing_left" -> {
                        Text("Playing tone — which ear do you hear it in?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }

                    headphoneTestState == "ask" -> {
                        Text("Which side did you hear it?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Left", "Right", "Both / Neither").forEach { answer ->
                                OutlinedButton(
                                    onClick = {
                                        headphoneAnswer = answer
                                        val passed = answer == "Left"
                                        headphoneTestState = if (passed) "done_pass" else "done_fail"
                                        onChange(session.copy(headphoneCheckStatus = if (passed) CheckStatus.PASSED else CheckStatus.FAILED))
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) { Text(answer, fontSize = 12.sp) }
                            }
                        }
                    }
                }

                if (session.headphoneCheckStatus == CheckStatus.FAILED) {
                    Spacer(Modifier.height(8.dp))
                    Text("Check failed — check headphone connection or try a different pair. You may still proceed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, lineHeight = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        headphoneTestState = "idle"
                        onChange(session.copy(headphoneCheckStatus = CheckStatus.PENDING))
                    }) { Text("Retry", fontSize = 12.sp) }
                }

                if (session.headphoneCheckStatus == CheckStatus.PENDING && headphoneTestState == "idle") {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { onChange(session.copy(headphoneCheckStatus = CheckStatus.SKIPPED)) }) {
                        Text("Skip this check", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))


        // ── Check 2: Ambient noise check ─────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (session.noiseCheckStatus) {
                CheckStatus.PASSED  -> MaterialTheme.colorScheme.primaryContainer
                CheckStatus.FAILED  -> MaterialTheme.colorScheme.errorContainer
                CheckStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceContainerHigh
                else                -> MaterialTheme.colorScheme.surfaceContainerLow
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Ambient noise check", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    CheckStatusBadge(session.noiseCheckStatus)
                }
                Spacer(Modifier.height(6.dp))
                Text("We sample ambient noise for 2 seconds via microphone to check if the room is quiet enough.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)

                if (session.measuredAmbientDb != null) {
                    Spacer(Modifier.height(8.dp))
                    val db = session.measuredAmbientDb
                    val label = when {
                        db < 45f -> "Quiet — good conditions"
                        db < 60f -> "Moderate noise — results may be affected"
                        else     -> "Too loud — move to a quieter space"
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("~${db.toInt()} dB", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (db < 45f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (session.noiseCheckStatus == CheckStatus.UNAVAILABLE) {
                    Spacer(Modifier.height(6.dp))
                    Text("Microphone unavailable — permission denied or device unsupported. This check will be skipped.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }

                Spacer(Modifier.height(10.dp))
                when (noiseCheckState) {
                    "idle" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    // Request RECORD_AUDIO if not yet granted; launcher fires measurement on grant
                                    val isAlreadyGranted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (isAlreadyGranted) {
                                        noiseCheckState = "checking"
                                        scope.launch {
                                            val db     = withContext(Dispatchers.IO) { measureAmbientDb() }
                                            val status = when {
                                                db == null -> CheckStatus.UNAVAILABLE
                                                db < 60f   -> CheckStatus.PASSED
                                                else       -> CheckStatus.FAILED
                                            }
                                            onChange(session.copy(noiseCheckStatus = status, measuredAmbientDb = db))
                                            noiseCheckState = "done"
                                        }
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                shape = RoundedCornerShape(50), modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check noise level", fontSize = 13.sp)
                            }
                            TextButton(onClick = { onChange(session.copy(noiseCheckStatus = CheckStatus.SKIPPED)); noiseCheckState = "done" }) {
                                Text("Skip", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    "checking" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Sampling 2 seconds...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    "done" -> {
                        if (session.noiseCheckStatus == CheckStatus.FAILED) {
                            TextButton(onClick = {
                                noiseCheckState = "idle"
                                onChange(session.copy(noiseCheckStatus = CheckStatus.PENDING, measuredAmbientDb = null))
                            }) { Text("Retry after moving", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))


        // ── Volume guidance ───────────────────────────────────────────────────
        NoticeBox(
            Icons.Default.VolumeUp,
            "Set your volume to a comfortable listening level — loud enough to hear speech clearly, not uncomfortably loud.",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) { Text("Back") }
            Button(
                onClick = onNext,
                enabled = checksReady,
                modifier = Modifier.weight(2f).height(52.dp),
                shape = RoundedCornerShape(50)
            ) { Text("Continue", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { showSkipWarning = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip all checks (unverified result)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}


// ─── Step 5: Practice ─────────────────────────────────────────────────────────

// States: idle → countdown (3,2,1) → playing → responding → done_heard | done_missed
@Composable
private fun PracticeStep(onReady: () -> Unit, onBack: () -> Unit) {

    var practiceState  by remember { mutableStateOf("idle") }
    var countdownValue by remember { mutableIntStateOf(3) }
    val scope          = rememberCoroutineScope()

    fun launchPracticeTone() {
        scope.launch {
            // Countdown 3 → 2 → 1
            practiceState  = "countdown"
            countdownValue = 3
            repeat(3) { i ->
                countdownValue = 3 - i
                delay(800)
            }

            // Play tone for 1500ms at higher amplitude so it's obvious
            practiceState = "playing"
            launch { withContext(Dispatchers.IO) { playTone(1000.0, 1500, "RIGHT", amplitude = 0.7) } }

            // After tone ends, keep button live 1.5s more before timing out
            delay(1500)
            if (practiceState == "playing") {
                practiceState = "responding"
                delay(1500)
                if (practiceState == "responding") practiceState = "done_missed"
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(step = 4, total = 5, title = "Practice Round")
        Spacer(Modifier.height(12.dp))
        Text(
            "A practice tone will play in your right ear. Tap the moment you hear anything — even very faintly. Real test tones will be softer.",
            style      = MaterialTheme.typography.bodyLarge,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        when (practiceState) {

            "idle" -> {
                OutlinedButton(
                    onClick  = { launchPracticeTone() },
                    modifier = Modifier.height(52.dp),
                    shape    = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play practice tone", fontWeight = FontWeight.SemiBold)
                }
            }

            "countdown" -> {
                Surface(
                    shape    = RoundedCornerShape(50),
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$countdownValue",
                            fontSize   = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Get ready — tone plays in your right ear",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            "playing", "responding" -> {
                FilledIconButton(
                    onClick  = { if (practiceState == "playing" || practiceState == "responding") practiceState = "done_heard" },
                    modifier = Modifier.size(110.dp),
                    shape    = RoundedCornerShape(50),
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("I heard it", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (practiceState == "playing") "Tone playing — tap now if you hear it"
                    else                            "Did you catch it? Tap above",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            "done_heard" -> {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(12.dp))
                Text("You heard it!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(Modifier.height(6.dp))
                Text(
                    "Respond exactly like that during the real test.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            "done_missed" -> {
                Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(12.dp))
                Text("Didn't catch it?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "That's fine — tap the instant you hear anything, even very faintly. You can try again or skip straight to the test.",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick  = { launchPracticeTone() },
                    shape    = RoundedCornerShape(50),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Try again")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (practiceState == "done_heard" || practiceState == "done_missed") {
            Button(
                onClick  = onReady,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start real test", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(20.dp))
    }
}


// ─── Step 6: Active test ──────────────────────────────────────────────────────

@Composable
private fun TestActiveStep(
    session: SessionState,
    isRetry: Boolean,
    onResult: (List<ToneResponse>) -> Unit
) {
    val responses   = remember { mutableStateListOf<ToneResponse>() }
    var toneIndex   by remember { mutableIntStateOf(0) }
    var toneStartMs by remember { mutableLongStateOf(0L) }
    var isPlaying   by remember { mutableStateOf(false) }
    var countdown   by remember { mutableIntStateOf(TONE_WINDOW_MS / 1000) }
    val scope       = rememberCoroutineScope()

    val totalTones  = TEST_TONES.size
    val currentTone = TEST_TONES.getOrNull(toneIndex)

    LaunchedEffect(Unit) {
        if (isRetry) responses.clear()
    }

    // Play tone + countdown on each index change
    LaunchedEffect(toneIndex) {
        if (toneIndex >= totalTones) {
            onResult(responses.toList())
            return@LaunchedEffect
        }

        val (ear, freq) = TEST_TONES[toneIndex]
        isPlaying   = true
        countdown   = TONE_WINDOW_MS / 1000
        toneStartMs = System.currentTimeMillis()

        // Play tone async
        scope.launch {
            withContext(Dispatchers.IO) { playTone(freq.toDouble(), TONE_DURATION_MS, ear) }
        }

        // Countdown and auto-advance
        repeat(TONE_WINDOW_MS / 1000) {
            delay(1000)
            countdown--
        }

        // Auto-advance — not heard
        if (isPlaying) {
            responses.add(ToneResponse(ear = ear, frequencyHz = freq, heard = false, responseTimeMs = 0))
            isPlaying = false
            toneIndex++
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isRetry) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(" Retry — first attempt was inconclusive ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            "Tone ${toneIndex + 1} of $totalTones — ${currentTone?.first?.let { if (it == "LEFT") "Left ear" else "Right ear" } ?: ""}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { toneIndex.toFloat() / totalTones },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(20.dp))

        // Ear indicator
        val ear = currentTone?.first ?: ""
        Surface(shape = RoundedCornerShape(50), color = if (ear == "LEFT") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (ear == "LEFT") Icons.Default.West else Icons.Default.East,
                    null,
                    tint = if (ear == "LEFT") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (ear == "LEFT") "Left ear" else "Right ear",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (ear == "LEFT") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // Freq + wave display
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FREQUENCY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${currentTone?.second ?: ""}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1).sp)
                    Text(" Hz", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                // Animated wave bars
                Row(Modifier.height(36.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(10, 22, 34, 28, 16, 30, 34, 20, 10).forEachIndexed { i, h ->
                        val animH by rememberInfiniteTransition(label = "b$i").animateFloat(
                            initialValue = h.toFloat(), targetValue = h * 0.25f,
                            animationSpec = infiniteRepeatable(tween(900, delayMillis = i * 80), RepeatMode.Reverse), label = "bar$i"
                        )
                        Box(Modifier.width(5.dp).height(animH.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 1f else 0.3f)))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isPlaying) "Sound is playing — tap if you hear it (${countdown}s)" else "Preparing...",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Primary response — large prominent tap target
        FilledIconButton(
            onClick = {
                if (isPlaying && currentTone != null) {
                    val elapsed = System.currentTimeMillis() - toneStartMs
                    responses.add(ToneResponse(ear = currentTone.first, frequencyHz = currentTone.second, heard = true, responseTimeMs = elapsed))
                    isPlaying = false
                    toneIndex++
                }
            },
            modifier = Modifier.size(124.dp),
            shape = RoundedCornerShape(50),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Hearing, null,
                    tint     = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "I heard it",
                    color      = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Generous gap prevents accidental taps on the secondary button
        Spacer(Modifier.height(32.dp))

        // Secondary response — equally accessible, full-width tonal button
        FilledTonalButton(
            onClick = {
                if (isPlaying && currentTone != null) {
                    responses.add(ToneResponse(ear = currentTone.first, frequencyHz = currentTone.second, heard = false, responseTimeMs = 0))
                    isPlaying = false
                    toneIndex++
                }
            },
            enabled  = isPlaying,
            shape    = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.VolumeOff, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("I couldn't hear this", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}


// ─── Step 7: Result ───────────────────────────────────────────────────────────

@Composable
private fun ResultStep(
    session: SessionState,
    onBookConsult: () -> Unit,
    onBrowseHAs: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val outcome = session.outcome ?: TestOutcome.INCONCLUSIVE


    // Plain-language lead sentence — most prominent element (improvement #4)
    val leadSentence = when (outcome) {
        TestOutcome.PASS         -> "No concern detected."
        TestOutcome.REFER        -> "Possible hearing difficulty detected."
        TestOutcome.INCONCLUSIVE -> "We couldn't reliably assess your hearing."
    }

    // Semantic result colors — fixed regardless of theme (health convention: green/red/amber)
    val heroColor = when (outcome) {
        TestOutcome.PASS         -> Color(0xFF2E7D32)   // green 800
        TestOutcome.REFER        -> Color(0xFFC62828)   // red 800
        TestOutcome.INCONCLUSIVE -> Color(0xFFE65100)   // deep orange 900
    }
    val heroBg = when (outcome) {
        TestOutcome.PASS         -> Color(0xFFE8F5E9)   // green 50
        TestOutcome.REFER        -> Color(0xFFFFEBEE)   // red 50
        TestOutcome.INCONCLUSIVE -> Color(0xFFFFF3E0)   // orange 50
    }
    val heroIcon = when (outcome) {
        TestOutcome.PASS         -> Icons.Default.CheckCircle
        TestOutcome.REFER        -> Icons.Default.HearingDisabled
        TestOutcome.INCONCLUSIVE -> Icons.Default.HelpOutline
    }

    // Shareable text report (improvement #3)
    val reportText = buildShareableReport(session)


    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        StepHeader(step = 5, total = 5, title = "Your Result")
        Spacer(Modifier.height(16.dp))

        // ── Hero card with plain-language lead ───────────────────────────────
        Surface(shape = RoundedCornerShape(24.dp), color = heroBg, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(heroIcon, null, tint = heroColor, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))

                // Plain-language lead — headline size, most prominent
                Text(
                    leadSentence,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = heroColor,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))

                // Unverified badge if checks were skipped
                if (session.checksSkipped) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Unverified screening — environment checks skipped",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            textAlign  = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Clinical classification label
                Text(
                    when (outcome) {
                        TestOutcome.PASS         -> "PASS — No Screening Concern"
                        TestOutcome.REFER        -> "REFER — Possible Hearing Difficulty"
                        TestOutcome.INCONCLUSIVE -> "INCONCLUSIVE"
                    },
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = heroColor.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))

                // Supporting explanation
                Text(
                    when (outcome) {
                        TestOutcome.PASS         -> "No significant hearing difficulty was detected. If you still have concerns, see an audiologist."
                        TestOutcome.REFER        -> "A comprehensive hearing assessment by a qualified audiologist is recommended."
                        TestOutcome.INCONCLUSIVE -> "This may be due to environmental noise, equipment issues, or response inconsistencies."
                    },
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Screening audiogram (improvement #2) ─────────────────────────────
        if (outcome != TestOutcome.INCONCLUSIVE && session.responses.isNotEmpty()) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("SCREENING AUDIOGRAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(14.dp))
                    ScreeningAudiogram(responses = session.responses)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Ear-specific summary ──────────────────────────────────────────────
        if (outcome != TestOutcome.INCONCLUSIVE) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("EAR-SPECIFIC SUMMARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(12.dp))
                    EarSummaryRow("Left ear",  session.leftMissed,  5)
                    Spacer(Modifier.height(8.dp))
                    EarSummaryRow("Right ear", session.rightMissed, 5)
                    Spacer(Modifier.height(10.dp))
                    DisclaimerBox("Smartphone audio is not audiologically calibrated. These results indicate a possible concern only.")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Recommended next step ─────────────────────────────────────────────
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("RECOMMENDED NEXT STEP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (outcome) {
                        TestOutcome.PASS         -> "Your screening was normal. If you still have concerns or symptoms, please see an audiologist — a PASS result does not rule out hearing difficulties."
                        TestOutcome.REFER        -> "Book a comprehensive hearing assessment with a certified audiologist. Bring this screening result to your appointment."
                        TestOutcome.INCONCLUSIVE -> "Ensure you are in a very quiet room with headphones on full volume and retry the test. If problems persist, consult an audiologist directly."
                    },
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    lineHeight = 20.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Primary CTA — one clear next action (improvement #4) ─────────────
        if (outcome == TestOutcome.PASS || outcome == TestOutcome.REFER) {
            Button(
                onClick  = onBookConsult,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Book professional assessment", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Share result button (improvement #3)
        OutlinedButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "HearinGuru Screening Result")
                    putExtra(Intent.EXTRA_TEXT, reportText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share screening result"))
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share result", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))

        // Browse hearing aids — secondary for REFER
        if (outcome == TestOutcome.REFER) {
            OutlinedButton(
                onClick  = onBrowseHAs,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.HearingDisabled, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse hearing aids", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick  = onRetake,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start over", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}


// ─── Outcome classification ───────────────────────────────────────────────────

private fun classifyOutcome(responses: List<ToneResponse>, retryCount: Int): TestOutcome {
    if (responses.size < TEST_TONES.size) return TestOutcome.INCONCLUSIVE

    // Reliability check: 3+ suspiciously fast responses (<200ms) suggests button mashing
    val tooFastCount = responses.count { it.heard && it.responseTimeMs in 1..<MIN_VALID_RESPONSE_MS }
    val isUnreliable = tooFastCount >= 3 && retryCount == 0
    if (isUnreliable) return TestOutcome.INCONCLUSIVE

    val leftMissed  = responses.count { it.ear == "LEFT"  && !it.heard }
    val rightMissed = responses.count { it.ear == "RIGHT" && !it.heard }

    // REFER: missed 2+ in either ear (standard single-level screening threshold)
    val isRefer = leftMissed >= 2 || rightMissed >= 2
    return if (isRefer) TestOutcome.REFER else TestOutcome.PASS
}


// ─── Audio: correct stereo panning per ear ────────────────────────────────────

private fun playTone(freqHz: Double, durationMs: Int, ear: String, amplitude: Double = 0.45) {
    val sampleRate   = 44100
    val numFrames    = (sampleRate * durationMs / 1000.0).toInt()
    val fadeFrames   = (sampleRate * 0.015).toInt()   // 15ms fade in/out to avoid clicks

    // Stereo interleaved: [L0, R0, L1, R1, ...]
    val buffer = ShortArray(numFrames * 2) { i ->
        val frame         = i / 2
        val isRightChannel = i % 2 == 1
        val playInChannel = if (ear == "LEFT") !isRightChannel else isRightChannel

        if (!playInChannel) return@ShortArray 0

        // Apply fade envelope
        val fadeFactor = when {
            frame < fadeFrames                     -> frame.toDouble() / fadeFrames
            frame > numFrames - fadeFrames         -> (numFrames - frame).toDouble() / fadeFrames
            else                                   -> 1.0
        }
        val sample = sin(2 * PI * freqHz * frame / sampleRate) * amplitude * fadeFactor
        (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC, sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT,
        buffer.size * 2,
        AudioTrack.MODE_STATIC
    )
    audioTrack.write(buffer, 0, buffer.size)
    audioTrack.play()
    Thread.sleep(durationMs.toLong())
    audioTrack.stop()
    audioTrack.release()
}


// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun StepHeader(step: Int, total: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
            Text(
                "$step/$total",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun YesNoQuestion(
    question: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    isUrgent: Boolean = false
) {
    val borderColor = if (value && isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (value && isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(question, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(true to "Yes", false to "No").forEach { (option, label) ->
                    val selected = value == option
                    OutlinedButton(
                        onClick = { onChange(option) },
                        shape   = RoundedCornerShape(50),
                        colors  = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = if (option && isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, contentColor = if (option && isUrgent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary)
                                  else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.height(38.dp)
                    ) { Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

@Composable
private fun CheckConfirmCard(icon: ImageVector, title: String, body: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, null, tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).padding(top = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun EarSummaryRow(label: String, missed: Int, total: Int) {
    val heard        = total - missed
    val isProblem    = missed >= 2
    val resultColor  = if (isProblem) Color(0xFFC62828) else Color(0xFF2E7D32)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$heard/$total heard",
                style      = MaterialTheme.typography.bodyMedium,
                color      = resultColor,
                fontWeight = FontWeight.Bold
            )
            Icon(
                if (isProblem) Icons.Default.Warning else Icons.Default.CheckCircle,
                null,
                tint     = resultColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DisclaimerBox(text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(top = 1.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
fun NoticeBox(icon: ImageVector, text: String, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = containerColor) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp).padding(top = 1.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor, lineHeight = 20.sp)
        }
    }
}

// Destructuring helper for result display data
private data class ResultDisplay(
    val color: Color, val bg: Color, val icon: ImageVector, val title: String, val body: String
)


@Composable
private fun CheckStatusBadge(status: CheckStatus) {
    val text = when (status) {
        CheckStatus.PASSED      -> "Passed"
        CheckStatus.FAILED      -> "Failed"
        CheckStatus.SKIPPED     -> "Skipped"
        CheckStatus.PENDING     -> "Pending"
        CheckStatus.UNAVAILABLE -> "N/A"
    }
    val bg = when (status) {
        CheckStatus.PASSED      -> MaterialTheme.colorScheme.primaryContainer
        CheckStatus.FAILED      -> MaterialTheme.colorScheme.errorContainer
        CheckStatus.SKIPPED     -> MaterialTheme.colorScheme.surfaceContainerHigh
        CheckStatus.PENDING     -> MaterialTheme.colorScheme.surfaceContainerLow
        CheckStatus.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val fg = when (status) {
        CheckStatus.PASSED      -> MaterialTheme.colorScheme.onPrimaryContainer
        CheckStatus.FAILED      -> MaterialTheme.colorScheme.onErrorContainer
        else                    -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text,
            style      = MaterialTheme.typography.labelSmall,
            color      = fg,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}


// ─── Screening audiogram (improvement #2) ────────────────────────────────────
// Binary pass/fail chart at 1000, 2000, 4000 Hz per ear.
// ○ = left ear,  ✕ = right ear.  Not a calibrated audiogram.

@Composable
private fun ScreeningAudiogram(
    responses: List<ToneResponse>,
    modifier: Modifier = Modifier
) {
    val freqs       = listOf(1000, 2000, 3000, 4000, 8000)
    val leftByFreq  = responses.filter { it.ear == "LEFT"  }.associate { it.frequencyHz to it.heard }
    val rightByFreq = responses.filter { it.ear == "RIGHT" }.associate { it.frequencyHz to it.heard }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor   = MaterialTheme.colorScheme.error
    val gridColor    = MaterialTheme.colorScheme.outlineVariant
    val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier) {

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(primaryColor, CircleShape))
                Text("Left ear (○)", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(errorColor, RoundedCornerShape(2.dp)))
                Text("Right ear (✕)", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
        Spacer(Modifier.height(10.dp))

        // Chart area: y-axis labels + canvas
        Row(Modifier.fillMaxWidth().height(110.dp)) {

            // Y-axis labels
            Column(
                modifier            = Modifier.width(44.dp).fillMaxHeight().padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Not\nheard", style = MaterialTheme.typography.labelSmall, color = labelColor, lineHeight = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text("Heard",     style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }

            // Canvas plot
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val w       = size.width
                val h       = size.height
                val dotR    = 9.dp.toPx()
                val crossR  = 7.dp.toPx()
                val strokeW = 2.5.dp.toPx()
                val connW   = 1.5.dp.toPx()

                // Even x-positions for 5 frequencies
                val xPositions = listOf(w * 0.05f, w * 0.27f, w * 0.50f, w * 0.73f, w * 0.95f)

                // y: "not heard" = top 15%,  "heard" = bottom 85%
                fun yFor(heard: Boolean) = if (heard) h * 0.85f else h * 0.15f

                // Dashed grid lines at the two levels
                listOf(h * 0.15f, h * 0.85f).forEach { y ->
                    drawLine(
                        color       = gridColor,
                        start       = Offset(0f, y),
                        end         = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
                    )
                }

                // Connect left-ear points with a thin line
                for (i in 0 until freqs.size - 1) {
                    val isFirstHeard  = leftByFreq[freqs[i]]
                    val isSecondHeard = leftByFreq[freqs[i + 1]]
                    if (isFirstHeard != null && isSecondHeard != null) {
                        drawLine(
                            color       = primaryColor.copy(alpha = 0.35f),
                            start       = Offset(xPositions[i],     yFor(isFirstHeard)),
                            end         = Offset(xPositions[i + 1], yFor(isSecondHeard)),
                            strokeWidth = connW
                        )
                    }
                }

                // Connect right-ear points with a thin line
                for (i in 0 until freqs.size - 1) {
                    val isFirstHeard  = rightByFreq[freqs[i]]
                    val isSecondHeard = rightByFreq[freqs[i + 1]]
                    if (isFirstHeard != null && isSecondHeard != null) {
                        drawLine(
                            color       = errorColor.copy(alpha = 0.35f),
                            start       = Offset(xPositions[i] + 14.dp.toPx(),     yFor(isFirstHeard)),
                            end         = Offset(xPositions[i + 1] + 14.dp.toPx(), yFor(isSecondHeard)),
                            strokeWidth = connW
                        )
                    }
                }

                // Plot each frequency
                freqs.forEachIndexed { idx, freq ->
                    val xLeft  = xPositions[idx]
                    val xRight = xPositions[idx] + 14.dp.toPx()

                    // Left ear — filled circle (○ symbol)
                    leftByFreq[freq]?.let { heard ->
                        drawCircle(
                            color  = primaryColor,
                            radius = dotR,
                            center = Offset(xLeft, yFor(heard))
                        )
                    }

                    // Right ear — cross mark (✕ symbol), slightly offset right
                    rightByFreq[freq]?.let { heard ->
                        val cx = xRight
                        val cy = yFor(heard)
                        drawLine(errorColor, Offset(cx - crossR, cy - crossR), Offset(cx + crossR, cy + crossR), strokeWidth = strokeW)
                        drawLine(errorColor, Offset(cx + crossR, cy - crossR), Offset(cx - crossR, cy + crossR), strokeWidth = strokeW)
                    }
                }
            }
        }

        // X-axis frequency labels
        Row(Modifier.fillMaxWidth().padding(start = 44.dp)) {
            freqs.forEach { freq ->
                val label = when {
                    freq < 1000  -> "${freq}Hz"
                    freq == 3000 -> "3k"
                    freq == 8000 -> "8k"
                    else         -> "${freq / 1000}k"
                }
                Text(
                    label,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = labelColor,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "Frequency (Hz)",
            style     = MaterialTheme.typography.labelSmall,
            color     = labelColor,
            modifier  = Modifier.fillMaxWidth().padding(start = 44.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))
        DisclaimerBox("Screening chart — not a calibrated audiogram. Pass/fail at a single test level only.")
    }
}


// ─── Shareable text report (improvement #3) ──────────────────────────────────

private fun buildShareableReport(session: SessionState): String {
    val outcome   = session.outcome ?: TestOutcome.INCONCLUSIVE
    val dateLabel = java.time.LocalDate.now().toString()

    val outcomeLine = when (outcome) {
        TestOutcome.PASS         -> "PASS — No Screening Concern"
        TestOutcome.REFER        -> "REFER — Possible Hearing Difficulty"
        TestOutcome.INCONCLUSIVE -> "INCONCLUSIVE"
    }

    val sb = StringBuilder()

    sb.appendLine("=== HearinGuru Hearing Screening Result ===")
    sb.appendLine("Date:   $dateLabel")
    sb.appendLine("Method: Pure-tone screening (1000, 2000, 4000 Hz)")
    sb.appendLine()
    sb.appendLine("RESULT: $outcomeLine")

    if (session.checksSkipped) {
        sb.appendLine("Note:   Environment checks were skipped — unverified screening.")
    }

    if (outcome != TestOutcome.INCONCLUSIVE && session.responses.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("EAR RESULTS:")

        fun appendEar(earLabel: String, responses: List<ToneResponse>) {
            val freqSummary = responses.joinToString(", ") { r ->
                "${r.frequencyHz}Hz: ${if (r.heard) "heard" else "not heard"}"
            }
            sb.appendLine("  $earLabel: $freqSummary")
        }

        appendEar("Left ear",  session.responses.filter { it.ear == "LEFT"  })
        appendEar("Right ear", session.responses.filter { it.ear == "RIGHT" })
    }

    sb.appendLine()
    sb.appendLine(
        "DISCLAIMER: This is a hearing screening for informational purposes only. " +
        "It is not a clinical diagnosis and does not replace professional audiological evaluation. " +
        "Results should be confirmed by a qualified audiologist."
    )
    sb.appendLine()
    sb.appendLine("Generated by HearinGuru")

    return sb.toString()
}


// ─── Ambient noise measurement ────────────────────────────────────────────────

private fun measureAmbientDb(): Float? {
    val sampleRate = 44100
    val minBufSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    if (minBufSize <= 0) return null

    val recorder = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            minBufSize * 4
        )
    } catch (e: Exception) { return null }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        return null
    }

    val durationMs   = 2000
    val totalSamples = sampleRate * durationMs / 1000
    val sampleBuffer = ShortArray(totalSamples)
    var samplesRead  = 0

    recorder.startRecording()
    while (samplesRead < totalSamples) {
        val chunkSize = minOf(minBufSize, totalSamples - samplesRead)
        val readCount = recorder.read(sampleBuffer, samplesRead, chunkSize)
        if (readCount <= 0) break
        samplesRead += readCount
    }
    recorder.stop()
    recorder.release()

    if (samplesRead == 0) return null

    val sumOfSquares = sampleBuffer.take(samplesRead).sumOf { s -> s.toLong() * s }
    val rmsValue     = sqrt(sumOfSquares.toDouble() / samplesRead)
    val dbSpl        = (20.0 * log10(rmsValue.coerceAtLeast(1.0) / 32768.0) + 90.0).toFloat()

    return dbSpl.coerceIn(0f, 120f)
}
