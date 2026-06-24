package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.proxies.ProxyListScreen
import com.example.ui.sources.ProxySourcesScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "sources") {
        composable("sources") {
            ProxySourcesScreen(
                onSourceClick = { url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate("proxies/$encodedUrl")
                }
            )
        }
        composable(
            route = "proxies/{sourceUrl}",
            arguments = listOf(navArgument("sourceUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("sourceUrl") ?: ""
            val url = URLDecoder.decode(encodedUrl, "UTF-8")
            ProxyListScreen(
                sourceUrl = url,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
