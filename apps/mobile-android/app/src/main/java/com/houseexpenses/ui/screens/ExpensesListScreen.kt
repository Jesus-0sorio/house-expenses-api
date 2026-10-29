package com.houseexpenses.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
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
import com.houseexpenses.ui.components.CopCurrencyVisualTransformation
import com.houseexpenses.ui.components.EditExpenseDialog
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import com.houseexpenses.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private data class DayExpensesGroup(
    val dayKey: String,
    val total: Double,
    val expenses: List<ExpenseEntity>
)

private data class MonthExpensesGroup(
    val monthKey: String,
    val total: Double,
    val days: List<DayExpensesGroup>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    viewModel: ExpenseViewModel
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val householdName by viewModel.householdName.collectAsState()
    val hasActiveHousehold = !viewModel.sessionManager.activeHouseholdId.isNullOrBlank()
    val currentUserId = viewModel.sessionManager.userId

    LaunchedEffect(Unit) {
        viewModel.loadHouseholdBalance()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedScopeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedMonthFilter by remember { mutableStateOf<String?>(null) }
    var startDateFilter by remember { mutableStateOf("") }
    var endDateFilter by remember { mutableStateOf("") }
    var minAmountFilter by remember { mutableStateOf("") }
    var maxAmountFilter by remember { mutableStateOf("") }

    var showAdvancedFiltersSheet by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Paginación reactiva / scroll infinito
    var displayedCount by remember { mutableStateOf(25) }
    val lazyListState = rememberLazyListState()

    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    val prevMonthKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    val categories = listOf(
        "Supermercado", "Restaurantes", "Servicios", "Transporte",
        "Salud", "Entretenimiento", "Hogar", "Otros"
    )

    // Contar filtros activos
    val activeFiltersCount = remember(
        selectedCategoryFilter, selectedMonthFilter,
        startDateFilter, endDateFilter, minAmountFilter, maxAmountFilter
    ) {
        var count = 0
        if (selectedCategoryFilter != null) count++
        if (selectedMonthFilter != null) count++
        if (startDateFilter.isNotBlank() || endDateFilter.isNotBlank()) count++
        if (minAmountFilter.isNotBlank() || maxAmountFilter.isNotBlank()) count++
        count
    }

    // Filtrado de gastos
    val filteredExpenses = remember(
        expenses, currentUserId, searchQuery, selectedScopeFilter, selectedCategoryFilter,
        selectedMonthFilter, startDateFilter, endDateFilter,
        minAmountFilter, maxAmountFilter
    ) {
        expenses.filter { exp ->
            // Solo gastos propios del usuario actual
            val matchesUser = exp.userId.isBlank() || exp.userId == currentUserId
            if (!matchesUser) return@filter false

            // Filtro por Ámbito
            val matchesScope = when (selectedScopeFilter) {
                "PERSONAL" -> exp.scope == "PERSONAL"
                "HOUSEHOLD" -> exp.scope == "HOUSEHOLD"
                else -> true
            }

            // Filtro por Búsqueda de Texto
            val matchesSearch = if (searchQuery.isBlank()) true else {
                exp.description.contains(searchQuery.trim(), ignoreCase = true) ||
                        exp.category.contains(searchQuery.trim(), ignoreCase = true)
            }

            // Filtro por Categoría
            val matchesCategory = if (selectedCategoryFilter == null) true else {
                exp.category.equals(selectedCategoryFilter, ignoreCase = true)
            }

            // Filtro por Mes
            val matchesMonth = if (selectedMonthFilter == null) true else {
                exp.date.startsWith(selectedMonthFilter!!)
            }

            // Filtro por Rango de Fechas
            val matchesDateRange = run {
                val matchesStart = if (startDateFilter.isBlank()) true else exp.date >= startDateFilter.trim()
                val matchesEnd = if (endDateFilter.isBlank()) true else exp.date <= endDateFilter.trim()
                matchesStart && matchesEnd
            }

            // Filtro por Monto
            val matchesAmount = run {
                val minVal = minAmountFilter.toDoubleOrNull() ?: 0.0
                val maxVal = maxAmountFilter.toDoubleOrNull() ?: Double.MAX_VALUE
                exp.amount in minVal..maxVal
            }

            matchesScope && matchesSearch && matchesCategory && matchesMonth && matchesDateRange && matchesAmount
        }
    }

    // Ordenar gastos por fecha descendente
    val sortedExpenses = remember(filteredExpenses) {
        filteredExpenses.sortedWith(
            compareByDescending<ExpenseEntity> { it.date }
                .thenByDescending { it.id }
        )
    }

    // Resetear conteo paginado al cambiar filtros
    LaunchedEffect(searchQuery, selectedScopeFilter, selectedCategoryFilter, selectedMonthFilter, startDateFilter, endDateFilter, minAmountFilter, maxAmountFilter) {
        displayedCount = 25
    }

    // Detectar scroll cerca del final para cargar más ítems
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 4 && displayedCount < sortedExpenses.size
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            displayedCount = (displayedCount + 25).coerceAtMost(sortedExpenses.size)
        }
    }

    // Gastos que se muestran actualmente según el scroll infinito
    val pagedExpenses = remember(sortedExpenses, displayedCount) {
        sortedExpenses.take(displayedCount)
    }

    // Agrupación jerárquica: Mes -> Día
    val groupedExpenses = remember(pagedExpenses) {
        val byMonth = pagedExpenses.groupBy { exp ->
            if (exp.date.length >= 7) exp.date.substring(0, 7) else "Sin Fecha"
        }

        byMonth.map { (monthKey, monthExps) ->
            val byDay = monthExps.groupBy { if (it.date.length >= 10) it.date.substring(0, 10) else it.date }
            val dayGroups = byDay.map { (dayKey, dayExps) ->
                DayExpensesGroup(
                    dayKey = dayKey,
                    total = dayExps.sumOf { it.amount },
                    expenses = dayExps
                )
            }
            MonthExpensesGroup(
                monthKey = monthKey,
                total = monthExps.sumOf { it.amount },
                days = dayGroups
            )
        }
    }

    val totalFilteredAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Gastos",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Barra de Búsqueda en Vivo
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, tienda o nota...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de Filtros Rápidos
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Filtro de Ámbito (Todos / Personal / Hogar)
                items(listOf("ALL" to "Todos", "PERSONAL" to "Personal", "HOUSEHOLD" to "Hogar")) { (id, label) ->
                    val isSelected = selectedScopeFilter == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedScopeFilter = id },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (id == "HOUSEHOLD") PurpleAccent else EmeraldPrimary,
                            selectedLabelColor = if (id == "HOUSEHOLD") Color.White else Color.Black
                        )
                    )
                }

                // Botón de Filtros Avanzados
                item {
                    FilterChip(
                        selected = activeFiltersCount > 0,
                        onClick = { showAdvancedFiltersSheet = true },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                text = if (activeFiltersCount == 0) "Filtros Avanzados" else "Filtros ($activeFiltersCount)",
                                fontWeight = if (activeFiltersCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }

                // Botón Limpiar Filtros
                if (activeFiltersCount > 0 || searchQuery.isNotBlank() || selectedScopeFilter != "ALL") {
                    item {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedScopeFilter = "ALL"
                                selectedCategoryFilter = null
                                selectedMonthFilter = null
                                startDateFilter = ""
                                endDateFilter = ""
                                minAmountFilter = ""
                                maxAmountFilter = ""
                            }
                        ) {
                            Text("Limpiar", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Barra de Resumen de Resultados
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredExpenses.size} gasto(s) registrado(s)" + if (pagedExpenses.size < filteredExpenses.size) " (mostrando ${pagedExpenses.size})" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: ${formatCOP.format(totalFilteredAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No se encontraron gastos con los filtros aplicados.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedScopeFilter = "ALL"
                                selectedCategoryFilter = null
                                selectedMonthFilter = null
                                startDateFilter = ""
                                endDateFilter = ""
                                minAmountFilter = ""
                                maxAmountFilter = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Restablecer Filtros", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedExpenses.forEach { monthGroup ->
                        // Encabezado de Mes
                        item(key = "month_${monthGroup.monthKey}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMonthHeader(monthGroup.monthKey),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = formatCOP.format(monthGroup.total),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        monthGroup.days.forEach { dayGroup ->
                            // Sub-encabezado de Día
                            item(key = "day_${dayGroup.dayKey}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDayHeader(dayGroup.dayKey),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatCOP.format(dayGroup.total),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Gastos individuales de ese día
                            items(dayGroup.expenses, key = { it.id }) { exp ->
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
                                            .padding(14.dp),
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
                                                    color = if (exp.scope == "HOUSEHOLD") PurpleAccent.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f)
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
                                            }
                                        }

                                        Text(
                                            text = formatCOP.format(exp.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Indicador de carga si hay más elementos
                    if (displayedCount < sortedExpenses.size) {
                        item(key = "loading_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EmeraldPrimary)
                            }
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // ModalBottomSheet de Filtros Avanzados
    if (showAdvancedFiltersSheet) {
        val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAdvancedFiltersSheet = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros Avanzados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            selectedCategoryFilter = null
                            selectedMonthFilter = null
                            startDateFilter = ""
                            endDateFilter = ""
                            minAmountFilter = ""
                            maxAmountFilter = ""
                        }
                    ) {
                        Text("Limpiar", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Filtro por Mes
                Text("Filtrar por Mes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMonthFilter == currentMonthKey,
                        onClick = {
                            selectedMonthFilter = if (selectedMonthFilter == currentMonthKey) null else currentMonthKey
                        },
                        label = { Text("Este Mes") }
                    )
                    FilterChip(
                        selected = selectedMonthFilter == prevMonthKey,
                        onClick = {
                            selectedMonthFilter = if (selectedMonthFilter == prevMonthKey) null else prevMonthKey
                        },
                        label = { Text("Mes Anterior") }
                    )
                }

                // Filtro por Categoría
                Text("Categoría", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else cat
                            },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                // Filtro por Rango de Fechas
                Text("Rango de Fechas (YYYY-MM-DD)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startDateFilter,
                        onValueChange = { startDateFilter = it },
                        label = { Text("Desde") },
                        placeholder = { Text("2026-08-01") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDateFilter,
                        onValueChange = { endDateFilter = it },
                        label = { Text("Hasta") },
                        placeholder = { Text("2026-08-31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Filtro por Monto / Rango de Valor
                Text("Filtrar por Monto (COP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountFilter,
                        onValueChange = { input -> minAmountFilter = input.filter { it.isDigit() }.take(12) },
                        visualTransformation = CopCurrencyVisualTransformation(),
                        label = { Text("Mínimo") },
                        placeholder = { Text("$ 0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmountFilter,
                        onValueChange = { input -> maxAmountFilter = input.filter { it.isDigit() }.take(12) },
                        visualTransformation = CopCurrencyVisualTransformation(),
                        label = { Text("Máximo") },
                        placeholder = { Text("$ 10.000.000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = { showAdvancedFiltersSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Aplicar Filtros", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }

    // Diálogo de Edición
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

private fun formatMonthHeader(yearMonth: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdfIn.parse(yearMonth) ?: return yearMonth
        val sdfOut = SimpleDateFormat("MMMM yyyy", Locale("es", "CO"))
        sdfOut.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
    } catch (e: Exception) {
        yearMonth
    }
}

private fun formatDayHeader(dateStr: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfIn.parse(dateStr) ?: return dateStr

        val calTarget = Calendar.getInstance().apply { time = date }
        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val isToday = calTarget.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calTarget.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)
        val isYesterday = calTarget.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calTarget.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

        val dayFormatter = SimpleDateFormat("d 'de' MMMM", Locale("es", "CO"))
        val formattedDate = dayFormatter.format(date)

        when {
            isToday -> "Hoy • $formattedDate"
            isYesterday -> "Ayer • $formattedDate"
            else -> {
                val dayOfWeekFormatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "CO"))
                dayOfWeekFormatter.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
            }
        }
    } catch (e: Exception) {
        dateStr
    }
}
