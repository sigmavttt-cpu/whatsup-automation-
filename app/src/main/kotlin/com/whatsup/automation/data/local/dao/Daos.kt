package com.whatsup.automation.data.local.dao

import androidx.room.*
import com.whatsup.automation.data.local.entity.GroupEntity
import com.whatsup.automation.data.local.entity.GroupLogEntity
import com.whatsup.automation.data.local.entity.GroupMemberEntity
import com.whatsup.automation.data.local.entity.LogEntity
import com.whatsup.automation.data.local.entity.RuleEntity
import com.whatsup.automation.data.local.entity.StatusEntity
import com.whatsup.automation.data.local.entity.VaultMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority ASC")
    fun getEnabledRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleRule(id: Long, enabled: Boolean)

    @Query("SELECT * FROM rules ORDER BY priority ASC")
    suspend fun getAllRulesList(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RuleEntity>)

    @Query("DELETE FROM rules")
    suspend fun clearAllRules()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<LogEntity>>

    @Query("SELECT * FROM activity_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM activity_logs")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM activity_logs WHERE status = 'SUCCESS'")
    suspend fun getRepliesSentCount(): Int

    @Query("SELECT COUNT(*) FROM activity_logs WHERE status = 'NO_MATCH'")
    suspend fun getNoMatchCount(): Int

    @Query("DELETE FROM activity_logs WHERE status = 'NO_MATCH' OR actionExecuted LIKE '%تجاهل%'")
    suspend fun purgeClutterLogs(): Int

    @Query("DELETE FROM activity_logs WHERE id NOT IN (SELECT id FROM activity_logs ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldLogs(keepCount: Int = 1000): Int

    @Query("SELECT COUNT(*) FROM activity_logs WHERE extractedName IS NOT NULL AND extractedName != ''")
    suspend fun getContactsSavedCount(): Int

    @Query("SELECT matchedRule, COUNT(*) as count FROM activity_logs WHERE status = 'SUCCESS' AND matchedRule != '' AND matchedRule != 'درع منع التكرار — حماية LID' GROUP BY matchedRule ORDER BY count DESC LIMIT :limit")
    suspend fun getTopUsedRules(limit: Int = 5): List<RuleUsageStat>

    @Query("SELECT timestamp FROM activity_logs WHERE timestamp >= :sinceTimestamp")
    suspend fun getLogTimestampsSince(sinceTimestamp: Long): List<Long>
}

data class RuleUsageStat(
    val matchedRule: String,
    val count: Int
)

/**
 * DAO لقصص الحالات (Statuses) والتفاعل التلقائي.
 */
@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :statusId LIMIT 1")
    suspend fun getStatusById(statusId: String): StatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("UPDATE statuses SET isViewed = 1, viewedAt = :timestamp WHERE id = :statusId")
    suspend fun markAsViewed(statusId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE statuses SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusViewed(statusId: String)

    @Query("UPDATE statuses SET isReacted = 1, reactionEmoji = :emoji, reactionSentAt = :timestamp, isViewed = 1, viewedAt = CASE WHEN viewedAt IS NULL THEN :timestamp ELSE viewedAt END WHERE id = :statusId")
    suspend fun updateReaction(statusId: String, emoji: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE statuses SET isReacted = 1, reactionEmoji = :emoji WHERE id = :statusId")
    suspend fun reactToStatus(statusId: String, emoji: String)

    @Query("DELETE FROM statuses WHERE timestamp < :olderThanTimestamp AND isDeleted = 0")
    suspend fun deleteExpiredStatuses(olderThanTimestamp: Long)

    @Query("UPDATE statuses SET isFavorite = :isFavorite WHERE id = :statusId")
    suspend fun updateFavorite(statusId: String, isFavorite: Boolean)

    @Query("UPDATE statuses SET isMuted = :isMuted WHERE id = :statusId")
    suspend fun updateMuted(statusId: String, isMuted: Boolean)

    @Query("UPDATE statuses SET isFavorite = :isFavorite WHERE senderPhone = :senderPhone")
    suspend fun updateFavoriteBySender(senderPhone: String, isFavorite: Boolean)

    @Query("UPDATE statuses SET isMuted = :isMuted WHERE senderPhone = :senderPhone")
    suspend fun updateMutedBySender(senderPhone: String, isMuted: Boolean)

    @Query("UPDATE statuses SET isDeleted = 1, deletedAt = :timestamp WHERE id = :statusId")
    suspend fun markAsDeleted(statusId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM statuses WHERE isDeleted = 1 AND deletedAt < :olderThanTimestamp")
    suspend fun deleteExpiredDeletedStatuses(olderThanTimestamp: Long)
}

data class GroupWithMemberCount(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val memberCount: Int
)

/**
 * DAO للمجموعات.
 */
@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT g.id, g.name, g.description, g.createdAt, COUNT(m.id) AS memberCount FROM groups g LEFT JOIN group_members m ON g.id = m.groupId GROUP BY g.id ORDER BY g.createdAt DESC")
    fun getAllGroupsWithMemberCount(): Flow<List<GroupWithMemberCount>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)
}

/**
 * DAO لأعضاء المجموعة.
 */
@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroupSync(groupId: Long): List<GroupMemberEntity>

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    fun getMemberCount(groupId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity): Long

    @Query("DELETE FROM group_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND phone = :phone")
    suspend fun deleteMemberByPhone(groupId: Long, phone: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: Long)
}

/**
 * DAO لسجلات بث المجموعة.
 */
@Dao
interface GroupLogDao {
    @Query("SELECT * FROM group_logs WHERE groupId = :groupId ORDER BY sentAt DESC, id DESC")
    fun getLogsForGroup(groupId: Long): Flow<List<GroupLogEntity>>

    @Query("SELECT * FROM group_logs WHERE groupId = :groupId AND status = 'IN_PROGRESS' ORDER BY id DESC")
    fun getInsiteLogsForGroup(groupId: Long): Flow<List<GroupLogEntity>>

    @Query("SELECT * FROM group_logs WHERE groupId = :groupId AND status != 'IN_PROGRESS' ORDER BY id DESC")
    fun getCompletedLogsForGroup(groupId: Long): Flow<List<GroupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GroupLogEntity): Long

    @Query("UPDATE group_logs SET status = :status, sentAt = :sentAt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateLogStatus(id: Long, status: String, sentAt: Long?, errorMessage: String?)
}

/**
 * DAO للخزينة السرية (وسائط View-Once، الرسائل المحذوفة، والحالات المحفوظة).
 */
@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_media ORDER BY timestamp DESC")
    fun getAllVaultItems(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isViewOnce = 1 ORDER BY timestamp DESC")
    fun getViewOnceItems(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedItems(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteItems(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): VaultMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultMediaEntity)

    @Update
    suspend fun updateItem(item: VaultMediaEntity)

    @Query("UPDATE vault_media SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: String)

    @Query("UPDATE vault_media SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT COUNT(*) FROM vault_media WHERE isViewOnce = 1")
    fun getViewOnceCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE isDeleted = 1")
    fun getDeletedCount(): Flow<Int>
}

