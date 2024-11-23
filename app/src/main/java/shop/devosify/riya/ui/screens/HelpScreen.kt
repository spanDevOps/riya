@Composable
fun HelpScreen(
    viewModel: HelpViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var selectedGuide by remember { mutableStateOf<Guide?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Guides") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedGuide == null) {
            // Show guide list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(Guide.values()) { guide ->
                    GuideItem(
                        guide = guide,
                        onClick = { selectedGuide = guide }
                    )
                }
            }
        } else {
            // Show selected guide content
            GuideContent(
                guide = selectedGuide!!,
                onBack = { selectedGuide = null }
            )
        }
    }
}

@Composable
private fun GuideItem(
    guide: Guide,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(guide.title) },
        supportingContent = { Text(guide.description) },
        leadingContent = { Icon(guide.icon, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun GuideContent(
    guide: Guide,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = guide.title,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(8.dp))
        
        MarkdownText(
            markdown = guide.content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

enum class Guide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val content: String
) {
    GETTING_STARTED(
        title = "Getting Started",
        description = "Learn the basics of using Riya",
        icon = Icons.Default.Start,
        content = loadGuideContent("getting_started.md")
    ),
    VOICE_COMMANDS(
        title = "Voice Commands",
        description = "All available voice commands",
        icon = Icons.Default.Mic,
        content = loadGuideContent("voice_commands.md")
    ),
    SHOPPING(
        title = "Shopping Guide",
        description = "How to shop with Riya",
        icon = Icons.Default.ShoppingCart,
        content = loadGuideContent("shopping_guide.md")
    ),
    AUTOMATION(
        title = "Automation Guide",
        description = "Create and manage automations",
        icon = Icons.Default.AutoAwesome,
        content = loadGuideContent("automation_guide.md")
    )
} 