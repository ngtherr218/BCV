# Điểm thưởng theo từng nhiệm vụ

## Dữ liệu và trao thưởng

quest.reward_points là integer NOT NULL DEFAULT 20 CHECK 0..100000. Đây là mặc định cho một bản ghi mới/khả năng tương thích SQL, không phải mức chung dùng trong logic. Mỗi nhiệm vụ có thể giữ giá trị khác nhau do admin chọn; không suy điểm từ DAILY/WEEKLY.

Admin tạo/sửa/đọc danh sách/chi tiết đều có reward_points. Mô tả tối đa 2000 ký tự. Nhiệm vụ đã có tiến độ vẫn cho sửa điểm, tên, mô tả và trạng thái; điều kiện, mục tiêu, chu kỳ và vật phẩm giữ bất biến để không làm sai tiến độ hiện có. Chuyển cấu hình điều kiện đó bằng cách tạo nhiệm vụ mới.

QuestProgress khóa chia sẻ những hàng nhiệm vụ liên quan trước khi ghi tiến độ. Khi đạt mục tiêu, chốt awarded_points và points_settled cùng giao dịch ghi ledger. Điểm 0 cũng được chốt; đổi điểm từ 0 lên 50 không trao ngược 50 cho lượt hoàn thành cũ. Ledger chỉ chứa số điểm >0; snapshot lưu cả trường hợp nhận 0.

Khóa UNIQUE(user_id,quest_id,cycle_key) tái sử dụng. Chu kỳ theo ngày/ISO tuần giờ Việt Nam. Sang chu kỳ mới sinh quest_progress_id mới, nên được thưởng lại nếu hoàn thành; request lặp trong cùng chu kỳ không tạo thêm giao dịch. Sổ điểm dùng quest_progress_id làm source_key. Thay cấu hình không cập nhật awarded_points của lượt hoàn thành cũ.

## Điểm và vật phẩm độc lập

Có thể chỉ thưởng điểm, chỉ vật phẩm, cả hai hoặc không điểm/không vật phẩm. reward_item_id và reward_quantity phải cùng có hoặc cùng trống. Cơ chế cấp vật phẩm vào user_item khi đọc nhiệm vụ vẫn dùng transaction và reward_status để không cấp trùng. Tiến độ không có vật phẩm cũng được ghi nhận là đã xử lý nhận thưởng, nhưng không INSERT vật phẩm null.

API app: rankingPoints đọc quest.reward_points, awardedRankingPoints đọc snapshot quest_progress.awarded_points. Android hiện mức tiềm năng với nhiệm vụ chưa hoàn thành, mức thực nhận với nhiệm vụ đã hoàn thành. Không hiện điểm mới của admin như một khoản chưa nhận đối với nhiệm vụ cũ.
