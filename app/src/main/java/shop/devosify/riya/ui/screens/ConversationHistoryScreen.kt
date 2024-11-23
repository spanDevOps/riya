package shop.devosify.riya.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import shop.devosify.riya.viewmodel.ConversationHistoryViewModel

@Composable
fun ConversationHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(conversations) { conversationWithMessages ->
            ConversationItem(
                conversation = conversationWithMessages,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationWithMessages,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            conversation.messages.forEach { message ->
                MessageBubble(
                    content = message.content,
                    isUser = message.isUser,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isUser) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
} 