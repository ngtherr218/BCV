# Các ca kiểm thử hệ thống điểm

| Mã | Tình huống | Kiểm chứng tự động |
|---|---|---|
| P01 | Khám phá đầu tiên có một khoản điểm cấu hình | API kiểm tra rankingPoints và một DISCOVERY trong ledger |
| P02 | Tạo, chưa hoàn thành, từ bỏ không có điểm khám phá | API kiểm tra trước/sau và trạng thái |
| P03 | Hoàn thành lặp, đồng thời không cộng trùng | Hai HTTP sync đồng thời và gửi lại |
| P04 | Không chọn lại địa danh đã đi bộ/vé bay | Regression API 400, giữ hành trình hiện tại |
| P05 | Vé bay không có điểm khám phá/nhiệm vụ do vé | So sánh tổng điểm trước/sau |
| P06 | Tạo/sửa quest điểm riêng ngày/tuần, 0 | Admin API và Chrome CRUD |
| P07 | Điểm âm, quá lớn, thập phân, sai kiểu | API 400; web inline validation |
| P08 | Điểm nhiệm vụ đúng cấu hình, nhiều nhiệm vụ một sự kiện | Snapshot 17/83/0, đối chiếu tổng ledger |
| P09 | Không trả lại nhiệm vụ trong cùng chu kỳ | Đồng bộ/đọc lại, unique source key |
| P10 | Chu kỳ mới nhận thưởng mới | Đổi cycle key của riêng fixture sang kỳ QA cũ, tạo hoạt động mới; không đổi đồng hồ hệ thống |
| P11 | Sửa điểm giữ nguyên điểm lịch sử kể cả 0 | Đổi 17→41, 0→59 và so actual/current |
| P12 | Transaction rollback khi ledger thất bại | CHECK tạm chỉ chặn user QA, xác nhận không tiến độ/khám phá/điểm; gỡ CHECK trong finally, retry |
| P13 | Điểm và vật phẩm độc lập | Nhiệm vụ chỉ điểm, chỉ vật phẩm tương thích, cả hai; đọc inventory hai lần không cấp trùng |
| P14 | Migration không mất ledger, seed không trùng | MD5 toàn bộ hàng sổ điểm trước/sau và số nhiệm vụ trước/sau seed |
| P15 | Snapshot thực nhận và nguồn event nhất quán | SQL reconcile và uniqueness |
| P16 | Quyền admin, validation cấu hình, revision | 401/403/400; payload ba ô cũ bị từ chối |
| P17 | Web lỗi lưu/loading giữ dữ liệu | Chrome trì hoãn PUT, trả 503 mô phỏng, thử lại thành công |
| P18 | Web tìm kiếm, lọc ngày/tuần, empty state, mobile | Chrome thật ở 1440px và 390px |
| P19 | Android đăng nhập, mức riêng và trạng thái nhận | Instrumentation với API thật; kiểm tra từng mức dự kiến/thực nhận |
| P20 | Android thành quả khám phá và thẻ | Chọn địa danh có thẻ ACTIVE, hoàn thành qua API, mở controller thật, kiểm tra số điểm từ backend |
| P21 | Leaderboard, thông báo, collection không hồi quy | API regression và 5 instrumentation tests |

Lệnh chính (PowerShell từ workspace):

```powershell
./backend/run-dev-v2.ps1 -Port 5084 -BuildOutput output/dev-v2/ranking-api
./backend/test-points-v3-dev-v2.ps1
./backend/test-dev-v2.ps1 -Api http://127.0.0.1:5084 -ExerciseAwards
./backend/test-admin-dev-v2.ps1 -Api http://127.0.0.1:5084
./backend/invoke-dev-v2-sql.ps1 -SqlFile output/dev-v2/verification/points-v3-database.sql
node output/playwright/verify-points-v3.cjs
```

Web kiểm thử cần Vite 5174 trỏ BCV_API_TARGET tới 5084. Android APK dùng 127.0.0.1:5082, ADB reverse tcp:5082 tcp:5084 chỉ trong kiểm thử; trả về tcp:5082 tcp:5082 sau khi xong. Không lấy tài khoản thật; test user dùng example.invalid. Admin fixture được chuyển INACTIVE trong finally, không xóa nhiệm vụ hiện có. CHECK gây lỗi chỉ nhắm user QA và được gỡ trong finally. Cấu hình khám phá được khôi phục với revision guard để không ghi đè chỉnh sửa khác.

Giới hạn: bước chân synthetic gửi API local; không xác nhận cảm biến/Health Connect/hoạt động đi bộ vật lý. Chu kỳ mới được mô phỏng ở dữ liệu QA; kiểm tra thời gian ISO riêng nằm trong các kiểm tra sẵn có. Bộ database StepChecks yêu cầu database bcv_step_checks_* riêng, không chạy nó trên local v2.
