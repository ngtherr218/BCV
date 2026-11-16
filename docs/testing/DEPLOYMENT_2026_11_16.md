# Kiểm tra bàn giao BCV v2 ngày 16/11/2026

Kiểm tra trên Windows với PostgreSQL 18.6/PostGIS 3.6. Tất cả kiểm tra ghi dữ liệu bên dưới dùng DB thử riêng; không chạy migration lên DB `bcv`, DB local v2 hoặc cloud.

| Kiểm tra | Kết quả |
| --- | --- |
| Cài mới bằng `sql/install.sql` | Đạt; 33 bảng nghiệp vụ, 312 địa danh tham chiếu, 0 tài khoản lúc vừa cài |
| Nâng cấp schema gốc với dataset chuẩn, 12 loại và mô hình ACTIVE | Đạt |
| Chạy lại `sql/upgrade.sql` | Đạt, giữ cấu hình và không phát điểm bù |
| Build API sau gộp nhánh | Đạt |
| `Bcv.Checks` | Đạt: sở thích, geocoding, phân loại dataset, mô tả các trường hợp đổi hạng |
| `Bcv.Checks --ranking-integration` | Đạt: dùng tên hồ sơ, hạng 3 → 1, tổng 250 điểm gồm điểm cũ 50 + khám phá 150 + nhiệm vụ bước 20 + nhiệm vụ hành trình 30; đồng bộ lặp không thưởng/thông báo trùng |
| `Bcv.StepChecks` | Đạt: bước tích lũy, vật phẩm, vé bay, nhiệm vụ, đăng nhập, bộ sưu tập và thẻ chưa xem |
| `Bcv.AdminDeleteChecks` | Đạt: thẻ của địa danh ngừng hoạt động, danh mục xem trước, kế thừa đặc trưng, quyền ảnh, kiểm duyệt, báo cáo, nhiệm vụ ảnh, cleanup và bảo vệ xóa danh mục liên kết |
| Web `npm --prefix web run build` | Đạt |
| Android `testDebugUnitTest` và `assembleDebug` | Đạt; 69 tests, 0 lỗi, 0 bỏ qua |
| Báo cáo Word | 6 trang; xuất bằng Microsoft Word và kiểm tra ảnh từng trang |
| Kiểm tra whitespace, marker xung đột và mẫu secret trong phần SQL mới | Đạt |

DB thử được tạo: `bcv_deploy_checks_20261116_fresh`, `bcv_deploy_checks_20261116_upgrade`, `bcv_step_checks_20261116_deployment`. Sau kiểm tra, DB thử có fixture và được giữ lại để đối chiếu; không dùng làm DB vận hành. Fixture API quản trị dùng schema riêng, tự xóa schema đó khi kết thúc. Không sao chép tài khoản hoặc lịch sử từ DB nguồn vào seed triển khai.

Bộ test cũ được cập nhật theo nghiệp vụ hiện tại: ảnh xem trước không cấp quyền sở hữu; danh mục và chi tiết dùng cùng quy tắc thẻ của địa danh ngừng hoạt động; video không được cộng tiến độ; cleanup fixture có xét khóa ngoại sổ điểm. Kiểm tra HTTP dung lượng lớn dùng Expect 100 Continue để nhận phản hồi từ chối trước khi gửi toàn bộ body trên Windows. Ràng buộc chỉ nhận ảnh được giữ trên schema triển khai; chỉ fixture mô phỏng DB cũ mới bỏ ràng buộc này trong schema thử.

Build .NET có cảnh báo NU1900 vì không truy cập được nguồn dữ liệu kiểm tra lỗ hổng NuGet trong phiên kiểm tra; không có lỗi biên dịch. Android có cảnh báo API deprecated và tùy chọn kiểm tra đường dẫn Unicode hiện có. Không coi đây là kết quả kiểm toán lỗ hổng dependency.

Máy không cài LibreOffice nên `render_docx.py` báo thiếu `soffice.exe`. Báo cáo được xuất PDF QA bằng Microsoft Word có sẵn, rasterize bằng pypdfium2 trong runtime tài liệu và kiểm tra cả 6 trang. PDF/PNG QA, log, APK và output build được Git bỏ qua.

Không chạy instrumentation hoặc thử cảm biến/Health Connect trên điện thoại vật lý trong lần bàn giao này. Không tự restart phiên API đang do VSCode chạy. Sửa tên và thông báo cần backend nạp bản mới; thông báo lịch sử không được tính lại. Nội dung thứ hạng là thứ hạng toàn quốc của tuần tại thời điểm hoàn thành, bao gồm điểm nhiệm vụ phát sinh trong cùng lần đồng bộ.
