---
name: room-sqlite-migration-guard
description: >-
  Enforces zero-data-loss SQLite and Room database migrations, schema preservation,
  and automated migration verification testing in Android.
  Trigger on: "room", "migration", "database", "sqlite", "قاعدة البيانات", "ترحيل",
  "ميجريشن", "AppDatabase", "MIGRATION_", "exportSchema", "MigrationTestHelper",
  "fallbackToDestructiveMigration", "Room cannot verify the data integrity",
  "تحديث قاعدة البيانات", "سكيمة", "RoomDatabase", "Room schema", "room-testing".
---

# Room & SQLite Migration Guard (Zero Data Loss Protocol)

## 1. Overview & Core Directive

This skill governs all schema changes, entity updates, and version migrations for Android Room databases within WhatsUp Automation (`AppDatabase.kt`).
The primary mandate is **Absolute Zero Data Loss**: user automation rules, message logs, synced contacts, media vault entries, and outbox queues must never be dropped or corrupted during database evolution.

---

## 2. Hard Architectural Rules

1. **Strict Prohibition of Destructive Migration:**
   - `fallbackToDestructiveMigration()` is **strictly banned** in production builds.
   - `fallbackToDestructiveMigrationOnDowngrade()` is permitted only for downgrade safety in emergency rollbacks, but forward migrations must always be explicit.
   - Any missing migration step will cause an unhandled crash or wipe user data; every version bump ($V \to V+1$) requires a dedicated, tested `Migration` object.

2. **Schema Export & Version Control:**
   - Every production Room database must have `exportSchema = true` enabled in `@Database`.
   - Schema JSON snapshots must be committed to git under `app/schemas/com.whatsup.automation.data.local.AppDatabase/`.
   - Never manually edit or delete generated schema JSON files.

3. **Atomic Incremental Migrations:**
   - Never skip versions in individual migrations (e.g., always write $10 \to 11$, $11 \to 12$; do not rely exclusively on a combined $10 \to 12$).
   - Register all migration steps in both `AppDatabase.kt` and `DatabaseModule.kt` via `.addMigrations(...)`.

---

## 3. Room Schema Preservation & Export Setup

### 3.1 KSP Schema Location in Gradle

In `app/build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    defaultConfig {
        // ...
    }
    sourceSets {
        // Make schema directory available to instrumented tests
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}
```

### 3.2 Activating Schema Export in `AppDatabase.kt`

```kotlin
@Database(
    entities = [
        RuleEntity::class,
        LogEntity::class,
        SyncedContactEntity::class,
        StatusEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        GroupLogEntity::class,
        OutboxMessageEntity::class,
        VaultMediaEntity::class
    ],
    version = 11, // Bump version monotonically
    exportSchema = true // Must be true in production
)
abstract class AppDatabase : RoomDatabase() { ... }
```

---

## 4. Safe Migration Protocol (v10 -> v11+)

### 4.1 Room SQLite Type Mappings

| Kotlin Type | SQLite Type | Nullability / Default Value Rule |
|:---|:---|:---|
| `String` | `TEXT NOT NULL` | Requires default, e.g. `DEFAULT ''` if added to existing table |
| `String?` | `TEXT` | Nullable, defaults to `NULL` |
| `Int`, `Long` | `INTEGER NOT NULL` | Requires default, e.g. `DEFAULT 0` |
| `Int?`, `Long?` | `INTEGER` | Nullable, defaults to `NULL` |
| `Boolean` | `INTEGER NOT NULL` | Stored as `0` (false) or `1` (true). Default `DEFAULT 0` |
| `Boolean?` | `INTEGER` | Nullable, defaults to `NULL` |
| `Float`, `Double` | `REAL NOT NULL` | Requires default, e.g. `DEFAULT 0.0` |

### 4.2 Adding Columns to Existing Tables (Non-Destructive)

When adding fields to existing tables (e.g. `statuses`, `vault_media`, `activity_logs`), follow SQLite `ALTER TABLE` constraints:

```kotlin
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Adding NOT NULL column requires an explicit DEFAULT value in SQLite
        db.execSQL("ALTER TABLE statuses ADD COLUMN isSaved INTEGER NOT NULL DEFAULT 0")
        
        // Nullable column
        db.execSQL("ALTER TABLE statuses ADD COLUMN savedFilePath TEXT")
        db.execSQL("ALTER TABLE statuses ADD COLUMN savedAt INTEGER")
        
        // Add indices safely
        db.execSQL("CREATE INDEX IF NOT EXISTS index_statuses_isSaved ON statuses (isSaved)")
    }
}
```

### 4.3 Table Recreation Pattern (Complex Alterations)

