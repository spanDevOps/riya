package shop.devosify.riya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import shop.devosify.riya.ui.theme.HomeScreen
import shop.devosify.riya.ui.theme.RiyaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiyaTheme {
                HomeScreen(
                    onAssistantStart = {
                        // Logic to start the assistant (e.g., start listening in the background)
                    },
                    onViewHistory = {
                        // Logic to navigate to the chat history screen
                    }
                )
            }
        }
    }
}
