package me.geckotv.apkmanager.core.auth.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.geckotv.apkmanager.core.auth.di.CredentialsDataStore
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import java.io.IOException
import javax.inject.Inject

/** The stored sign-in: the decrypted token and the account it belongs to. */
internal data class StoredCredentials(
    val token: String,
    val account: GitHubAccount,
    val tokenInvalid: Boolean,
)

/**
 * Persists the token (encrypted with [TokenCipher]) and the account details in a DataStore file
 * that is excluded from backups.
 */
internal class CredentialsStore
    @Inject
    constructor(
        @param:CredentialsDataStore private val dataStore: DataStore<Preferences>,
        private val cipher: TokenCipher,
    ) {
        val credentials: Flow<StoredCredentials?> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences -> preferences.toCredentials() }
                .distinctUntilChanged()

        suspend fun save(
            token: String,
            account: GitHubAccount,
        ) {
            val encrypted = cipher.encrypt(token)
            dataStore.edit { preferences ->
                preferences.clear()
                preferences[Keys.TOKEN] = encrypted
                preferences[Keys.AUTH_METHOD] = account.authMethod.name
                account.accessLevel?.let { preferences[Keys.ACCESS_LEVEL] = it.name }
                preferences[Keys.LOGIN] = account.login
                account.name?.let { preferences[Keys.NAME] = it }
                account.avatarUrl?.let { preferences[Keys.AVATAR_URL] = it }
            }
        }

        suspend fun updateAccount(
            login: String,
            name: String?,
            avatarUrl: String?,
        ) {
            dataStore.edit { preferences ->
                if (preferences[Keys.TOKEN] == null) return@edit
                preferences[Keys.LOGIN] = login
                if (name != null) preferences[Keys.NAME] = name else preferences.remove(Keys.NAME)
                if (avatarUrl != null) preferences[Keys.AVATAR_URL] = avatarUrl else preferences.remove(Keys.AVATAR_URL)
                preferences[Keys.TOKEN_INVALID] = false
            }
        }

        suspend fun markTokenInvalid() {
            dataStore.edit { preferences ->
                if (preferences[Keys.TOKEN] != null) preferences[Keys.TOKEN_INVALID] = true
            }
        }

        suspend fun clear() {
            dataStore.edit { it.clear() }
        }

        private fun Preferences.toCredentials(): StoredCredentials? {
            val token = this[Keys.TOKEN]?.let(cipher::decrypt)
            val login = this[Keys.LOGIN]
            // A token that cannot be decrypted (e.g. restored to another device) counts as signed out.
            if (token == null || login == null) return null
            return StoredCredentials(
                token = token,
                account =
                    GitHubAccount(
                        login = login,
                        name = this[Keys.NAME],
                        avatarUrl = this[Keys.AVATAR_URL],
                        authMethod =
                            this[Keys.AUTH_METHOD]?.let { name -> AuthMethod.entries.firstOrNull { it.name == name } }
                                ?: AuthMethod.OAuth,
                        accessLevel =
                            this[Keys.ACCESS_LEVEL]?.let { name ->
                                GitHubAccessLevel.entries.firstOrNull { it.name == name }
                            },
                    ),
                tokenInvalid = this[Keys.TOKEN_INVALID] ?: false,
            )
        }

        private object Keys {
            val TOKEN = stringPreferencesKey("token")
            val AUTH_METHOD = stringPreferencesKey("auth_method")
            val ACCESS_LEVEL = stringPreferencesKey("access_level")
            val LOGIN = stringPreferencesKey("login")
            val NAME = stringPreferencesKey("name")
            val AVATAR_URL = stringPreferencesKey("avatar_url")
            val TOKEN_INVALID = booleanPreferencesKey("token_invalid")
        }
    }
