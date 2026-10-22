package com.hearinguru.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


// ─── Building blocks ─────────────────────────────────────────────────────────

data class ArticleSection(val heading: String? = null, val body: String)

enum class ArticleDifficulty { BEGINNER, INTERMEDIATE }


// ─── Core article entity ──────────────────────────────────────────────────────

data class KnowledgeArticle(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val readingTimeMinutes: Int,
    val audienceTags: List<String>,       // beginner | concerned | buyer | existing-user | caregiver
    val intentTags: List<String>,         // awareness | symptoms | testing | treatment | buying | maintenance
    val difficulty: ArticleDifficulty,
    val prerequisiteIds: List<String>,
    val relatedArticleIds: List<String>,
    val sections: List<ArticleSection>,
) {
    val meta: String get() = "$readingTimeMinutes min · $category"
}


// ─── Learning path entity ─────────────────────────────────────────────────────

data class LearningPath(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val colorIndex: Int,            // 0–5, mapped to Material colour containers in composables
    val articleIds: List<String>,
)


// ─── Intent entry-point entity ────────────────────────────────────────────────

data class LearningIntent(
    val label: String,
    val icon: ImageVector,
    val targetPathId: String,
)


// ─── Lookup helpers ───────────────────────────────────────────────────────────

fun indexOfArticle(id: String): Int = knowledgeArticles.indexOfFirst { it.id == id }

fun indexOfPath(id: String): Int = learningPaths.indexOfFirst { it.id == id }


// ─── Article library ──────────────────────────────────────────────────────────

