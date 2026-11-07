package com.project.bcv.feature.account.profile.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.project.bcv.R
import com.project.bcv.core.di.ProfileModule
import com.project.bcv.core.di.StatsNotifyModule
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.core.ui.Skeleton
import com.project.bcv.core.ui.openSignIn
import com.project.bcv.core.ui.setUpDetailScreen
import com.project.bcv.databinding.ActivityProfileBinding
import com.project.bcv.databinding.ItemProfileLinkBinding
import com.project.bcv.feature.account.activitylevel.presentation.ActivityLevelActivity
import com.project.bcv.feature.account.auth.presentation.AccountActivity
import com.project.bcv.feature.account.healthconnect.presentation.HealthConnectActivity
import com.project.bcv.feature.account.interests.presentation.InterestsActivity
import com.project.bcv.feature.account.profile.domain.Profile
import com.project.bcv.feature.journey.presentation.JourneyHistoryActivity
import com.project.bcv.feature.statsnotify.notifications.presentation.NotificationsActivity
import com.project.bcv.feature.statsnotify.stats.presentation.StatsActivity
import kotlinx.coroutines.launch

/** Account summary, personalisation settings, the user's data screens and sign-out. */
class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var model: ProfileViewModel
    private var profileShown = false

    // Preferences are edited in two steps, as during sign-up: interests, then activity level.
    private val interestsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) activityLevelResult.launch(
            Intent(this, ActivityLevelActivity::class.java).putExtra(ActivityLevelActivity.EXTRA_EDIT_ONLY, true))
    }
    private val activityLevelResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
            Snackbar.make(binding.root, "Đã cập nhật sở thích và mức vận động.", Snackbar.LENGTH_SHORT).show()
    }

    private val editProfileResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) model.loadProfile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setUpDetailScreen(binding.root)
        binding.topBar.screenTitle.text = "Hồ sơ & thiết lập"
        binding.topBar.backButton.setOnClickListener { finish() }
        binding.accountStatus.text = "Tài khoản đang hoạt động"
        model = ViewModelProvider(this, viewModelFactory {
            ProfileViewModel(ProfileModule.repository(this), StatsNotifyModule.notifications(this))
        })[ProfileViewModel::class.java]

        link(binding.personalLink, R.drawable.ic_person_outline, "Hồ sơ cá nhân", "Họ tên, ảnh đại diện và mật khẩu") {
            editProfileResult.launch(Intent(this, EditProfileActivity::class.java))
        }
        link(binding.preferencesLink, R.drawable.ic_tune, "Sở thích & mức vận động", "Thông tin cá nhân hóa gợi ý") {
            interestsResult.launch(Intent(this, InterestsActivity::class.java))
        }
        link(binding.healthLink, R.drawable.ic_favorite_outline, "Health Connect", "Quyền đọc dữ liệu bước chân") {
            startActivity(Intent(this, HealthConnectActivity::class.java))
        }
        link(binding.statsLink, R.drawable.ic_bar_chart, "Thống kê cá nhân", "Bước chân, địa danh, thẻ và nhiệm vụ") {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        link(binding.historyLink, R.drawable.ic_history, "Lịch sử hành trình", "Các chặng đường và trạng thái") {
            startActivity(Intent(this, JourneyHistoryActivity::class.java))
        }
        link(binding.notificationsLink, R.drawable.ic_explore_bell, "Thông báo", "Sự kiện hoàn thành và vật phẩm") {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.logoutButton.setOnClickListener { confirmLogout() }

        model.profile.observe(this) { state ->
            // Only the account card comes from the server; the links below are shown at once.
            if (state != Remote.Loading) {
                Skeleton.hide(binding.skeleton.root)
                binding.accountCard.visibility = View.VISIBLE
            }
            when (state) {
                is Remote.Ready -> { profileShown = true; show(state.value) }
                is Remote.Failed -> binding.email.text = state.message
                Remote.SessionExpired -> openSignIn()
                Remote.Loading -> if (!profileShown) Skeleton.show(binding.skeleton.root, binding.accountCard)
            }
        }
        model.unread.observe(this) { unread ->
            binding.notificationsLink.rowSubtitle.text = if (unread > 0) "$unread thông báo chưa đọc"
                else "Sự kiện hoàn thành và vật phẩm"
        }
        model.signedOut.observe(this) { event -> event.take()?.let { openSignInFresh() } }
        model.message.observe(this) { event ->
            event.take()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        ScreenMotion.reveal(binding.content)
    }

    override fun onResume() {
        super.onResume()
        model.refreshUnread()
    }

    private fun link(row: ItemProfileLinkBinding, icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
        row.rowIcon.setImageResource(icon)
        row.rowTitle.text = title
        row.rowSubtitle.text = subtitle
        row.root.setOnClickListener { onClick() }
    }

    private fun show(account: Profile) = with(binding) {
        val displayName = account.name.ifBlank { account.email.substringBefore('@') }
        name.text = displayName
        email.text = account.email
        initials.text = Avatars.initials(displayName)
        val immediate = Avatars.immediate(account.avatar)
        avatarImage.setImageBitmap(immediate)
        avatarImage.visibility = if (immediate == null) View.GONE else View.VISIBLE
        if (immediate == null && account.avatar != null) lifecycleScope.launch {
            Avatars.load(account.avatar)?.let {
                avatarImage.setImageBitmap(it)
                avatarImage.visibility = View.VISIBLE
            }
        }
        accountCard.contentDescription = "$displayName, ${account.email}, tài khoản đang hoạt động"
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất?")
            .setMessage("Bạn sẽ cần đăng nhập lại để tiếp tục hành trình.")
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Đăng xuất") { _, _ -> model.logout() }
            .show()
    }

    private fun openSignInFresh() {
        startActivity(Intent(this, AccountActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
