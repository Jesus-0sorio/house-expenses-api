package com.houseexpenses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.ui.components.CategoryChipSelector
import com.houseexpenses.ui.components.CopCurrencyVisualTransformation
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    initialDescription: String = "",
    initialAmount: String = "",
    onNavigateBack: () -> Unit
) {
    var description by remember { mutableStateOf(initialDescription) }
    var amountDigits by remember { mutableStateOf(initialAmount.filter { it.isDigit() }) }
    var category by remember { mutableStateOf("Supermercado") }
    val hasActiveHousehold = !viewModel.sessionManager.activeHouseholdId.isNullOrBlank()
    val householdName by viewModel.householdName.collectAsState()
    var scope by remember { mutableStateOf(if (hasActiveHousehold) "HOUSEHOLD" else "PERSONAL") }

    val currentDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var isCustomDateEnabled by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf(currentDate) }

    var isRecurring by remember { mutableStateOf(false) }
    var dueDayStr by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Gasto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Monto Input
            Column {
                Text(
                    text = "Monto",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                    },
                    visualTransformation = CopCurrencyVisualTransformation(),
                    placeholder = { Text("$ 0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Descripcion Input
            Column {
                Text(
                    text = "Descripción",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Ej. Mercado semanal D1 o Éxito", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Opción de Fecha Personalizada
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isCustomDateEnabled,
                        onCheckedChange = { isCustomDateEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "¿Gasto con fecha anterior o diferente a hoy?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isCustomDateEnabled) {
                    OutlinedTextField(
                        value = customDate,
                        onValueChange = { customDate = it },
                        label = { Text("Fecha del Gasto (YYYY-MM-DD)") },
                        placeholder = { Text("Ej. 2026-08-28") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Selector de Ambito (Solo si tiene Hogar Activo)
            if (hasActiveHousehold) {
                val householdLabel = if (householdName.isNotBlank()) householdName else "Hogar Compartido"
                Column {
                    Text(
                        text = "Tipo de Cuenta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { scope = "PERSONAL" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "PERSONAL") EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                "Personal",
                                fontWeight = if (scope == "PERSONAL") FontWeight.Bold else FontWeight.Normal,
                                color = if (scope == "PERSONAL") Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { scope = "HOUSEHOLD" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scope == "HOUSEHOLD") PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                householdLabel,
                                fontWeight = if (scope == "HOUSEHOLD") FontWeight.Bold else FontWeight.Normal,
                                color = if (scope == "HOUSEHOLD") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Selector de Categorias
            CategoryChipSelector(
                selectedCategory = category,
                onCategorySelected = { category = it }
            )

            // Opción de Gasto Recurrente Mensual
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Es un gasto recurrente mensual?",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Arriendo, suscripción, servicio fijo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = dueDayStr,
                            onValueChange = { input ->
                                dueDayStr = input.filter { it.isDigit() }.take(2)
                            },
                            label = { Text("Día límite de pago cada mes (1 - 31)") },
                            placeholder = { Text("Ej: 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boton de Guardado
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val isValid = description.isNotBlank() && amountVal > 0

            Button(
                onClick = {
                    if (isValid) {
                        val finalDate = if (isCustomDateEnabled && customDate.isNotBlank()) customDate.trim() else currentDate
                        viewModel.createExpense(
                            description = description.trim(),
                            amount = amountVal,
                            category = category,
                            scope = scope,
                            date = finalDate
                        )
                        if (isRecurring) {
                            val dueDay = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 1
                            viewModel.createRecurringDebt(
                                name = description.trim(),
                                amount = amountVal,
                                dueDay = dueDay,
                                scope = scope
                            )
                        }
                        onNavigateBack()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = EmeraldPrimary.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "Guardar Gasto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
