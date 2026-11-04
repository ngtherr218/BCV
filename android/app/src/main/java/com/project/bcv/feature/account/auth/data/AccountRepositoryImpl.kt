package com.project.bcv.feature.account.auth.data

import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.domain.AccountRepository
import com.project.bcv.feature.account.auth.domain.AccountSession
import com.project.bcv.feature.account.auth.domain.AccountUser
import org.json.JSONObject

class AccountRepositoryImpl(
    private val remote: JsonApiClient,
    private val local: SessionPreferences
) : AccountRepository {
    override fun register(email: String, password: String, confirmPassword: String): AccountSession {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword)
        return remote.post("api/account/register", body).toSession().also { local.token = it.token }
    }

    override fun login(email: String, password: String): AccountSession {
        val body = JSONObject().put("email", email).put("password", password)
        val response = remote.post("api/account/login", body)
        val session = response.toSession()
        local.token = session.token
        return session
    }

    override fun logout() {
        val token = local.token
        local.token = null
        if (token != null) runCatching {
            remote.post("api/account/logout", JSONObject(), token)
        }
    }

    override fun restore(): AccountSession? {
        val token = local.token ?: return null
        val me = try {
            remote.get("api/account/me", token)
        } catch (error: ApiFailure) {
            if (error.code == "UNAUTHORIZED") local.token = null
            throw error
        }
        return AccountSession(token, me.toAccountUser(), me.getBoolean("needsInterestSelection"),
            me.getBoolean("needsActivityLevel"), me.getBoolean("needsVirtualLocation"))
    }

    override fun requestPasswordReset(email: String): Int = remote.post(
        "api/account/password-reset/request", JSONObject().put("email", email)
    ).optInt("resendAfterSeconds", 60)

    override fun verifyPasswordResetCode(email: String, code: String): String = remote.post(
        "api/account/password-reset/verify", JSONObject().put("email", email).put("code", code)
    ).getString("resetToken")

    override fun completePasswordReset(resetToken: String, password: String, confirmPassword: String) {
        remote.post("api/account/password-reset/complete", JSONObject()
            .put("resetToken", resetToken)
            .put("password", password)
            .put("confirmPassword", confirmPassword))
    }

    private fun JSONObject.toAccountUser() = AccountUser(
        getLong("userId"),
        getString("fullName"),
        getString("email")
    )

    private fun JSONObject.toSession() = AccountSession(
        getString("token"),
        getJSONObject("user").toAccountUser(),
        getBoolean("needsInterestSelection"),
        getBoolean("needsActivityLevel"),
        getBoolean("needsVirtualLocation")
    )
}
