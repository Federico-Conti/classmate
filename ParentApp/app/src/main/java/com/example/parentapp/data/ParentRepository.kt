package com.example.parentapp.data

import android.content.Context
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.model.Child
import com.example.parentapp.model.Parent
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ParentRepository(
    context: Context
) {
    private val prefs = UserPreferencesRepository(context)
    private val firestore by lazy { FirebaseFirestore.getInstance() }

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
                childrens = (0 until children.length()).map { children.getJSONObject(it).getString("id") }
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getChildren(code: String): List<Child> = withContext(Dispatchers.IO) {
        val connection = (URL("http://${com.example.parentapp.BuildConfig.POC_HOST}:1880/api/v1/children")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("x-parent-code", code)
        }
        try {
            if (connection.responseCode != 200) throw java.io.IOException("Children request failed: HTTP ${connection.responseCode}")
            val children = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("children")
            (0 until children.length()).map { index ->
                val child = children.getJSONObject(index)
                Child(child.getString("id"), child.optString("name"), child.optString("surname"))
            }
        } finally {
            connection.disconnect()
        }
    }

    data class AttendancePage(
        val records: List<AttendanceRecord>,
        val nextCursor: DocumentSnapshot? // current cursor
    )

    /**
     * Paging for History: takes a page of records sorted by time desc.
     * startAfterSnapshot = previous page cursor (null for first page).
     */
    suspend fun getAttendancePage(
        childId: String,
        pageSize: Long,
        startAfterSnapshot: DocumentSnapshot?
    ): AttendancePage {
        var q = firestore.collection("childrens").document(childId)
            .collection("attendance")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(pageSize)

        if (startAfterSnapshot != null) {
            q = q.startAfter(startAfterSnapshot)
        }

        val snap = q.get().await()
        val docs = snap.documents
        val records = docs.map { it.toAttendanceRecord() }
        val next = if (docs.size == pageSize.toInt()) docs.lastOrNull() else null
        return AttendancePage(records = records, nextCursor = next) // update cursor
    }

    private fun DocumentSnapshot.toAttendanceRecord(): AttendanceRecord {
        val type = getString("type").orEmpty()
        val ts = getTimestamp("time") ?: Timestamp.now()
        return AttendanceRecord(
            id = id,
            type = type,
            time = ts.toDate().time
        )
    }
}
