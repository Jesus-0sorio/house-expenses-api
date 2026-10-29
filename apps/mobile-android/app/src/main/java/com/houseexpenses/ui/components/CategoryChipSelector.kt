package com.houseexpenses.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseexpenses.ui.theme.EmeraldPrimary

data class CategoryItem(val id: String, val name: String, val emoji: String)

val DefaultExpenseCategories = listOf(
    CategoryItem("SUPERMERCADO", "Supermercado", "🛒"),
    CategoryItem("RESTAURANTES", "Restaurantes", "🍔"),
    CategoryItem("SERVICIOS", "Servicios", "💡"),
    CategoryItem("TRANSPORTE", "Transporte", "🚗"),
    CategoryItem("VIVIENDA", "Vivienda", "🏠"),
    CategoryItem("SALUD", "Salud", "💊"),
    CategoryItem("ENTRETENIMIENTO", "Ocio", "🎬"),
    CategoryItem("OTROS", "Otros", "📦")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipSelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Categoría",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(DefaultExpenseCategories) { item ->
                val isSelected = selectedCategory.equals(item.name, ignoreCase = true) ||
                                 selectedCategory.equals(item.id, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(item.name) },
                    label = {
                        Text(
                            text = "${item.emoji} ${item.name}",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        selectedContainerColor = EmeraldPrimary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = Color.Black
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = EmeraldPrimary,
                        borderWidth = 1.dp
                    )
                )
            }
        }
    }
}
