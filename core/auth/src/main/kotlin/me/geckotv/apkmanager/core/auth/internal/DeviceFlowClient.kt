package me.geckotv.apkmanager.core.auth.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import me.geckotv.apkmanager.core.auth.DeviceCode
import me.geckotv.apkmanager.core.auth.GitHubOAuthConfig
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Runs GitHub's OAuth device flow (no client secret needed). */
internal class DeviceFlowClient
    @Inject
    constructor(
        private val api: GitHubOAuthApi,
        private val config: GitHubOAuthConfig,
        private val clock: Clock,
    ) {
        sealed interface StartResult {
            data class Started(
                val deviceCode: DeviceCode,
            ) : StartResult

            data class Failed(
                val reason: SignInFailure,
            ) : StartResult
        }

        sealed interface TokenResult {
            data class Success(
                val accessToken: String,
                val scopes: Set<String>,
            ) : TokenResult

            data class Failed(
                val reason: SignInFailure,
            ) : TokenResult
        }

        suspend fun start(accessLevel: GitHubAccessLevel): StartResult =
            request(onFailure = StartResult::Failed) {
                val response = api.requestDeviceCode(config.clientId, scopeFor(accessLevel))
                val deviceCode = response.deviceCode
                val userCode = response.userCode
                val verificationUri = response.verificationUri
                if (deviceCode == null || userCode == null || verificationUri == null) {
                    // e.g. "device_flow_disabled" or an unknown client ID
                    StartResult.Failed(SignInFailure.Unknown)
                } else {
                    StartResult.Started(
                        DeviceCode(
                            userCode = userCode,
                            verificationUri = verificationUri,
                            expiresAt = Instant.now(clock).plusSeconds(response.expiresIn),
                            deviceCode = deviceCode,
                            intervalSeconds = response.interval,
                            accessLevel = accessLevel,
                        ),
                    )
                }
            }

        /** Polls until the user authorized or denied the code, or the code expired. */
        suspend fun awaitToken(deviceCode: DeviceCode): TokenResult {
            var intervalSeconds = deviceCode.intervalSeconds.coerceAtLeast(MIN_INTERVAL_SECONDS)
            var result: TokenResult? = null
            while (result == null) {
                delay(intervalSeconds * MILLIS_PER_SECOND)
                if (Instant.now(clock).isAfter(deviceCode.expiresAt)) {
                    result = TokenResult.Failed(SignInFailure.Expired)
                } else {
                    val poll = poll(deviceCode)
                    when {
                        poll is PollResult.Done -> {
                            result = poll.result
                        }

                        poll is PollResult.SlowDown -> {
                            intervalSeconds =
                                poll.intervalSeconds ?: (intervalSeconds + SLOW_DOWN_SECONDS)
                        }
                    }
                }
            }
            return result
        }

        private sealed interface PollResult {
            data object Pending : PollResult

            data class SlowDown(
                val intervalSeconds: Int?,
            ) : PollResult

            data class Done(
                val result: TokenResult,
            ) : PollResult
        }

        private suspend fun poll(deviceCode: DeviceCode): PollResult =
            request(onFailure = { reason -> PollResult.Done(TokenResult.Failed(reason)) }) {
                val response = api.requestAccessToken(config.clientId, deviceCode.deviceCode)
                val token = response.accessToken
                when {
                    token != null -> PollResult.Done(TokenResult.Success(token, parseScopes(response.scope)))
                    response.error == "authorization_pending" -> PollResult.Pending
                    response.error == "slow_down" -> PollResult.SlowDown(response.interval)
                    response.error == "expired_token" -> PollResult.Done(TokenResult.Failed(SignInFailure.Expired))
                    response.error == "access_denied" -> PollResult.Done(TokenResult.Failed(SignInFailure.Denied))
                    else -> PollResult.Done(TokenResult.Failed(SignInFailure.Unknown))
                }
            }

        private suspend fun <T> request(
            onFailure: (SignInFailure) -> T,
            block: suspend () -> T,
        ): T =
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: IOException) {
                onFailure(SignInFailure.Network)
            } catch (_: HttpException) {
                onFailure(SignInFailure.Unknown)
            } catch (_: SerializationException) {
                onFailure(SignInFailure.Unknown)
            }

        companion object {
            private const val MIN_INTERVAL_SECONDS = 5
            private const val SLOW_DOWN_SECONDS = 5
            private const val MILLIS_PER_SECOND = 1_000L

            /** Private repositories need the `repo` scope; public data needs no scope at all. */
            fun scopeFor(accessLevel: GitHubAccessLevel): String =
                when (accessLevel) {
                    GitHubAccessLevel.Public -> ""
                    GitHubAccessLevel.Private -> "repo"
                }

            fun parseScopes(scope: String?): Set<String> =
                scope
                    .orEmpty()
                    .split(",", " ")
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toSet()
        }
    }
