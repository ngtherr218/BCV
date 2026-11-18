# Sửa mô phỏng bước chân trên điện thoại ngày 16/11/2026

## Hiện tượng và giới hạn xác minh

Người dùng báo máy thật vẫn hiện đang mô phỏng nhưng số bước và vị trí không thay đổi. Môi trường hiện tại chỉ kết nối ADB với emulator-5554, chưa có log hoặc thiết bị vật lý để kết luận nguyên nhân duy nhất trên điện thoại đó.

Mã cũ từ chối bước thử khi loading/autoSyncing hoặc đang đồng bộ. Máy thật có quyền cảm biến/Health Connect có thể gặp trạng thái này trong khi emulator không có dữ liệu bước. Đây là tình huống đã tái hiện bằng nguồn bước có quyền, có một lần đọc đang chờ.

## Thay đổi

- Đặt quyền ưu tiên cho yêu cầu test ngay khi bấm, ngăn đồng bộ/refresh mới xen vào. Chờ yêu cầu đang chạy hoàn tất tối đa 30 giây rồi đọc sourceSteps mới nhất và cộng bước thử từ tổng đã được server xác nhận.
- Chặn bấm gửi lặp trong lúc đang chờ. Dừng mô phỏng khi đang chờ không gửi thêm một lượt test. Yêu cầu đã gửi vẫn được xác nhận, giữ cơ chế chống cộng trùng sau timeout.
- JourneyStepService không khởi động lại cảm biến cho hành trình đã đánh dấu mô phỏng, kể cả lệnh start đến trễ hoặc sticky restart. Capture đã được xếp lịch cũng kiểm tra lại nguồn trước khi chạy.
- Nếu server xác nhận bước gốc nhưng tiến độ không tăng, dừng và báo rõ; không tăng số lượt test thành công. Giữ tổng mới đã được server xác nhận trên màn hình, không tự bịa tiến độ.
- Bảng test phân biệt chờ xác nhận lượt đầu với mô phỏng đã có lượt được xác nhận, ghi rõ số bước tiến độ tăng trong phản hồi.
- Bản đồ cập nhật thẳng tới tiến độ xác nhận khi Android tắt animator, thay vì phụ thuộc hiệu ứng chuyển động. Vị trí mô phỏng là vị trí trên hành trình ảo; chức năng này không giả lập GPS hoặc ghi bước vào Health Connect.

Nút test vẫn chỉ có ở APK debug, không bật vào release. Thử bước tác động dữ liệu hành trình/nhiệm vụ thật của tài khoản đang đăng nhập như cơ chế trước đó.

## Kiểm tra

- Build debug và 69 unit test: đạt.
- 10 instrumentation test StepSimulationTest trên emulator API 37: đạt, không lỗi/bỏ qua. Bao gồm nguồn không có quyền, nguồn thiết bị có quyền đang đọc chậm, tổng sau đồng bộ thật, bấm lặp, dừng khi chờ, timeout sau commit, thiếu sourceSteps và backend nhận tổng nhưng không tăng tiến độ.
- Nguồn giả có quyền trả 250 bước thật trong lúc lượt test +100 đang chờ: sau đồng bộ nhận tổng 350, không ghi đè bằng dữ liệu cảm biến cũ. Refresh được trì hoãn đến khi lượt test hoàn tất.
- Không gọi API thật hoặc ghi DB trong instrumentation test; repository trong các ca này là fixture.
- Chưa kiểm tra trực tiếp trên điện thoại vật lý hoặc xác minh cấu hình tiết kiệm pin của máy người dùng.

APK cài thử ở output/step-simulation-fix/BCV-step-test-debug.apk, trỏ https://bcv-admin-test.onrender.com/ theo máy chủ Test mặc định của app. Output/APK/log không được Git theo dõi. Nếu kiểm tra backend local trên điện thoại, build theo IP LAN phù hợp; localhost chỉ dùng được với ADB reverse. App cần backend có trường sourceSteps và cơ chế sync tích lũy tương ứng.