val knowledgeArticles: List<KnowledgeArticle> = listOf(


    // ── Stage 1: Discover Hearing ─────────────────────────────────────────────

    KnowledgeArticle(
        id                 = "how-hearing-works",
        title              = "How hearing works — a plain-English guide",
        summary            = "From sound wave to brain signal: understand the mechanics of hearing in five minutes.",
        category           = "Hearing Basics",
        readingTimeMinutes = 5,
        audienceTags       = listOf("beginner"),
        intentTags         = listOf("awareness"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("signs-checklist", "causes-of-hearing-loss"),
        sections           = listOf(
            ArticleSection(
                body = "Sound is invisible movement — pressure waves rippling through air. Your ears exist to catch those waves and convert them into electrical signals the brain can interpret. Here is how that remarkable process works."
            ),
            ArticleSection(
                heading = "The outer ear: collecting sound",
                body    = "The part of the ear you can see — the pinna — is shaped specifically to funnel sound waves into the ear canal. Its asymmetric curves help you locate a sound vertically. Sound travels down the ear canal, roughly 2.5 cm, to the eardrum — a thin membrane that vibrates in response."
            ),
            ArticleSection(
                heading = "The middle ear: amplification",
                body    = "On the other side of the eardrum sit three tiny bones: the malleus, incus, and stapes — the hammer, anvil, and stirrup. They form the smallest bones in the human body. Their job is to amplify vibration and transmit it inward. Together they increase the force of sound by around 22 times."
            ),
            ArticleSection(
                heading = "The inner ear: conversion to signal",
                body    = "The stapes presses against the oval window of the cochlea — a fluid-filled, spiral-shaped structure. Vibrations ripple through the cochlear fluid and move thousands of microscopic hair cells, each tuned to a specific frequency. When a hair cell moves, it fires an electrical signal along the auditory nerve to the brain.\n\nThis is where hearing loss most commonly originates. Hair cells cannot regenerate. Once destroyed — by noise, age, or disease — they are gone permanently."
            ),
            ArticleSection(
                heading = "The brain: making meaning",
                body    = "The auditory cortex processes incoming signals and assembles them into recognisable speech, music, and environmental sound. When hearing loss reduces input over years, the auditory cortex weakens from lack of stimulation. This is one reason early intervention matters — the brain's plasticity is preserved best when treatment starts early."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "signs-checklist",
        title              = "Signs you may have hearing loss — a simple checklist",
        summary            = "A practical self-assessment covering communication, environment, and physical signs.",
        category           = "Hearing Loss",
        readingTimeMinutes = 3,
        audienceTags       = listOf("beginner", "concerned"),
        intentTags         = listOf("awareness", "symptoms"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("how-hearing-tests-work", "when-see-doctor"),
        sections           = listOf(
            ArticleSection(
                body = "Hearing loss creeps up slowly. Most people wait 7–10 years from first signs to seeking help. Use this checklist to evaluate yourself or someone you care about."
            ),
            ArticleSection(
                heading = "Communication signs",
                body    = "☐  You frequently ask people to repeat themselves\n☐  You hear speech but struggle to understand it, especially in background noise\n☐  Phone conversations feel unclear even with good signal\n☐  You misunderstand similar-sounding words ('fifteen' vs 'fifty')\n☐  You find yourself watching people's lips\n☐  You avoid group conversations or restaurants"
            ),
            ArticleSection(
                heading = "Environment signs",
                body    = "☐  The TV volume feels right to you but others find it too loud\n☐  You can't hear the doorbell or phone from another room\n☐  You can't tell which direction a sound is coming from\n☐  Birds and high-pitched sounds seem to have faded\n☐  You struggle to follow conversations in a car"
            ),
            ArticleSection(
                heading = "Physical signs",
                body    = "☐  You have ringing or buzzing in your ears (tinnitus)\n☐  You have a history of working in noisy environments\n☐  You've been exposed to a sudden very loud noise\n☐  You take medications known to affect hearing (high-dose aspirin, certain antibiotics)"
            ),
            ArticleSection(
                heading = "What to do",
                body    = "If you checked 3 or more boxes in any section, take the 5-minute hearing test in the Tools tab. If you checked 5 or more across all sections, see an audiologist promptly. Hearing loss is treatable — but the sooner you act, the better the outcome."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "causes-of-hearing-loss",
        title              = "What causes hearing loss?",
        summary            = "Age, noise, genetics, illness — the five main causes and what each means for treatment.",
        category           = "Hearing Loss",
        readingTimeMinutes = 4,
        audienceTags       = listOf("beginner", "concerned"),
        intentTags         = listOf("awareness", "symptoms"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("how-hearing-works"),
        relatedArticleIds  = listOf("protect-from-noise", "signs-checklist"),
        sections           = listOf(
            ArticleSection(
                body = "Hearing loss is not a single condition — it's an outcome with several distinct causes. Knowing the cause matters because it determines whether the loss is preventable, reversible, or manageable."
            ),
            ArticleSection(
                heading = "1. Age-related hearing loss (presbycusis)",
                body    = "The most common cause worldwide. As we age, cochlear hair cells gradually die and the auditory nerve loses efficiency. High-frequency sounds go first — consonants like 's', 'f', and 'th' become difficult to distinguish. Presbycusis typically becomes noticeable in the 50s but begins decades earlier. It is not reversible, but hearing aids are highly effective."
            ),
            ArticleSection(
                heading = "2. Noise-induced hearing loss",
                body    = "Prolonged or intense sound permanently destroys hair cells. A single exposure above 140 dB (gunshot, industrial explosion) can cause instant damage. Repeated exposure above 85 dB (busy traffic, live music) causes cumulative damage over years. This is the most preventable form of hearing loss — ear protection works."
            ),
            ArticleSection(
                heading = "3. Genetic causes",
                body    = "About 50% of congenital (from-birth) hearing loss has a genetic cause. Genes can also predispose individuals to earlier age-related loss. If several family members have early-onset hearing loss, genetic counselling is worth considering."
            ),
            ArticleSection(
                heading = "4. Medical and medication causes",
                body    = "Several conditions damage hearing: ear infections (particularly untreated childhood otitis media), Menière's disease, acoustic neuroma, and autoimmune conditions.\n\nCertain medications are ototoxic — they damage the inner ear. High-dose aspirin, some chemotherapy agents, loop diuretics, and aminoglycoside antibiotics are the most common. Always flag hearing changes to your doctor if you're on long-term medication."
            ),
            ArticleSection(
                heading = "5. Sudden sensorineural hearing loss",
                body    = "Sudden onset — often in one ear, sometimes overnight — is a medical emergency. If treated within 72 hours with corticosteroids, significant recovery is possible. After two weeks, the treatment window narrows sharply. See an ENT immediately if this happens to you."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "why-hearing-worsens",
        title              = "Why hearing gets worse after 40 — and what to do",
        summary            = "Understand the biology behind age-related hearing decline and the steps you can take at any stage.",
        category           = "Hearing Loss",
        readingTimeMinutes = 5,
        audienceTags       = listOf("beginner", "concerned"),
        intentTags         = listOf("awareness"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("how-hearing-works"),
        relatedArticleIds  = listOf("read-audiogram", "signs-checklist"),
        sections           = listOf(
            ArticleSection(
                body = "Most people assume hearing loss is something that happens to the elderly. But the inner ear starts showing measurable decline from your mid-30s — and by 40, many people have already lost some high-frequency hearing without knowing it."
            ),
            ArticleSection(
                heading = "The biology behind it",
                body    = "Deep inside your inner ear are roughly 15,000 tiny hair cells that convert sound vibrations into electrical signals for the brain. Unlike most cells in the body, these hair cells cannot regenerate. They are permanently destroyed by loud noise, ageing, and certain medications.\n\nAfter 40, cumulative damage begins to add up. High frequencies go first — the 's', 'f', and 'th' consonants critical for understanding speech. You may still hear vowels clearly but can't make out what's being said."
            ),
            ArticleSection(
                heading = "Signs it's happening to you",
                body    = "• You ask people to repeat themselves frequently\n• You struggle in noisy restaurants even when they're not very loud\n• You turn the TV up louder than others prefer\n• Phone calls feel unclear even with good signal\n• You mishear similar-sounding words"
            ),
            ArticleSection(
                heading = "What you can do",
                body    = "First: get a baseline audiogram. This gives you a reference point so future tests can measure progression.\n\nSecond: protect your ears now. Every loud concert, power tool session, or earphone at high volume destroys more hair cells.\n\nThird: if you already have measurable loss, consider hearing aids early. Research shows early intervention preserves brain plasticity and keeps the auditory cortex active."
            ),
            ArticleSection(
                heading = "The good news",
                body    = "Modern hearing aids are nothing like the bulky beige devices of the past. Many RIC and CIC models are nearly invisible, Bluetooth-capable, and AI-powered. An audiologist fits them precisely to your audiogram — not as generic amplifiers."
            ),
        )
    ),


    // ── Stage 2: Understand Your Hearing ──────────────────────────────────────

    KnowledgeArticle(
        id                 = "how-hearing-tests-work",
        title              = "How hearing tests work",
        summary            = "What happens during a pure-tone audiometry screening and what the audiologist is actually measuring.",
        category           = "Testing",
        readingTimeMinutes = 4,
        audienceTags       = listOf("concerned"),
        intentTags         = listOf("testing"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("signs-checklist"),
        relatedArticleIds  = listOf("read-audiogram", "what-to-expect-assessment"),
        sections           = listOf(
            ArticleSection(
                body = "A hearing test sounds simple — you press a button when you hear a beep. But there's careful science behind what the audiologist is measuring and why it matters."
            ),
            ArticleSection(
                heading = "Screening vs diagnostic testing",
                body    = "A screening test (like the one in this app) gives a quick pass/refer result. It tests a limited set of frequencies at a fixed level to flag likely hearing loss. A diagnostic test, performed by an audiologist in a soundproof booth, maps the full threshold at every frequency for each ear — giving your complete audiogram."
            ),
            ArticleSection(
                heading = "Pure-tone audiometry",
                body    = "The standard test presents tones at specific frequencies: typically 250, 500, 1000, 2000, 4000, and 8000 Hz. Each tone is presented at different volumes until the softest audible level — your threshold — is found. The audiologist uses an ascending method: starting quietly, increasing until you hear it, then confirming at a slightly lower level."
            ),
            ArticleSection(
                heading = "What the frequencies mean",
                body    = "Low frequencies (250–500 Hz) correspond to vowel sounds and environmental noise. Mid frequencies (1000–2000 Hz) are critical for understanding speech. High frequencies (4000–8000 Hz) carry consonant detail — the sounds that distinguish similar words.\n\nNoise-induced loss typically shows a characteristic dip at 4000 Hz. Age-related loss shows a gradual slope downward from 2000 Hz upwards."
            ),
            ArticleSection(
                heading = "What a screening can and can't tell you",
                body    = "A screening reliably tells you whether further testing is needed. It cannot determine the type of hearing loss (sensorineural vs conductive), precisely how much loss you have in each ear, or whether hearing aids are appropriate. That requires a full diagnostic assessment."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "read-audiogram",
        title              = "How to read your hearing test results — explained simply",
        summary            = "A plain-English guide to audiograms: what the axes mean, the categories of loss, and what to do with your results.",
        category           = "Testing",
        readingTimeMinutes = 8,
        audienceTags       = listOf("concerned"),
        intentTags         = listOf("testing"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("how-hearing-tests-work"),
        relatedArticleIds  = listOf("why-hearing-worsens", "when-see-doctor"),
        sections           = listOf(
            ArticleSection(
                body = "A hearing test, or audiogram, is the most important tool for understanding how well you hear. But for most people, looking at the result sheet feels like reading a foreign language. Here's a plain-English guide."
            ),
            ArticleSection(
                heading = "What is an audiogram?",
                body    = "An audiogram is a graph that plots the softest sounds you can hear at different pitches (frequencies). The horizontal axis shows frequency in Hertz (Hz) — from low-pitched sounds on the left (250 Hz, like a bass drum) to high-pitched sounds on the right (8000 Hz, like a bird chirp).\n\nThe vertical axis shows loudness in decibels (dB HL). Numbers go from 0 at the top (very quiet) to 120 at the bottom (extremely loud). The higher your mark on the graph, the better your hearing."
            ),
            ArticleSection(
                heading = "The hearing loss categories",
                body    = "• Normal: 0–25 dB — you can hear whispers\n• Mild: 26–40 dB — you miss soft speech and consonants\n• Moderate: 41–55 dB — speech must be loud to understand\n• Moderately severe: 56–70 dB — you struggle even with raised voices\n• Severe: 71–90 dB — you rely heavily on lip-reading\n• Profound: 91+ dB — you hear very little without amplification"
            ),
            ArticleSection(
                heading = "Air conduction vs bone conduction",
                body    = "Your audiogram shows two sets of marks — air conduction (AC, marked with circles or Xs) and bone conduction (BC, marked with brackets). AC tests the full hearing pathway. BC bypasses the outer and middle ear and tests only the inner ear.\n\nIf AC and BC marks sit at the same level, the problem is sensorineural (inner ear). If AC is worse than BC, the problem is conductive (outer or middle ear). Both patterns together mean mixed hearing loss."
            ),
            ArticleSection(
                heading = "What to do with your results",
                body    = "Share the audiogram with an audiologist who can prescribe the right hearing aid. Mild-to-moderate loss often responds very well to modern digital aids. Don't wait — untreated hearing loss is linked to cognitive decline and social isolation."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "what-to-expect-assessment",
        title              = "What to expect at a hearing assessment",
        summary            = "A step-by-step walk through the full audiological appointment — from case history to leaving with your results.",
        category           = "Testing",
        readingTimeMinutes = 5,
        audienceTags       = listOf("concerned"),
        intentTags         = listOf("testing"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("how-hearing-tests-work"),
        relatedArticleIds  = listOf("read-audiogram", "when-see-doctor"),
        sections           = listOf(
            ArticleSection(
                body = "Many people put off seeing an audiologist because they're not sure what to expect. A full hearing assessment is straightforward, takes about 45–60 minutes, and is completely painless."
            ),
            ArticleSection(
                heading = "Case history",
                body    = "The appointment starts with the audiologist asking about your hearing history: when you first noticed difficulty, whether it affects one or both ears, whether onset was gradual or sudden, any tinnitus, occupational noise exposure, and medications. Bring a list of regular medications. If possible, bring someone who has observed your hearing difficulty — their description often adds useful detail."
            ),
            ArticleSection(
                heading = "Physical examination",
                body    = "Using a handheld otoscope, the audiologist examines your ear canals and eardrums. This rules out simple causes: earwax blockage, perforation, or infection. If significant wax is present, they may recommend removal before testing — wax can create a conductive hearing loss that masks the true result."
            ),
            ArticleSection(
                heading = "Tympanometry",
                body    = "A small probe creates a gentle pressure change in the ear canal and measures how well your eardrum moves. This assesses middle-ear function and can detect fluid behind the eardrum, perforations, or stiffness in the ossicular chain — all causes of conductive hearing loss."
            ),
            ArticleSection(
                heading = "Pure-tone audiometry",
                body    = "In a soundproof booth, you listen through headphones and press a button each time you hear a tone, however faint. The audiologist tests multiple frequencies for each ear and may also conduct bone conduction testing. This takes about 20–30 minutes."
            ),
            ArticleSection(
                heading = "Results and next steps",
                body    = "At the end of the appointment the audiologist explains your audiogram and what it means for daily life. If hearing aids are appropriate, they may take ear impressions or schedule a fitting. You leave with a copy of your audiogram — bring results from the HearinGuru screening test if you've taken it, as they give the audiologist a useful baseline."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "when-see-doctor",
        title              = "When should you see a doctor about your hearing?",
        summary            = "A guide to knowing when to act — urgent red flags versus gradual changes that warrant professional assessment.",
        category           = "FAQ",
        readingTimeMinutes = 4,
        audienceTags       = listOf("concerned"),
        intentTags         = listOf("symptoms", "testing"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("read-audiogram", "common-qa"),
        sections           = listOf(
            ArticleSection(
                body = "Many people with hearing loss wait years before seeing a professional. Sometimes symptoms are mild. Often, they're not sure whether the problem warrants a visit. This guide will help you decide."
            ),
            ArticleSection(
                heading = "See a doctor immediately if:",
                body    = "• You experience sudden hearing loss — waking unable to hear in one ear is a medical emergency. Get to an ENT within 24–48 hours; early steroid treatment dramatically improves outcomes.\n• Hearing loss follows a head injury or pressure change (e.g. diving)\n• You have pain, discharge, or bleeding from the ear\n• Tinnitus is only in one ear\n• Dizziness or balance problems accompany hearing changes"
            ),
            ArticleSection(
                heading = "See a doctor soon (within a few weeks) if:",
                body    = "• You've noticed progressive hearing difficulty over months\n• Your hearing test shows moderate loss or worse\n• You are having social or professional difficulties due to hearing trouble\n• You have children — undetected hearing loss significantly impacts speech and language development"
            ),
            ArticleSection(
                heading = "GP vs audiologist vs ENT",
                body    = "A GP can rule out simple causes (earwax, infection) and refer you onward. An audiologist performs detailed hearing tests and fits hearing aids — your main point of contact for most hearing loss. An ENT handles medical and surgical conditions. For most people, an audiologist is the right first call after ruling out simple causes with a GP."
            ),
            ArticleSection(
                heading = "What to expect",
                body    = "The audiologist will take a history, examine your ears, and run a hearing test in a soundproof booth — about 45–60 minutes. You'll leave with an audiogram and, if appropriate, a recommendation for hearing aids or further investigation."
            ),
        )
    ),


    // ── Stage 3: Explore Solutions ────────────────────────────────────────────

    KnowledgeArticle(
        id                 = "options-for-hearing-loss",
        title              = "Options for managing hearing loss",
        summary            = "Not everyone needs hearing aids. Understand the full spectrum from communication strategies to cochlear implants.",
        category           = "Treatment",
        readingTimeMinutes = 6,
        audienceTags       = listOf("concerned", "buyer"),
        intentTags         = listOf("treatment"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("read-audiogram"),
        relatedArticleIds  = listOf("types-of-hearing-aids", "communication-strategies"),
        sections           = listOf(
            ArticleSection(
                body = "A hearing loss diagnosis doesn't automatically mean hearing aids. The right approach depends on the type and degree of loss, your lifestyle, and your goals. Here's an honest overview of what's available."
            ),
            ArticleSection(
                heading = "Communication strategies",
                body    = "For mild loss or while waiting to adjust, communication strategies alone can significantly reduce daily difficulty. Positioning, lighting, background noise reduction, and being direct about your needs all help. They work best when loss is mild and situational, and should supplement — not replace — professional care."
            ),
            ArticleSection(
                heading = "Hearing aids",
                body    = "The primary treatment for sensorineural hearing loss. Modern digital aids process sound in real time, suppress background noise, and link to smartphones and televisions. They are prescribed and fitted to your specific audiogram — not generic amplifiers.\n\nHearing aids don't restore normal hearing, but they significantly improve speech understanding. For mild-to-moderate loss, most users notice a major quality-of-life improvement within weeks."
            ),
            ArticleSection(
                heading = "Assistive listening devices",
                body    = "TV streamers, remote microphones, amplified telephones, and loop systems can supplement hearing aids or provide help in specific situations. They're especially useful for mild loss or as supplements when aids alone are insufficient."
            ),
            ArticleSection(
                heading = "Medical and surgical treatment",
                body    = "Conductive hearing loss — caused by outer or middle ear problems — is often treatable medically or surgically. Ear infections, perforations, and fluid behind the eardrum may resolve with medication or minor procedures. Otosclerosis is treated with surgery. An ENT specialist determines whether medical intervention is appropriate."
            ),
            ArticleSection(
                heading = "Cochlear implants",
                body    = "For severe to profound sensorineural loss where hearing aids provide insufficient benefit. An implant bypasses damaged hair cells and directly stimulates the auditory nerve with electrical signals. The procedure involves surgery and an intensive rehabilitation programme. Outcomes are best when implanted early, particularly for children with congenital loss."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "common-qa",
        title              = "Common questions about hearing loss — answered",
        summary            = "Honest answers to the questions people search for most: reversal, hearing aids, age, and heredity.",
        category           = "FAQ",
        readingTimeMinutes = 5,
        audienceTags       = listOf("beginner", "concerned"),
        intentTags         = listOf("awareness", "treatment"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("read-audiogram", "when-see-doctor"),
        sections           = listOf(
            ArticleSection(
                body = "Hearing loss affects over 1.5 billion people worldwide. Yet there's enormous confusion about what it is, what causes it, and what can be done. Here are honest answers to the most common questions."
            ),
            ArticleSection(
                heading = "Can hearing loss be reversed?",
                body    = "In most cases, no — but it depends on the type. Sensorineural hearing loss (damage to inner ear hair cells) is permanent. However, conductive hearing loss (from blockage, infection, or middle ear issues) is often treatable with medicine or surgery. An audiologist can tell you which type you have."
            ),
            ArticleSection(
                heading = "Are hearing aids the only option?",
                body    = "For sensorineural loss: hearing aids are the primary treatment. For severe to profound loss, cochlear implants are highly effective. For conductive loss: medical treatment often restores hearing without aids. Assistive listening devices can supplement aids in specific situations."
            ),
            ArticleSection(
                heading = "Will wearing a hearing aid make my hearing worse?",
                body    = "No. This is one of the most persistent myths. Properly fitted hearing aids amplify only the frequencies you've lost, at the level needed. They don't damage remaining hearing. What makes hearing worse is leaving it untreated — the auditory cortex weakens from lack of stimulation."
            ),
            ArticleSection(
                heading = "At what age does hearing loss start?",
                body    = "High-frequency loss can begin from age 25 onwards from noise exposure. Age-related loss (presbycusis) typically becomes noticeable after 50 but begins earlier. 1 in 3 people over 65 has significant hearing loss."
            ),
            ArticleSection(
                heading = "Is hearing loss hereditary?",
                body    = "About 50% of congenital (from birth) hearing loss has a genetic cause. Age-related hearing loss also has genetic components. However, acquired hearing loss from noise, illness, or medication is not inherited."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "communication-strategies",
        title              = "Communication strategies for hearing loss",
        summary            = "Practical techniques to communicate more easily in daily life — with or without hearing aids.",
        category           = "Treatment",
        readingTimeMinutes = 5,
        audienceTags       = listOf("concerned", "existing-user", "caregiver"),
        intentTags         = listOf("treatment", "maintenance"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("getting-used-to-aids", "help-family-member"),
        sections           = listOf(
            ArticleSection(
                body = "Communication strategies are practical techniques that reduce the effort hearing requires. They work regardless of whether you use hearing aids — and significantly so when used alongside them."
            ),
            ArticleSection(
                heading = "Position and lighting",
                body    = "Always face the person you are speaking with directly. Lip movement provides 30–40% of speech intelligibility for people with hearing loss. Make sure their face is well lit — backlight makes lip-reading nearly impossible. Stay close: speech clarity drops significantly beyond 2 metres, even with hearing aids."
            ),
            ArticleSection(
                heading = "Reduce background noise",
                body    = "Turn off the television or radio when having a conversation. In restaurants, ask for a quieter corner or visit outside peak hours. At work, request meetings in small, quiet rooms rather than open-plan spaces. Modern hearing aids struggle with background noise despite improvements — removing it at source is always more effective."
            ),
            ArticleSection(
                heading = "Manage conversations actively",
                body    = "Let people know you have hearing difficulty before a conversation — most people adjust naturally once they know. Ask for key information to be repeated or written down. Don't bluff: saying \"yes\" when you haven't understood leads to confusion and erodes trust."
            ),
            ArticleSection(
                heading = "At work",
                body    = "Request that meeting agendas and key decisions are followed up in writing. If your employer has an HR process for reasonable adjustments, hearing loss qualifies. A remote microphone on a speaker or colleague can dramatically improve conference room listening."
            ),
            ArticleSection(
                heading = "At home",
                body    = "Visual alerting devices (flashing doorbell, vibrating alarm) reduce reliance on hearing for safety. Captioned content on streaming platforms removes the need to increase TV volume. With family members, establish simple signals for when you've missed something — normalising the conversation reduces frustration on both sides."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "tinnitus-guide",
        title              = "Tinnitus: causes, symptoms and relief options",
        summary            = "What tinnitus is, why it happens, and the most effective strategies for managing it.",
        category           = "Hearing Loss",
        readingTimeMinutes = 6,
        audienceTags       = listOf("concerned", "existing-user"),
        intentTags         = listOf("symptoms", "treatment"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("protect-from-noise", "when-see-doctor"),
        sections           = listOf(
            ArticleSection(
                body = "Tinnitus — the perception of sound without an external source — affects around 15% of adults. For most, it's a mild annoyance. For about 2% of people, it's severely debilitating. Here's what you need to know."
            ),
            ArticleSection(
                heading = "What causes tinnitus?",
                body    = "Tinnitus is a symptom, not a disease. It arises when the auditory system produces phantom signals, usually due to:\n\n• Noise-induced hearing loss (most common cause)\n• Age-related hearing loss\n• Ear infections or blockage with earwax\n• Certain medications (high-dose aspirin, some antibiotics, diuretics)\n• Head or neck injuries\n• High blood pressure or cardiovascular disease\n• Anxiety and stress (worsens existing tinnitus)"
            ),
            ArticleSection(
                heading = "What it sounds like",
                body    = "Tinnitus manifests as ringing, buzzing, hissing, whistling, clicking, or roaring. It can be in one ear, both ears, or appear to come from inside the head. It can be constant or intermittent. It's often worse in quiet environments — this is why it feels most severe at bedtime."
            ),
            ArticleSection(
                heading = "Relief options",
                body    = "There is currently no cure for most chronic tinnitus, but there are effective management strategies:\n\n• Sound therapy: Background sounds (white noise, pink noise, nature sounds) mask the tinnitus signal. The Tinnitus Relief tool in this app provides four sound options.\n\n• Hearing aids: Since tinnitus often accompanies hearing loss, treating the hearing loss reduces tinnitus severity in many patients.\n\n• Cognitive Behavioural Therapy (CBT): The most evidence-backed psychological approach. It doesn't eliminate tinnitus but significantly reduces its emotional impact.\n\n• Lifestyle: Reduce caffeine, alcohol, and salt. Exercise regularly. Manage stress."
            ),
            ArticleSection(
                heading = "When to see a doctor",
                body    = "See a doctor promptly if tinnitus started after a head injury, is only in one ear, pulsates in rhythm with your heartbeat, or is accompanied by sudden hearing loss."
            ),
        )
    ),


    // ── Stage 4: Choose a Hearing Aid ─────────────────────────────────────────

    KnowledgeArticle(
        id                 = "types-of-hearing-aids",
        title              = "Types of hearing aids explained",
        summary            = "BTE, RIC, ITE, IIC — understand what each style means for comfort, performance, and who it suits.",
        category           = "Hearing Aids",
        readingTimeMinutes = 6,
        audienceTags       = listOf("buyer"),
        intentTags         = listOf("buying"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("read-audiogram"),
        relatedArticleIds  = listOf("hearing-aid-costs", "how-aids-are-fitted"),
        sections           = listOf(
            ArticleSection(
                body = "There is no single best hearing aid. The right style depends on your degree of loss, ear anatomy, manual dexterity, lifestyle, and how visible you want the device to be. Here's a clear guide to what's available."
            ),
            ArticleSection(
                heading = "Behind-the-ear (BTE)",
                body    = "The device sits behind the ear and connects to an earmould in the canal via a thin tube. BTE aids are the most powerful style and suit all degrees of hearing loss. They're easy to handle and have the largest battery capacity, giving longer battery life. More visible than other styles, though modern designs are compact."
            ),
            ArticleSection(
                heading = "Receiver-in-canal (RIC / RITE)",
                body    = "The most popular style globally. Similar to BTE but the receiver (speaker) sits inside the ear canal rather than in the main body. This makes them smaller, more discreet, and acoustically more natural. The thin wire is barely visible. RICs suit mild to moderately severe loss and are available rechargeable. Most AI-powered and Bluetooth-capable aids are RIC style."
            ),
            ArticleSection(
                heading = "In-the-ear (ITE) and in-the-canal (ITC)",
                body    = "Custom-moulded to sit entirely within the outer ear (ITE) or partially in the ear canal (ITC). More discreet than BTE or RIC but require good dexterity to handle. Battery life is shorter due to smaller size. Suitable for mild to moderately severe loss."
            ),
            ArticleSection(
                heading = "Completely-in-canal (CIC) and invisible-in-canal (IIC)",
                body    = "The most discreet options, fitting deep inside the ear canal. Nearly invisible in most ears. Their tiny size limits battery life, power output, and features — they typically lack Bluetooth, directional microphones, and rechargeable options. Suitable only for mild to moderate loss."
            ),
            ArticleSection(
                heading = "Features to consider",
                body    = "• Rechargeable vs disposable battery: rechargeable is increasingly preferred for convenience\n• Bluetooth: direct streaming from phone, TV, and compatible devices\n• Directional microphones: critical for speech in noise\n• App control: discreet volume and programme adjustment from your phone\n• IP rating: water and dust resistance matters if you're active\n\nYour audiologist will recommend a style and feature set matched to your audiogram and lifestyle."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "hearing-aid-costs",
        title              = "How much do hearing aids cost — and what drives the price?",
        summary            = "Understand what you're actually paying for, price ranges in India, and how to avoid overpaying.",
        category           = "Hearing Aids",
        readingTimeMinutes = 5,
        audienceTags       = listOf("buyer"),
        intentTags         = listOf("buying"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("types-of-hearing-aids", "how-aids-are-fitted"),
        sections           = listOf(
            ArticleSection(
                body = "Hearing aid pricing is notoriously confusing. Prices in India range from ₹8,000 to over ₹3,00,000 per device. Understanding what drives that range helps you get the right device at a fair price — not the most expensive one."
            ),
            ArticleSection(
                heading = "What drives the price",
                body    = "The cost is primarily driven by the processing chip level (basic, mid-range, premium, ultra-premium). Higher chip tiers offer:\n\n• Better noise suppression in complex environments\n• More accurate directional processing\n• More Bluetooth and streaming features\n• Faster, more natural sound processing\n\nThe chip level is the main variable; hardware form factor adds to it."
            ),
            ArticleSection(
                heading = "Approximate price ranges in India",
                body    = "• Entry level: ₹8,000–₹25,000 — basic digital processing, suitable for quiet environments\n• Mid-range: ₹25,000–₹80,000 — reliable noise management, Bluetooth on some models, good for most users\n• Premium: ₹80,000–₹1,50,000 — advanced noise processing, full Bluetooth, rechargeable, app control\n• Ultra-premium: ₹1,50,000–₹3,00,000+ — best available noise suppression, AI features\n\nPrices are typically per device; most people need two."
            ),
            ArticleSection(
                heading = "Does expensive mean better for you?",
                body    = "Not necessarily. The difference between mid-range and premium is significant in noisy environments. The difference between premium and ultra-premium is incremental for most users. A mid-range device properly fitted to your audiogram will outperform a poorly fitted premium device.\n\nThe fitting and follow-up care from your audiologist matters more than the chip tier."
            ),
            ArticleSection(
                heading = "Government schemes in India",
                body    = "The ADIP scheme provides hearing aids free or at subsidised cost to below-poverty-line individuals. ALIMCO produces and supplies government-approved aids. Several state disability welfare boards offer additional support. Ask your audiologist or district disability office about eligibility."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "how-aids-are-fitted",
        title              = "How hearing aids are fitted and adjusted",
        summary            = "What happens at a fitting appointment, why real-ear measurement matters, and what to expect in the first weeks.",
        category           = "Hearing Aids",
        readingTimeMinutes = 5,
        audienceTags       = listOf("buyer"),
        intentTags         = listOf("buying"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("read-audiogram"),
        relatedArticleIds  = listOf("getting-used-to-aids", "types-of-hearing-aids"),
        sections           = listOf(
            ArticleSection(
                body = "Buying a hearing aid is only the first step. The fitting process determines whether it will actually work for you — a well-fitted basic aid beats a poorly fitted premium one every time."
            ),
            ArticleSection(
                heading = "The fitting appointment",
                body    = "At your fitting, the audiologist programs the hearing aid to a prescription derived from your audiogram. Each frequency band is adjusted to match your specific hearing loss pattern. Initial settings are typically conservative — slightly quieter than the target prescription — to allow gradual acclimatisation."
            ),
            ArticleSection(
                heading = "Real-ear measurement (REM)",
                body    = "The gold standard for fitting verification. A thin probe microphone is placed in your ear canal alongside the hearing aid. Speech and tones are played and the audiologist measures what the aid is actually delivering to your eardrum — confirming it reaches the prescribed targets. Always ask whether your audiologist uses real-ear measurement."
            ),
            ArticleSection(
                heading = "Wearing schedule",
                body    = "Your audiologist will recommend a graduated wearing schedule — starting with a few hours per day in quiet environments, expanding over 2–4 weeks to all-day wear in progressively more complex situations. Rushing this process leads to fatigue and rejection. The brain genuinely needs time to relearn sounds it has been missing."
            ),
            ArticleSection(
                heading = "Follow-up appointments",
                body    = "The first few weeks are critical. Plan for at least two follow-up appointments in the first month. Common adjustments include reducing feedback, fine-tuning volume in specific situations, and adjusting the fitting for comfort. Most aids come with a 30–90 day trial period — use it fully before deciding."
            ),
        )
    ),


    // ── Stage 5: Live Well with Better Hearing ────────────────────────────────

    KnowledgeArticle(
        id                 = "getting-used-to-aids",
        title              = "Getting used to hearing aids",
        summary            = "What to expect in the first weeks, how to care for your devices, and when to go back to your audiologist.",
        category           = "Hearing Aids",
        readingTimeMinutes = 6,
        audienceTags       = listOf("existing-user"),
        intentTags         = listOf("maintenance"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = listOf("how-aids-are-fitted"),
        relatedArticleIds  = listOf("communication-strategies", "tinnitus-guide"),
        sections           = listOf(
            ArticleSection(
                body = "New hearing aids don't sound natural straight away. That's not a sign of a problem — it's the brain doing exactly what it should. Here's what to expect and how to get through the adjustment period successfully."
            ),
            ArticleSection(
                heading = "Why everything sounds strange",
                body    = "Your brain has been compensating for reduced input for months or years. When hearing is suddenly restored, ordinary sounds — your own voice, footsteps on a hard floor, a refrigerator hum — feel startlingly loud and unnatural. This is normal. The auditory cortex needs time to recalibrate, a process that takes 6–12 weeks for most people."
            ),
            ArticleSection(
                heading = "The first two weeks",
                body    = "Wear the aids in quiet, familiar environments first. Your own home is ideal. Limit initial wear to 3–4 hours per day to avoid listening fatigue. Keep a brief daily log of situations where hearing felt good and where it was difficult — this is invaluable feedback for your audiologist at the follow-up appointment."
            ),
            ArticleSection(
                heading = "Weeks three to six",
                body    = "Gradually introduce more complex listening situations: small group conversations, television (with closed captions as a bridge), quiet cafes. Pay attention to the programmes your hearing aid offers — many aids have dedicated settings for different environments. By week six, most users are wearing aids comfortably all day."
            ),
            ArticleSection(
                heading = "Daily cleaning and care",
                body    = "• Remove aids at night and wipe with a dry cloth\n• Use the cleaning tools provided (brush, wax pick) to clear earwax from the receiver opening daily\n• Store in the charging case or a dry box overnight — moisture is the most common cause of damage\n• Never wear aids in the shower, swimming, or heavy rain\n• Replace wax filters as instructed — typically every 2–4 weeks"
            ),
            ArticleSection(
                heading = "When to go back",
                body    = "Return if: you notice feedback (whistling) that wasn't there before, one aid sounds quieter than it used to, a specific environment is consistently difficult, or you experience ear pain or discharge. Don't wait for your annual review if something has changed. Adjustments usually take 15 minutes."
            ),
        )
    ),


    KnowledgeArticle(
        id                 = "protect-from-noise",
        title              = "How to protect your hearing from everyday noise",
        summary            = "The 85 dB rule, the sounds to watch out for, and how to protect yourself without missing out.",
        category           = "Prevention",
        readingTimeMinutes = 4,
        audienceTags       = listOf("beginner", "concerned"),
        intentTags         = listOf("awareness"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("causes-of-hearing-loss", "tinnitus-guide"),
        sections           = listOf(
            ArticleSection(
                body = "Noise-induced hearing loss is the most preventable form of hearing loss — and one of the most common. The damage is permanent. Here's how to avoid it."
            ),
            ArticleSection(
                heading = "The 85 dB rule",
                body    = "Damage begins at 85 decibels. The louder the sound, the faster the damage. At 85 dB (busy traffic, a lawn mower), you have about 8 hours before risk begins. At 100 dB (live music, nightclub), damage can occur in 15 minutes. At 120 dB (jet engine close up), pain and instant damage happen immediately."
            ),
            ArticleSection(
                heading = "Everyday sounds to watch",
                body    = "• Earphones at 80%+ volume: 90–100 dB. Use the 60/60 rule — no more than 60% volume for 60 minutes at a stretch.\n• Motorcycles and auto-rickshaws: 95–100 dB. Wear helmet ear inserts.\n• Power tools (drills, grinders): 100–110 dB. Always use ear protection.\n• Firecrackers: 150+ dB. Keep at maximum possible distance.\n• Concerts and clubs: 105–115 dB. Use musician's earplugs."
            ),
            ArticleSection(
                heading = "Protecting yourself without missing out",
                body    = "Musician's earplugs reduce volume by 15–25 dB while preserving clarity. They're small, nearly invisible, and inexpensive. You can enjoy music at a concert without dangerous peak volumes.\n\nFor workplaces, standard foam earplugs offer 25–33 dB of protection and cost almost nothing. Use them whenever operating machinery."
            ),
            ArticleSection(
                heading = "After a loud event",
                body    = "Temporary threshold shift — the muffled, ringing sensation after a concert — means your hair cells are stressed. Give your ears 16–18 hours of quiet to recover. If ringing (tinnitus) persists for more than a day, see an audiologist."
            ),
        )
    ),


    // ── Stage 6: Helping a Loved One ──────────────────────────────────────────

    KnowledgeArticle(
        id                 = "help-family-member",
        title              = "How to help a family member who won't admit hearing loss",
        summary            = "Why denial is common, what to avoid, and how to have a conversation that leads to action.",
        category           = "For Families",
        readingTimeMinutes = 7,
        audienceTags       = listOf("caregiver"),
        intentTags         = listOf("awareness"),
        difficulty         = ArticleDifficulty.BEGINNER,
        prerequisiteIds    = emptyList(),
        relatedArticleIds  = listOf("signs-checklist", "communication-strategies"),
        sections           = listOf(
            ArticleSection(
                body = "\"I can hear fine — you just mumble.\" If this sounds familiar, you are not alone. Denial is one of the most common responses to hearing loss, particularly among older adults. Here's how to help without creating conflict."
            ),
            ArticleSection(
                heading = "Why people deny hearing loss",
                body    = "Hearing loss carries a stigma that vision loss doesn't. Glasses are fashionable; hearing aids still feel like a badge of old age. Many people also don't notice gradual decline — the brain compensates for years by filling in the gaps, lip-reading unconsciously, and asking for repetition without connecting the dots.\n\nFor others, admitting hearing loss means confronting ageing, dependence, and cost."
            ),
            ArticleSection(
                heading = "What not to do",
                body    = "• Don't shout or over-enunciate — it feels infantilising\n• Don't have the conversation in front of others — humiliation creates resistance\n• Don't make it about inconvenience to you — it triggers defensiveness\n• Don't give ultimatums"
            ),
            ArticleSection(
                heading = "What works",
                body    = "Have the conversation one-on-one in a quiet space. Frame it around safety — missed doorbells, emergency announcements, crossing the road. Ask them to do it for you: \"I worry about you. Would you get a hearing test just so we know for sure?\"\n\nMake the first step easy. A free hearing test at home removes the barrier of scheduling an audiologist visit. Once someone sees their own results, denial becomes harder to maintain."
            ),
            ArticleSection(
                heading = "When they see their results",
                body    = "Accompany them to the audiologist appointment. Many people shut down when alone but engage differently with a family member present. Take notes. Ask questions. The audiologist has seen this many times before and knows how to work with reluctant patients."
            ),
            ArticleSection(
                heading = "Managing the adjustment period",
                body    = "New hearing aid users need time. The brain has to relearn sounds it hasn't processed properly in years. Everyday sounds (refrigerator hum, footsteps) feel suddenly loud. This is normal and passes within 2–6 weeks. Be patient and encouraging during this period."
            ),
        )
    ),
)


// ─── Learning paths ───────────────────────────────────────────────────────────

val learningPaths: List<LearningPath> = listOf(

    LearningPath(
        id          = "discover-hearing",
        title       = "Discover Hearing",
        subtitle    = "The universal entry point",
        description = "Understand how hearing works, what normal listening involves, and what hearing difficulties can look like.",
        icon        = Icons.Default.School,
        colorIndex  = 0,
        articleIds  = listOf("how-hearing-works", "signs-checklist", "causes-of-hearing-loss", "why-hearing-worsens"),
    ),

    LearningPath(
        id          = "understand-your-hearing",
        title       = "Understand Your Hearing",
        subtitle    = "From concern to clarity",
        description = "Learn how hearing screening works, what results mean, and when professional assessment is appropriate.",
        icon        = Icons.Default.Hearing,
        colorIndex  = 1,
        articleIds  = listOf("how-hearing-tests-work", "read-audiogram", "what-to-expect-assessment", "when-see-doctor"),
    ),

    LearningPath(
        id          = "explore-solutions",
        title       = "Explore Solutions",
        subtitle    = "Understand the options",
        description = "Explore available approaches without assuming every hearing difficulty requires a hearing aid.",
        icon        = Icons.Default.Psychology,
        colorIndex  = 2,
        articleIds  = listOf("options-for-hearing-loss", "common-qa", "communication-strategies", "tinnitus-guide"),
    ),

    LearningPath(
        id          = "choose-hearing-aid",
        title       = "Choose a Hearing Aid",
        subtitle    = "For users considering a device",
        description = "Understand device types, features, fitting, and costs to make an informed decision.",
        icon        = Icons.Default.HearingDisabled,
        colorIndex  = 3,
        articleIds  = listOf("types-of-hearing-aids", "hearing-aid-costs", "how-aids-are-fitted", "read-audiogram"),
    ),

    LearningPath(
        id          = "live-well",
        title       = "Live Well with Better Hearing",
        subtitle    = "For long-term support",
        description = "Everyday listening, device care, communication in challenging environments, and follow-up guidance.",
        icon        = Icons.Default.FavoriteBorder,
        colorIndex  = 4,
        articleIds  = listOf("getting-used-to-aids", "communication-strategies", "protect-from-noise", "tinnitus-guide"),
    ),

    LearningPath(
        id          = "helping-a-loved-one",
        title       = "Helping a Loved One",
        subtitle    = "For family and caregivers",
        description = "Help a family member recognise and address their hearing loss with confidence.",
        icon        = Icons.Default.FamilyRestroom,
        colorIndex  = 5,
        articleIds  = listOf("help-family-member", "signs-checklist", "common-qa", "when-see-doctor"),
    ),
)


// ─── Intent entry points ──────────────────────────────────────────────────────

val learningIntents: List<LearningIntent> = listOf(
    LearningIntent(
        label        = "I want to learn about hearing",
        icon         = Icons.Default.School,
        targetPathId = "discover-hearing",
    ),
    LearningIntent(
        label        = "I'm concerned about my hearing",
        icon         = Icons.Default.HelpOutline,
        targetPathId = "understand-your-hearing",
    ),
    LearningIntent(
        label        = "I'm considering a hearing aid",
        icon         = Icons.Default.HearingDisabled,
        targetPathId = "choose-hearing-aid",
    ),
    LearningIntent(
        label        = "I already use a hearing aid",
        icon         = Icons.Default.Hearing,
        targetPathId = "live-well",
    ),
    LearningIntent(
        label        = "I'm helping someone else",
        icon         = Icons.Default.FamilyRestroom,
        targetPathId = "helping-a-loved-one",
    ),
)
