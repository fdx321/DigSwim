package com.digswim.app.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserProfile(
    val nickname: String,
    val bio: String,
    val avatarUrl: String? = null,
    val gender: String = "Male",
    val poolLength: Int = 25
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val BIO = stringPreferencesKey("bio")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val GENDER = stringPreferencesKey("gender")
        val POOL_LENGTH = intPreferencesKey("pool_length")
    }

    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            nickname = prefs[Keys.NICKNAME] ?: "",
            bio = prefs[Keys.BIO] ?: "脚步丈量世界的第239日",
            avatarUrl = prefs[Keys.AVATAR_URL],
            gender = prefs[Keys.GENDER] ?: "Male",
            poolLength = prefs[Keys.POOL_LENGTH] ?: 25
        )
    }

    suspend fun updateUserProfile(nickname: String, bio: String, avatarUrl: String?, gender: String, poolLength: Int) {
        // Persist permission for content URIs (e.g. from Photo Picker)
        if (avatarUrl != null) {
            try {
                val uri = Uri.parse(avatarUrl)
                // Only content URIs require permission persistence
                if (uri.scheme == "content") {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                }
            } catch (e: Exception) {
                // Ignore if not a valid URI or if permission taking fails (e.g. not a persistent URI)
                e.printStackTrace()
            }
        }

        dataStore.edit { prefs ->
            prefs[Keys.NICKNAME] = nickname
            prefs[Keys.BIO] = bio
            if (avatarUrl != null) {
                prefs[Keys.AVATAR_URL] = avatarUrl
            }
            prefs[Keys.GENDER] = gender
            prefs[Keys.POOL_LENGTH] = poolLength
        }
    }
}
