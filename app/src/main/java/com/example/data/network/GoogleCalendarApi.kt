package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class GoogleCalendarListResponse(
    @Json(name = "items") val items: List<GoogleEvent> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GoogleEvent(
    @Json(name = "id") val id: String,
    @Json(name = "summary") val summary: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "start") val start: GoogleTime?,
    @Json(name = "end") val end: GoogleTime?
)

@JsonClass(generateAdapter = true)
data class GoogleTime(
    @Json(name = "dateTime") val dateTime: String?, // "2026-05-21T15:00:00Z"
    @Json(name = "date") val date: String? // "2026-05-21"
)

@JsonClass(generateAdapter = true)
data class CreateGoogleEventRequest(
    @Json(name = "summary") val summary: String,
    @Json(name = "description") val description: String,
    @Json(name = "start") val start: GoogleTime,
    @Json(name = "end") val end: GoogleTime
)

interface GoogleCalendarApi {
    @GET("calendars/primary/events")
    suspend fun getEvents(
        @Header("Authorization") authHeader: String,
        @Query("timeMin") timeMin: String? = null,
        @Query("maxResults") maxResults: Int = 100,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("singleEvents") singleEvents: Boolean = true
    ): GoogleCalendarListResponse

    @POST("calendars/primary/events")
    suspend fun createEvent(
        @Header("Authorization") authHeader: String,
        @Body request: CreateGoogleEventRequest
    ): GoogleEvent
}
