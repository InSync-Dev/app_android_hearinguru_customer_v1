package com.hearinguru.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private data class AidType(
    val name: String,
    val abbrev: String,
    val tagline: String,
    val priceRange: String,
    val bestFor: String,
    val pros: List<String>,
    val cons: List<String>,
)


private val aidTypes = listOf(
    AidType(
        name = "Behind-the-Ear",
        abbrev = "BTE",
        tagline = "Versatile & powerful",
        priceRange = "₹15,000 – ₹1,20,000",
        bestFor = "Mild to profound hearing loss",
        pros = listOf("Long battery life", "Easy to handle", "Powerful amplification"),
        cons = listOf("Visible behind ear", "May whistle with glasses"),
    ),
    AidType(
        name = "Receiver-in-Canal",
        abbrev = "RIC",
        tagline = "Discreet & natural",
        priceRange = "₹25,000 – ₹2,00,000",
        bestFor = "Mild to severe hearing loss",
        pros = listOf("Nearly invisible", "Natural sound", "Comfortable fit"),
        cons = listOf("Receiver exposed to moisture", "Higher cost"),
    ),
    AidType(
        name = "In-the-Ear",
        abbrev = "ITE",
        tagline = "Easy to wear",
        priceRange = "₹20,000 – ₹1,50,000",
        bestFor = "Mild to moderately severe loss",
        pros = listOf("Easy to insert", "Volume control on shell", "Good for arthritis"),
        cons = listOf("More visible", "May feel occluded"),
    ),
    AidType(
        name = "Completely-in-Canal",
        abbrev = "CIC",
        tagline = "Maximum discretion",
        priceRange = "₹40,000 – ₹2,50,000",
        bestFor = "Mild to moderate hearing loss",
        pros = listOf("Nearly invisible", "Reduced wind noise", "Natural feel"),
        cons = listOf("Tiny batteries", "No manual controls", "Not for severe loss"),
    ),
)


private data class BrandInfo(
    val name: String,
    val origin: String,
    val priceSegment: String,
)


private val brands = listOf(
    BrandInfo("Phonak", "Swiss", "Mid–Premium"),
    BrandInfo("Signia", "German", "Mid–Premium"),
    BrandInfo("Widex", "Danish", "Premium"),
    BrandInfo("Starkey", "American", "Mid–Premium"),
    BrandInfo("ReSound", "Danish", "Mid–Premium"),
    BrandInfo("Oticon", "Danish", "Premium"),
    BrandInfo("Siemens / Signia", "German", "Mid"),
    BrandInfo("Unitron", "Canadian", "Entry–Mid"),
    BrandInfo("Bernafon", "Swiss", "Mid"),
    BrandInfo("Interton", "Danish", "Entry"),
)


private data class FaqItem(val question: String, val answer: String)


private val faqs = listOf(
    FaqItem(
        "Do I really need a prescription?",
        "In India, a hearing aid can be purchased over the counter, but a professional hearing evaluation by an audiologist ensures you get the right model and fitting for your specific hearing loss pattern.",
    ),
    FaqItem(
        "How long do hearing aids last?",
        "Most hearing aids last 5–7 years with proper maintenance. Daily cleaning, moisture protection, and regular audiologist visits extend lifespan significantly.",
    ),
    FaqItem(
        "Are there government subsidies?",
        "Yes. The ADIP scheme (Assistance to Disabled Persons) by the Ministry of Social Justice provides free or subsidised hearing aids to persons with disability below the income threshold. Contact your local district rehabilitation centre.",
    ),
    FaqItem(
        "Bluetooth vs non-Bluetooth — which is better?",
        "Bluetooth hearing aids stream audio directly from phones and TVs, which is very convenient. However, they cost more and drain batteries faster. For most first-time users, a quality non-BT model is a practical start.",
    ),
)


@Composable
fun HearingAidsScreen(onSeeDoctor: () -> Unit = {}) {
    var selectedAidIndex by remember { mutableStateOf(0) }
    var expandedFaqIndex by remember { mutableStateOf(-1) }

    val selectedAid = aidTypes[selectedAidIndex]

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {

        // ── Hero banner ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HearingDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hearing Aid Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Types · Brands · Prices · FAQs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }


        // ── Section: Types ───────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                icon = Icons.Default.Category,
                title = "Types of Hearing Aids",
            )
            Spacer(modifier = Modifier.height(12.dp))
        }


        // ── Aid type selector tabs ────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(aidTypes.indices.toList()) { index ->
                    val isSelected = index == selectedAidIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAidIndex = index },
                        label = {
                            Text(
                                text = aidTypes[index].abbrev,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }


        // ── Selected aid detail card ──────────────────────────────────
        item {
            AidDetailCard(aid = selectedAid)
            Spacer(modifier = Modifier.height(24.dp))
        }


        // ── Section: Price table ──────────────────────────────────────
        item {
            SectionHeader(
                icon = Icons.Default.CurrencyRupee,
                title = "Price Ranges in India",
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            PriceTable()
            Spacer(modifier = Modifier.height(24.dp))
        }


        // ── Section: Brands ──────────────────────────────────────────
        item {
            SectionHeader(
                icon = Icons.Default.Star,
                title = "Popular Brands",
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(brands) { brand ->
            BrandRow(brand = brand)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }


        // ── Section: FAQs ────────────────────────────────────────────
        item {
            SectionHeader(
                icon = Icons.Default.Help,
                title = "Frequently Asked Questions",
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(faqs.indices.toList()) { index ->
            FaqCard(
                faq = faqs[index],
                isExpanded = expandedFaqIndex == index,
                onToggle = {
                    expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }


        // ── CTA card ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            CtaBanner(onSeeDoctor = onSeeDoctor)
        }
    }
}


@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@Composable
private fun AidDetailCard(aid: AidType) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = aid.abbrev,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = aid.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = aid.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Price range", value = aid.priceRange)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(label = "Best for", value = aid.bestFor)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProConColumn(
                    title = "Pros",
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    items = aid.pros,
                    modifier = Modifier.weight(1f),
                )
                ProConColumn(
                    title = "Cons",
                    titleColor = MaterialTheme.colorScheme.error,
                    items = aid.cons,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@Composable
private fun ProConColumn(
    title: String,
    titleColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}


@Composable
private fun PriceTable() {
    val rows = listOf(
        Triple("Entry", "Basic amplification, mono", "₹8,000 – ₹25,000"),
        Triple("Mid", "Digital, directional mics", "₹25,000 – ₹80,000"),
        Triple("Advanced", "Noise reduction, BT ready", "₹80,000 – ₹1,80,000"),
        Triple("Premium", "AI processing, rechargeable", "₹1,80,000 – ₹3,50,000"),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Segment",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Features",
                    modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Price (pair)",
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.End,
                )
            }

            rows.forEachIndexed { index, (segment, features, price) ->
                val isEven = index % 2 == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isEven) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = segment,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = features,
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = price,
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}


@Composable
private fun BrandRow(brand: BrandInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = brand.name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = brand.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = brand.origin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = brand.priceSegment,
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )
    }
}


@Composable
private fun FaqCard(faq: FaqItem, isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(250))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}


@Composable
private fun CtaBanner(onSeeDoctor: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Not sure which hearing aid suits you?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Our audiologists can guide you to the right fit for your hearing loss, budget, and lifestyle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSeeDoctor,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Book Free Consultation")
            }
        }
    }
}
