package com.example.parentapp.data

import android.content.Context
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.model.Child
import com.example.parentapp.model.Parent
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MyAppRepository(
    context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val prefs = UserPreferencesRepository(context)

    // ---------- DataStore ----------
    val parentIdFlow: Flow<String?> = prefs.parentIdFlow
    suspend fun saveParentId(parentId: String) = prefs.saveParentId(parentId)
    suspend fun clearParentId() = prefs.clearParentId()

    // ---------- Firestore ----------
    suspend fun getParent(parentId: String): Parent? {
        val doc = firestore.collection("parents").document(parentId).get().await()
        if (!doc.exists()) return null

        val name = doc.getString("name").orEmpty()
        val surname = doc.getString("surname").orEmpty()
        val childrens = (doc.get("childrens") as? List<*>)?.mapNotNull { it as? String }.orEmpty()

        return Parent(
            id = parentId,
            name = name,
            surname = surname,
            childrens = childrens
        )
    }

    /**
     * Recupera i dati dei children usando documentId whereIn (max 10 per query),
     * quindi chunking se la lista è più lunga.
     */
    suspend fun getChildren(childIds: List<String>): List<Child> {
        if (childIds.isEmpty()) return emptyList()

        val chunks = childIds.chunked(10)
        val results = mutableListOf<Child>()

        for (chunk in chunks) {
            val snap = firestore.collection("childrens")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .await()

            results += snap.documents.map { d ->
                Child(
                    id = d.id,
                    name = d.getString("name").orEmpty(),
                    surname = d.getString("surname").orEmpty()
                )
            }
        }

        // Mantengo un ordine stabile (alfabetico)
        return results.sortedWith(compareBy({ it.surname.lowercase() }, { it.name.lowercase() }))
    }

    /**
     * Listener realtime sugli ultimi N record.
     * Ogni nuovo record su Firestore aggiorna la UI automaticamente.
     */
    fun listenLastAttendance(childId: String, limit: Long = 5): Flow<List<AttendanceRecord>> = callbackFlow {
        var reg: ListenerRegistration? = null

        reg = firestore.collection("childrens").document(childId)
            .collection("attendance")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // In caso di errore chiudiamo il flow con eccezione
                    close(err)
                    return@addSnapshotListener
                }
                val records = snap?.documents.orEmpty().map { doc ->
                    doc.toAttendanceRecord()
                }
                trySend(records)
            }

        awaitClose { reg?.remove() }
    }

    data class AttendancePage(
        val records: List<AttendanceRecord>,
        val nextCursor: DocumentSnapshot?
    )

    /**
     * Paging per History: prende una pagina di record ordinati per time desc.
     * startAfterSnapshot = cursore della pagina precedente (null per prima pagina).
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
        val next = if (docs.size == pageSize.toInt()) docs.lastOrNull() else null // <-- solo se c'è ancora una pagina
        return AttendancePage(records = records, nextCursor = next)
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
