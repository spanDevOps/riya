@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val rules by viewModel.activeRules.collectAsState()
    val locations by viewModel.frequentLocations.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation & Locations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Navigate Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Rules") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Locations") }
                )
            }

            when (selectedTab) {
                0 -> AutomationRulesList(
                    rules = rules,
                    onRuleToggle = viewModel::toggleRule,
                    onRuleEdit = viewModel::editRule,
                    onRuleDelete = viewModel::deleteRule
                )
                1 -> LocationsList(
                    locations = locations,
                    onLocationEdit = viewModel::editLocation,
                    onLocationDelete = viewModel::deleteLocation
                )
            }
        }
    }
}

@Composable
private fun AutomationRulesList(
    rules: List<AutomationRuleUiModel>,
    onRuleToggle: (String) -> Unit,
    onRuleEdit: (String) -> Unit,
    onRuleDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(rules) { rule ->
            AutomationRuleCard(
                rule = rule,
                onToggle = { onRuleToggle(rule.id) },
                onEdit = { onRuleEdit(rule.id) },
                onDelete = { onRuleDelete(rule.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AutomationRuleCard(
    rule: AutomationRuleUiModel,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun LocationsList(
    locations: List<LocationUiModel>,
    onLocationEdit: (String) -> Unit,
    onLocationDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(locations) { location ->
            LocationCard(
                location = location,
                onEdit = { onLocationEdit(location.id) },
                onDelete = { onLocationDelete(location.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = location.type.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${location.visitCount} visits",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    imageVector = when (location.type) {
                        GeofenceType.HOME -> Icons.Default.Home
                        GeofenceType.WORK -> Icons.Default.Work
                        GeofenceType.FREQUENT -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

data class AutomationRuleUiModel(
    val id: String,
    val name: String,
    val description: String,
    val type: PatternType,
    val isEnabled: Boolean,
    val confidence: Float
)

data class LocationUiModel(
    val id: String,
    val name: String,
    val type: GeofenceType,
    val visitCount: Int,
    val lastVisit: Long
) 