package shop.devosify.riya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,        // The crucial information
    val context: String,        // Where/how this information was obtained
    val timestamp: Long = System.currentTimeMillis(),
    val importance: Int,        // 1-5 scale of importance
    val category: String,       // Category of information (e.g., "preference", "fact", "habit")
    val synced: Boolean = false // Whether this has been synced to cloud
) 