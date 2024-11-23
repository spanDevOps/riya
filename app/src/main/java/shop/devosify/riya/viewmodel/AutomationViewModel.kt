@HiltViewModel
class AutomationViewModel @Inject constructor(
    private val locationAutomationService: LocationAutomationService,
    private val placesService: PlacesService,
    private val patternAnalysisService: PatternAnalysisService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _activeRules = MutableStateFlow<List<AutomationRuleUiModel>>(emptyList())
    val activeRules: StateFlow<List<AutomationRuleUiModel>> = _activeRules.asStateFlow()

    private val _frequentLocations = MutableStateFlow<List<LocationUiModel>>(emptyList())
    val frequentLocations: StateFlow<List<LocationUiModel>> = _frequentLocations.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Combine rules and locations updates
                combine(
                    locationAutomationService.activeRules,
                    placesService.getFrequentLocations()
                ) { rules, locations ->
                    _activeRules.value = rules.map { it.toUiModel() }
                    _frequentLocations.value = locations.map { it.toUiModel() }
                    _uiState.value = UiState.Success
                }.collect()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load automation data: ${e.message}")
                analyticsService.logError("automation_load", e.message ?: "Unknown error")
            }
        }
    }

    fun toggleRule(ruleId: String) {
        viewModelScope.launch {
            try {
                locationAutomationService.toggleRule(ruleId)
                analyticsService.logAutomationAction("rule_toggle", ruleId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to toggle rule: ${e.message}")
                analyticsService.logError("rule_toggle", e.message ?: "Unknown error")
            }
        }
    }

    fun editRule(ruleId: String) {
        viewModelScope.launch {
            try {
                // Get current rule
                val rule = _activeRules.value.find { it.id == ruleId }
                    ?: throw IllegalStateException("Rule not found")

                // Update UI state to show edit dialog
                _uiState.value = UiState.Editing(rule)
                analyticsService.logAutomationAction("rule_edit", ruleId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to edit rule: ${e.message}")
                analyticsService.logError("rule_edit", e.message ?: "Unknown error")
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            try {
                locationAutomationService.deleteRule(ruleId)
                analyticsService.logAutomationAction("rule_delete", ruleId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete rule: ${e.message}")
                analyticsService.logError("rule_delete", e.message ?: "Unknown error")
            }
        }
    }

    fun editLocation(locationId: String) {
        viewModelScope.launch {
            try {
                val location = _frequentLocations.value.find { it.id == locationId }
                    ?: throw IllegalStateException("Location not found")

                _uiState.value = UiState.EditingLocation(location)
                analyticsService.logAutomationAction("location_edit", locationId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to edit location: ${e.message}")
                analyticsService.logError("location_edit", e.message ?: "Unknown error")
            }
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                placesService.deleteLocation(locationId)
                analyticsService.logAutomationAction("location_delete", locationId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete location: ${e.message}")
                analyticsService.logError("location_delete", e.message ?: "Unknown error")
            }
        }
    }

    fun updateRule(rule: AutomationRuleUiModel) {
        viewModelScope.launch {
            try {
                locationAutomationService.updateRule(rule.toDomainModel())
                _uiState.value = UiState.Success
                analyticsService.logAutomationAction("rule_update", rule.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to update rule: ${e.message}")
                analyticsService.logError("rule_update", e.message ?: "Unknown error")
            }
        }
    }

    fun updateLocation(location: LocationUiModel) {
        viewModelScope.launch {
            try {
                placesService.updateLocation(location.toDomainModel())
                _uiState.value = UiState.Success
                analyticsService.logAutomationAction("location_update", location.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to update location: ${e.message}")
                analyticsService.logError("location_update", e.message ?: "Unknown error")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
        data class Editing(val rule: AutomationRuleUiModel) : UiState()
        data class EditingLocation(val location: LocationUiModel) : UiState()
    }

    private fun AutomationRule.toUiModel() = AutomationRuleUiModel(
        id = id,
        name = name,
        description = description,
        type = type,
        isEnabled = isEnabled,
        confidence = confidence
    )

    private fun FrequentLocation.toUiModel() = LocationUiModel(
        id = id,
        name = name,
        type = type,
        visitCount = visitCount,
        lastVisit = lastVisit
    )

    private fun AutomationRuleUiModel.toDomainModel() = AutomationRule(
        id = id,
        type = type,
        condition = createCondition(),
        action = createAction(),
        confidence = confidence
    )

    private fun LocationUiModel.toDomainModel() = Location(
        id = id,
        name = name,
        type = type,
        latitude = 0.0, // Get from original location
        longitude = 0.0 // Get from original location
    )
} 