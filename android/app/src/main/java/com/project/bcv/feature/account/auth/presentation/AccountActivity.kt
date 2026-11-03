package com.project.bcv.feature.account.auth.presentation

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.animation.ValueAnimator
import android.graphics.drawable.Animatable
import android.text.method.PasswordTransformationMethod
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.project.bcv.R
import com.project.bcv.core.di.AccountModule
import com.project.bcv.databinding.ActivityAccountBinding
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.account.auth.data.local.RememberedLoginStore
import android.widget.Toast
import com.project.bcv.feature.account.interests.presentation.InterestsActivity
import com.project.bcv.feature.account.activitylevel.presentation.ActivityLevelActivity
import com.project.bcv.feature.location.startingpoint.presentation.StartingPointActivity
import com.project.bcv.feature.discovery.presentation.ExploreActivity

class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding
    private lateinit var model: AccountViewModel
    private var switchingMode = false
    private var interestsLaunched = false
    private var activityLevelLaunched = false
    private var virtualLocationLaunched = false
    private var exploreLaunched = false
    private var previousState: AccountUiState? = null
    private lateinit var rememberedLogin: RememberedLoginStore
    private val interestsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        interestsLaunched = false
        if (result.resultCode == Activity.RESULT_OK) model.interestsSaved() else model.interestsCancelled()
    }
    private val activityLevelResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        activityLevelLaunched = false
        if (result.resultCode == Activity.RESULT_OK) model.activityLevelSaved()
        else model.activityLevelCancelled()
    }
    private val virtualLocationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        virtualLocationLaunched = false
        if (result.resultCode == Activity.RESULT_OK) model.virtualLocationSaved()
        else model.virtualLocationCancelled()
    }

    private val passwordResetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        rememberedLogin.clear()
        binding.rememberLogin.isChecked = false
        result.data?.getStringExtra(PasswordResetActivity.EXTRA_EMAIL)?.let(binding.emailInput::setText)
        clearPasswords()
        binding.passwordInput.requestFocus()
        Snackbar.make(binding.root, "Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionPreferences(this).readyForHome) {
            startActivity(Intent(this, ExploreActivity::class.java))
            finish()
            return
        }
        interestsLaunched = savedInstanceState?.getBoolean("interestsLaunched") ?: false
        activityLevelLaunched = savedInstanceState?.getBoolean("activityLevelLaunched") ?: false
        virtualLocationLaunched = savedInstanceState?.getBoolean("virtualLocationLaunched") ?: false
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurePasswordToggle(binding.passwordLayout, binding.passwordInput)
        configurePasswordToggle(binding.confirmLayout, binding.confirmInput)
        rememberedLogin = RememberedLoginStore(applicationContext)
        if (savedInstanceState == null && SessionPreferences(this).token == null) prefillRememberedLogin()
        binding.rememberLogin.setOnCheckedChangeListener { _, checked ->
            if (!checked) rememberedLogin.clear()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }

        model = ViewModelProvider(
            this,
            AccountViewModelFactory(AccountModule.repository(this), rememberedLogin)
        )[AccountViewModel::class.java]
        model.state.observe(this, ::render)
        // A saved sign-in continues straight into the app; the form appears only if it is rejected.
        // A recreated process has a fresh ViewModel even when Android restores a Bundle.
        if (SessionPreferences(this).token != null) model.restoreSession()

        binding.emailInput.doAfterTextChanged { model.onFieldEdited(AccountField.EMAIL) }
        binding.passwordInput.doAfterTextChanged { model.onFieldEdited(AccountField.PASSWORD) }
        binding.confirmInput.doAfterTextChanged { model.onFieldEdited(AccountField.CONFIRM_PASSWORD) }
        binding.emailInput.setOnFocusChangeListener { _, focused ->
            if (focused) model.onFieldEdited(AccountField.EMAIL)
        }
        binding.passwordInput.setOnFocusChangeListener { _, focused ->
            if (focused) model.onFieldEdited(AccountField.PASSWORD)
        }
        binding.confirmInput.setOnFocusChangeListener { _, focused ->
            if (focused) model.onFieldEdited(AccountField.CONFIRM_PASSWORD)
        }

        binding.submitButton.setOnClickListener {
            model.submit(
                binding.emailInput.text?.toString().orEmpty(),
                binding.passwordInput.text?.toString().orEmpty(),
                binding.confirmInput.text?.toString().orEmpty(),
                binding.rememberLogin.isChecked
            )
        }
        binding.switchMode.setOnClickListener {
            animateModeChange()
        }
        binding.forgotPassword.setOnClickListener {
            passwordResetResult.launch(Intent(this, PasswordResetActivity::class.java)
                .putExtra(PasswordResetActivity.EXTRA_EMAIL, binding.emailInput.text?.toString()?.trim()))
        }
        binding.successButton.setOnClickListener {
            if (model.state.value?.registrationSuccess == true) model.continueRegistration()
            else model.signOut()
        }
        binding.authBackButton.setOnClickListener {
            if (model.state.value?.registrationSuccess == true) model.signOut()
            else animateModeChange()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("interestsLaunched", interestsLaunched)
        outState.putBoolean("activityLevelLaunched", activityLevelLaunched)
        outState.putBoolean("virtualLocationLaunched", virtualLocationLaunched)
        super.onSaveInstanceState(outState)
    }

    private fun render(state: AccountUiState) = with(binding) {
        val priorState = previousState
        previousState = state
        if (state.rememberSaveFailed && priorState?.rememberSaveFailed != true) {
            Toast.makeText(this@AccountActivity, "Đăng nhập thành công nhưng chưa lưu được thông tin đăng nhập trên thiết bị.", Toast.LENGTH_LONG).show()
        }
        if (state.completed && !state.registrationSuccess && !exploreLaunched) {
            SessionPreferences(this@AccountActivity).readyForHome = true
            exploreLaunched = true
            startActivity(Intent(this@AccountActivity, ExploreActivity::class.java),
                ActivityOptionsCompat.makeCustomAnimation(this@AccountActivity,
                    R.anim.interests_enter, R.anim.account_to_interests_exit).toBundle())
            finish()
            return@with
        }
        if (state.needsInterestSelection && !interestsLaunched) {
            interestsLaunched = true
            val transition = ActivityOptionsCompat.makeCustomAnimation(
                this@AccountActivity,
                R.anim.interests_enter,
                R.anim.account_to_interests_exit
            )
            interestsResult.launch(Intent(this@AccountActivity, InterestsActivity::class.java), transition)
        } else if (!state.registrationSuccess && !state.needsInterestSelection &&
            state.needsActivityLevel && !activityLevelLaunched) {
            activityLevelLaunched = true
            val transition = ActivityOptionsCompat.makeCustomAnimation(
                this@AccountActivity,
                R.anim.interests_enter,
                R.anim.account_to_interests_exit
            )
            activityLevelResult.launch(Intent(this@AccountActivity, ActivityLevelActivity::class.java), transition)
        } else if (!state.registrationSuccess && !state.needsInterestSelection &&
            !state.needsActivityLevel && state.needsVirtualLocation && !virtualLocationLaunched) {
            virtualLocationLaunched = true
            val transition = ActivityOptionsCompat.makeCustomAnimation(
                this@AccountActivity, R.anim.interests_enter, R.anim.account_to_interests_exit
            )
            virtualLocationResult.launch(Intent(this@AccountActivity, StartingPointActivity::class.java), transition)
        }
        val registering = state.mode == AccountMode.REGISTER
        restoreContainer.visibility = if (state.restoring) View.VISIBLE else View.GONE
        val showingSuccess = state.registrationSuccess || state.completed
        heroHeadline.setText(if (registering) R.string.register_hero_headline else R.string.login_hero_headline)
        authBackButton.visibility = if (registering && !state.completed) View.VISIBLE else View.GONE
        authBackButton.isEnabled = !state.loading
        if (showingSuccess) {
            when {
                state.registrationSuccess -> {
                    successTitle.text = "Đăng ký thành công!"
                    successMessage.text = "Chào mừng bạn đến với Bước Chân Việt.\nTài khoản của bạn đã sẵn sàng."
                    successNoteTitle.text = "Cá nhân hóa hành trình của bạn"
                    successNoteBody.text = "Tiếp theo, thiết lập sở thích, mức vận động\nvà điểm xuất phát cho hành trình của bạn."
                    successButton.text = "Tiếp tục thiết lập"
                    successFooter.text = "Hành trình khám phá đang chờ bạn."
                }
                state.onboardingCompleted -> {
                    successTitle.text = "Thiết lập hoàn tất!"
                    successMessage.text = "Sở thích, mức vận động và điểm xuất phát đã được lưu."
                    successNoteTitle.text = "Hành trình của bạn"
                    successNoteBody.text = "Gợi ý khám phá sẽ được cá nhân hóa theo lựa chọn của bạn."
                    successButton.text = "Đăng xuất"
                    successFooter.text = "Bạn có thể cập nhật sở thích sau trong Hồ sơ."
                }
                else -> {
                    successTitle.text = "Đăng nhập thành công!"
                    successMessage.text = "Chào mừng bạn trở lại!"
                    successNoteTitle.text = "Hành trình của bạn"
                    successNoteBody.text = "Tài khoản của bạn đã sẵn sàng."
                    successButton.text = "Đăng xuất"
                    successFooter.text = "Hành trình khám phá đang chờ bạn."
                }
            }
            val shouldAnimateRegistrationSuccess = ValueAnimator.areAnimatorsEnabled() &&
                priorState?.mode == AccountMode.REGISTER && priorState.loading &&
                !priorState.registrationSuccess && state.registrationSuccess
            if (shouldAnimateRegistrationSuccess) {
                animateRegistrationSuccess()
            } else {
                successCheck.setImageResource(R.drawable.ic_check)
                formContainer.visibility = View.GONE
                successContainer.visibility = View.VISIBLE
            }
            root.smoothScrollTo(0, 0)
            return@with
        }

        if (state.restoring) {
            formContainer.visibility = View.GONE
            successContainer.visibility = View.GONE
            return@with
        }
        formContainer.visibility = View.VISIBLE
        successContainer.visibility = View.GONE

        formTitle.setText(if (registering) R.string.register_title else R.string.login_title)
        formSubtitle.text = if (registering) {
            "Cùng khám phá Việt Nam qua từng bước chân."
        } else {
            getString(R.string.login_subtitle)
        }
        confirmGroup.visibility = if (registering) View.VISIBLE else View.GONE
        forgotPassword.visibility = if (registering) View.GONE else View.VISIBLE
        forgotPassword.isEnabled = !state.loading
        rememberLogin.visibility = if (registering) View.GONE else View.VISIBLE
        rememberLogin.isEnabled = !state.loading
        switchPrompt.text = if (registering) "Đã có tài khoản?" else "Chưa có tài khoản?"
        switchMode.text = if (registering) "Đăng nhập" else "Đăng ký"
        switchMode.isEnabled = !state.loading
        submitButton.text = if (state.loading) "" else if (registering) "Tạo tài khoản" else "Đăng nhập"
        submitButton.isEnabled = !state.loading
        submitButton.icon = if (state.loading) null else getDrawable(R.drawable.ic_arrow)
        submitButton.backgroundTintList = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(color(R.color.auth_loading_bg), color(R.color.teal))
        )
        submitLoadingText.text = if (registering) "Đang tạo tài khoản..." else "Đang đăng nhập..."
        submitLoadingContent.visibility = if (state.loading) View.VISIBLE else View.GONE
        submitProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
        emailInput.isEnabled = !state.loading
        passwordInput.isEnabled = !state.loading
        confirmInput.isEnabled = !state.loading
        showError(emailLayout, state.errors.email)
        showError(passwordLayout, state.errors.password)
        showError(confirmLayout, state.errors.confirmPassword)
        requestError.text = state.requestError
        requestError.visibility = if (state.requestError == null) View.INVISIBLE else View.VISIBLE
    }

    private fun animateRegistrationSuccess() = with(binding) {
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, root).hide(WindowInsetsCompat.Type.ime())

        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds().apply { duration = 320L })
            addTransition(MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
                addTarget(formContainer)
                addTarget(successContainer)
                duration = 320L
            })
        }
        successIcon.scaleX = 0.78f
        successIcon.scaleY = 0.78f
        successCheck.setImageResource(R.drawable.avd_success_check)
        TransitionManager.beginDelayedTransition(root, transition)
        formContainer.visibility = View.GONE
        successContainer.visibility = View.VISIBLE
        successIcon.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(100L)
            .setDuration(300L)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()
        successCheck.postDelayed({ (successCheck.drawable as? Animatable)?.start() }, 180L)
    }

    private fun animateModeChange() {
        val state = model.state.value ?: return
        if (state.loading || switchingMode) return

        switchingMode = true
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())

        val outgoing = binding.formContainer
        val direction = if (state.mode == AccountMode.REGISTER) -1f else 1f
        val offset = 20f * resources.displayMetrics.density
        outgoing.animate()
            .alpha(0f)
            .translationX(direction * offset)
            .setDuration(140L)
            .withEndAction {
                clearPasswords()
                model.changeMode()
                if (state.mode == AccountMode.REGISTER) prefillRememberedLogin()

                binding.formContainer.apply {
                    alpha = 0f
                    translationX = -direction * offset
                    animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(200L)
                        .withEndAction { switchingMode = false }
                        .start()
                }
            }
            .start()
    }

    private fun clearPasswords() {
        binding.passwordInput.text?.clear()
        binding.confirmInput.text?.clear()
        resetPasswordToggle(binding.passwordLayout, binding.passwordInput)
        resetPasswordToggle(binding.confirmLayout, binding.confirmInput)
    }

    private fun prefillRememberedLogin() {
        lifecycleScope.launch {
            val login = withContext(Dispatchers.IO) { rememberedLogin.load() } ?: return@launch
            if (binding.emailInput.text.isNullOrEmpty() && binding.passwordInput.text.isNullOrEmpty() &&
                model.state.value?.loading != true) {
                binding.emailInput.setText(login.first)
                binding.passwordInput.setText(login.second)
                binding.rememberLogin.isChecked = true
            }
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

    private fun resetPasswordToggle(layout: TextInputLayout, input: TextInputEditText) {
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        layout.setEndIconDrawable(R.drawable.ic_visibility)
        layout.endIconContentDescription = getString(R.string.show_password)
    }

    private fun showError(layout: TextInputLayout, message: String?) {
        layout.error = message
        layout.post {
            layout.findViewById<TextView>(com.google.android.material.R.id.textinput_error)?.apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                gravity = Gravity.END
            }
        }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)
}
