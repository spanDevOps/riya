@Singleton
class SystemControlService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devicePolicyManager: DevicePolicyManager
) {
    suspend fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        context.startActivity(intent)
    }

    suspend fun installApp(appId: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$appId")
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
    }

    suspend fun lockScreen() {
        devicePolicyManager.lockNow()
    }

    suspend fun cleanSystem() {
        // Clear cache
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)
        packages.forEach { packageInfo ->
            val dir = File(context.cacheDir, packageInfo.packageName)
            dir.deleteRecursively()
        }

        // Clear RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.clearApplicationUserData()
    }
} 