package com.project.bcv.feature.discovery.presentation

import android.content.Intent
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.project.bcv.BuildConfig
import com.project.bcv.feature.questitem.inventory.presentation.InventoryActivity
import com.project.bcv.feature.questitem.quest.presentation.QuestActivity

object StepTestDialog {
    fun show(activity: AppCompatActivity, model: ExploreViewModel) {
        if (!BuildConfig.STEP_TEST_ENABLED) return
        fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(12))
        }
        fun text(value: String) = TextView(activity).apply { text = value; textSize = 14f; setPadding(0, dp(8), 0, dp(8)) }
        content.addView(text("Bước thử được lưu thật vào tài khoản đang đăng nhập và tác động hành trình, nhiệm vụ, vật phẩm. Hành trình đã test sẽ dùng bước mô phỏng thay cho bộ đếm thiết bị đến khi kết thúc. Không ghi vào Health Connect."))
        fun number(label: String, initial: String): EditText {
            content.addView(text(label))
            return EditText(activity).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                contentDescription = label
                setText(initial)
                isSingleLine = true
                content.addView(this, LinearLayout.LayoutParams(-1, dp(48)))
            }
        }
        val amount = number("Số bước mỗi lần (1–100.000)", "200")
        val seconds = number("Chu kỳ gửi (2–3.600 giây)", "5")
        val status = text("")
        content.addView(status)
        fun button(label: String, action: () -> Unit) = MaterialButton(activity).apply {
            text = label
            setOnClickListener { action() }
            content.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        fun amountValue(): Int? = amount.text.toString().toIntOrNull()?.takeIf { it in 1..100_000 }
            .also { if (it == null) amount.error = "Nhập từ 1 đến 100.000 bước" }
        val send = button("Gửi bước một lần") { amountValue()?.let(model::sendTestSteps) }
        val start = button("Bắt đầu mô phỏng") {
            val value = amountValue()
            val interval = seconds.text.toString().toIntOrNull()?.takeIf { it in 2..3600 }
            if (interval == null) seconds.error = "Nhập từ 2 đến 3.600 giây"
            if (value != null && interval != null) model.startStepSimulation(value, interval)
        }
        val stop = button("Dừng mô phỏng") { model.stopStepSimulation() }
        content.addView(text("Đóng bảng này để xem tiến trình và bản đồ cập nhật. Mô phỏng tự dừng khi rời màn Khám phá, có lỗi hoặc hành trình hoàn thành. Chu kỳ tính sau khi máy chủ xác nhận lần trước."))
        val dialog = AlertDialog.Builder(activity).setTitle("Test bước chân")
            .setView(ScrollView(activity).apply { addView(content) })
            .setPositiveButton("Xem bản đồ", null).create()
        button("Kiểm tra nhiệm vụ") { model.stopStepSimulation(); dialog.dismiss(); activity.startActivity(Intent(activity, QuestActivity::class.java)) }
        button("Kiểm tra / dùng vật phẩm") { model.stopStepSimulation(); dialog.dismiss(); activity.startActivity(Intent(activity, InventoryActivity::class.java)) }
        fun render() {
            val test = model.stepTest.value ?: StepTestUiState()
            val journey = model.state.value?.snapshot?.journey
            status.text = "${journey?.destinationName ?: "Chưa có hành trình"}\n" +
                "Bước gốc: ${journey?.sourceSteps ?: "—"}; tiến độ: ${journey?.recordedSteps ?: 0}/${journey?.requiredSteps ?: 0}\n" +
                "${if (test.running) "Đang mô phỏng" else "Đã dừng"}${if (test.busy) " · Đang gửi…" else ""}\n${test.message}"
            send.isEnabled = !test.busy && !test.running && journey?.status == "IN_PROGRESS"
            start.isEnabled = send.isEnabled
            stop.isEnabled = test.running
        }
        val testObserver = Observer<StepTestUiState> { render() }
        val stateObserver = Observer<ExploreUiState> { render() }
        model.stepTest.observe(activity, testObserver)
        model.state.observe(activity, stateObserver)
        dialog.setOnDismissListener { model.stepTest.removeObserver(testObserver); model.state.removeObserver(stateObserver) }
        render()
        dialog.show()
    }
}
