package com.hearinguru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun LearningPathScreen(
    pathIndex: Int,
    onArticleClick: (Int) -> Unit = {},
) {
    val path = learningPaths.getOrNull(pathIndex) ?: return

    val pathArticles = path.articleIds
        .mapNotNull { articleId -> knowledgeArticles.find { it.id == articleId } }

    val containerColor   = pathContainerColorStatic(path.colorIndex)
    val onContainerColor = pathOnContainerColorStatic(path.colorIndex)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {

        // ── Path header ───────────────────────────────────────────────────────

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                // Path icon badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(onContainerColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        path.icon,
                        contentDescription = null,
                        tint     = onContainerColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = onContainerColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        path.subtitle,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = onContainerColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    path.title,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = onContainerColor,
                    lineHeight = 30.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    path.description,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = onContainerColor.copy(alpha = 0.80f),
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = onContainerColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${pathArticles.size} articles in this path",
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style      = MaterialTheme.typography.labelMedium,
                        color      = onContainerColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }


        // ── Article sequence ──────────────────────────────────────────────────

        Spacer(Modifier.height(16.dp))

        Text(
            "IN THIS PATH",
            modifier      = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        Spacer(Modifier.height(8.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            pathArticles.forEachIndexed { stepIndex, article ->
                val articleIndex     = indexOfArticle(article.id)
                val stepNumber       = stepIndex + 1
                val isLastArticle    = stepIndex == pathArticles.size - 1

                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step number + connector line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stepNumber.toString(),
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color      = onContainerColor
                            )
                        }

                        if (!isLastArticle) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(16.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    // Article card
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onArticleClick(articleIndex) }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    article.title,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    article.meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    article.summary,
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp,
                                    maxLines   = 2
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (!isLastArticle) Spacer(Modifier.height(4.dp))
            }
        }
    }
}


// ─── Static colour helpers (no @Composable needed for Box backgrounds) ────────
//     These return Color directly — called inside @Composable via MaterialTheme.

@Composable
private fun pathContainerColorStatic(colorIndex: Int): androidx.compose.ui.graphics.Color =
    when (colorIndex % 6) {
        0    -> MaterialTheme.colorScheme.primaryContainer
        1    -> MaterialTheme.colorScheme.secondaryContainer
        2    -> MaterialTheme.colorScheme.tertiaryContainer
        3    -> MaterialTheme.colorScheme.errorContainer
        4    -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
private fun pathOnContainerColorStatic(colorIndex: Int): androidx.compose.ui.graphics.Color =
    when (colorIndex % 6) {
        0    -> MaterialTheme.colorScheme.onPrimaryContainer
        1    -> MaterialTheme.colorScheme.onSecondaryContainer
        2    -> MaterialTheme.colorScheme.onTertiaryContainer
        3    -> MaterialTheme.colorScheme.onErrorContainer
        4    -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
