package com.example.campuslostfound

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * The app's single Supabase client — replaces the old FirebaseAuth /
 * FirebaseFirestore / FirebaseStorage instances that were scattered across the
 * repositories.
 *
 * Credentials come from `gradle.properties` (supabase.url / supabase.anonKey)
 * and are baked into BuildConfig at build time.
 */
object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    /**
     * Scope for fire-and-forget writes, so the old non-suspend repository
     * methods (markAsRead, sendNotification, …) keep their exact signatures.
     */
    val ioScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    /**
     * The Firestore-snapshot equivalent on Supabase: emits [query] immediately,
     * then re-runs it after every INSERT/UPDATE/DELETE on [table] received over
     * Supabase Realtime.
     *
     * The full result set is re-fetched on each change instead of patching the
     * list from the event payload — simpler and always correct (e.g. rows that
     * stop matching the filter are properly dropped).
     *
     * Query failures emit an empty list instead of crashing the screen, which is
     * the same contract the old Firestore listeners had.
     */
    fun <T> liveTableFlow(table: String, query: suspend () -> List<T>): Flow<List<T>> =
        callbackFlow {
            val channel = client.realtime.channel("live_${table}_${UUID.randomUUID()}")

            // Must be created BEFORE subscribing to the channel.
            val changes = channel.postgresChangeFlow<PostgresAction>("public") {
                this.table = table
            }

            // Re-runs [query], keeping the last good result when it fails so a
            // transient error never blanks a screen that already had data.
            suspend fun emitQuery() {
                val result = try {
                    query()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Never swallow silently: an empty list here is
                    // indistinguishable from "the database is empty".
                    Log.e(TAG, "Query failed for table '$table'", e)
                    null
                }
                if (result != null) trySend(result)
            }

            // Re-run the query for every change event — and whenever the session
            // changes, because the first query runs before the stored JWT is
            // restored (RLS would otherwise return nothing until the next event).
            val watcher = launch {
                try {
                    merge(
                        changes.map { Unit },
                        client.auth.sessionStatus.drop(1).map { Unit }
                    ).collect {
                        emitQuery()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Realtime can reject the channel (bad JWT, realtime disabled).
                    // viewModelScope is a SupervisorJob, so an uncaught error here
                    // would reach the default handler and crash the app.
                    Log.e(TAG, "Realtime watcher for '$table' failed", e)
                }
            }

            // Cleanup when the screen stops collecting: leave the realtime
            // channel so nothing leaks. NonCancellable so the websocket frame
            // still gets sent while this scope is already cancelled.
            launch {
                try {
                    awaitCancellation()
                } finally {
                    watcher.cancel()
                    withContext(NonCancellable) {
                        try {
                            client.realtime.removeChannel(channel)
                        } catch (_: Exception) {
                            // Best effort — the channel dies with the socket anyway.
                        }
                    }
                }
            }

            // Initial snapshot (Firestore's "first listener fire").
            emitQuery()

            // Subscribes and auto-connects the websocket (connectOnSubscribe default).
            try {
                channel.subscribe()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Realtime unavailable → the initial snapshot above still shows data.
            }

            awaitClose { }
        }

    private const val TAG = "SupabaseProvider"
}
