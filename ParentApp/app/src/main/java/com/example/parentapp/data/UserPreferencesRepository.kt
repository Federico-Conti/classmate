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
        val SELECTED_CHILD_ID: Preferences.Key<String> = stringPreferencesKey("selected_child_id")
    }

    /** Stream del Parent ID salvato (null se non presente). */
    val parentIdFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.PARENT_ID] }

    val selectedChildIdFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.SELECTED_CHILD_ID] }

    suspend fun saveSelectedChildId(childId: String) {
        context.dataStore.edit { it[Keys.SELECTED_CHILD_ID] = childId }
    }

    suspend fun saveParentId(parentId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PARENT_ID] = parentId
        }
    }

    suspend fun clearParentId() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PARENT_ID)
            prefs.remove(Keys.SELECTED_CHILD_ID)
        }
    }
}
