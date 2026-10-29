package com.houseexpenses.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: ExpenseEntity,
    hasActiveHousehold: Boolean,
    householdName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String, scope: String) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember { mutableStateOf(expense.description) }
    var amountDigits by remember {
        mutableStateOf(expense.amount.toLong().toString())
    }
    var category by remember { mutableStateOf(expense.category) }
    var scope by remember { mutableStateOf(expense.scope) }
    val householdLabel = if (householdName.isNotBlank()) householdName else "Hogar Compartido"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editar Gasto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Monto
                Column {
                    Text(
                        text = "Monto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountDigits,
                        onValueChange = { input ->
                            amountDigits = input.filter { it.isDigit() }.take(12)
                        },
                        visualTransformation = CopCurrencyVisualTransformation(),
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

                // Descripcion
                Column {
                    Text(
                        text = "Descripción",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
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

                // Categoria
                CategoryChipSelector(
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )

                // Ambito (si tiene hogar activo)
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
                        FilterChip(
                            selected = scope == "PERSONAL",
                            onClick = { scope = "PERSONAL" },
                            label = {
                                Text(
                                    "Personal",
                                    fontWeight = if (scope == "PERSONAL") FontWeight.Bold else FontWeight.Normal,
                                    color = if (scope == "PERSONAL") Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                selectedContainerColor = EmeraldPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = scope == "PERSONAL",
                                borderColor = if (scope == "PERSONAL") EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = EmeraldPrimary,
                                borderWidth = 1.dp
                            )
                        )
                        FilterChip(
                            selected = scope == "HOUSEHOLD",
                            onClick = { scope = "HOUSEHOLD" },
                            label = {
                                Text(
                                    householdLabel,
                                    fontWeight = if (scope == "HOUSEHOLD") FontWeight.Bold else FontWeight.Normal,
                                    color = if (scope == "HOUSEHOLD") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                selectedContainerColor = PurpleAccent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = scope == "HOUSEHOLD",
                                borderColor = if (scope == "HOUSEHOLD") PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = PurpleAccent,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val isValid = description.isNotBlank() && amountVal > 0
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(description.trim(), amountVal, category, scope)
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = EmeraldPrimary.copy(alpha = 0.4f)
                )
            ) {
                Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
