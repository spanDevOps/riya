package shop.devosify.riya.repository

import kotlinx.coroutines.flow.Flow
import shop.devosify.riya.data.local.ConversationEntity
import shop.devosify.riya.data.local.MessageEntity
import shop.devosify.riya.data.local.dao.ConversationDao
import shop.devosify.riya.data.local.dao.ConversationWithMessages
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {
    suspend fun createConversation(threadId: String): Long {
        return conversationDao.insertConversation(
            ConversationEntity(threadId = threadId)
        )
    }

    suspend fun addMessage(conversationId: Long, content: String, isUser: Boolean) {
        conversationDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                content = content,
                isUser = isUser
            )
        )
    }

    fun getConversationsWithMessages(): Flow<List<ConversationWithMessages>> {
        return conversationDao.getConversationsWithMessages()
    }

    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>> {
        return conversationDao.getMessagesByConversationId(conversationId)
    }
} 