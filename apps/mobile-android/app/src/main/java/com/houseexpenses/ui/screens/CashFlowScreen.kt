package com.houseexpenses.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.data.local.entity.IncomeEntity
import com.houseexpenses.data.remote.dto.RecurringDebtDto
import com.houseexpenses.ui.components.CopCurrencyVisualTransformation
import com.houseexpenses.ui.components.EditIncomeDialog
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val incomes by viewModel.allIncomes.collectAsState()

    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<IncomeEntity?>(null) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<RecurringDebtDto?>(null) }

    val recurringDebts by viewModel.recurringDebts.collectAsState()
    val hasActiveHousehold = !viewModel.sessionManager.activeHouseholdId.isNullOrBlank()

    LaunchedEffect(Unit) {
        viewModel.loadRecurringDebts()
    }

    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Gastos individuales de hoy
    val todayExpenses = remember(expenses, todayDate) {
        expenses.filter {
            val d = if (it.date.length >= 10) it.date.substring(0, 10) else it.date
            d == todayDate
        }.sortedByDescending { it.id }
    }

    // Calculo de gastos de hoy
    val spentToday = remember(todayExpenses) {
        todayExpenses.sumOf { it.amount }
    }

    // Calculo total de ingresos y gastos
    val totalIncomes = remember(incomes) {
        incomes.sumOf { it.amount }
    }

    val totalExpenses = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    val availableBalance = totalIncomes - totalExpenses

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Flujo de Caja",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Tarjeta Principal de Disponible y Gastado Hoy
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Disponible en este Ciclo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatCOP.format(availableBalance.coerceAtLeast(0.0)),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (availableBalance >= 0) EmeraldPrimary else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Gastado Hoy",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCOP.format(spentToday),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Ingresos",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCOP.format(totalIncomes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Boton para registrar Ingreso
            item {
                Button(
                    onClick = { showAddIncomeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Registrar Ingreso (Quincenal / Extra)",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Lista de Ingresos Registrados
            if (incomes.isNotEmpty()) {
                item {
                    Text(
                        text = "Tus Fuentes de Ingreso (Toca para editar)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(incomes) { inc ->
                    Card(
                        onClick = { incomeToEdit = inc },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = inc.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${inc.type} • ${inc.date}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "+ " + formatCOP.format(inc.amount),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Sección de Compromisos Recurrentes y Suscripciones
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compromisos Recurrentes y Suscripciones",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showAddDebtDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar Compromiso", tint = EmeraldPrimary)
                    }
                }
            }

            if (recurringDebts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Sin compromisos fijos registrados.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Registra aquí arriendos, cuotas fijas o suscripciones como Netflix y servicios.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recurringDebts, key = { it.id }) { debt ->
                    Card(
                        onClick = { debtToEdit = debt },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (debt.scope == "HOUSEHOLD") PurpleAccent.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (debt.scope == "HOUSEHOLD") "Hogar" else "Personal",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (debt.scope == "HOUSEHOLD") PurpleAccent else EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Corte día ${debt.due_day} de cada mes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCOP.format(debt.monthly_amount),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Sección de Gastos de Hoy
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gastos de Hoy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (todayExpenses.isNotEmpty()) {
                        Text(
                            text = formatCOP.format(spentToday),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (todayExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No has realizado gastos hoy.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¡Excelente control financiero para el día de hoy!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todayExpenses, key = { it.id }) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
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
                                        text = "${exp.category} • ${exp.date.take(10)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCOP.format(exp.amount),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
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

    // Modal para Registrar Ingreso
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onSave = { source, amount, type ->
                viewModel.createIncome(
                    source = source,
                    amount = amount,
                    type = type,
                    date = todayDate
                )
                showAddIncomeDialog = false
            }
        )
    }

    // Modal para Editar Ingreso
    incomeToEdit?.let { inc ->
        EditIncomeDialog(
            income = inc,
            onDismiss = { incomeToEdit = null },
            onConfirm = { source, amount, type ->
                viewModel.updateIncome(
                    id = inc.id,
                    source = source,
                    amount = amount,
                    type = type,
                    date = inc.date
                )
                incomeToEdit = null
            },
            onDelete = {
                viewModel.deleteIncome(inc.id)
                incomeToEdit = null
            }
        )
    }

    // Modal para Registrar Compromiso Recurrente
    if (showAddDebtDialog) {
        AddRecurringDebtDialog(
            hasActiveHousehold = hasActiveHousehold,
            onDismiss = { showAddDebtDialog = false },
            onSave = { name, amount, dueDay, scope ->
                viewModel.createRecurringDebt(
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    scope = scope
                )
                showAddDebtDialog = false
            }
        )
    }

    // Modal para Editar Compromiso Recurrente
    debtToEdit?.let { debt ->
        EditRecurringDebtDialog(
            debt = debt,
            hasActiveHousehold = hasActiveHousehold,
            onDismiss = { debtToEdit = null },
            onConfirm = { name, amount, dueDay, scope ->
                viewModel.updateRecurringDebt(
                    id = debt.id,
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    scope = scope
                )
                debtToEdit = null
            },
            onDelete = {
                viewModel.deleteRecurringDebt(debt.id)
                debtToEdit = null
            }
        )
    }
}

@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onSave: (source: String, amount: Double, type: String) -> Unit
) {
    var source by remember { mutableStateOf("") }
    var amountDigits by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("QUINCENAL") }

    val incomeTypes = listOf(
        "QUINCENAL" to "Quincenal",
        "MENSUAL" to "Mensual",
        "EXTRAORDINARIO" to "Extraordinario (Prima/Bono)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Ingreso",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Concepto o Fuente") },
                    placeholder = { Text("Ej. Salario Empresa o Prima Junio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                    },
                    visualTransformation = CopCurrencyVisualTransformation(),
                    label = { Text("Monto") },
                    placeholder = { Text("$ 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Frecuencia o Tipo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    incomeTypes.forEach { (typeId, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedType == typeId,
                                onClick = { selectedType = typeId }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (source.isNotBlank() && amountVal > 0) {
                        onSave(source.trim(), amountVal, selectedType)
                    }
                },
                enabled = source.isNotBlank() && amountVal > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddRecurringDebtDialog(
    hasActiveHousehold: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, dueDay: Int, scope: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountDigits by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("1") }
    var scope by remember { mutableStateOf("PERSONAL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Compromiso o Suscripción",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o Concepto") },
                    placeholder = { Text("Ej. Arriendo, Netflix, Fibra Óptica") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                    },
                    visualTransformation = CopCurrencyVisualTransformation(),
                    label = { Text("Monto Mensual") },
                    placeholder = { Text("$ 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { input ->
                        dueDayStr = input.filter { it.isDigit() }.take(2)
                    },
                    label = { Text("Día límite de pago (1 - 31)") },
                    placeholder = { Text("Ej. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasActiveHousehold) {
                    Text(
                        text = "Ámbito",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope = "PERSONAL" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "PERSONAL") EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Personal",
                                color = if (scope == "PERSONAL") Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (scope == "PERSONAL") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Button(
                            onClick = { scope = "HOUSEHOLD" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "HOUSEHOLD") PurpleAccent else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Hogar",
                                color = if (scope == "HOUSEHOLD") Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (scope == "HOUSEHOLD") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val dueDayVal = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 1
            Button(
                onClick = {
                    if (name.isNotBlank() && amountVal > 0) {
                        onSave(name.trim(), amountVal, dueDayVal, scope)
                    }
                },
                enabled = name.isNotBlank() && amountVal > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditRecurringDebtDialog(
    debt: RecurringDebtDto,
    hasActiveHousehold: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, dueDay: Int, scope: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(debt.name) }
    var amountDigits by remember { mutableStateOf(debt.monthly_amount.toLong().toString()) }
    var dueDayStr by remember { mutableStateOf(debt.due_day.toString()) }
    var scope by remember { mutableStateOf(debt.scope) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar compromiso?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción eliminará '${debt.name}' de tus compromisos recurrentes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editar Compromiso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Eliminar", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o Concepto") },
                    placeholder = { Text("Ej. Arriendo, Netflix, Fibra Óptica") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                    },
                    visualTransformation = CopCurrencyVisualTransformation(),
                    label = { Text("Monto Mensual") },
                    placeholder = { Text("$ 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { input ->
                        dueDayStr = input.filter { it.isDigit() }.take(2)
                    },
                    label = { Text("Día límite de pago (1 - 31)") },
                    placeholder = { Text("Ej. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasActiveHousehold) {
                    Text(
                        text = "Ámbito",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope = "PERSONAL" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "PERSONAL") EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Personal",
                                color = if (scope == "PERSONAL") Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (scope == "PERSONAL") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Button(
                            onClick = { scope = "HOUSEHOLD" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "HOUSEHOLD") PurpleAccent else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Hogar",
                                color = if (scope == "HOUSEHOLD") Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (scope == "HOUSEHOLD") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val dueDayVal = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 1
            Button(
                onClick = {
                    if (name.isNotBlank() && amountVal > 0) {
                        onConfirm(name.trim(), amountVal, dueDayVal, scope)
                    }
                },
                enabled = name.isNotBlank() && amountVal > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

