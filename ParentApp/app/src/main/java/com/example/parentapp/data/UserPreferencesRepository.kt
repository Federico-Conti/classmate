package com.example.parentapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "parentapp_prefs")

class UserPreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val PARENT_ID: Preferences.Key<String> = stringPreferencesKey("parent_id")
    }

    /** Stream del Parent ID salvato (null se non presente). */
    val parentIdFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.PARENT_ID] }

    /** Salva Parent ID. */
    suspend fun saveParentId(parentId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PARENT_ID] = parentId
        }
    }

    /** Cancella Parent ID (logout). */
    suspend fun clearParentId() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PARENT_ID)
        }
    }
}
