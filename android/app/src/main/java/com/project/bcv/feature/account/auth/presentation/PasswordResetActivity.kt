package com.project.bcv.feature.account.auth.presentation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.project.bcv.R
import com.project.bcv.core.di.AccountModule
import com.project.bcv.databinding.ActivityPasswordResetBinding

/** Three-step password recovery: email → 6-digit code → new password. */
class PasswordResetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordResetBinding
    private lateinit var model: PasswordResetViewModel
    private var renderedStep: PasswordResetStep? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        binding = ActivityPasswordResetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }
        configurePasswordToggle(binding.passwordLayout, binding.passwordInput)
        configurePasswordToggle(binding.confirmLayout, binding.confirmInput)

        model = ViewModelProvider(this, PasswordResetViewModelFactory(AccountModule.repository(this)))[
            PasswordResetViewModel::class.java]
        intent.getStringExtra(EXTRA_EMAIL)?.takeIf(String::isNotBlank)?.let(model::prefill)
        model.state.observe(this, ::render)

        listOf(binding.emailInput, binding.codeInput, binding.passwordInput, binding.confirmInput).forEach { input ->
            input.doAfterTextChanged { if (input.hasFocus()) model.clearErrors() }
        }
        binding.emailInput.setOnEditorActionListener(::onImeAction)
        binding.codeInput.setOnEditorActionListener(::onImeAction)
        // Digits are spaced out once typed; the empty field shows its hint in normal spacing.
        binding.codeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.codeInput.letterSpacing = if (s.isNullOrEmpty()) 0f else 0.3f
            }
        })
        binding.confirmInput.setOnEditorActionListener(::onImeAction)
        binding.submitButton.setOnClickListener { submit() }
        binding.resendButton.setOnClickListener { model.resendCode() }
        binding.backToLogin.setOnClickListener { finish() }
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!model.back()) finish()
            }
        })
    }

    private fun onImeAction(view: TextView, actionId: Int, event: android.view.KeyEvent?): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) return false
        submit()
        return true
    }

    private fun submit() {
        val state = model.state.value ?: return
        when (state.step) {
            PasswordResetStep.EMAIL -> model.sendCode(binding.emailInput.text?.toString().orEmpty())
            PasswordResetStep.CODE -> model.verifyCode(binding.codeInput.text?.toString().orEmpty())
            PasswordResetStep.PASSWORD -> model.resetPassword(
                binding.passwordInput.text?.toString().orEmpty(),
                binding.confirmInput.text?.toString().orEmpty()
            )
            PasswordResetStep.DONE -> {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_EMAIL, state.email))
                finish()
            }
        }
    }

    private fun render(state: PasswordResetUiState) = with(binding) {
        if (renderedStep != state.step) showStep(state)

        emailLayout.error = state.errors.email
        codeLayout.error = state.errors.code
        passwordLayout.error = state.errors.password
        confirmLayout.error = state.errors.confirmPassword
        notice.text = state.notice
        notice.visibility = if (state.notice != null && state.step == PasswordResetStep.CODE) View.VISIBLE else View.GONE
        requestError.text = state.requestError
        requestError.visibility = if (state.requestError == null) View.GONE else View.VISIBLE

        val canResend = state.resendSeconds == 0 && !state.loading
        resendButton.isEnabled = canResend
        resendButton.text = if (state.resendSeconds > 0) "Gửi lại sau ${state.resendSeconds}s" else "Gửi lại mã"
        resendButton.setTextColor(color(if (canResend) R.color.teal else R.color.muted))

        listOf(emailInput, codeInput, passwordInput, confirmInput).forEach { it.isEnabled = !state.loading }
        submitButton.isEnabled = !state.loading
        submitButton.text = if (state.loading) "" else when (state.step) {
            PasswordResetStep.EMAIL -> "Gửi mã xác minh"
            PasswordResetStep.CODE -> "Xác minh"
            PasswordResetStep.PASSWORD -> "Đặt lại mật khẩu"
            PasswordResetStep.DONE -> "Đăng nhập"
        }
        submitButton.icon = if (state.loading) null else ContextCompat.getDrawable(this@PasswordResetActivity, R.drawable.ic_arrow)
        submitLoading.visibility = if (state.loading) View.VISIBLE else View.GONE
        submitLoadingText.text = when (state.step) {
            PasswordResetStep.EMAIL -> "Đang gửi mã..."
            PasswordResetStep.CODE -> "Đang xử lý..."
            else -> "Đang lưu mật khẩu..."
        }
        backToLogin.visibility = if (state.step == PasswordResetStep.DONE) View.GONE else View.VISIBLE
        backButton.visibility = backToLogin.visibility
    }

    private fun showStep(state: PasswordResetUiState) = with(binding) {
        val first = renderedStep == null
        renderedStep = state.step
        subtitle.text = when (state.step) {
            PasswordResetStep.EMAIL -> "Nhập email đã đăng ký để nhận mã xác minh."
            PasswordResetStep.CODE -> "Nhập mã 6 chữ số đã gửi tới ${state.email}."
            PasswordResetStep.PASSWORD -> "Tạo mật khẩu mới cho tài khoản của bạn."
            PasswordResetStep.DONE -> "Mật khẩu của bạn đã được cập nhật."
        }
        if (emailInput.text.isNullOrEmpty() && state.email.isNotEmpty()) emailInput.setText(state.email)
        if (state.step == PasswordResetStep.CODE) codeInput.text?.clear()
        if (state.step != PasswordResetStep.PASSWORD) {
            passwordInput.text?.clear()
            confirmInput.text?.clear()
        }

        val index = state.step.ordinal
        listOf(stepEmailLabel to stepEmailBar, stepCodeLabel to stepCodeBar, stepPasswordLabel to stepPasswordBar)
            .forEachIndexed { position, (label, bar) ->
                val reached = position <= index
                label.setTextColor(if (reached) color(R.color.teal) else 0xFF9AABA2.toInt())
                label.setTypeface(null, if (position == index) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                bar.backgroundTintList = ColorStateList.valueOf(if (reached) color(R.color.teal) else 0xFFE3ECE8.toInt())
            }
        stepIndicator.contentDescription = when (state.step) {
            PasswordResetStep.DONE -> "Đã hoàn tất 3 bước"
            else -> "Bước ${index + 1} trên 3"
        }

        val target = when (state.step) {
            PasswordResetStep.EMAIL -> emailGroup
            PasswordResetStep.CODE -> codeGroup
            PasswordResetStep.PASSWORD -> passwordGroup
            PasswordResetStep.DONE -> doneGroup
        }
        listOf(emailGroup, codeGroup, passwordGroup, doneGroup).forEach {
            it.visibility = if (it === target) View.VISIBLE else View.GONE
        }
        if (!first) {
            stepContent.alpha = 0f
            stepContent.translationX = 16 * resources.displayMetrics.density
            stepContent.animate().alpha(1f).translationX(0f).setDuration(220L).start()
        }
        when (state.step) {
            PasswordResetStep.CODE -> codeInput.requestFocus()
            PasswordResetStep.PASSWORD -> passwordInput.requestFocus()
            else -> currentFocus?.clearFocus()
        }
    }

    private fun configurePasswordToggle(layout: TextInputLayout, input: TextInputEditText) {
        layout.endIconContentDescription = getString(R.string.show_password)
        layout.setEndIconOnClickListener {
            val hidden = input.transformationMethod is PasswordTransformationMethod
            val selection = input.selectionStart.coerceAtLeast(0)
            input.transformationMethod = if (hidden) null else PasswordTransformationMethod.getInstance()
            layout.setEndIconDrawable(if (hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
            layout.endIconContentDescription = getString(if (hidden) R.string.hide_password else R.string.show_password)
            input.setSelection(minOf(selection, input.text?.length ?: 0))
        }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    companion object {
        const val EXTRA_EMAIL = "email"
    }
}
