package com.example.parentapp.data

import android.content.Context
import android.net.Uri
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.model.Child
import com.example.parentapp.model.Parent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ParentRepository(
    context: Context
) {
    private val prefs = UserPreferencesRepository(context)

    // DataStore4
    val parentIdFlow: Flow<String?> = prefs.parentIdFlow
    val selectedChildIdFlow: Flow<String?> = prefs.selectedChildIdFlow
    suspend fun saveParentId(parentId: String) = prefs.saveParentId(parentId)
    suspend fun saveSelectedChildId(childId: String) = prefs.saveSelectedChildId(childId)
    suspend fun clearParentId() = prefs.clearParentId()

    suspend fun getParent(code: String): Parent? = withContext(Dispatchers.IO) {
        val connection = (URL("http://${com.example.parentapp.BuildConfig.POC_HOST}:1880/api/v1/parents/resolve")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(JSONObject().put("code", code).toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode == 404) return@withContext null
            if (connection.responseCode != 200) throw java.io.IOException("Code resolution failed: HTTP ${connection.responseCode}")
            val result = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val children = result.getJSONArray("children")
            Parent(
                id = result.getString("id"),
                name = result.optString("name"),
                surname = result.optString("surname"),
                childrens = (0 until children.length()).map { index ->
                    val child = children.getJSONObject(index)
                    Child(child.getString("id"), child.optString("name"), child.optString("surname"))
                }
            )
        } finally {
            connection.disconnect()
        }
    }

    data class AttendancePage(
        val records: List<AttendanceRecord>,
        val nextCursor: String?
    )

    suspend fun getAttendancePage(
        childId: String,
        code: String,
        pageSize: Int,
        cursor: String?
    ): AttendancePage = withContext(Dispatchers.IO) {
        val uri = Uri.parse("http://${com.example.parentapp.BuildConfig.POC_HOST}:1880/api/v1/children")
            .buildUpon()
            .appendPath(childId)
            .appendPath("events")
            .appendQueryParameter("limit", pageSize.toString())
            .apply { cursor?.let { appendQueryParameter("cursor", it) } }
            .build()
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("x-parent-code", code)
        }
        try {
            if (connection.responseCode != 200) {
                throw java.io.IOException("History request failed: HTTP ${connection.responseCode}")
            }
            val result = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val events = result.getJSONArray("events")
            val records = (0 until events.length()).map { index ->
                val event = events.getJSONObject(index)
                if (event.getString("childId") != childId) {
                    throw java.io.IOException("History response contains an unrelated child")
                }
                AttendanceRecord(
                    id = event.getString("eventId"),
                    type = event.getString("type"),
                    time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
                        .parse(event.getString("receivedAt"))!!.time
                )
            }.distinctBy { it.id }
            AttendancePage(
                records = records,
                nextCursor = if (result.isNull("nextCursor")) null else result.getString("nextCursor")
            )
        } finally {
            connection.disconnect()
        }
    }
}
