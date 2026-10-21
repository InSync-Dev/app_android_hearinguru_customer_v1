package com.hearinguru.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class ArticleSection(val heading: String? = null, val body: String)


data class ArticleFullContent(
    val title: String,
    val meta: String,
    val icon: ImageVector = Icons.Default.Hearing,
    val sections: List<ArticleSection>,
)


val articleContents = listOf(
    ArticleFullContent(
        title = "How to read your hearing test results — explained simply",
        meta = "8 min read · Hearing Loss",
        sections = listOf(
            ArticleSection(body = "A hearing test, or audiogram, is the most important tool for understanding how well you hear. But for most people, looking at the result sheet feels like reading a foreign language. Here's a plain-English guide."),
            ArticleSection(
                heading = "What is an audiogram?",
                body = "An audiogram is a graph that plots the softest sounds you can hear at different pitches (frequencies). The horizontal axis shows frequency in Hertz (Hz) — from low-pitched sounds on the left (250 Hz, like a bass drum) to high-pitched sounds on the right (8000 Hz, like a bird chirp).\n\nThe vertical axis shows loudness in decibels (dB HL). The numbers go from 0 at the top (very quiet) down to 120 at the bottom (extremely loud). The higher your mark sits on the graph, the better your hearing."
            ),
            ArticleSection(
                heading = "The hearing loss categories",
                body = "• Normal: 0–25 dB — you can hear whispers\n• Mild: 26–40 dB — you miss soft speech and consonants\n• Moderate: 41–55 dB — speech must be loud to understand\n• Moderately severe: 56–70 dB — you struggle even with raised voices\n• Severe: 71–90 dB — you rely heavily on lip-reading\n• Profound: 91+ dB — you hear very little without amplification"
            ),
            ArticleSection(
                heading = "Air conduction vs bone conduction",
                body = "Your audiogram shows two sets of marks — air conduction (AC, marked with circles or Xs) and bone conduction (BC, marked with brackets). AC tests the full hearing pathway. BC bypasses the outer and middle ear and tests only the inner ear.\n\nIf AC and BC marks sit at the same level, the problem is in the inner ear (sensorineural). If AC is worse than BC, the problem is in the outer or middle ear (conductive). Both patterns together mean mixed hearing loss."
            ),
            ArticleSection(
                heading = "What to do with your results",
                body = "Share the audiogram with an audiologist who can prescribe the right hearing aid. Mild-to-moderate loss often responds very well to modern digital aids. Don't wait — untreated hearing loss is linked to cognitive decline and social isolation."
            ),
        )
    ),
    ArticleFullContent(
        title = "Why hearing gets worse after 40 — and what to do",
        meta = "5 min read · Hearing Loss",
        sections = listOf(
            ArticleSection(body = "Most people assume hearing loss is something that happens to the elderly. But the inner ear starts showing measurable decline from your mid-30s — and by 40, many people have already lost some high-frequency hearing without knowing it."),
            ArticleSection(
                heading = "The biology behind it",
                body = "Deep inside your inner ear are roughly 15,000 tiny hair cells that convert sound vibrations into electrical signals for the brain. Unlike most cells in the body, these hair cells cannot regenerate. They are permanently destroyed by loud noise, ageing, and certain medications.\n\nAfter 40, cumulative damage begins to add up. High frequencies go first — the 's', 'f', and 'th' consonants that are critical for understanding speech. You may still hear vowels clearly but can't make out what's being said."
            ),
            ArticleSection(
                heading = "Signs it's happening to you",
                body = "• You ask people to repeat themselves frequently\n• You struggle in noisy restaurants even when they're not very loud\n• You turn the TV up louder than others prefer\n• Phone calls feel unclear even with good signal\n• You mishear similar-sounding words"
            ),
            ArticleSection(
                heading = "What you can do",
                body = "First: get a baseline audiogram. This gives you a reference point so future tests can measure progression.\n\nSecond: protect your ears now. Every loud concert, power tool session, or earphone session at high volume destroys more hair cells. Use earplugs.\n\nThird: if you already have measurable loss, consider hearing aids early. Research shows early intervention preserves brain plasticity and keeps the auditory cortex active."
            ),
            ArticleSection(
                heading = "The good news",
                body = "Modern hearing aids are nothing like the bulky beige devices of the past. Many RIC and CIC models are nearly invisible, Bluetooth-capable, and AI-powered. An audiologist can fit you with a pair matched precisely to your audiogram — not a generic amplifier."
            ),
        )
    ),
    ArticleFullContent(
        title = "How to protect your hearing from everyday noise",
        meta = "4 min read · Prevention",
        sections = listOf(
            ArticleSection(body = "Noise-induced hearing loss is the most preventable form of hearing loss — and one of the most common. The damage is permanent. Here's how to avoid it."),
            ArticleSection(
                heading = "The 85 dB rule",
                body = "Damage begins at 85 decibels. The louder the sound, the faster the damage. At 85 dB (busy traffic, a lawn mower), you have about 8 hours before risk begins. At 100 dB (live music, nightclub), damage can occur in 15 minutes. At 120 dB (jet engine close up), pain and instant damage happen immediately."
            ),
            ArticleSection(
                heading = "Everyday sounds to watch",
                body = "• Earphones at 80%+ volume: 90–100 dB. Use the 60/60 rule — no more than 60% volume for 60 minutes at a stretch.\n• Motorcycles and auto-rickshaws: 95–100 dB. Wear helmet ear inserts.\n• Power tools (drills, grinders): 100–110 dB. Always use ear protection.\n• Firecrackers: 150+ dB. Keep at maximum possible distance.\n• Concerts and clubs: 105–115 dB. Use musician's earplugs."
            ),
            ArticleSection(
                heading = "Protecting yourself without missing out",
                body = "Musician's earplugs (also called hi-fi earplugs) reduce volume by 15–25 dB while preserving clarity. They're small, nearly invisible, and inexpensive. You can enjoy music and conversation at a concert without the dangerous peak volumes.\n\nFor workplaces, standard foam earplugs offer 25–33 dB of protection and cost almost nothing. Use them whenever operating machinery."
            ),
            ArticleSection(
                heading = "After a loud event",
                body = "Temporary threshold shift — the muffled, ringing sensation after a concert — means your hair cells are stressed. Give your ears 16–18 hours of quiet to recover. If ringing (tinnitus) persists for more than a day, see an audiologist."
            ),
        )
    ),
    ArticleFullContent(
        title = "How to help a family member who won't admit hearing loss",
        meta = "7 min read · For Families",
        sections = listOf(
            ArticleSection(body = "\"I can hear fine — you just mumble.\" If this sounds familiar, you are not alone. Denial is one of the most common responses to hearing loss, particularly among older adults. Here's how to help without creating conflict."),
            ArticleSection(
                heading = "Why people deny hearing loss",
                body = "Hearing loss carries a stigma that vision loss doesn't. Glasses are fashionable; hearing aids still feel like a badge of old age. Many people also don't notice gradual decline — the brain compensates for years by filling in the gaps, lip-reading unconsciously, and asking for repetition without connecting the dots.\n\nFor others, admitting hearing loss means confronting ageing, dependence, and cost."
            ),
            ArticleSection(
                heading = "What not to do",
                body = "• Don't shout or over-enunciate — it feels infantilising\n• Don't have the conversation in front of others — humiliation creates resistance\n• Don't make it about inconvenience to you — it triggers defensiveness\n• Don't give ultimatums"
            ),
            ArticleSection(
                heading = "What works",
                body = "Have the conversation one-on-one in a quiet space. Frame it around safety — missed doorbells, emergency announcements, crossing the road. Ask them to do it for you: \"I worry about you. Would you get a hearing test just so we know for sure?\"\n\nMake the first step easy. A free hearing test at home (like the one in this app) removes the barrier of scheduling an audiologist visit. Once someone sees their own results, denial becomes harder to maintain."
            ),
            ArticleSection(
                heading = "When they see their results",
                body = "Accompany them to the audiologist appointment. Many people shut down when alone but engage differently with a family member present. Take notes. Ask questions. The audiologist has seen this many times before and knows how to work with reluctant patients."
            ),
            ArticleSection(
                heading = "Managing the adjustment period",
                body = "New hearing aid users need time. The brain has to relearn sounds it hasn't processed properly in years. Everyday sounds (refrigerator hum, footsteps) feel suddenly loud. This is normal and passes within 2–6 weeks. Be patient and encouraging during this period."
            ),
        )
    ),
    ArticleFullContent(
        title = "Signs you may have hearing loss — a simple checklist",
        meta = "3 min read · Hearing Loss",
        sections = listOf(
            ArticleSection(body = "Hearing loss creeps up slowly. Most people wait 7–10 years from first signs to seeking help. Use this checklist to evaluate yourself or someone you care about."),
            ArticleSection(
                heading = "Communication signs",
                body = "☐  You frequently ask people to repeat themselves\n☐  You hear speech but struggle to understand it, especially in background noise\n☐  Phone conversations feel unclear even with good signal\n☐  You misunderstand words that sound similar ('fifteen' vs 'fifty')\n☐  You find yourself watching people's lips to understand them\n☐  You avoid conversations in groups or restaurants"
            ),
            ArticleSection(
                heading = "Environment signs",
                body = "☐  The TV volume feels right to you but others find it too loud\n☐  You can't hear the doorbell or phone ring from another room\n☐  You can't tell which direction a sound is coming from\n☐  Birds, crickets, or high-pitched sounds seem to have faded\n☐  You have difficulty following conversations in a car"
            ),
            ArticleSection(
                heading = "Physical signs",
                body = "☐  You have ringing or buzzing in your ears (tinnitus)\n☐  You have a history of working in noisy environments\n☐  You've been exposed to a sudden very loud noise\n☐  You take medications known to affect hearing (high-dose aspirin, certain antibiotics)"
            ),
            ArticleSection(
                heading = "What to do",
                body = "If you checked 3 or more boxes in any section, take the 5-minute hearing test in the Tools tab. If you checked 5 or more across all sections, see an audiologist promptly. Hearing loss is treatable — but the sooner you act, the better the outcome."
            ),
        )
    ),
    ArticleFullContent(
        title = "Common questions about hearing loss — answered",
        meta = "5 min read · FAQ",
        sections = listOf(
            ArticleSection(body = "Hearing loss affects over 1.5 billion people worldwide. Yet there's enormous confusion about what it is, what causes it, and what can be done. Here are honest answers to the questions people search for most."),
            ArticleSection(
                heading = "Can hearing loss be reversed?",
                body = "In most cases, no — but it depends on the type. Sensorineural hearing loss (damage to inner ear hair cells) is permanent. However, conductive hearing loss (from blockage, infection, or middle ear issues) is often treatable with medicine or surgery. An audiologist can tell you which type you have."
            ),
            ArticleSection(
                heading = "Are hearing aids the only option?",
                body = "For sensorineural loss: hearing aids are the primary treatment. For severe to profound loss, cochlear implants are highly effective. For conductive loss: medical treatment often restores hearing without aids. Assistive listening devices (amplified phones, TV streamers) can supplement aids in specific situations."
            ),
            ArticleSection(
                heading = "Will wearing a hearing aid make my hearing worse?",
                body = "No. This is one of the most persistent myths. Properly fitted hearing aids amplify only the frequencies you've lost, at the level needed. They don't damage remaining hearing. What does make hearing worse is leaving it untreated — the auditory cortex weakens from lack of stimulation."
            ),
            ArticleSection(
                heading = "At what age does hearing loss start?",
                body = "High-frequency loss can begin from age 25 onwards from noise exposure. Age-related loss (presbycusis) typically becomes noticeable after 50 but begins earlier. 1 in 3 people over 65 has significant hearing loss."
            ),
            ArticleSection(
                heading = "Is hearing loss hereditary?",
                body = "About 50% of congenital (from birth) hearing loss has a genetic cause. Age-related hearing loss also has genetic components. However, acquired hearing loss from noise, illness, or medication is not inherited."
            ),
        )
    ),
    ArticleFullContent(
        title = "Tinnitus: causes, symptoms and relief options",
        meta = "6 min read · Hearing Loss",
        sections = listOf(
            ArticleSection(body = "Tinnitus — the perception of sound without an external source — affects around 15% of adults. For most, it's a mild annoyance. For about 2% of people, it's severely debilitating. Here's what you need to know."),
            ArticleSection(
                heading = "What causes tinnitus?",
                body = "Tinnitus is a symptom, not a disease. It arises when the auditory system produces phantom signals, usually due to:\n\n• Noise-induced hearing loss (most common cause)\n• Age-related hearing loss\n• Ear infections or blockage with earwax\n• Certain medications (high-dose aspirin, some antibiotics, diuretics)\n• Head or neck injuries\n• High blood pressure or cardiovascular disease\n• Anxiety and stress (worsens existing tinnitus)"
            ),
            ArticleSection(
                heading = "What it sounds like",
                body = "Tinnitus manifests as ringing, buzzing, hissing, whistling, clicking, or roaring. It can be in one ear, both ears, or appear to come from inside the head. It can be constant or intermittent. It's often worse in quiet environments — this is why it feels most severe at bedtime."
            ),
            ArticleSection(
                heading = "Relief options",
                body = "There is currently no cure for most chronic tinnitus, but there are effective management strategies:\n\n• Sound therapy: Background sounds (white noise, pink noise, nature sounds) mask the tinnitus signal. The Tinnitus Relief tool in this app provides four sound options.\n\n• Hearing aids: Since tinnitus often accompanies hearing loss, treating the hearing loss reduces tinnitus severity in many patients.\n\n• Cognitive Behavioural Therapy (CBT): The most evidence-backed psychological approach. It doesn't eliminate tinnitus but significantly reduces its emotional impact.\n\n• Lifestyle: Reduce caffeine, alcohol, and salt. Exercise regularly. Manage stress."
            ),
            ArticleSection(
                heading = "When to see a doctor",
                body = "See a doctor promptly if tinnitus started after a head injury, is only in one ear, is pulsating in rhythm with your heartbeat (this needs cardiovascular evaluation), or is accompanied by sudden hearing loss."
            ),
        )
    ),
    ArticleFullContent(
        title = "When should you see a doctor about your hearing?",
        meta = "4 min read · FAQ",
        sections = listOf(
            ArticleSection(body = "Many people with hearing loss wait years before seeing a professional. Sometimes symptoms are mild. Often, they're not sure whether the problem warrants a visit. This guide will help you decide."),
            ArticleSection(
                heading = "See a doctor immediately if:",
                body = "• You experience sudden hearing loss — waking unable to hear well in one ear is a medical emergency. Get to an ENT within 24–48 hours; early steroid treatment dramatically improves outcomes.\n• Hearing loss follows a head injury or pressure change (e.g. diving)\n• You have pain, discharge, or bleeding from the ear\n• Tinnitus is only in one ear\n• Dizziness or balance problems accompany hearing changes"
            ),
            ArticleSection(
                heading = "See a doctor soon (within a few weeks) if:",
                body = "• You've noticed progressive hearing difficulty over months\n• Your hearing test shows moderate loss or worse\n• You are having social or professional difficulties due to hearing trouble\n• You have children — undetected hearing loss in children significantly impacts speech and language development"
            ),
            ArticleSection(
                heading = "GP vs audiologist vs ENT",
                body = "A GP can rule out simple causes (earwax, infection) and refer you onward. An audiologist performs detailed hearing tests and fits hearing aids — they are your main point of contact for most hearing loss. An ENT handles medical and surgical conditions. For most people, an audiologist is the right first call after ruling out simple causes with a GP."
            ),
            ArticleSection(
                heading = "What to expect at an appointment",
                body = "The audiologist will take a history, examine your ears, and run a standard hearing test in a soundproof booth. The whole appointment takes about 45–60 minutes. You'll leave with an audiogram and, if appropriate, a recommendation for hearing aids or further investigation. If you have results from this app, bring them — they give the audiologist a useful starting point."
            ),
        )
    ),
)


@Composable
fun ArticleDetailScreen(articleIndex: Int) {
    val article = articleContents.getOrNull(articleIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {

        // Hero
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        article.meta,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 30.sp
                )
            }
        }

        // Sections
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            article.sections.forEachIndexed { index, section ->
                if (index > 0) Spacer(Modifier.height(20.dp))

                if (section.heading != null) {
                    Text(
                        section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    section.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
