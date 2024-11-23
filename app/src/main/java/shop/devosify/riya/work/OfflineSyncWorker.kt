package shop.devosify.riya.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import shop.devosify.riya.repository.ConversationRepository
import shop.devosify.riya.sync.OfflineManager
import shop.devosify.riya.util.NetworkUtils
import shop.devosify.riya.util.analytics.AnalyticsService

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val offlineManager: OfflineManager,
    private val networkUtils: NetworkUtils,
    private val analyticsService: AnalyticsService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.retry()
        }

        return try {
            // Collect sync status
            var syncResult: OfflineManager.SyncStatus? = null
            val job = CoroutineScope(Dispatchers.IO).launch {
                offlineManager.syncStatus.collect { status ->
                    syncResult = status
                    if (status is OfflineManager.SyncStatus.Success || 
                        status is OfflineManager.SyncStatus.Error) {
                        this.cancel()
                    }
                }
            }

            // Wait for sync to complete or timeout
            withTimeout(10.minutes) {
                job.join()
            }

            when (syncResult) {
                is OfflineManager.SyncStatus.Success -> {
                    analyticsService.logEvent("offline_sync_completed", mapOf(
                        "actions_processed" to (syncResult as OfflineManager.SyncStatus.Success).actionCount
                    ))
                    Result.success()
                }
                is OfflineManager.SyncStatus.Error -> {
                    analyticsService.logError(
                        "offline_sync_failed",
                        (syncResult as OfflineManager.SyncStatus.Error).message
                    )
                    Result.retry()
                }
                else -> Result.retry()
            }
        } catch (e: Exception) {
            analyticsService.logError("offline_sync_worker", e.message ?: "Unknown error")
            Result.failure()
        }
    }
} 