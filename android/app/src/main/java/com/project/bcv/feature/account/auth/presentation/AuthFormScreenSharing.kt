package com.project.bcv.feature.account.auth.presentation

import android.os.Build
import android.view.View
import com.project.bcv.databinding.ActivityAccountBinding

/** Share the auth form without changing password masking, autofill or the eye button. */
internal fun ActivityAccountBinding.allowAuthFormScreenSharing() {
    if (Build.VERSION.SDK_INT >= 35) {
        // A parent override does not override sensitive child fields. Password inputs receive
        // automatic sensitive autofill hints even when the form does not declare any explicitly.
        listOf(emailInput, passwordInput, confirmInput).forEach { field ->
            field.contentSensitivity = View.CONTENT_SENSITIVITY_NOT_SENSITIVE
        }
    }
}
