package shop.devosify.riya

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import shop.devosify.riya.navigation.RiyaNavigation
import shop.devosify.riya.ui.theme.RiyaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var permissionsGranted by mutableStateOf(false)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        
        setContent {
            RiyaTheme {
                DisposableEffect(Unit) {
                    onDispose {
                        // Cleanup any resources when the activity is destroyed
                    }
                }
                
                if (permissionsGranted) {
                    RiyaNavigation()
                } else {
                    PermissionsRequiredScreen(
                        onRequestPermissions = { checkPermissions() }
                    )
                }
            }
        }
    }

    private fun checkPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }
}

@Composable
private fun PermissionsRequiredScreen(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Microphone permission is required for voice interaction",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}
