# Chuyển đổi dữ liệu điểm v3

Migration: db/028_unified_discovery_quest_points.sql. Đã áp dụng vào vietnam_explorer_dev_v2; không chạy trên bcv nguồn.

1. Thêm explorer_reward_config.discovery_points, lần đầu lấy journey_points + first_place_points (local 50 + 100 = 150). Giữ các cột cấu hình cũ để không phá dữ liệu; source/API/UI mới không đọc chúng để tính điểm.
2. Thêm quest.reward_points, khởi tạo nhiệm vụ cũ từ mức quest_points tại thời điểm migration, hoặc 20 nếu không có hàng cấu hình. Sau đó từng nhiệm vụ độc lập; không dùng cấu hình nhiệm vụ chung nữa. Thêm description để hỗ trợ mô tả trên biểu mẫu.
3. Thêm quest_progress.awarded_points và points_settled. Chụp lại số điểm từ ledger cho tiến độ đã hoàn thành, kể cả 0 nếu chưa từng có giao dịch. Không cấp lại thưởng. Nhiệm vụ mới chốt ảnh chụp này ngay trong transaction hoàn thành, kể cả khi mức thưởng bằng 0.
4. Cho phép thêm loại DISCOVERY trong CHECK của ledger; giữ nguyên FIRST_PLACE, JOURNEY, QUEST và mọi bản ghi cũ. UNIQUE nguồn hiện có được tái sử dụng.

Migration không xóa bảng, nhiệm vụ, thẻ, vật phẩm hay điểm. Các cột mới thêm bằng IF NOT EXISTS; backfill chỉ hàng chưa có dữ liệu/chưa được chốt. Đã kiểm tra chạy lại với hash của toàn bộ hàng ledger trước/sau bằng nhau. Constraint chỉ thay đúng tên CHECK loại sự kiện đã biết; không quét/xóa CHECK không liên quan.

Seed: db/seed_dev_v2_quest_rewards.sql, chỉ cho DB local v2. Bảy nhiệm vụ ACTIVE: 1000/3000/8000 bước ngày = 10/25/60 điểm; khám phá 1 địa danh ngày = 30; 30000 bước tuần = 150; khám phá 3 địa danh tuần = 100; hoàn thành 5 hành trình tuần = 180. Đây là dữ liệu có thể chỉnh bằng admin, không là hằng số logic. Không ghi đè nhiệm vụ cùng tên. Advisory lock và NOT EXISTS bảo đảm chạy lại không tăng số nhiệm vụ.

Triển khai môi trường khác: dừng backend cũ, sao lưu DB, áp dụng 027 nếu chưa có, áp dụng 028, triển khai backend/web mới rồi khởi động lại. Không dùng đồng thời binary cũ/mới để xử lý hoàn thành. Seed demo không tự chạy ngoài DB local. Nếu rollback ứng dụng, giữ schema mở rộng và ledger; không DROP cột hoặc đổi DISCOVERY về hai sự kiện. Cần bản ứng dụng tương thích v3 khi tiếp tục ghi điểm.

Nguồn bcv được kiểm tra read-only lại, schema và số dòng từng bảng vẫn khớp snapshot gốc. Ảnh chụp schema mới ở docs/technical/database_schema_after.snapshot.json; kiểm chứng trong schema_comparison_results.md. Không có migration tự chạy lúc startup local.
