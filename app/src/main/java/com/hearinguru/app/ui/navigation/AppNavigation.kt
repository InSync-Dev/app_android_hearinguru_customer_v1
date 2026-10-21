package com.hearinguru.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.hearinguru.app.R
import com.hearinguru.app.ui.screens.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(isDark: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        Screen.Tools.route,
        Screen.Learn.route,
        Screen.HearingAids.route,
        Screen.SeeADoctor.route
    )
    val isTopLevel = currentRoute in topLevelRoutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter            = painterResource(R.drawable.logo),
                        contentDescription = "HearinGuru",
                        modifier           = Modifier.height(36.dp),
                        colorFilter        = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector        = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    if (isTopLevel) {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                when (item.screen) {
                                    Screen.Tools       -> Icon(Icons.Default.Build, contentDescription = null)
                                    Screen.Learn       -> Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                                    Screen.HearingAids -> Icon(Icons.Default.HearingDisabled, contentDescription = null)
                                    Screen.SeeADoctor  -> Icon(Icons.Default.MedicalServices, contentDescription = null)
                                    else -> Icon(Icons.Default.Circle, contentDescription = null)
                                }
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Tools.route,
            modifier         = Modifier.padding(padding),
            enterTransition  = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec  = tween(280)
                ) + fadeIn(tween(280))
            },
            exitTransition   = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(280)
                ) + fadeOut(tween(280))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec  = tween(280)
                ) + fadeIn(tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280)
                ) + fadeOut(tween(280))
            },
        ) {
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigate         = { navController.navigate(it) },
                    onNavigateToDoctor = { navController.navigate(Screen.SeeADoctor.route) }
                )
            }
            composable(Screen.HearingTest.route) {
                HearingTestScreen(
                    onNavigateToDoctor      = { navController.navigate(Screen.SeeADoctor.route) },
                    onNavigateToHearingAids = { navController.navigate(Screen.HearingAids.route) }
                )
            }
            composable(Screen.SoundAmplifier.route) { SoundAmplifierScreen() }
            composable(Screen.NoiseMeter.route)     { NoiseMeterScreen() }
            composable(Screen.Tinnitus.route)       { TinnitusScreen() }
            composable(Screen.Learn.route) {
                LearnScreen(
                    onArticleClick = { index -> navController.navigate(Screen.ArticleDetail.route(index)) }
                )
            }
            composable(Screen.HearingAids.route) {
                HearingAidsScreen(onSeeDoctor = { navController.navigate(Screen.SeeADoctor.route) })
            }
            composable(Screen.SeeADoctor.route) { SeeADoctorScreen() }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Profile.route)       { ProfileScreen() }
            composable(
                route     = Screen.ArticleDetail.route,
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                ArticleDetailScreen(articleIndex = index)
            }
        }
    }
}
