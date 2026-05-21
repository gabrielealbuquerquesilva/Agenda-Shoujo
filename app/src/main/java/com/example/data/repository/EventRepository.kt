package com.example.data.repository

import com.example.data.local.EventDao
import com.example.data.local.EventEntity
import com.example.data.network.GoogleCalendarApi
import com.example.data.network.OAuthTokenManager
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val localEvents: Flow<List<EventEntity>> = eventDao.getLocalEvents()
    val googleEvents: Flow<List<EventEntity>> = eventDao.getGoogleEvents()

    private val api: GoogleCalendarApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/calendar/v3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GoogleCalendarApi::class.java)
    }

    suspend fun insertLocalEvent(
        title: String,
        description: String,
        dateTime: Long,
        category: String
    ): EventEntity {
        val event = EventEntity(
            title = title,
            description = description,
            dateTime = dateTime,
            category = category,
            isCompleted = false,
            isGoogleEvent = false
        )
        val id = eventDao.insertEvent(event)
        return event.copy(id = id.toInt())
    }

    suspend fun updateEvent(event: EventEntity) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(id: Int) {
        eventDao.deleteEventById(id)
    }

    suspend fun toggleCompleted(event: EventEntity) {
        eventDao.updateEvent(event.copy(isCompleted = !event.isCompleted))
    }

    suspend fun syncGoogleCalendar(tokenManager: OAuthTokenManager): Result<List<EventEntity>> {
        val accessToken = tokenManager.getAccessToken() ?: return Result.failure(Exception("Não autorizado no Google"))
        val authHeader = "Bearer $accessToken"

        return try {
            // Fetch events starting from 7 days ago to keep it fresh
            val timeMinISO = Instant.now().minusSeconds(7 * 24 * 3600).toString()
            val response = api.getEvents(authHeader, timeMin = timeMinISO)

            val mappedEvents = response.items.map { gevent ->
                val start = gevent.start
                val epoch = when {
                    start?.dateTime != null -> {
                        try {
                            Instant.parse(start.dateTime).toEpochMilli()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    }
                    start?.date != null -> {
                        try {
                            LocalDate.parse(start.date)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    }
                    else -> System.currentTimeMillis()
                }

                EventEntity(
                    title = gevent.summary ?: "Compromisso de Luna",
                    description = gevent.description ?: "Importado da Google Agenda",
                    dateTime = epoch,
                    category = "Mágico", // Imported Google Calendar events styled in magic theme by default
                    isCompleted = false,
                    isGoogleEvent = true,
                    googleEventId = gevent.id
                )
            }

            // Transactional update: clear old google events, and write new ones
            eventDao.clearGoogleEvents()
            eventDao.insertEvents(mappedEvents)

            Result.success(mappedEvents)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
