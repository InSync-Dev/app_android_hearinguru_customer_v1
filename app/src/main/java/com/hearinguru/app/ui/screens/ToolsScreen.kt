package com.hearinguru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hearinguru.app.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private data class CarouselSlide(
    val eyebrow: String,
    val title: String,
    val chipLabel: String,
    val chipIcon: ImageVector,
    val route: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private data class ToolTile(
    val route: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val badge: String,
    val iconBg: Color,
    val iconTint: Color,
)


@Composable
fun ToolsScreen(
    onNavigate: (String) -> Unit,
    onNavigateToDoctor: () -> Unit,
) {
    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor  = MaterialTheme.colorScheme.tertiary

    val slides = listOf(
        CarouselSlide(
            eyebrow       = "Free · 5 minutes",
            title         = "Check your\nhearing today",
            chipLabel     = "Start test",
            chipIcon      = Icons.Default.PlayArrow,
            route         = Screen.HearingTest.route,
            gradientStart = primaryColor,
            gradientEnd   = secondaryColor,
        ),
        CarouselSlide(
            eyebrow       = "Boost · Real-time",
            title         = "Hear every\nconversation clearly",
            chipLabel     = "Open amplifier",
            chipIcon      = Icons.Default.VolumeUp,
            route         = Screen.SoundAmplifier.route,
            gradientStart = secondaryColor,
            gradientEnd   = tertiaryColor,
        ),
        CarouselSlide(
            eyebrow       = "Relief · Calming sounds",
            title         = "Quiet the\nringing in your ears",
            chipLabel     = "Start relief",
            chipIcon      = Icons.Default.SelfImprovement,
            route         = Screen.Tinnitus.route,
            gradientStart = tertiaryColor,
            gradientEnd   = primaryColor,
        ),
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope      = rememberCoroutineScope()

    // Auto-advance carousel every 4 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val isLastPage = pagerState.currentPage == slides.lastIndex
            val nextPage   = if (isLastPage) 0 else pagerState.currentPage + 1
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val tools = listOf(
        ToolTile(
            route    = Screen.HearingTest.route,
            icon     = Icons.Default.Hearing,
            title    = "Hearing Test",
            subtitle = "Check how well you hear in 5 min",
            badge    = "FREE",
            iconBg   = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        ToolTile(
            route    = Screen.SoundAmplifier.route,
            icon     = Icons.Default.VolumeUp,
            title    = "Sound Amplifier",
            subtitle = "Boost voices, reduce background noise",
            badge    = "FREE",
            iconBg   = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        ToolTile(
            route    = Screen.NoiseMeter.route,
            icon     = Icons.Default.GraphicEq,
            title    = "Noise Meter",
            subtitle = "Is your environment safe for your ears?",
            badge    = "FREE",
            iconBg   = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        ToolTile(
            route    = Screen.Tinnitus.route,
            icon     = Icons.Default.SelfImprovement,
            title    = "Tinnitus Relief",
            subtitle = "Soothing sounds to quiet the ringing",
            badge    = "FREE",
            iconBg   = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Carousel
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val slide = slides[pageIndex]

            val contentColor = MaterialTheme.colorScheme.onPrimary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(slide.gradientStart, slide.gradientEnd))
                    )
                    .clickable { onNavigate(slide.route) }
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column {
                    Text(
                        text          = slide.eyebrow.uppercase(),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = contentColor.copy(alpha = 0.7f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text       = slide.title,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = contentColor,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = contentColor.copy(alpha = 0.18f),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(slide.chipIcon, null, tint = contentColor, modifier = Modifier.size(16.dp))
                            Text(slide.chipLabel, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Dot indicators
        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(slides.size) { dotIndex ->
                val isActive = pagerState.currentPage == dotIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isActive) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(dotIndex) }
                        }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Section header
        Text(
            text          = "Free Tools",
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(8.dp))

        // 2-column tool tile grid
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.chunked(2).forEach { rowTools ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTools.forEach { tile ->
                        ToolTileCard(
                            tile    = tile,
                            onClick = { onNavigate(tile.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // If odd number of tiles, fill remaining space
                    if (rowTools.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


@Composable
private fun ToolTileCard(tile: ToolTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    Surface(
        modifier          = modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Icon box
            Box(
                modifier        = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tile.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(tile.icon, contentDescription = null, tint = tile.iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(10.dp))

            // FREE badge
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFC8E8CC)
            ) {
                Text(
                    tile.badge,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color      = Color(0xFF2A5C35),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                tile.title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                tile.subtitle,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}


// Keep ToolListItem for backwards-compat if referenced elsewhere
data class ToolItem(
    val route: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val iconBg: Color,
    val iconTint: Color,
)
