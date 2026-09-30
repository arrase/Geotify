package dev.arrase.geotify.data

import com.google.android.gms.location.Geofence
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

    suspend fun createReminder(
        location: LocationEntity,
        message: String,
        transitionType: Int
    ): ReminderEntity = withContext(ioDispatcher) {
        require(message.isNotBlank()) { "Reminder message must not be blank" }
        require(transitionType in SUPPORTED_TRANSITIONS) {
            "Unsupported geofence transition type: $transitionType"
        }
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            locationId = location.id,
            message = message.trim(),
            transitionType = transitionType,
            createdAt = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        reminder
    }

    suspend fun updateReminder(reminder: ReminderEntity) = withContext(ioDispatcher) {
        require(reminder.message.isNotBlank()) { "Reminder message must not be blank" }
        require(reminder.transitionType in SUPPORTED_TRANSITIONS) {
            "Unsupported geofence transition type: ${reminder.transitionType}"
        }
        reminderDao.update(reminder)
    }

    suspend fun deactivateReminder(reminderId: String) = withContext(ioDispatcher) {
        reminderDao.deactivate(reminderId)
    }

    /** @return `true` if a reminder was removed. */
    suspend fun cancelReminder(reminderId: String): Boolean = withContext(ioDispatcher) {
        reminderDao.deleteById(reminderId) > 0
    }

    suspend fun getActiveReminders(): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderDao.getActiveReminders()
    }

    suspend fun getActiveRemindersForLocation(locationId: String): List<ReminderEntity> =
        withContext(ioDispatcher) {
            reminderDao.getActiveByLocationId(locationId)
        }

    suspend fun updateInRangeStatus(locationIds: List<String>) = withContext(ioDispatcher) {
        reminderDao.updateInRangeStatus(locationIds)
    }

    private companion object {
        val SUPPORTED_TRANSITIONS = setOf(
            Geofence.GEOFENCE_TRANSITION_ENTER,
            Geofence.GEOFENCE_TRANSITION_EXIT
        )
    }
}