SQLite prior to recent versions does not support dropping columns, altering column types, or changing primary keys directly. When modifying table structure:

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create temporary table with the exact new schema
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS outbox_messages_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recipient TEXT NOT NULL,
                messageText TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Copy data from the old table
        db.execSQL("""
            INSERT INTO outbox_messages_new (id, recipient, messageText, status, createdAt)
            SELECT id, recipient, messageText, status, createdAt FROM outbox_messages
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE outbox_messages")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE outbox_messages_new RENAME TO outbox_messages")

        // 5. Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_messages_status ON outbox_messages (status)")
    }
}
```

### 4.4 Registering Migrations in DI (`DatabaseModule.kt`)

Always ensure the new migration object is added to the `addMigrations(...)` builder list:

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
    .addCallback(AppDatabase.createCallback())
    .addMigrations(
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8,
        AppDatabase.MIGRATION_8_9,
        AppDatabase.MIGRATION_9_10,
        AppDatabase.MIGRATION_10_11,
        AppDatabase.MIGRATION_11_12 // Newly registered migration
    )
    .fallbackToDestructiveMigrationOnDowngrade()
    .build()
```

---

## 5. Automated Migration Verification Testing

### 5.1 Gradle Testing Setup

Ensure `room-testing` is declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`:

```toml
[libraries]
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

In `app/build.gradle.kts`:
```kotlin
dependencies {
    androidTestImplementation(libs.room.testing)
}
```

### 5.2 Verification with `MigrationTestHelper`

Room validates the database using an internal identity hash stored in `room_master_table`. If a column's default value, collation, nullability, or index differs by even a single character, Room throws:
`IllegalStateException: Room cannot verify the data integrity. Expected hash: ..., found: ...`

To prevent production crashes, write an instrumented migration test:

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate10To11_preservesExistingData() {
        // 1. Create database in version 10 and populate test data
        var db = helper.createDatabase(TEST_DB, 10).apply {
            execSQL("INSERT INTO statuses (id, senderPhone, senderName, timestamp, isViewed, isReacted) VALUES ('s1', '12345', 'Ali', 1600000000, 1, 0)")
            close()
        }

        // 2. Run migration to version 11 and validate schema against exported JSON
        db = helper.runMigrationsAndValidate(TEST_DB, 11, true, AppDatabase.MIGRATION_10_11)

        // 3. Verify data was preserved and default values applied
        val cursor = db.query("SELECT id, isSaved, savedFilePath FROM statuses WHERE id = 's1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("s1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isSaved")))
        assertNull(cursor.getString(cursor.getColumnIndexOrThrow("savedFilePath")))
        cursor.close()
    }
}
```

---

## 6. WhatsUp Data Protection Matrix

| Table / Entity | Sensitivity | Protection Priority | Special Handling |
|:---|:---|:---|:---|
| `rules` (`RuleEntity`) | Critical | 🔴 Highest | User automation logic. Never drop. Do not auto-delete user custom rules in `onOpen`. |
| `synced_contacts` | High | 🔴 Highest | Normalized JID/LID pairs and sync states. Loss breaks message targeting. |
| `activity_logs` | Medium | 🟡 High | Audit history. Purge invalid logs only (e.g. `status = 'NO_MATCH'`). |
| `outbox_messages` | High | 🔴 Highest | Pending messages in queue. Must survive app restarts and migrations. |
| `statuses` | Medium | 🟡 High | Viewed/reacted status flags. Preserve `isReacted` to prevent duplicate story reactions. |
| `vault_media` | High | 🔴 Highest | Saved media and View-Once records. Loss causes media disassociation. |

### 6.1 Database Callback Safeguards (`createCallback`)

In `AppDatabase.kt`:
- **`onCreate`**: Initialize baseline system metadata only.
- **`onOpen`**: Clean transient or malformed data with precise `WHERE` clauses. **Never** run unbounded `DELETE FROM rules` or `DELETE FROM activity_logs`.

---

## 7. Migration Execution Protocol (Step-by-Step)

```
Step 1 — Entity Update:      Modify @Entity class (add field, annotations, or index).
Step 2 — Version Increment:  Increment version = N + 1 in AppDatabase.
Step 3 — Write Migration:    Implement MIGRATION_N_NPLUS1 with exact SQLite types and NOT NULL defaults.
Step 4 — Register in DI:     Add MIGRATION_N_NPLUS1 to DatabaseModule.kt addMigrations().
Step 5 — Schema Build:       Run silent compile (`./gradlew kspDebugKotlin -q`) to generate new schema JSON.
Step 6 — Verify Test:        Run MigrationTestHelper test to ensure hash parity and zero data loss.
Step 7 — Sync:               Update PROJECT_STATE.md with the new database version and migration details.
```
