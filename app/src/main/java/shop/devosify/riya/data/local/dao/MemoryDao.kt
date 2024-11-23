package shop.devosify.riya.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import shop.devosify.riya.data.local.entity.MemoryEntity

@Dao
interface MemoryDao {
    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC, timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE synced = 0")
    suspend fun getUnsynced(): List<MemoryEntity>

    @Query("UPDATE memories SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>
} 