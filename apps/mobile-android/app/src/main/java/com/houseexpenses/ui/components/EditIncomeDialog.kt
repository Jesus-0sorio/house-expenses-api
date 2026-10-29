package com.houseexpenses.ui.components

import androidx.compose.foundation.layout.*
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
import com.houseexpenses.data.local.entity.IncomeEntity
import com.houseexpenses.ui.theme.EmeraldPrimary

@Composable
fun EditIncomeDialog(
    income: IncomeEntity,
    onDismiss: () -> Unit,
    onConfirm: (source: String, amount: Double, type: String) -> Unit,
    onDelete: () -> Unit
) {
    var source by remember { mutableStateOf(income.description) }
    var amountDigits by remember {
        mutableStateOf(income.amount.toLong().toString())
    }
    var selectedType by remember { mutableStateOf(income.type) }

    val incomeTypes = listOf(
        "QUINCENAL" to "Quincenal",
        "MENSUAL" to "Mensual",
        "EXTRAORDINARIO" to "Extraordinario (Prima/Bono)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editar Ingreso", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Concepto o Fuente") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frecuencia o Tipo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

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
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val isValid = source.isNotBlank() && amountVal > 0
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(source.trim(), amountVal, selectedType)
                    }
                },
                enabled = isValid,
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
