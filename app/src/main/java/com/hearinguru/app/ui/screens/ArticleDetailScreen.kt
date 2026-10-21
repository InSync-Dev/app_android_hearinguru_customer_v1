package com.hearinguru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ArticleDetailScreen(
    articleIndex: Int,
    onArticleClick: (Int) -> Unit = {},
) {
    val article = knowledgeArticles.getOrNull(articleIndex) ?: return

    val relatedArticles = article.relatedArticleIds
        .mapNotNull { relatedId -> knowledgeArticles.find { it.id == relatedId } }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {

        // Hero header
        Surface(
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        article.meta,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    article.title,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 30.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    article.summary,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    lineHeight = 22.sp
                )
            }
        }


        // Article sections
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            article.sections.forEachIndexed { index, section ->
                if (index > 0) Spacer(Modifier.height(20.dp))

                if (section.heading != null) {
                    Text(
                        section.heading,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    section.body,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }


        // Continue Learning section
        if (relatedArticles.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "CONTINUE LEARNING",
                modifier      = Modifier.padding(horizontal = 20.dp),
                style         = MaterialTheme.typography.labelMedium,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(10.dp))

            Column(Modifier.padding(horizontal = 16.dp)) {
                relatedArticles.forEachIndexed { index, related ->
                    val relatedIndex = indexOfArticle(related.id)

                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArticleClick(relatedIndex) }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    related.title,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    related.meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < relatedArticles.size - 1) Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
