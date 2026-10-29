package com.houseexpenses.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.R
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.data.remote.dto.HouseholdMemberDto
import com.houseexpenses.data.remote.dto.MemberSplitDto
import com.houseexpenses.data.remote.dto.CycleSummaryDto
import com.houseexpenses.data.remote.dto.CycleDetailResponse
import com.houseexpenses.ui.components.CopCurrencyVisualTransformation
import com.houseexpenses.ui.components.EditExpenseDialog
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    viewModel: ExpenseViewModel,
    initialJoinCode: String? = null
) {
    val context = LocalContext.current
    val balance by viewModel.householdBalance.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val hasHousehold by viewModel.hasHousehold.collectAsState()
    val inviteCode by viewModel.householdInviteCode.collectAsState()
    val householdName by viewModel.householdName.collectAsState()
    val isHouseholdAdmin by viewModel.isHouseholdAdmin.collectAsState()
    val billingCycleDay by viewModel.billingCycleDay.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val cycles by viewModel.householdCycles.collectAsState()
    val settlementPayments by viewModel.settlementPayments.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showSplitsDialog by remember { mutableStateOf(false) }
    var showCycleDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var memberToEject by remember { mutableStateOf<HouseholdMemberDto?>(null) }

    // Estados para el detalle de ciclo histórico
    var selectedCycleIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCycleDetail by remember { mutableStateOf<CycleDetailResponse?>(null) }
    var isLoadingCycleDetail by remember { mutableStateOf(false) }

    // Estados para Crear / Unirse
    var createNameInput by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf(initialJoinCode ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    val householdExpenses = remember(expenses) {
        expenses.filter { it.scope == "HOUSEHOLD" }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHouseholdBalance()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hasHousehold && householdName.isNotBlank()) householdName else "Hogar & Roomies",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (hasHousehold) {
                        IconButton(onClick = { viewModel.loadHouseholdBalance() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Mensaje de feedback si existe
            if (feedbackMessage != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isErrorMessage) MaterialTheme.colorScheme.errorContainer else EmeraldPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = feedbackMessage!!,
                            color = if (isErrorMessage) MaterialTheme.colorScheme.onErrorContainer else EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (!hasHousehold) {
                // Estado 1: No pertenece a ningún hogar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Modo Personal Activo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Actualmente no estás en un hogar compartido. Puedes crear uno nuevo para tu casa o unirte al de tus roomies o familia.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Tarjeta 1: Crear Nuevo Hogar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Crear Nuevo Hogar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Asigna un nombre a tu hogar (por ejemplo: Apartamento 402 o Casa Familia) y obtén un código para invitar a los demás miembros.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = createNameInput,
                                onValueChange = { createNameInput = it },
                                label = { Text("Nombre del Hogar") },
                                placeholder = { Text("Ej. Apto 402") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (createNameInput.isNotBlank()) {
                                        isSubmitting = true
                                        feedbackMessage = null
                                        viewModel.createHousehold(
                                            name = createNameInput.trim(),
                                            onSuccess = { code ->
                                                isSubmitting = false
                                                feedbackMessage = "¡Hogar creado exitosamente! Código de invitación: $code"
                                                isErrorMessage = false
                                            },
                                            onError = { err ->
                                                isSubmitting = false
                                                feedbackMessage = err
                                                isErrorMessage = true
                                            }
                                        )
                                    }
                                },
                                enabled = createNameInput.isNotBlank() && !isSubmitting,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("Crear Hogar", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Tarjeta 2: Unirse a un Hogar con Código (8 Caracteres)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Unirse con Código",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Si alguien ya creó el hogar, ingresa el código de 8 caracteres que te compartieron para unirte y sincronizar cuentas.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { input ->
                                    if (input.length <= 8) {
                                        joinCodeInput = input.uppercase()
                                    }
                                },
                                label = { Text("Código de Invitación (8 caracteres)") },
                                placeholder = { Text("Ej. 7FB908CB") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        isSubmitting = true
                                        feedbackMessage = null
                                        viewModel.joinHousehold(
                                            inviteCode = joinCodeInput.trim(),
                                            onSuccess = {
                                                isSubmitting = false
                                                feedbackMessage = "¡Te has unido al hogar exitosamente!"
                                                isErrorMessage = false
                                            },
                                            onError = { err ->
                                                isSubmitting = false
                                                feedbackMessage = err
                                                isErrorMessage = true
                                            }
                                        )
                                    }
                                },
                                enabled = joinCodeInput.length == 8 && !isSubmitting,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Text("Unirse al Hogar", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            } else {
                // Estado 2: Ya pertenece a un hogar

                // Tarjeta 1: Información del Hogar y Código de Invitación
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = householdName.ifBlank { "Hogar Compartido" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHouseholdAdmin) "Administrador del Hogar" else "Miembro del Hogar",
                                        fontSize = 12.sp,
                                        color = if (isHouseholdAdmin) PurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isHouseholdAdmin) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isHouseholdAdmin) "ADMIN" else "MIEMBRO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHouseholdAdmin) PurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Código de Invitación
                            if (inviteCode.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CÓDIGO DE INVITACIÓN (8 CARACTERES)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PurpleAccent
                                            )
                                            Text(
                                                text = inviteCode,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 3.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Código Hogar", inviteCode)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "¡Código copiado!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_copy),
                                                    contentDescription = "Copiar Código",
                                                    tint = PurpleAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            FilledTonalIconButton(
                                                onClick = {
                                                    val shareText = "¡Hola! Únete a nuestro hogar en House Expenses para compartir gastos.\n" +
                                                            "Código de invitación: $inviteCode\n" +
                                                            "Enlace directo: https://houseexpenses.app/join/$inviteCode"
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        type = "text/plain"
                                                    }
                                                    val shareIntent = Intent.createChooser(sendIntent, "Invitar al hogar")
                                                    context.startActivity(shareIntent)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "Compartir",
                                                    tint = PurpleAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tarjeta 2: Ciclo de Facturación Configurable
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Ciclo de Facturación",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isHouseholdAdmin) {
                                    TextButton(onClick = { showCycleDialog = true }) {
                                        Text("Configurar", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = "Día de corte mensual: $billingCycleDay de cada mes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Período actual: ${formatCyclePeriod(billingCycleDay)}",
                                    fontSize = 12.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Tarjeta 3: Integrantes del Hogar y Reparto Variable
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Integrantes del Hogar (${members.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isHouseholdAdmin && members.isNotEmpty()) {
                                    TextButton(onClick = { showSplitsDialog = true }) {
                                        Text("Ajustar Reparto", fontSize = 12.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (members.isEmpty()) {
                                Text(
                                    text = "Cargando integrantes...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    members.forEach { m ->
                                        val isMe = m.user_id == viewModel.sessionManager.userId
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (m.isAdmin) PurpleAccent.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = m.displayName.take(1).uppercase(),
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (m.isAdmin) PurpleAccent else EmeraldPrimary,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                    }

                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text(
                                                                text = m.displayName + if (isMe) " (Tú)" else "",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (m.isAdmin) {
                                                                Text(
                                                                    text = "• Admin",
                                                                    fontSize = 10.sp,
                                                                    color = PurpleAccent,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        if (m.displayEmail.isNotBlank()) {
                                                            Text(
                                                                text = m.displayEmail,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Porcentaje asignado
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                    ) {
                                                        Text(
                                                            text = "${"%.1f".format(m.split_percentage)}%",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }

                                                    // Botón de expulsión para Admin
                                                    if (isHouseholdAdmin && !isMe) {
                                                        IconButton(
                                                            onClick = { memberToEject = m },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Expulsar integrante",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tarjeta 4: Balance del Ciclo y Liquidación Detallada (Rediseñada)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Balance del Ciclo Activo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                    if (!balance?.cycle_start.isNullOrBlank() && !balance?.cycle_end.isNullOrBlank()) {
                                        Text(
                                            text = "${balance?.cycle_start} al ${balance?.cycle_end}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Total Acumulado del Hogar (con fallback en caliente si el backend aún no sincroniza)
                            val localTotalHousehold = remember(householdExpenses) { householdExpenses.sumOf { it.amount } }
                            val displayTotalHousehold = if ((balance?.total_household ?: 0.0) > 0.0) {
                                balance!!.total_household
                            } else {
                                localTotalHousehold
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurpleAccent.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "TOTAL GASTOS DEL HOGAR",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent
                                        )
                                        Text(
                                            text = formatCOP.format(displayTotalHousehold),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleAccent.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${members.size} integrantes",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Desglose individual por integrante (con cálculo reactivo si el servidor reporta 0)
                            val memberBalances = remember(balance, householdExpenses, members) {
                                if (balance != null && balance!!.balances.isNotEmpty() && balance!!.total_household > 0.0) {
                                    balance!!.balances
                                } else if (members.isNotEmpty()) {
                                    val total = householdExpenses.sumOf { it.amount }
                                    val count = if (members.isEmpty()) 1 else members.size
                                    members.map { m ->
                                        val paid = householdExpenses.filter { it.userId == m.user_id }.sumOf { it.amount }
                                        val pct = if (m.split_percentage > 0) m.split_percentage else (100.0 / count)
                                        val share = total * (pct / 100.0)
                                        val net = paid - share
                                        com.houseexpenses.data.remote.dto.RoomieBalanceDto(
                                            user_id = m.user_id,
                                            user_name = m.displayName,
                                            split_percentage = pct,
                                            total_paid = paid,
                                            expected_share = share,
                                            net_balance = net
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            }

                            if (memberBalances.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Aportes y Cuotas por Integrante",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    memberBalances.forEach { rb ->
                                        val isMe = rb.user_id == viewModel.sessionManager.userId
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = rb.user_name + if (isMe) " (Tú)" else "",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Pagó: ${formatCOP.format(rb.total_paid)} • Cuota: ${formatCOP.format(rb.expected_share)} (${"%.1f".format(rb.split_percentage)}%)",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Saldo neto (a favor / en contra)
                                                val net = rb.net_balance
                                                val isPositive = net > 0.01
                                                val isNegative = net < -0.01
                                                val badgeBg = when {
                                                    isPositive -> EmeraldPrimary.copy(alpha = 0.15f)
                                                    isNegative -> MaterialTheme.colorScheme.errorContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                                val badgeTextColor = when {
                                                    isPositive -> EmeraldPrimary
                                                    isNegative -> MaterialTheme.colorScheme.onErrorContainer
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                val badgeText = when {
                                                    isPositive -> "+${formatCOP.format(net)} a favor"
                                                    isNegative -> "-${formatCOP.format(abs(net))} debe"
                                                    else -> "Al día"
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeBg
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Resumen de liquidación directa (a quién se le debe dar el dinero para cuadrar)
                            val myUserId = viewModel.sessionManager.userId
                            val myBal = memberBalances.find { it.user_id == myUserId }
                            val myNet = myBal?.net_balance ?: 0.0

                            val dynamicSettlement = remember(myNet, memberBalances, balance) {
                                if (myNet < -0.01) {
                                    val creditor = memberBalances.filter { it.user_id != myUserId }.maxByOrNull { it.net_balance }
                                    val creditorName = creditor?.user_name ?: "tu roomie"
                                    "Debes pagar ${formatCOP.format(abs(myNet))} a $creditorName para cuadrar cuentas."
                                } else if (myNet > 0.01) {
                                    val debtor = memberBalances.filter { it.user_id != myUserId }.minByOrNull { it.net_balance }
                                    val debtorName = debtor?.user_name ?: "Tu roomie"
                                    "$debtorName debe pagarte ${formatCOP.format(myNet)} a ti para cuadrar cuentas."
                                } else {
                                    if (balance?.settlement.isNullOrBlank()) {
                                        "Las cuentas están equilibradas. ¡Nadie se debe nada!"
                                    } else {
                                        balance!!.settlement
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "LIQUIDACIÓN SUGERIDA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dynamicSettlement,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón Registrar Pago / Transferencia
                item {
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text(
                            text = "Registrar Pago / Transferencia",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                // Sección: Registro de Pagos y Liquidaciones del Hogar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Registro de Pagos y Liquidaciones",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (settlementPayments.isNotEmpty()) {
                                    Text(
                                        text = "${settlementPayments.size} pagos",
                                        fontSize = 12.sp,
                                        color = PurpleAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (settlementPayments.isEmpty()) {
                                Text(
                                    text = "Aún no hay pagos o transferencias de liquidación registrados en este hogar.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    settlementPayments.forEach { payment ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val payerLabel = if (payment.payerName.isNotBlank()) payment.payerName else "Integrante"
                                                    val receiverLabel = if (payment.receiverName.isNotBlank()) payment.receiverName else "Integrante"
                                                    Text(
                                                        text = "$payerLabel ➔ $receiverLabel",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${payment.channel} • ${payment.date}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (payment.notes.isNotBlank()) {
                                                        Text(
                                                            text = payment.notes,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = formatCOP.format(payment.amount),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurpleAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tarjeta 5: Historial de Ciclos Anteriores (Mes a Mes) con Detalle al Tocar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Historial de Ciclos Mensuales",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (cycles.isEmpty()) {
                                Text(
                                    text = "Aún no se han generado ciclos anteriores.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    cycles.forEach { c ->
                                        Card(
                                            onClick = {
                                                selectedCycleIndex = c.cycle_index
                                                isLoadingCycleDetail = true
                                                viewModel.getCycleDetail(
                                                    cycleIndex = c.cycle_index,
                                                    onSuccess = { detail ->
                                                        selectedCycleDetail = detail
                                                        isLoadingCycleDetail = false
                                                    },
                                                    onError = { err ->
                                                        isLoadingCycleDetail = false
                                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${c.start_date} al ${c.end_date}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Total: ${formatCOP.format(c.total_amount)} • Toca para ver detalle",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                val isSettled = c.status == "SETTLED"
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSettled) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFFFA000).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (isSettled) "Saldado" else "Pendiente",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSettled) EmeraldPrimary else Color(0xFFFFA000),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // Lista de Gastos Compartidos del Hogar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gastos del Hogar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toca tus gastos para editar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (householdExpenses.isEmpty()) {
                    item {
                        Text(
                            text = "No hay gastos del hogar registrados.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(householdExpenses) { exp ->
                        val isMe = exp.userId == viewModel.sessionManager.userId
                        val payerMember = members.find { it.user_id == exp.userId }
                        val payerName = if (isMe) "Tú" else (payerMember?.displayName ?: "Roomie")

                        Card(
                            onClick = { if (isMe) expenseToEdit = exp },
                            enabled = isMe,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isMe) EmeraldPrimary.copy(alpha = 0.15f) else PurpleAccent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isMe) "Pagado por: Tú" else "Pagado por: $payerName",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) EmeraldPrimary else PurpleAccent,
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

                // Sección de Gestión y Salida del Hogar
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isHouseholdAdmin) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar Hogar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showLeaveConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abandonar Hogar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal de Pago / Transferencia
    if (showPaymentDialog) {
        val currentUserId = viewModel.sessionManager.userId
        val userNet = balance?.getUserNet(currentUserId) ?: 0.0
        val suggestedAmount = if (userNet < 0) abs(userNet) else 0.0
        val otherMembers = members.filter { it.user_id != currentUserId }
        val topCreditor = balance?.balances?.filter { it.user_id != currentUserId && it.net_balance > 0.01 }
            ?.maxByOrNull { it.net_balance }?.user_id
            ?: otherMembers.firstOrNull()?.user_id

        SettlementPaymentDialog(
            members = otherMembers,
            defaultReceiverId = topCreditor,
            defaultAmount = suggestedAmount,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { receiverUserId, channel, amount, notes ->
                viewModel.createSettlementPayment(
                    receiverUserId = receiverUserId,
                    amount = amount,
                    channel = channel,
                    notes = notes,
                    onSuccess = {
                        feedbackMessage = "¡Pago de liquidación registrado correctamente!"
                        isErrorMessage = false
                    },
                    onError = { err ->
                        feedbackMessage = err
                        isErrorMessage = true
                    }
                )
                showPaymentDialog = false
            }
        )
    }

    // Modal de Edición de Gastos del Hogar
    expenseToEdit?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            hasActiveHousehold = true,
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

    // Modal para Ajustar Reparto (Admin)
    if (showSplitsDialog) {
        AdjustSplitsDialog(
            members = members,
            onDismiss = { showSplitsDialog = false },
            onConfirm = { splits ->
                viewModel.updateSplits(
                    splits = splits,
                    onSuccess = {
                        showSplitsDialog = false
                        Toast.makeText(context, "¡Reparto actualizado con éxito!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Modal para Configurar Día de Corte de Facturación (Admin)
    if (showCycleDialog) {
        UpdateBillingCycleDialog(
            currentDay = billingCycleDay,
            onDismiss = { showCycleDialog = false },
            onConfirm = { newDay ->
                viewModel.updateBillingCycle(
                    day = newDay,
                    onSuccess = {
                        showCycleDialog = false
                        Toast.makeText(context, "¡Día de corte actualizado al día $newDay!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Modal Confirmación Expulsión de Miembro (Admin)
    if (memberToEject != null) {
        AlertDialog(
            onDismissRequest = { memberToEject = null },
            title = { Text("¿Expulsar del hogar?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Estás seguro de que deseas expulsar a ${memberToEject?.displayName} del hogar? Los porcentajes de reparto se rebalancearán automáticamente entre los integrantes restantes.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = memberToEject?.user_id
                        memberToEject = null
                        if (targetId != null) {
                            viewModel.removeMember(
                                userId = targetId,
                                onSuccess = {
                                    Toast.makeText(context, "Miembro expulsado y porcentajes rebalanceados.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Expulsar", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEject = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Confirmación Eliminar Hogar
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("¿Eliminar este hogar?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta acción eliminará el hogar y desvinculará a todos los miembros. Los gastos registrados no se perderán pero volverán a ser personales.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteHousehold(
                            onSuccess = {
                                Toast.makeText(context, "Hogar eliminado.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Confirmación Abandonar Hogar
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("¿Abandonar el hogar?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Saldrás de este hogar compartido y tu cuenta volverá al modo personal independiente.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        viewModel.leaveHousehold(
                            onSuccess = {
                                Toast.makeText(context, "Has abandonado el hogar.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Abandonar", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // BottomSheet para Detalle del Ciclo Seleccionado
    if (selectedCycleIndex != null) {
        CycleDetailBottomSheet(
            cycleIndex = selectedCycleIndex!!,
            isLoading = isLoadingCycleDetail,
            detail = selectedCycleDetail,
            onDismiss = {
                selectedCycleIndex = null
                selectedCycleDetail = null
            }
        )
    }
}

fun formatCyclePeriod(cycleDay: Int): String {
    val day = if (cycleDay < 1) 1 else if (cycleDay > 31) 31 else cycleDay
    val now = Calendar.getInstance()
    val today = now.get(Calendar.DAY_OF_MONTH)

    val startCal = Calendar.getInstance()
    if (today < day) {
        startCal.add(Calendar.MONTH, -1)
    }
    val maxStartDay = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    startCal.set(Calendar.DAY_OF_MONTH, minOf(day, maxStartDay))

    val endCal = startCal.clone() as Calendar
    endCal.add(Calendar.MONTH, 1)
    val maxEndDay = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    endCal.set(Calendar.DAY_OF_MONTH, minOf(day, maxEndDay))
    endCal.add(Calendar.DAY_OF_MONTH, -1)

    val sdf = SimpleDateFormat("d 'de' MMMM", Locale("es", "CO"))
    return "${sdf.format(startCal.time)} al ${sdf.format(endCal.time)}"
}

@Composable
fun AdjustSplitsDialog(
    members: List<HouseholdMemberDto>,
    onDismiss: () -> Unit,
    onConfirm: (List<MemberSplitDto>) -> Unit
) {
    val splitsMap = remember {
        mutableStateMapOf<String, String>().apply {
            members.forEach { m ->
                put(m.user_id, "%.1f".format(Locale.US, m.split_percentage))
            }
        }
    }

    val sum = splitsMap.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val isValid = abs(sum - 100.0) < 0.05

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajustar Reparto de Gastos", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Configura el porcentaje de gastos que asume cada miembro. La suma total debe ser exactamente 100%.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val count = members.size
                        if (count > 0) {
                            val equalShare = 100.0 / count
                            members.forEachIndexed { index, m ->
                                val pct = if (index == 0) {
                                    100.0 - (equalShare * (count - 1))
                                } else {
                                    equalShare
                                }
                                splitsMap[m.user_id] = "%.1f".format(Locale.US, pct)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Dividir en partes iguales", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                members.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = m.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = splitsMap[m.user_id] ?: "0",
                            onValueChange = { input ->
                                splitsMap[m.user_id] = input.filter { it.isDigit() || it == '.' }
                            },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isValid) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isValid) "Total: 100.0% ✓" else "Total: ${"%.1f".format(sum)}% (debe sumar 100%)",
                        color = if (isValid) EmeraldPrimary else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val list = members.map { m ->
                            MemberSplitDto(
                                user_id = m.user_id,
                                split_percentage = splitsMap[m.user_id]?.toDoubleOrNull() ?: 0.0
                            )
                        }
                        onConfirm(list)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Guardar Reparto", color = Color.White, fontWeight = FontWeight.Bold)
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
fun UpdateBillingCycleDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dayInput by remember { mutableStateOf(currentDay.toString()) }
    val dayVal = dayInput.toIntOrNull()
    val isValid = dayVal != null && dayVal in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Día de Corte Mensual", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Elige el día del mes en que inicia y finaliza el ciclo de gastos compartidos (ejemplo: día 29 de cada mes).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(2)
                        dayInput = digits
                    },
                    label = { Text("Día de corte (1 - 31)") },
                    placeholder = { Text("Ej. 29") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = !isValid && dayInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isValid) {
                    Text(
                        text = "El ciclo abarcará del día $dayVal al día ${if (dayVal == 1) 31 else dayVal - 1} del mes siguiente.",
                        fontSize = 11.sp,
                        color = EmeraldPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(dayVal!!)
                    }
                },
                enabled = isValid,
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
fun SettlementPaymentDialog(
    members: List<HouseholdMemberDto>,
    defaultReceiverId: String?,
    defaultAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (receiverUserId: String, channel: String, amount: Double, notes: String) -> Unit
) {
    val channels = listOf("Transferencia", "Efectivo")
    var selectedChannel by remember { mutableStateOf(channels.first()) }
    var selectedReceiverId by remember {
        mutableStateOf(defaultReceiverId ?: members.firstOrNull()?.user_id ?: "")
    }
    var amountDigits by remember {
        mutableStateOf(if (defaultAmount > 0) defaultAmount.toLong().toString() else "")
    }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Pago / Liquidación",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "¿A quién le vas a pagar?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (members.isEmpty()) {
                    Text(
                        text = "No hay otros integrantes en el hogar para transferir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEach { m ->
                            val isSelected = selectedReceiverId == m.user_id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReceiverId = m.user_id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedReceiverId = m.user_id },
                                        colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = m.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (m.displayEmail.isNotBlank()) {
                                            Text(
                                                text = m.displayEmail,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Medio o Canal de Pago",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    channels.forEach { channel ->
                        val isSelected = selectedChannel == channel
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedChannel = channel },
                            label = { Text(channel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                    },
                    visualTransformation = CopCurrencyVisualTransformation(),
                    label = { Text("Monto a Saldar") },
                    placeholder = { Text("$ 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota o referencia (opcional)") },
                    placeholder = { Text("Ej. Liquidación de mercado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amountVal = amountDigits.toDoubleOrNull() ?: 0.0
            val canSubmit = amountVal > 0 && selectedReceiverId.isNotBlank()
            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(selectedReceiverId, selectedChannel, amountVal, notes.trim())
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Confirmar Pago", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailBottomSheet(
    cycleIndex: Int,
    isLoading: Boolean,
    detail: CycleDetailResponse?,
    onDismiss: () -> Unit
) {
    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (detail != null) "${detail.start_date} al ${detail.end_date}" else "Detalle del Ciclo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (detail != null) {
                        Text(
                            text = "Historial y balance del ciclo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (detail != null) {
                    val isSettled = detail.status == "SETTLED"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSettled) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFFFA000).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isSettled) "Saldado" else "Pendiente",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSettled) EmeraldPrimary else Color(0xFFFFA000),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PurpleAccent)
                }
            } else if (detail != null) {
                // Total
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurpleAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL GASTOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                        Text(
                            text = formatCOP.format(detail.total_amount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Liquidación
                if (detail.settlement.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ESTADO DE LIQUIDACIÓN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detail.settlement,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Balances por integrante
                if (detail.balances.isNotEmpty()) {
                    Text(
                        text = "Aportes de los Integrantes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        detail.balances.forEach { rb ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = rb.user_name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Pagó: ${formatCOP.format(rb.total_paid)} (Cuota: ${formatCOP.format(rb.expected_share)})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val net = rb.net_balance
                                val isPositive = net > 0.01
                                val isNegative = net < -0.01
                                val badgeColor = when {
                                    isPositive -> EmeraldPrimary
                                    isNegative -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = when {
                                        isPositive -> "+${formatCOP.format(net)}"
                                        isNegative -> "-${formatCOP.format(abs(net))}"
                                        else -> "$0"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }

                // Gastos del ciclo
                Text(
                    text = "Gastos Registrados (${detail.expenses.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (detail.expenses.isEmpty()) {
                    Text(
                        text = "No hubo gastos registrados en este ciclo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        detail.expenses.forEach { exp ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exp.description,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${exp.category} • Pagó: ${exp.payer_name} • ${exp.date}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatCOP.format(exp.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
