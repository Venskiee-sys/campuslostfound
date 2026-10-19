package com.example.campuslostfound

import com.example.campuslostfound.model.Chat
import com.example.campuslostfound.model.Message
import com.example.campuslostfound.model.Notification
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

/**
 * Chats + messages on Supabase Postgres (was Firestore "chats" with a
 * "messages" subcollection — now a flat messages table filtered by chat_id).
 * Reads are live flows kept up to date by Supabase Realtime.
 */
class ChatRepository {

    private val supabase get() = SupabaseProvider.client
    private val notifRepo = NotificationRepository()

    fun getChats(userId: String): Flow<List<Chat>> =
        SupabaseProvider.liveTableFlow("chats") {
            supabase.postgrest["chats"].select {
                filter {
                    contains("participant_ids", listOf(userId))
                }
                order("last_timestamp", Order.DESCENDING)
            }.decodeList<Chat>()
        }

    fun getMessages(chatId: String): Flow<List<Message>> =
        SupabaseProvider.liveTableFlow("messages") {
            supabase.postgrest["messages"].select {
                filter {
                    eq("chat_id", chatId)
                }
                order("created_at", Order.ASCENDING)
            }.decodeList<Message>()
        }

    suspend fun sendMessage(chatId: String, senderId: String, content: String) {
        val chat = supabase.postgrest["chats"].select {
            filter {
                eq("id", chatId)
            }
            limit(1)
        }.decodeSingleOrNull<Chat>()
            // Fail loudly: writing the message without the chat row would skip the
            // "last message" update and the recipient's notification.
            ?: throw IllegalStateException("This conversation no longer exists.")

        val otherId = chat.participantIds.firstOrNull { it != senderId } ?: ""
        val senderName = chat.participantNames[senderId] ?: "Someone"

        val message = Message(
            senderId = senderId,
            content = content,
            chatId = chatId,
            timestamp = System.currentTimeMillis()
        )

        supabase.postgrest["messages"].insert(message)

        supabase.postgrest["chats"].update({
            set("last_message", content)
            set("last_timestamp", System.currentTimeMillis())
        }) {
            filter {
                eq("id", chatId)
            }
        }

        // Create Notification
        notifRepo.sendNotification(
            Notification(
                userId = otherId,
                title = "New Message",
                message = "$senderName: $content",
                type = "Message",
                senderName = senderName,
                relatedId = chatId
            )
        )
    }

    suspend fun getOrCreateChat(
        myId: String,
        myName: String,
        otherId: String,
        otherName: String
    ): String {
        val myChats = supabase.postgrest["chats"].select {
            filter {
                contains("participant_ids", listOf(myId))
            }
        }.decodeList<Chat>()

        val existingChat = myChats.find { chat ->
            chat.participantIds.contains(otherId)
        }
        if (existingChat != null) {
            return existingChat.id
        }

        val newChat = Chat(
            participantIds = listOf(myId, otherId),
            participantNames = mapOf(myId to myName, otherId to otherName),
            lastMessage = "No messages yet",
            lastTimestamp = System.currentTimeMillis()
        )
        // id stays "" → omitted from the insert → the database generates a uuid.
        val inserted = supabase.postgrest["chats"].insert(newChat) {
            select()
        }.decodeSingle<Chat>()
        return inserted.id
    }
}
