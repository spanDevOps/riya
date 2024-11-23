@Singleton
class TaskValidator @Inject constructor(
    private val securityManager: SecurityManager,
    private val systemMonitorService: SystemMonitorService
) {
    suspend fun validateTask(task: CustomTask): ValidationResult {
        val checks = listOf(
            checkPermissions(task),
            checkResourceRequirements(task),
            checkSecurityImplications(task),
            checkSystemCompatibility(task)
        )

        val failures = checks.filterIsInstance<ValidationResult.Failure>()
        return if (failures.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(
                failures.flatMap { it.reasons }
            )
        }
    }

    private suspend fun checkPermissions(task: CustomTask): ValidationResult {
        val missingPermissions = task.requiredPermissions.filter { permission ->
            !securityManager.hasPermission(permission)
        }

        return if (missingPermissions.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(
                missingPermissions.map { "Missing permission: $it" }
            )
        }
    }

    private suspend fun checkResourceRequirements(task: CustomTask): ValidationResult {
        val systemState = systemMonitorService.getCurrentState()
        val issues = mutableListOf<String>()

        if (systemState.memoryUsage > ResourceLimits.MAX_MEMORY_USAGE) {
            issues.add("Insufficient memory available")
        }
        if (systemState.batteryLevel < ResourceLimits.MIN_BATTERY_LEVEL) {
            issues.add("Battery level too low")
        }
        if (!systemState.isNetworkAvailable && task.requiresNetwork) {
            issues.add("Network connection required")
        }

        return if (issues.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(issues)
        }
    }

    // Add other validation methods...
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val reasons: List<String>) : ValidationResult()
} 