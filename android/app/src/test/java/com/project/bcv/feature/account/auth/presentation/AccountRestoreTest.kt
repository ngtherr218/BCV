package com.project.bcv.feature.account.auth.presentation

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStore
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.account.auth.domain.AccountRepository
import com.project.bcv.feature.account.auth.domain.AccountSession
import com.project.bcv.feature.account.auth.domain.AccountUser
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccountRestoreTest {
    private val release = CountDownLatch(1)
    private val store = ViewModelStore()
    @Before fun immediateLiveData() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
    }
    @After fun close() {
        release.countDown()
        store.clear()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    private fun restored(result: () -> AccountSession?, whileChecking: (AccountViewModel) -> Unit = {}): AccountUiState {
        val repository = FakeAccountRepository { release.await(3, TimeUnit.SECONDS); result() }
        val model = AccountViewModel(repository)
        store.put("account", model)
        val done = CountDownLatch(1)
        val observer = Observer<AccountUiState> { if (!it.restoring && repository.calls > 0) done.countDown() }
        model.state.observeForever(observer)
        model.restoreSession()
        assertTrue(model.state.value!!.restoring)
        whileChecking(model)
        release.countDown()
        assertTrue("Restore must leave its loading state", done.await(3, TimeUnit.SECONDS))
        model.restoreSession() // A recreated Activity observes the same ViewModel.
        assertEquals(1, repository.calls)
        model.state.removeObserver(observer)
        return model.state.value!!
    }
    private fun session(needsInterests: Boolean = false) = AccountSession("test-token",
        AccountUser(1, "Test", "test@example.invalid"), needsInterests, false, false)

    @Test fun validSessionContinuesIntoTheApp() {
        assertTrue(restored({ session() }).completed)
    }
    @Test fun rejectedSessionReturnsToTheFormWithAnExplanation() {
        val state = restored({ throw ApiFailure("UNAUTHORIZED", "Phiên đã hết hạn") })
        assertFalse(state.restoring)
        assertFalse(state.completed)
        assertEquals("Phiên đã hết hạn", state.requestError)
    }
    @Test fun networkFailurePreservesTheExistingOfflineEntryBehavior() {
        assertTrue(restored({ throw IOException("offline") }).completed)
    }
    @Test fun incompleteOnboardingContinuesToInterests() {
        val state = restored({ session(needsInterests = true) })
        assertTrue(state.needsInterestSelection)
        assertFalse(state.completed)
    }
    @Test fun editsAndSubmitsCannotReplaceAnInFlightRestore() {
        restored({ session() }) { model ->
            model.changeMode()
            model.onFieldEdited(AccountField.EMAIL)
            model.submit("other@example.invalid", "password", "password")
            assertTrue(model.state.value!!.restoring)
            assertEquals(AccountMode.LOGIN, model.state.value!!.mode)
        }
    }
    @Test fun missingSessionReturnsToTheForm() {
        val state = restored({ null })
        assertFalse(state.restoring)
        assertFalse(state.completed)
    }
}

private class FakeAccountRepository(private val restoreResult: () -> AccountSession?) : AccountRepository {
    @Volatile var calls = 0
    override fun restore(): AccountSession? { calls++; return restoreResult() }
    override fun register(email: String, password: String, confirmPassword: String): AccountSession = error("Unexpected register")
    override fun login(email: String, password: String): AccountSession = error("Unexpected login")
    override fun logout() = Unit
    override fun requestPasswordReset(email: String) = 60
    override fun verifyPasswordResetCode(email: String, code: String) = "unused"
    override fun completePasswordReset(resetToken: String, password: String, confirmPassword: String) = Unit
}
