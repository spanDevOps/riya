@Composable
fun LanguageSelectionScreen(
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val languages = Language.values()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_language),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(languages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language == currentLanguage,
                    onSelect = { viewModel.setLanguage(language) }
                )
            }
        }
    }
} 