package com.project.bcv.feature.statsnotify.notifications.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.bcv.core.presentation.Event
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.launchRemote
import com.project.bcv.core.presentation.loadInto
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationPage
import com.project.bcv.feature.statsnotify.notifications.domain.NotificationRepository

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val mutableState = MutableLiveData<Remote<NotificationPage>>()
    val state: LiveData<Remote<NotificationPage>> = mutableState
    private val mutableMessage = MutableLiveData<Event<String>>()
    /** A failed "mark as read"; the list already shown stays. */
    val message: LiveData<Event<String>> = mutableMessage

    init { load() }

    fun load() = loadInto(mutableState, repository::notifications)

    fun markRead(id: Long) = launchRemote<NotificationPage>({ result ->
        when (result) {
            is Remote.Ready, Remote.SessionExpired -> mutableState.value = result
            is Remote.Failed -> mutableMessage.value = Event(result.message)
            Remote.Loading -> Unit
        }
    }) { repository.markRead(id) }
}
