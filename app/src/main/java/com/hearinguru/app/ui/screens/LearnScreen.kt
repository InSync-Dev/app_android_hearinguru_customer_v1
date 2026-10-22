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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun LearnScreen(
    onArticleClick: (Int) -> Unit = {},
    onPathClick: (Int) -> Unit    = {},
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val allCategories = listOf("All") + knowledgeArticles.map { it.category }.distinct().sorted()

    val visibleArticles = knowledgeArticles.filter { article ->
        val matchesSearch   = query.isBlank() || article.title.contains(query, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || article.category == selectedCategory
        matchesSearch && matchesCategory
    }

    val isShowingLibrary = query.isNotBlank() || selectedCategory != "All"


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ── Intent entry-point selector ───────────────────────────────────────

        if (!isShowingLibrary) {
            IntentSelectorSection(
                onPathClick = onPathClick
            )

            Spacer(Modifier.height(4.dp))
        }


        // ── Search bar ────────────────────────────────────────────────────────

        OutlinedTextField(
            value            = query,
            onValueChange    = { query = it },
            placeholder      = { Text("Search all articles...") },
            leadingIcon      = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon     = if (query.isNotEmpty()) {{
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }} else null,
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine       = true,
            shape            = RoundedCornerShape(50),
            colors           = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor      = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        )

        // ── Category chips ────────────────────────────────────────────────────

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick  = { selectedCategory = cat },
                    label    = { Text(cat, fontWeight = FontWeight.SemiBold) }
                )
            }
        }


        // ── Learning paths section (hidden when searching) ────────────────────

        if (!isShowingLibrary) {
            Spacer(Modifier.height(8.dp))

            Text(
                "LEARNING PATHS",
                modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style         = MaterialTheme.typography.labelMedium,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                learningPaths.forEachIndexed { pathIndex, path ->
                    PathCard(
                        path        = path,
                        onClick     = { onPathClick(pathIndex) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }


        // ── Article library ───────────────────────────────────────────────────

        val libraryLabel = if (isShowingLibrary) "RESULTS (${visibleArticles.size})" else "ALL ARTICLES"

        Text(
            libraryLabel,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        if (visibleArticles.isEmpty()) {
            Box(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment  = Alignment.Center
            ) {
                Text(
                    "No articles match \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                visibleArticles.forEachIndexed { i, article ->
                    val articleIndex = indexOfArticle(article.id)

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clickable { onArticleClick(articleIndex) }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        val iconBg   = categoryContainerColor(article.category)
                        val iconFg   = categoryOnContainerColor(article.category)
                        val icon     = categoryIcon(article.category)

                        Surface(
                            shape    = RoundedCornerShape(12.dp),
                            color    = iconBg,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = iconFg, modifier = Modifier.size(24.dp))
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                article.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                color      = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                article.meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }

                    if (i < visibleArticles.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}


// ─── Intent selector section ──────────────────────────────────────────────────

@Composable
private fun IntentSelectorSection(onPathClick: (Int) -> Unit) {
    val primaryGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(primaryGradient)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                "Where do you want to start?",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Pick your starting point — you can always explore freely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
            )

            Spacer(Modifier.height(14.dp))

            learningIntents.forEach { intent ->
                val targetPathIndex = indexOfPath(intent.targetPathId)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPathClick(targetPathIndex) }
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        intent.icon,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        intent.label,
                        style  = MaterialTheme.typography.bodyMedium,
                        color  = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}


// ─── Path card (horizontal scroll) ───────────────────────────────────────────

@Composable
private fun PathCard(path: LearningPath, onClick: () -> Unit) {
    val containerColor  = pathContainerColor(path.colorIndex)
    val onContainerColor = pathOnContainerColor(path.colorIndex)

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            // Coloured icon band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(containerColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    path.icon,
                    contentDescription = null,
                    tint     = onContainerColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Text info
            Column(Modifier.padding(12.dp)) {
                Text(
                    path.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    maxLines   = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${path.articleIds.size} articles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ─── Category → UI helpers ────────────────────────────────────────────────────

@Composable
private fun categoryContainerColor(category: String): androidx.compose.ui.graphics.Color =
    when (category) {
        "Hearing Basics", "Hearing Loss" -> MaterialTheme.colorScheme.primaryContainer
        "Testing"                        -> MaterialTheme.colorScheme.secondaryContainer
        "Treatment", "Hearing Aids"      -> MaterialTheme.colorScheme.tertiaryContainer
        "Prevention"                     -> MaterialTheme.colorScheme.secondaryContainer
        "For Families"                   -> MaterialTheme.colorScheme.tertiaryContainer
        "FAQ"                            -> MaterialTheme.colorScheme.errorContainer
        else                             -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

@Composable
private fun categoryOnContainerColor(category: String): androidx.compose.ui.graphics.Color =
    when (category) {
        "Hearing Basics", "Hearing Loss" -> MaterialTheme.colorScheme.onPrimaryContainer
        "Testing"                        -> MaterialTheme.colorScheme.onSecondaryContainer
        "Treatment", "Hearing Aids"      -> MaterialTheme.colorScheme.onTertiaryContainer
        "Prevention"                     -> MaterialTheme.colorScheme.onSecondaryContainer
        "For Families"                   -> MaterialTheme.colorScheme.onTertiaryContainer
        "FAQ"                            -> MaterialTheme.colorScheme.onErrorContainer
        else                             -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Hearing Basics"  -> Icons.Default.School
    "Hearing Loss"    -> Icons.Default.Hearing
    "Testing"         -> Icons.Default.Psychology
    "Treatment"       -> Icons.Default.MedicalServices
    "Hearing Aids"    -> Icons.Default.HearingDisabled
    "Prevention"      -> Icons.Default.VolumeOff
    "For Families"    -> Icons.Default.FamilyRestroom
    "FAQ"             -> Icons.Default.Quiz
    else              -> Icons.Default.Info
}

@Composable
private fun pathContainerColor(colorIndex: Int): androidx.compose.ui.graphics.Color =
    when (colorIndex % 6) {
        0    -> MaterialTheme.colorScheme.primaryContainer
        1    -> MaterialTheme.colorScheme.secondaryContainer
        2    -> MaterialTheme.colorScheme.tertiaryContainer
        3    -> MaterialTheme.colorScheme.errorContainer
        4    -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
private fun pathOnContainerColor(colorIndex: Int): androidx.compose.ui.graphics.Color =
    when (colorIndex % 6) {
        0    -> MaterialTheme.colorScheme.onPrimaryContainer
        1    -> MaterialTheme.colorScheme.onSecondaryContainer
        2    -> MaterialTheme.colorScheme.onTertiaryContainer
        3    -> MaterialTheme.colorScheme.onErrorContainer
        4    -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
