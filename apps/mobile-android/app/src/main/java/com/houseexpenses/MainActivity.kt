package com.houseexpenses

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.houseexpenses.ui.screens.*
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.HouseExpensesTheme
import com.houseexpenses.ui.viewmodel.AuthViewModel
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()

    private var pendingInviteCode = mutableStateOf<String?>(null)
    private var initialSelectedTab = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)

        setContent {
            val isDarkMode by expenseViewModel.isDarkMode.collectAsState()
            HouseExpensesTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                val serverUrl by authViewModel.serverUrl.collectAsState()
                var showServerConfig by remember { mutableStateOf(false) }

                val inviteCodeToJoin by pendingInviteCode
                val currentInitialTab by initialSelectedTab

                val startDestination = if (isLoggedIn) "dashboard" else "login"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        MainContainerScreen(
                            viewModel = expenseViewModel,
                            initialTab = currentInitialTab,
                            initialHouseholdJoinCode = inviteCodeToJoin,
                            onNavigateToAddExpense = {
                                navController.navigate("add_expense?desc=&amount=")
                            },
                            onNavigateToQRScanner = {
                                navController.navigate("qr_scanner")
                            },
                            onOpenServerConfig = {
                                showServerConfig = true
                            },
                            onLogout = {
                                authViewModel.logout {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = "add_expense?desc={desc}&amount={amount}",
                        arguments = listOf(
                            navArgument("desc") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("amount") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val rawDesc = backStackEntry.arguments?.getString("desc") ?: ""
                        val rawAmount = backStackEntry.arguments?.getString("amount") ?: ""
                        val desc = URLDecoder.decode(rawDesc, StandardCharsets.UTF_8.toString())

                        AddExpenseScreen(
                            viewModel = expenseViewModel,
                            initialDescription = desc,
                            initialAmount = rawAmount,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("qr_scanner") {
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        QRScannerScreen(
                            onQRScanned = { description, amount ->
                                val encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8.toString())
                                navController.navigate("add_expense?desc=$encodedDesc&amount=$amount") {
                                    popUpTo("dashboard")
                                }
                            },
                            onSaveDirectly = { description, amount, scope ->
                                expenseViewModel.createExpense(
                                    description = description,
                                    amount = amount,
                                    category = "Supermercado",
                                    scope = scope,
                                    date = today
                                )
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                // Diálogo de Confirmación de Deep Link para Unirse al Hogar
                inviteCodeToJoin?.let { code ->
                    if (isLoggedIn) {
                        AlertDialog(
                            onDismissRequest = { pendingInviteCode.value = null },
                            title = {
                                Text(
                                    text = "Unirse al Hogar",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            text = {
                                Text(
                                    text = "¿Deseas unirte al hogar con el código de invitación $code?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        expenseViewModel.joinHousehold(
                                            inviteCode = code,
                                            onSuccess = {
                                                Toast.makeText(this@MainActivity, "¡Te has unido al hogar exitosamente!", Toast.LENGTH_LONG).show()
                                                initialSelectedTab.value = 3
                                                pendingInviteCode.value = null
                                            },
                                            onError = { err ->
                                                Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                                pendingInviteCode.value = null
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("Unirme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingInviteCode.value = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }

                if (showServerConfig) {
                    ServerConfigDialog(
                        currentUrl = serverUrl,
                        onSave = { newUrl ->
                            authViewModel.updateServerUrl(newUrl)
                            showServerConfig = false
                        },
                        onDismiss = { showServerConfig = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val code = extractInviteCode(intent?.data)
        if (!code.isNullOrBlank()) {
            pendingInviteCode.value = code
            initialSelectedTab.value = 3
        }
    }

    private fun extractInviteCode(uri: Uri?): String? {
        if (uri == null) return null
        // Esquema custom: houseexpenses://household/join?code=XYZ123
        if (uri.scheme == "houseexpenses") {
            return uri.getQueryParameter("code")?.uppercase()
        }
        // Enlaces web: https://houseexpenses.app/join/XYZ123 o https://houseexpenses.app/join?code=XYZ123
        if (uri.scheme == "http" || uri.scheme == "https") {
            val queryCode = uri.getQueryParameter("code")
            if (!queryCode.isNullOrBlank()) {
                return queryCode.uppercase()
            }
            val segments = uri.pathSegments
            val joinIndex = segments.indexOf("join")
            if (joinIndex != -1 && joinIndex + 1 < segments.size) {
                return segments[joinIndex + 1].uppercase()
            }
        }
        return null
    }
}
