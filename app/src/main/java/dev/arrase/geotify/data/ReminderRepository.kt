package dev.arrase.geotify.data

import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun observeReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAll()

    fun observeActiveReminderCounts(): Flow<List<LocationReminderCount>> =
        reminderDao.observeActiveReminderCounts()

    fun observeActiveReminderCount(locationId: String): Flow<Int> =
        reminderDao.observeActiveCountForLocation(locationId)

    suspend fun createReminder(
        location: LocationEntity,
        message: String,
        transitionType: Int
    ): ReminderEntity = withContext(ioDispatcher) {
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            locationId = location.id,
            message = message,
            transitionType = transitionType,
            createdAt = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        reminder
    }

    suspend fun updateReminder(reminder: ReminderEntity) = withContext(ioDispatcher) {
        reminderDao.update(reminder)
    }

    suspend fun deactivateReminder(reminderId: String) = withContext(ioDispatcher) {
        reminderDao.deactivate(reminderId)
    }

    suspend fun cancelReminder(reminderId: String) = withContext(ioDispatcher) {
        reminderDao.deleteById(reminderId)
    }

    suspend fun findReminderById(id: String): ReminderEntity? = withContext(ioDispatcher) {
        reminderDao.findById(id)
    }

    suspend fun getActiveReminders(): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderDao.getActiveReminders()
    }

    suspend fun getActiveRemindersForLocation(locationId: String): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderDao.getActiveByLocationId(locationId)
    }

    suspend fun updateInRangeStatus(locationIds: List<String>) = withContext(ioDispatcher) {
        reminderDao.updateInRangeStatus(locationIds)
    }
}
