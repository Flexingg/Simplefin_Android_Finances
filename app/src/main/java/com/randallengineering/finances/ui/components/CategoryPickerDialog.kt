package com.randallengineering.finances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.domain.model.CategoryHierarchy

@Composable
fun CategoryPickerDialog(
    categories: List<CategoryHierarchy>,
    initialMainCategory: String = "",
    initialSubCategory: String = "",
    onDismiss: () -> Unit,
    onCategorySelected: (mainCategory: String, subCategory: String) -> Unit,
    onAddNewCategory: (mainCategory: String, subCategory: String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMain by remember { mutableStateOf(initialMainCategory.ifBlank { categories.firstOrNull()?.mainCategory.orEmpty() }) }
    var selectedSub by remember { mutableStateOf(initialSubCategory) }

    var isCreatingCustom by remember { mutableStateOf(categories.isEmpty()) }
    var customMainName by remember { mutableStateOf("") }
    var customSubName by remember { mutableStateOf("") }

    val activeCategory = categories.find { it.mainCategory.equals(selectedMain, ignoreCase = true) }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) categories
        else categories.filter { cat ->
            cat.mainCategory.contains(searchQuery, ignoreCase = true) ||
                    cat.subCategories.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Column {
                Text(
                    text = if (categories.isEmpty()) "Create Category" else "Select Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Structured Main Category & Subcategory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (categories.isNotEmpty()) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search categories...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        singleLine = true
                    )

                    // Main Category Horizontal Chips
                    Text("1. Main Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            val isSelected = cat.mainCategory.equals(selectedMain, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedMain = cat.mainCategory
                                    selectedSub = ""
                                },
                                label = { Text(cat.mainCategory) },
                                shape = Shapes.small,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Subcategories List for Selected Main Category
                    Text("2. Subcategory (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // General option
                            item {
                                val isGeneralSelected = selectedSub.isBlank()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(Shapes.small)
                                        .background(if (isGeneralSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .clickable { selectedSub = "" }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("General (No Subcategory)", fontWeight = if (isGeneralSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (isGeneralSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Subcategories list
                            val subList = activeCategory?.subCategories.orEmpty()
                            items(subList) { sub ->
                                val isSubSelected = sub.equals(selectedSub, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(Shapes.small)
                                        .background(if (isSubSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .clickable { selectedSub = sub }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sub, fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (isSubSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Category Creator
                if (!isCreatingCustom && categories.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { isCreatingCustom = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create New Category / Subcategory")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (categories.isEmpty()) "Create Your First Category" else "Create New Category",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = customMainName,
                                onValueChange = { customMainName = it },
                                label = { Text("Main Category") },
                                placeholder = { Text("e.g. Home, Food & Dining, Vehicle") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shapes.small,
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customSubName,
                                onValueChange = { customSubName = it },
                                label = { Text("Subcategory (Optional)") },
                                placeholder = { Text("e.g. Utilities, Groceries, Gas") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shapes.small,
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        if (customMainName.isNotBlank()) {
                                            onAddNewCategory(customMainName.trim(), customSubName.trim().ifBlank { null })
                                            selectedMain = customMainName.trim()
                                            selectedSub = customSubName.trim()
                                            onCategorySelected(selectedMain, selectedSub)
                                            customMainName = ""
                                            customSubName = ""
                                        }
                                    },
                                    enabled = customMainName.isNotBlank(),
                                    shape = Shapes.small
                                ) {
                                    Text("Create & Apply")
                                }
                                if (categories.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { isCreatingCustom = false },
                                        shape = Shapes.small
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (categories.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedMain.isNotBlank()) {
                            onCategorySelected(selectedMain, selectedSub)
                        }
                    },
                    enabled = selectedMain.isNotBlank(),
                    shape = Shapes.small
                ) {
                    Text("Apply Category")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
