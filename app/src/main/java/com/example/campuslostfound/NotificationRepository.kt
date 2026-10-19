package com.example.campuslostfound

import android.util.Log
import com.example.campuslostfound.model.Notification
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Notifications on Supabase Postgres (was Firestore collection "notifications").
 * Reads are live flows kept up to date by Supabase Realtime.
 * Writes keep the old fire-and-forget signatures (they run on [SupabaseProvider.ioScope]).
 */
class NotificationRepository {

    private val supabase get() = SupabaseProvider.client

    fun getNotifications(userId: String): Flow<List<Notification>> =
        SupabaseProvider.liveTableFlow("notifications") {
            supabase.postgrest["notifications"].select {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<Notification>()
        }

    fun markMessagesAsRead(userId: String, chatId: String) {
        SupabaseProvider.ioScope.launch {
            runCatching {
                supabase.postgrest["notifications"].update({
                    set("seen", true)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("related_id", chatId)
                        eq("type", "Message")
                        eq("seen", false)
                    }
                }
            }.onFailure { Log.e(TAG, "markMessagesAsRead failed", it) }
        }
    }

    fun markAsRead(notificationId: String) {
        SupabaseProvider.ioScope.launch {
            runCatching {
                supabase.postgrest["notifications"].update({
                    set("seen", true)
                }) {
                    filter {
                        eq("id", notificationId)
                    }
                }
            }.onFailure { Log.e(TAG, "markAsRead failed", it) }
        }
    }

    fun markAllAsRead(userId: String) {
        SupabaseProvider.ioScope.launch {
            runCatching {
                supabase.postgrest["notifications"].update({
                    set("seen", true)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("seen", false)
                    }
                }
            }.onFailure { Log.e(TAG, "markAllAsRead failed", it) }
        }
    }

    fun sendNotification(notification: Notification) {
        // A blank recipient (unknown other participant) would insert a row nobody
        // can ever read — skip it instead of writing orphaned data.
        if (notification.userId.isBlank()) return
        SupabaseProvider.ioScope.launch {
            // id stays "" → omitted → the database generates a uuid.
            runCatching {
                supabase.postgrest["notifications"].insert(notification)
            }.onFailure { Log.e(TAG, "sendNotification failed", it) }
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
