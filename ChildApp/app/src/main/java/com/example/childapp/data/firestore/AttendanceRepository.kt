package com.example.childapp.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AttendanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun childDoc(childId: String) =
        db.collection("childrens").document(childId)

    private fun attendanceCol(childId: String) =
        childDoc(childId).collection("attendance")

    /**
     * Ritorna l'ultimo tipo registrato ("entry"/"exit") oppure null se non esiste.
     */
    suspend fun getLastType(childId: String): String? {
        val snap = attendanceCol(childId)
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snap.documents.firstOrNull() ?: return null
        return doc.getString("type")
    }

    /**
     * Inserisce un nuovo record in attendance (docId autogenerato).
     */
    suspend fun addAttendance(childId: String, type: String, time: Timestamp = Timestamp.now()) {
        val data = mapOf(
            "type" to type,
            "time" to time
        )
        attendanceCol(childId).add(data).await()
    }

    /**
     * (Opzionale) crea/aggiorna anagrafica del bambino.
     */
    suspend fun upsertChild(childId: String, name: String, surname: String) {
        val data = mapOf(
            "name" to name,
            "surname" to surname
        )
        childDoc(childId).set(data).await()
    }
}
