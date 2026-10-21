package com.hearinguru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class Article(
    val title: String,
    val meta: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
)


@Composable
fun LearnScreen(onArticleClick: (Int) -> Unit = {}) {
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("All topics") }

    val categories = listOf("All topics", "Hearing Loss", "Prevention", "For Families", "FAQ")

    val articles = listOf(
        Article("How to read your hearing test results — explained simply", "8 min · Hearing Loss", Icons.Default.Hearing, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        Article("Why hearing gets worse after 40 — and what to do", "5 min · Hearing Loss", Icons.Default.Elderly, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        Article("How to protect your hearing from everyday noise", "4 min · Prevention", Icons.Default.VolumeOff, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
        Article("How to help a family member who won't admit hearing loss", "7 min · For Families", Icons.Default.FamilyRestroom, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
        Article("Signs you may have hearing loss — a simple checklist", "3 min · Hearing Loss", Icons.Default.Psychology, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        Article("Common questions about hearing loss — answered", "5 min · FAQ", Icons.Default.Quiz, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
        Article("Tinnitus: causes, symptoms and relief options", "6 min · Hearing Loss", Icons.Default.HearingDisabled, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
        Article("When should you see a doctor about your hearing?", "4 min · FAQ", Icons.Default.MedicalServices, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
    )

    val categoryKeyword = if (selectedCat == "All topics") null else selectedCat.lowercase()

    val visibleArticles = articles.filterIndexed { index, article ->
        val matchesSearch = query.isBlank() || article.title.contains(query, ignoreCase = true)
        val matchesCat = categoryKeyword == null || article.meta.contains(categoryKeyword, ignoreCase = true)
        matchesSearch && matchesCat
    }

    val primaryGradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )


    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Search bar
        OutlinedTextField(
            value            = query,
            onValueChange    = { query = it },
            placeholder      = { Text("Search articles...") },
            leadingIcon      = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon     = if (query.isNotEmpty()) {{
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }} else null,
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            singleLine       = true,
            shape            = RoundedCornerShape(50),
            colors           = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        )

        // Category chips — horizontally scrollable
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick  = { selectedCat = cat },
                    label    = { Text(cat, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Featured card — gradient header + text body
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onArticleClick(0) }
        ) {
            Column {
                // Gradient header with Guide tag + icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(primaryGradient, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    Icon(
                        Icons.Default.Hearing,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp).align(Alignment.Center),
                        tint     = Color.White.copy(alpha = 0.22f)
                    )
                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Guide",
                            modifier      = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color         = MaterialTheme.colorScheme.primary,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                // Text body
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "How to read your hearing test results — explained simply",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "8 min read · Hearing Loss",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "ALL ARTICLES",
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        // Article list (skip first since it's the featured card)
        Column(Modifier.padding(horizontal = 16.dp)) {
            visibleArticles.drop(1).forEachIndexed { i, article ->
                val articleIndex = articles.indexOf(article)
                Row(
                    modifier              = Modifier.fillMaxWidth().clickable { onArticleClick(articleIndex) }.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = article.iconBg,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(article.icon, null, tint = article.iconTint, modifier = Modifier.size(26.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(article.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(article.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
                if (i < visibleArticles.size - 2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
