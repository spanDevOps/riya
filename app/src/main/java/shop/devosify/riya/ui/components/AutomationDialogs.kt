@Composable
fun RuleEditDialog(
    rule: AutomationRuleUiModel,
    onDismiss: () -> Unit,
    onSave: (AutomationRuleUiModel) -> Unit
) {
    var name by remember { mutableStateOf(rule.name) }
    var description by remember { mutableStateOf(rule.description) }
    var isEnabled by remember { mutableStateOf(rule.isEnabled) }
    var type by remember { mutableStateOf(rule.type) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Automation Rule",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rule Type")
                    DropdownMenu(
                        expanded = false,
                        onDismissRequest = { },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        PatternType.values().forEach { patternType ->
                            DropdownMenuItem(
                                text = { Text(patternType.name) },
                                onClick = { type = patternType }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled")
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    horizontalSpacing = 8.dp
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        onSave(
                            rule.copy(
                                name = name,
                                description = description,
                                isEnabled = isEnabled,
                                type = type
                            )
                        )
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun LocationEditDialog(
    location: LocationUiModel,
    onDismiss: () -> Unit,
    onSave: (LocationUiModel) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var type by remember { mutableStateOf(location.type) }
    var showTypeMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Location",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = { },
                        label = { Text("Location Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTypeMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Select Type")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        GeofenceType.values().forEach { geofenceType ->
                            DropdownMenuItem(
                                text = { Text(geofenceType.name) },
                                onClick = {
                                    type = geofenceType
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "${location.visitCount} visits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    horizontalSpacing = 8.dp
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        onSave(
                            location.copy(
                                name = name,
                                type = type
                            )
                        )
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 