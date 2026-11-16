package com.project.bcv

import android.os.Build
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.databinding.ActivityAccountBinding
import com.project.bcv.feature.account.auth.presentation.allowAuthFormScreenSharing
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated auth form fixture: no session, login or registration request is changed. */
@RunWith(AndroidJUnit4::class)
class AuthScreenSharingTest {
    @Test fun formSharingPreservesPasswordMaskingAndAutofill() {
        assumeTrue(Build.VERSION.SDK_INT >= 35)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val app = instrumentation.targetContext
            val context = ContextThemeWrapper(app, app.applicationInfo.theme)
            val form = ActivityAccountBinding.inflate(LayoutInflater.from(context))
            val passwordHints = form.passwordInput.autofillHints?.toList()
            val confirmationHints = form.confirmInput.autofillHints?.toList()

            form.allowAuthFormScreenSharing()
            form.emailInput.setText("screen-share-fixture@example.invalid")
            form.passwordInput.setText("ScreenShareFixture")
            form.confirmInput.setText("ScreenShareFixture")

            listOf(form.emailInput, form.passwordInput, form.confirmInput).forEach { field ->
                assertEquals(View.CONTENT_SENSITIVITY_NOT_SENSITIVE, field.contentSensitivity)
                assertFalse(field.isContentSensitive)
            }
            listOf(form.passwordInput, form.confirmInput).forEach { field ->
                assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    field.inputType and InputType.TYPE_MASK_VARIATION)
                assertTrue(field.transformationMethod is PasswordTransformationMethod)
                assertNotEquals(field.text.toString(), field.transformationMethod
                    .getTransformation(field.text, field).toString())
            }
            assertEquals(passwordHints, form.passwordInput.autofillHints?.toList())
            assertEquals(confirmationHints, form.confirmInput.autofillHints?.toList())

            // The eye button changes only the transformation method. Sharing must survive both
            // directions of that change without altering the input's password/autofill type.
            form.passwordInput.requestFocus()
            form.passwordInput.transformationMethod = null
            assertFalse(form.passwordInput.isContentSensitive)
            assertEquals("ScreenShareFixture", form.passwordInput.text.toString())
            form.passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            assertFalse(form.passwordInput.isContentSensitive)
            assertNotEquals("ScreenShareFixture", form.passwordInput.transformationMethod
                .getTransformation(form.passwordInput.text, form.passwordInput).toString())
            assertEquals(passwordHints, form.passwordInput.autofillHints?.toList())

            // The register-only confirmation field keeps the policy when it becomes visible.
            form.confirmGroup.visibility = View.VISIBLE
            form.confirmInput.requestFocus()
            form.passwordInput.text?.clear()
            form.confirmInput.text?.clear()
            assertFalse(form.confirmInput.isContentSensitive)
            assertTrue(form.confirmInput.transformationMethod is PasswordTransformationMethod)
        }
    }
}
