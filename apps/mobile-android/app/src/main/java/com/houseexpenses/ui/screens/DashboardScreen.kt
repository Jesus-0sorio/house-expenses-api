package com.houseexpenses.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.R
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.ui.components.EditExpenseDialog
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onOpenServerConfig: () -> Unit,
    onLogout: () -> Unit
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val balance by viewModel.householdBalance.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val householdName by viewModel.householdName.collectAsState()
    val hasActiveHousehold = !viewModel.sessionManager.activeHouseholdId.isNullOrBlank()

    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadHouseholdBalance()
    }

    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    val currentUserId = viewModel.sessionManager.userId

    val currentMonthKey = remember {
        java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(java.util.Date())
    }

    val currentMonthName = remember {
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", Locale("es", "CO"))
        sdf.format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }

    val currentMonthExpenses = remember(expenses, currentMonthKey) {
        expenses.filter { it.date.startsWith(currentMonthKey) }
    }

    val currentMonthTotal = remember(currentMonthExpenses) {
        currentMonthExpenses.sumOf { it.amount }
    }

    val currentMonthPersonal = remember(currentMonthExpenses, currentUserId) {
        currentMonthExpenses.filter { it.userId == currentUserId && it.scope == "PERSONAL" }.sumOf { it.amount }
    }

    val currentMonthHousehold = remember(currentMonthExpenses) {
        currentMonthExpenses.filter { it.scope == "HOUSEHOLD" }.sumOf { it.amount }
    }

    val myMonthExpenses = remember(expenses, currentMonthKey, currentUserId) {
        expenses.filter {
            it.userId == currentUserId && it.date.startsWith(currentMonthKey)
        }.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hola, ${viewModel.sessionManager.userName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_theme_toggle),
                            contentDescription = "Cambiar Tema",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onOpenServerConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar Servidor", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Tarjeta de Total Gastos del Mes y Desglose
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Gastos de $currentMonthName",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCOP.format(currentMonthTotal),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Gastos Personales",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCOP.format(currentMonthPersonal),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Gastos del Hogar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCOP.format(currentMonthHousehold),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (pendingSync > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⏳ $pendingSync pendiente(s) de sincronización",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Tarjeta de Balance 50/50 Hogar
            if (balance != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Balance del Hogar",
                                    fontSize = 12.sp,
                                    color = PurpleAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.Home, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val settlementRaw = balance?.settlement?.ifBlank { "Cuentas al día" } ?: "Cuentas al día"
                            val settlementFormatted = formatSettlementCop(settlementRaw, formatCOP)
                            Text(
                                text = settlementFormatted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val myPaid = balance?.getUserPaid(currentUserId) ?: 0.0
                            val totalHousehold = balance?.total_household ?: 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tú has pagado: ${formatCOP.format(myPaid)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Total hogar: ${formatCOP.format(totalHousehold)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Transacciones del Mes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transacciones del Mes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "(Toca para editar)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (myMonthExpenses.isEmpty()) {
                item {
                    Text(
                        text = "No tienes gastos registrados en este mes. Presiona + para registrar uno.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(myMonthExpenses.take(15)) { exp ->
                    Card(
                        onClick = { expenseToEdit = exp },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (exp.scope == "HOUSEHOLD") PurpleAccent.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (exp.scope == "HOUSEHOLD") "Hogar" else "Personal",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (exp.scope == "HOUSEHOLD") PurpleAccent else EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = exp.category,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "• ${exp.date.take(10)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = formatCOP.format(exp.amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialogo de Edicion de Gasto
    expenseToEdit?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            hasActiveHousehold = hasActiveHousehold,
            householdName = householdName,
            onDismiss = { expenseToEdit = null },
            onConfirm = { desc, amount, cat, scope ->
                viewModel.updateExpense(
                    id = exp.id,
                    description = desc,
                    amount = amount,
                    category = cat,
                    scope = scope,
                    date = exp.date
                )
                expenseToEdit = null
            },
            onDelete = {
                viewModel.deleteExpense(exp.id)
                expenseToEdit = null
            }
        )
    }
}

private fun formatSettlementCop(raw: String, formatCOP: NumberFormat): String {
    if (raw.isBlank()) return "Cuentas al día"
    val regex = Regex("""\$([0-9]+(?:\.[0-9]+)?)(?:\s*COP)?""")
    return regex.replace(raw) { matchResult ->
        val num = matchResult.groupValues[1].toDoubleOrNull() ?: 0.0
        formatCOP.format(num)
    }
}
