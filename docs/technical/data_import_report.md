# Nhập dữ liệu bcv → vietnam_explorer_dev_v2

Thực hiện ngày 10/11/2026 theo yêu cầu rõ ràng “Đổ dữ liệu từ db cũ sang db mới”. Yêu cầu này cho phép nhập cả tài khoản và lịch sử nguồn; không còn chỉ nạp reference seed như giai đoạn trước. Database nguồn luôn được mở trong transaction READ ONLY/REPEATABLE READ.

## Phương pháp và kết quả

- Đã sao lưu toàn bộ v2 trước nhập: `output/db-import-20261110-141339/target-before-import.dump`; pg_restore --list đọc archive thành công. Không phục hồi đè hoặc xóa database nào.
- Import dữ liệu của đúng 25 bảng nghiệp vụ nguồn bằng một transaction Serializable ở target, khóa ghi các bảng liên quan trong lúc merge. Không CREATE/ALTER/DROP/TRUNCATE/DELETE bảng và không thay schema.
- Địa danh/vùng dùng ID danh mục; user dùng email; thẻ dùng địa danh; các bảng nối dùng khóa nghiệp vụ. ID trùng được ánh xạ trong bộ nhớ, không tạo bảng mapping. File mapping nằm trong output bị Git bỏ qua.
- Đã nhập đủ 6 tài khoản nguồn; 3 tài khoản phải đổi ID để giữ tài khoản v2 đã có. Các FK của profile, vị trí ảo, journey, step_data, discovery, card ownership, inventory, quest progress và notification đã được chuyển theo cùng mapping. Chuỗi health-connect:journey:{id} cũng được cập nhật theo journey ID mới.
- Giữ admin `admin@gmail.com` và mật khẩu đã được chủ dự án yêu cầu. Tài khoản nguồn giữ password hash, không reset hoặc in mật khẩu. Không sao chép file session giữa hai database.
- Giữ metadata/version/cấu hình active của mô hình v2, 10 tên/mô tả thẻ đặc trưng và các cột mới không có ở nguồn. Bổ sung rating snapshot còn thiếu bằng rating thực của địa danh nhập; không thay snapshot model đã có.
- Trạng thái DRAFT từ nguồn ban đầu ghi đè activation của mẫu v2. Đã khôi phục từ backup riêng 10 thẻ đặc trưng, 6 thử thách tuần và 2 vật phẩm mà v2 đã bật trước nhập. Nội dung cũ khác vẫn giữ trạng thái nguồn. Tool đã được sửa để giữ các activation này nếu phân tích/nhập lại.
- Giữ 10 loại địa danh seed v2 chưa dùng, đồng thời nhập 12 loại từ nguồn: target hiện có 22 loại, không phải import trùng. Không tự xóa danh mục sẵn có; địa danh nhập liên kết tới đúng loại nguồn.
- Không tính ngược điểm xếp hạng. Đã kiểm tra toàn bộ user ID nhập: không có explorer_score_event. Ghi JOURNEY mới còn kiểm tra lịch sử COMPLETED, tránh nhận lại điểm cho địa danh đã hoàn thành trước khi ledger tồn tại.
- Sở thích loại cũ vẫn có trong user_preference_place_type. Nhóm sở thích mới độc lập không được tự bịa/áp đặt; người dùng cũ có thể cần chọn lại 1–3 nhóm ở lần đăng nhập tiếp theo. Mức vận động/vị trí ảo được giữ.

## Đối chiếu từng bảng

Bảng dưới ghi ngay sau transaction nhập, trước tài khoản/bước fixture phát sinh trong lượt regression sau đó. Cột “đã giữ” bao gồm bản ghi cùng khóa được tái sử dụng. Các file báo cáo không chứa email, password hash hoặc bản ghi cá nhân.

| Bảng | Nguồn | V2 trước | Thêm | Cập nhật | Đã giống | V2 ngay sau |
|---|---|---|---|---|---|---|
| region | 63 | 63 | 0 | 63 | 0 | 63 |
| place_type | 12 | 10 | 12 | 0 | 0 | 22 |
| recommendation_criterion | 40 | 40 | 0 | 0 | 40 | 40 |
| recommendation_config | 1 | 1 | 0 | 1 | 0 | 1 |
| place | 312 | 311 | 1 | 311 | 0 | 312 |
| place_image | 61 | 61 | 0 | 0 | 61 | 61 |
| place_feature | 312 | 311 | 1 | 311 | 0 | 312 |
| place_feature_value | 10920 | 10885 | 35 | 0 | 10885 | 10920 |
| recommendation_weight | 5 | 5 | 0 | 0 | 5 | 5 |
| app_user | 6 | 7 | 6 | 0 | 0 | 13 |
| user_preference | 4 | 6 | 4 | 0 | 0 | 10 |
| user_preference_place_type | 7 | 0 | 7 | 0 | 0 | 7 |
| virtual_location | 4 | 6 | 4 | 0 | 0 | 10 |
| journey | 4 | 7 | 4 | 0 | 0 | 11 |
| step_data | 7 | 5 | 7 | 0 | 0 | 12 |
| user_discovered_place | 5 | 5 | 5 | 0 | 0 | 10 |
| card | 135 | 135 | 0 | 135 | 0 | 135 |
| collection | 7 | 5 | 2 | 5 | 0 | 7 |
| collection_card | 140 | 125 | 15 | 0 | 125 | 140 |
| user_card | 3 | 5 | 3 | 0 | 0 | 8 |
| item | 2 | 2 | 0 | 2 | 0 | 2 |
| user_item | 3 | 6 | 3 | 0 | 0 | 9 |
| quest | 11 | 11 | 0 | 11 | 0 | 11 |
| quest_progress | 4 | 33 | 4 | 0 | 0 | 37 |
| notification | 5 | 36 | 5 | 0 | 0 | 41 |
## Kiểm chứng đã chạy

- Plan chỉ đọc trước import đã phát hiện trùng ID và ánh xạ trước khi commit.
- Plan chỉ đọc sau import/khôi phục activation: 0 INSERT, 0 UPDATE nếu nhập lại, không sinh trùng.
- Kiểm tra READ ONLY: số bản ghi thuộc 6 user đã nhập khớp nguồn cho account (6), preference (4), journey (4), steps (7), discovery (5), user_card (3), inventory (3), quest_progress (4), notification (5).
- Đã kiểm tra các user được nhập không có điểm lịch sử trong ledger; thẻ đặc trưng active=10, thử thách tuần active=6.
- Schema/source recheck: bcv giữ nguyên schema và số bản ghi ban đầu; target vẫn 34 public tables, chỉ các schema đã duyệt từ giai đoạn trước. Xem schema_comparison_results.md.
- Regression HTTP sau import: 32/32 đạt, bao gồm login/onboarding/search/preview/quest, completion đồng thời, cấp thẻ và điểm chống trùng, vé và hành trình lặp. Dùng tài khoản example.invalid và bước fixture riêng, không sửa hành trình của user được nhập.

Snapshot sau cùng có thể nhiều hơn số dòng “V2 ngay sau” vì fixture regression vẫn được giữ. Không tự xóa dữ liệu để làm số liệu khớp giả. Source bcv không bị ghi dữ liệu hoặc reseed sequence; setval chỉ trên các sequence identity của target để tránh trùng ID ở lần tạo mới tiếp theo.
Tool là thao tác nhập một lần: lần apply tiếp theo bị chặn trước khi kết nối DB nếu đã có applied.json. Guard này đã được thực thi và trả thông báo đúng; chế độ mặc định vẫn là plan read-only.
