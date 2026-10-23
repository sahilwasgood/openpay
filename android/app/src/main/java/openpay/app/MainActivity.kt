package openpay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import openpay.app.ui.DetailScreen
import openpay.app.ui.HistoryScreen
import openpay.app.ui.Lang
import openpay.app.ui.NotesScreen
import openpay.app.ui.OnboardingScreen
import openpay.app.ui.PayScreen
import openpay.app.ui.SettingsScreen
import openpay.app.ui.theme.OpenPayTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OpenPayApp
        Lang.set(app.store.language.value)
        setContent {
            OpenPayTheme(elderly = app.store.elderly.collectAsState().value) {
                val onboarded by app.store.onboarded.collectAsState()
                val nav = rememberNavController()
                NavHost(
                    navController = nav,
                    startDestination = if (onboarded) "pay" else "onboard"
                ) {
                    composable("onboard") {
                        OnboardingScreen(app = app, onDone = {
                            nav.navigate("pay") { popUpTo("onboard") { inclusive = true } }
                        })
                    }
                    composable("pay") {
                        PayScreen(
                            app = app,
                            onOpenHistory = { nav.navigate("history") },
                            onOpenNotes = { nav.navigate("notes") },
                            onOpenSettings = { nav.navigate("settings") }
                        )
                    }
                    composable("notes") {
                        NotesScreen(app = app, onBack = { nav.popBackStack() })
                    }
                    composable("history") {
                        HistoryScreen(
                            app = app,
                            onBack = { nav.popBackStack() },
                            onOpen = { id -> nav.navigate("detail/$id") }
                        )
                    }
                    composable("detail/{id}") { entry ->
                        DetailScreen(
                            app = app,
                            id = entry.arguments?.getString("id").orEmpty(),
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(app = app, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
