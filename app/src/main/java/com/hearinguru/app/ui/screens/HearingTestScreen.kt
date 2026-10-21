package com.hearinguru.app.ui.screens

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin


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

private data class SessionState(

    val step: TestStep = TestStep.INTRO,

    // Consent + demographics
    val ageGroup: String = "",
    val consentGiven: Boolean = false,

    // History / triage
    val hasSuddenLoss: Boolean = false,
    val hasEarPain: Boolean = false,
    val hasSignificantDizziness: Boolean = false,
    val hasTinnitus: Boolean = false,
    val hasNoiseExposure: Boolean = false,

    // Environment
    val headphoneConfirmed: Boolean = false,
    val quietRoomConfirmed: Boolean = false,

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

// Screening protocol: 1000, 2000, 4000 Hz each ear
private val TEST_TONES = listOf(
    Pair("LEFT",  1000), Pair("LEFT",  2000), Pair("LEFT",  4000),
    Pair("RIGHT", 1000), Pair("RIGHT", 2000), Pair("RIGHT", 4000)
)

private const val TONE_DURATION_MS      = 2000
private const val TONE_WINDOW_MS        = 4000   // auto-advance after this if no response
private const val MIN_VALID_RESPONSE_MS = 200L   // <200ms = likely false positive per tone

private val AGE_GROUPS = listOf("Under 18", "18–40", "41–60", "61–80", "Over 80")


// ─── Entry point ─────────────────────────────────────────────────────────────

@Composable
fun HearingTestScreen(
    onNavigateToDoctor: () -> Unit = {},
    onNavigateToHearingAids: () -> Unit = {}
) {
    var session by remember { mutableStateOf(SessionState()) }
    val scope   = rememberCoroutineScope()

    AnimatedContent(targetState = session.step, label = "step") { step ->
        when (step) {
            TestStep.INTRO -> IntroStep(
                onNext = { session = session.copy(step = TestStep.CONSENT) }
            )
            TestStep.CONSENT -> ConsentStep(
                session  = session,
                onChange = { session = it },
                onNext   = { session = session.copy(step = TestStep.HISTORY) },
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
private fun IntroStep(onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        StepHeader(step = 2, total = 5, title = "Hearing History")
        Spacer(Modifier.height(8.dp))
        Text(
            "These questions help identify any symptoms that need urgent medical attention before proceeding.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        // Urgent symptoms section
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Column(Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Text("Urgent symptoms — answer carefully", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        YesNoQuestion(
            question   = "Did your hearing loss start suddenly in the last 72 hours (sudden sensorineural hearing loss)?",
            value      = session.hasSuddenLoss,
            onChange   = { onChange(session.copy(hasSuddenLoss = it)) },
            isUrgent   = true
        )
        Spacer(Modifier.height(16.dp))

        Text("OTHER SYMPTOMS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(10.dp))
        YesNoQuestion("Do you have significant ear pain or discharge from either ear?",       session.hasEarPain,               { onChange(session.copy(hasEarPain               = it)) })
        Spacer(Modifier.height(10.dp))
        YesNoQuestion("Are you experiencing significant dizziness or balance problems?",      session.hasSignificantDizziness,  { onChange(session.copy(hasSignificantDizziness  = it)) })
        Spacer(Modifier.height(10.dp))
        YesNoQuestion("Do you hear ringing, buzzing or other sounds in your ears (tinnitus)?",session.hasTinnitus,              { onChange(session.copy(hasTinnitus              = it)) })
        Spacer(Modifier.height(10.dp))
        YesNoQuestion("Have you been exposed to loud noise at work or regularly in daily life?",session.hasNoiseExposure,        { onChange(session.copy(hasNoiseExposure         = it)) })

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text("Back")
            }
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
    val canProceed = session.headphoneConfirmed && session.quietRoomConfirmed

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        StepHeader(step = 3, total = 5, title = "Test Environment")
        Spacer(Modifier.height(8.dp))
        Text("Good testing conditions are essential for reliable results.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        CheckConfirmCard(
            icon    = Icons.Default.Headphones,
            title   = "Headphones connected and on",
            body    = "Use wired or Bluetooth headphones. Do not use phone speaker — it cannot deliver sounds to individual ears.",
            checked = session.headphoneConfirmed,
            onToggle = { onChange(session.copy(headphoneConfirmed = !session.headphoneConfirmed)) }
        )
        Spacer(Modifier.height(12.dp))
        CheckConfirmCard(
            icon    = Icons.Default.VolumeOff,
            title   = "I am in a quiet room",
            body    = "Background noise causes false results. Move away from TVs, traffic, conversations and air conditioning.",
            checked = session.quietRoomConfirmed,
            onToggle = { onChange(session.copy(quietRoomConfirmed = !session.quietRoomConfirmed)) }
        )
        Spacer(Modifier.height(12.dp))
        NoticeBox(Icons.Default.VolumeUp, "Set your device volume to about 70%. Very low volume gives false 'not heard' results; very high volume may cause discomfort.", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) { Text("Back") }
            Button(onClick = onNext, enabled = canProceed, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}


// ─── Step 5: Practice ─────────────────────────────────────────────────────────

@Composable
private fun PracticeStep(onReady: () -> Unit, onBack: () -> Unit) {
    var practiceState by remember { mutableStateOf<String>("idle") } // idle | playing | done_heard | done_missed
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(step = 4, total = 5, title = "Practice Round")
        Spacer(Modifier.height(12.dp))
        Text(
            "We will play a practice tone so you know what to listen for. The real test tones may be much softer — tap immediately when you hear anything.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        when (practiceState) {
            "idle" -> {
                OutlinedButton(
                    onClick = {
                        practiceState = "playing"
                        scope.launch {
                            withContext(Dispatchers.IO) { playTone(1000.0, 1200, "RIGHT", amplitude = 0.7) }
                            practiceState = "done_missed"
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play practice tone (right ear)", fontWeight = FontWeight.SemiBold)
                }
            }
            "playing" -> {
                FilledIconButton(
                    onClick = { practiceState = "done_heard" },
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                        Text("Tap!\nI heard it", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Sound is playing — tap above if you hear it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            "done_heard" -> {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("You heard it!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("That is exactly how to respond during the real test.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            "done_missed" -> {
                Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Did not tap in time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Try again. Tap the button as soon as you hear anything, even very faintly.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        practiceState = "playing"
                        scope.launch {
                            withContext(Dispatchers.IO) { playTone(1000.0, 1200, "RIGHT", amplitude = 0.7) }
                            if (practiceState == "playing") practiceState = "done_missed"
                        }
                    },
                    shape = RoundedCornerShape(50)
                ) { Text("Try practice again") }
            }
        }

        Spacer(Modifier.weight(1f))

        if (practiceState == "done_heard" || practiceState == "done_missed") {
            Button(onClick = onReady, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(50)) {
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

        Spacer(Modifier.height(28.dp))

        // Hear button
        FilledIconButton(
            onClick = {
                if (isPlaying && currentTone != null) {
                    val elapsed = System.currentTimeMillis() - toneStartMs
                    responses.add(ToneResponse(ear = currentTone.first, frequencyHz = currentTone.second, heard = true, responseTimeMs = elapsed))
                    isPlaying = false
                    toneIndex++
                }
            },
            modifier = Modifier.size(110.dp),
            shape = RoundedCornerShape(50),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Hearing, null, tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                Text("Yes, I\nheard it", color = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                if (isPlaying && currentTone != null) {
                    responses.add(ToneResponse(ear = currentTone.first, frequencyHz = currentTone.second, heard = false, responseTimeMs = 0))
                    isPlaying = false
                    toneIndex++
                }
            },
            enabled = isPlaying,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(50.dp)
        ) {
            Text("I could not hear this", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
    val outcome = session.outcome ?: TestOutcome.INCONCLUSIVE

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        StepHeader(step = 5, total = 5, title = "Your Result")
        Spacer(Modifier.height(12.dp))

        // Result hero card
        val (heroColor, heroBg, heroIcon, heroTitle, heroBody) = when (outcome) {
            TestOutcome.PASS         -> ResultDisplay(
                color  = MaterialTheme.colorScheme.primary,
                bg     = MaterialTheme.colorScheme.primaryContainer,
                icon   = Icons.Default.CheckCircle,
                title  = "PASS — No Screening Concern",
                body   = "No significant hearing difficulty was detected in this screening. Persistent concerns should still be assessed by an audiologist."
            )
            TestOutcome.REFER        -> ResultDisplay(
                color  = MaterialTheme.colorScheme.error,
                bg     = MaterialTheme.colorScheme.errorContainer,
                icon   = Icons.Default.HearingDisabled,
                title  = "REFER — Possible Hearing Difficulty",
                body   = "This screening detected a possible hearing difficulty. A comprehensive hearing assessment by a qualified audiologist is recommended."
            )
            TestOutcome.INCONCLUSIVE -> ResultDisplay(
                color  = MaterialTheme.colorScheme.tertiary,
                bg     = MaterialTheme.colorScheme.tertiaryContainer,
                icon   = Icons.Default.HelpOutline,
                title  = "INCONCLUSIVE",
                body   = "The screening could not produce a reliable result. This may be due to environmental noise, equipment or response inconsistencies."
            )
        }

        Surface(shape = RoundedCornerShape(24.dp), color = heroBg, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(heroIcon, null, tint = heroColor, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(10.dp))
                Text(heroTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = heroColor, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(heroBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ear-specific summary
        if (outcome != TestOutcome.INCONCLUSIVE) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("EAR-SPECIFIC SUMMARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(12.dp))
                    EarSummaryRow("Left ear",  session.leftMissed,  3)
                    Spacer(Modifier.height(8.dp))
                    EarSummaryRow("Right ear", session.rightMissed, 3)
                    Spacer(Modifier.height(10.dp))
                    DisclaimerBox("Smartphone audio is not audiologically calibrated. These results indicate a possible concern only.")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Next steps
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("RECOMMENDED NEXT STEP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(8.dp))
                val nextStep = when (outcome) {
                    TestOutcome.PASS         -> "Your screening was normal. If you still have concerns or symptoms, please see an audiologist — a PASS result does not rule out hearing difficulties."
                    TestOutcome.REFER        -> "Book a comprehensive hearing assessment with a certified audiologist. Bring this screening result to your appointment."
                    TestOutcome.INCONCLUSIVE -> "Ensure you are in a very quiet room with headphones on full volume and retry the test. If problems persist, consult an audiologist directly."
                }
                Text(nextStep, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, lineHeight = 20.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // CTAs
        if (outcome == TestOutcome.REFER || outcome == TestOutcome.PASS) {
            Button(onClick = onBookConsult, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) {
                Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Book professional assessment", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (outcome == TestOutcome.REFER) {
            OutlinedButton(onClick = onBrowseHAs, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(50)) {
                Icon(Icons.Default.HearingDisabled, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse hearing aids", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(50)) {
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
    val heard     = total - missed
    val isProblem = missed >= 2
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$heard/$total heard", style = MaterialTheme.typography.bodyMedium, color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(
                if (isProblem) Icons.Default.Warning else Icons.Default.CheckCircle,
                null,
                tint = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
