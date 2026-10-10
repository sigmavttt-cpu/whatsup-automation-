package com.whatsup.automation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whatsup.automation.data.local.entity.SyncedContactDao
import com.whatsup.automation.data.local.dao.LogDao
import com.whatsup.automation.data.local.dao.RuleDao
import com.whatsup.automation.data.local.entity.SyncedContactEntity
import com.whatsup.automation.data.local.entity.LogEntity
import com.whatsup.automation.data.local.entity.RuleEntity

/**
 * قاعدة بيانات Room الرئيسية — المصدر الوحيد للحقيقة (Single Source of Truth).
 */
@Database(
    entities = [
        RuleEntity::class,
        LogEntity::class,
        SyncedContactEntity::class,
        com.whatsup.automation.data.local.entity.StatusEntity::class,
        com.whatsup.automation.data.local.entity.GroupEntity::class,
        com.whatsup.automation.data.local.entity.GroupMemberEntity::class,
        com.whatsup.automation.data.local.entity.GroupLogEntity::class,
        com.whatsup.automation.data.local.entity.OutboxMessageEntity::class,
        com.whatsup.automation.data.local.entity.VaultMediaEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao
    abstract fun syncedContactDao(): SyncedContactDao
    abstract fun statusDao(): com.whatsup.automation.data.local.dao.StatusDao
    abstract fun groupDao(): com.whatsup.automation.data.local.dao.GroupDao
    abstract fun groupMemberDao(): com.whatsup.automation.data.local.dao.GroupMemberDao
    abstract fun groupLogDao(): com.whatsup.automation.data.local.dao.GroupLogDao
    abstract fun outboxDao(): com.whatsup.automation.data.local.dao.OutboxDao
    abstract fun vaultDao(): com.whatsup.automation.data.local.dao.VaultDao

    companion object {
        const val DATABASE_NAME = "whatsup_automation.db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE statuses ADD COLUMN participant TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_timestamp ON activity_logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_senderPhone ON activity_logs (senderPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_logs_status ON activity_logs (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_timestamp ON statuses (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_senderPhone ON statuses (senderPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_isViewed ON statuses (isViewed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_isReacted ON statuses (isReacted)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS outbox_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipient TEXT NOT NULL,
                        messageText TEXT NOT NULL,
                        mediaUri TEXT,
                        status TEXT NOT NULL,
                        retryCount INTEGER NOT NULL,
                        nextRetryTimestamp INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastError TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_messages_status ON outbox_messages (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_messages_createdAt ON outbox_messages (createdAt)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE statuses ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE statuses ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_isFavorite ON statuses (isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_isMuted ON statuses (isMuted)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vault_media (
                        id TEXT PRIMARY KEY NOT NULL,
                        senderPhone TEXT NOT NULL,
                        senderName TEXT NOT NULL,
                        mediaType TEXT NOT NULL,
                        isViewOnce INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        originalText TEXT,
                        localFilePath TEXT,
                        mimeType TEXT,
                        fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_media_timestamp ON vault_media (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_media_senderPhone ON vault_media (senderPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_media_isViewOnce ON vault_media (isViewOnce)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_media_isDeleted ON vault_media (isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_media_isFavorite ON vault_media (isFavorite)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE statuses ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE statuses ADD COLUMN deletedAt INTEGER")
            }
        }

        /**
         * إنشاء callback لتزويد قاعدة البيانات بالبيانات الأولية (القواعد الافتراضية).
         */
        fun createCallback(): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // القاعدة 1: الرد على التحية والسلام
                    db.execSQL("""
                        INSERT INTO rules (name, description, patternType, patternValue, actionsJson, priority, isEnabled, createdAt)
                        VALUES (
                            'الرد على التحية والسلام',
                            'الرد بقرعة ترحيبية مهذبة عند استقبال عبارات السلام والتحية',
                            'CONTAINS',
                            'سلام, السلام عليكم, مرحبا, هلا, مساء الخير, صباح الخير',
                            '[{"type":"SEND_REPLY","message":"وعليكم السلام ورحمة الله وبركاته 🌹
أهلاً وسهلاً بك، تفضل كيف أقدر أساعدك؟
حياك الله أخي الكريم ✨"}]',
                            1,
                            1,
                            ${System.currentTimeMillis()}
                        )
                    """.trimIndent())
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("DELETE FROM activity_logs WHERE status = 'NO_MATCH' OR actionExecuted LIKE '%تجاهل%' OR senderPhone = 'null' OR senderPhone = '' OR senderPhone LIKE '%lid%'")
                }
            }
        }
    }
}
