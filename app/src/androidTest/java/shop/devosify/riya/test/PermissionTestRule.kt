class PermissionTestRule(
    private vararg val permissions: String
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        grantPermissions(*permissions)
    }

    private fun grantPermissions(vararg permissions: String) {
        val command = StringBuilder().apply {
            append("pm grant ${InstrumentationRegistry.getInstrumentation().targetContext.packageName}")
            permissions.forEach { permission ->
                append(" $permission")
            }
        }.toString()

        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    }
} 