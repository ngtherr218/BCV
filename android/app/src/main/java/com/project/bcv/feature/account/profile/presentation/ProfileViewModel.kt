package com.project.bcv.feature.account.profile.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.bcv.core.presentation.Event
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.launchRemote
import com.project.bcv.core.presentation.loadInto
import com.project.bcv.feature.account.profile.domain.Profile
import com.project.bcv.feature.account.profile.domain.ProfileRepository
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationRepository

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val notifications: NotificationRepository
) : ViewModel() {
    private val mutableProfile = MutableLiveData<Remote<Profile>>()
    val profile: LiveData<Remote<Profile>> = mutableProfile
    private val mutableUnread = MutableLiveData<Int>()
    val unread: LiveData<Int> = mutableUnread
    private val mutableSignedOut = MutableLiveData<Event<Unit>>()
    /** Logged out or account closed: the screen returns to sign-in. */
    val signedOut: LiveData<Event<Unit>> = mutableSignedOut
    private val mutableMessage = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>> = mutableMessage
    private var busy = false
    private val mutableSigningOut = MutableLiveData(false)
    val signingOut: LiveData<Boolean> = mutableSigningOut

    init { loadProfile() }

    fun loadProfile() = loadInto(mutableProfile, profiles::profile)

    /** The unread count is a hint on the row; failures leave the default text. */
    fun refreshUnread() = launchRemote<NotificationPage>({ result ->
        if (result is Remote.Ready) mutableUnread.value = result.value.unread
    }, notifications::notifications)

    fun logout() = signOut(profiles::logout)

    fun deleteAccount() = signOut(profiles::deleteAccount)

    private fun signOut(call: () -> Unit) {
        if (busy) return
        launchRemote<Unit>({ result ->
            busy = result == Remote.Loading
            mutableSigningOut.value = busy
            when (result) {
                is Remote.Ready, Remote.SessionExpired -> mutableSignedOut.value = Event(Unit)
                is Remote.Failed -> mutableMessage.value = Event(result.message)
                Remote.Loading -> Unit
            }
        }, call)
    }
}
