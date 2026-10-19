package com.example.campuslostfound

import com.example.campuslostfound.model.Item
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

/**
 * Items feed on Supabase Postgres (was Firestore collection "items").
 * Reads are live flows kept up to date by Supabase Realtime.
 */
class ItemRepository {

    private val supabase get() = SupabaseProvider.client

    fun getItems(type: String? = null): Flow<List<Item>> =
        SupabaseProvider.liveTableFlow("items") {
            supabase.postgrest["items"].select {
                filter {
                    eq("status", "Open")
                    if (type != null) eq("type", type)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<Item>()
        }

    fun getMyReports(userId: String): Flow<List<Item>> =
        SupabaseProvider.liveTableFlow("items") {
            supabase.postgrest["items"].select {
                filter {
                    eq("reported_by", userId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<Item>()
        }

    suspend fun getItem(itemId: String): Result<Item?> {
        return try {
            Result.success(
                supabase.postgrest["items"].select {
                    filter {
                        eq("id", itemId)
                    }
                    limit(1)
                }.decodeSingleOrNull<Item>()
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportItem(item: Item): Result<Unit> {
        return try {
            // id stays "" (its default) so it isn't serialized and the database
            // generates a uuid (same role as Firestore's auto doc id).
            supabase.postgrest["items"].insert(item) {
                select()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveItem(itemId: String): Result<Unit> {
        return try {
            supabase.postgrest["items"].update({
                set("status", "Resolved")
            }) {
                filter {
                    eq("id", itemId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
