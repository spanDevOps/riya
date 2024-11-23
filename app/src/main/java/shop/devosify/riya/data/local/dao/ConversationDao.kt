package shop.devosify.riya.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import shop.devosify.riya.data.local.ConversationEntity
import shop.devosify.riya.data.local.MessageEntity

@Dao
interface ConversationDao {
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getConversationsWithMessages(): Flow<List<ConversationWithMessages>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>>
}

data class ConversationWithMessages(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val messages: List<MessageEntity>
) 