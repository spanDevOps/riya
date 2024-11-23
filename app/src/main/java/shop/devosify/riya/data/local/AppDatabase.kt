package shop.devosify.riya.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportFactory
import shop.devosify.riya.data.local.dao.ConversationDao
import shop.devosify.riya.security.SecureStorage

@Database(
    entities = [MemoryEntity::class, ConversationEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        private const val DB_NAME = "riya_encrypted.db"

        fun build(context: Context, secureStorage: SecureStorage): AppDatabase {
            val passphrase = secureStorage.getDatabaseKey()
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(passphrase)))
                .build()
        }
    }
} 