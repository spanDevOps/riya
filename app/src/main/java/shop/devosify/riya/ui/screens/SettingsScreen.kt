@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var useGpt4 by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        useGpt4 = viewModel.getUseGpt4Setting()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Other settings...
        
        SwitchPreference(
            title = "Use GPT-4",
            subtitle = "Enable for more advanced capabilities (uses more credits)",
            checked = useGpt4,
            onCheckedChange = { enabled ->
                useGpt4 = enabled
                viewModel.setUseGpt4Setting(enabled)
            }
        )
        
        // Cost warning if GPT-4 is enabled
        if (useGpt4) {
            Text(
                text = "Note: GPT-4 uses significantly more API credits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
} 