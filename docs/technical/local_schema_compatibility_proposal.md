# Chênh lệch schema live và backend — ĐÃ PHÊ DUYỆT, ĐÃ TRIỂN KHAI

Chủ dự án xác nhận “Phê duyệt phương án tương thích trên DB v2” ngày 10/11/2026. SQL bên dưới đã được áp dụng đúng một lần trên v2, transaction thành công. Các mô tả yêu cầu phê duyệt phía dưới là nội dung phương án gốc được giữ để truy vết; không phải phần còn chờ duyệt.

Database nguồn được xác minh là localhost:5432/bcv. Có 25 bảng nghiệp vụ + spatial_ref_sys. Database vietnam_explorer_dev_v2 đã sao chép cấu trúc này, không sao chép bản ghi nghiệp vụ. Mã backend đang yêu cầu các thành phần dưới đây nhưng chúng chưa có trong cả hai database. Đây là migration tương thích mã hiện tại, khác với ledger xếp hạng.

**Không chạy nguyên chuỗi migration cũ.** Không áp dụng 016 vì nó xóa NOT NULL/default của place.rating_count, trái yêu cầu bảo toàn cột cũ. Không sửa bất kỳ PK/FK/constraint/index cũ nào. Không dùng startup EnsureAsync để vượt phê duyệt.

## Các cột thêm cần phê duyệt

| Bảng | Cột mới | Kiểu | NULL | Default cuối cùng | Ràng buộc | Mục đích và tác động |
|---|---|---|---|---|---|---|
|app_user|last_login_at|TIMESTAMPTZ|Có|Không|Không|Đăng nhập/profile/admin đang đọc/ghi; không bịa lịch sử|
|app_user|media_upload_blocked|BOOLEAN|Không|false|Không|Quyền upload media hiện có|
|user_card|seen_at|TIMESTAMPTZ|Có|Không|Không|Thẻ chưa xem; target không có user cần backfill|
|notification|hidden_at|TIMESTAMPTZ|Có|Không|Không|Ẩn thông báo thay vì xóa|
|place|feature_source|VARCHAR(10)|Không|'CUSTOM'|CHECK TYPE/CUSTOM trên cột mới|Giữ đặc trưng địa danh cũ; web chuyển inheritance rõ ràng|
|recommendation_config|settings|JSONB|Không sau backfill được duyệt|Không|NOT NULL của cột mới|Metadata model/nhóm sở thích/ngưỡng activity; seed config phải điền metadata thực|

Mọi cột cũ giữ nguyên. Thêm settings cần metadata mô hình được kiểm chứng; không tự backfill phiên bản custom. Schema target đang trống nên thêm NOT NULL trực tiếp không ảnh hưởng bản ghi. Nếu đã có seed lúc áp dụng, phải dùng backfill có guard như SQL 021 trước NOT NULL.

## Các bảng thiếu

### interest_group — bắt buộc cho mã sở thích hiện tại

Tái sử dụng place_type không đủ vì sáu nhóm sở thích mới độc lập với loại địa danh; user_preference_place_type giữ nguyên lịch sử. Backend Account/Recommendation và Android onboarding hiện dùng nhóm này; admin model cần mapping. Không có màn quản trị mới.

| Cột | Kiểu | NULL | PK/FK/UNIQUE | Default | Mô tả |
|---|---|---|---|---|---|
|interest_group_id|BIGINT|Không|PK|Không|ID nhóm|
|code|VARCHAR(50)|Không|UNIQUE|Không|Mã ổn định|
|name|VARCHAR(100)|Không|Không|Không|Tên hiển thị|

### user_preference_interest — bắt buộc cho mã sở thích hiện tại

Bảng nối N:N user_preference–interest_group, không thay user_preference_place_type; tối đa ba nhóm do service validate. Backend sở thích/gợi ý, Android chọn nhóm và regression onboarding bị ảnh hưởng.

| Cột | Kiểu | NULL | PK/FK | Default | Mô tả |
|---|---|---|---|---|---|
|user_preference_id|BIGINT|Không|PK ghép, FK user_preference|Không|Profile; ON DELETE CASCADE, UPDATE NO ACTION|
|interest_group_id|BIGINT|Không|PK ghép, FK interest_group|Không|Nhóm; DELETE/UPDATE NO ACTION|

### recommendation_rating_snapshot — bắt buộc cho model version hiện tại

place.rating tồn tại nhưng chưa lưu đánh giá theo phiên bản model. Không thay rating_count NOT NULL/default cũ. Backend gợi ý/import model, Android kết quả gợi ý và admin import bị ảnh hưởng.

| Cột | Kiểu | NULL | PK/FK/constraint | Default | Mô tả |
|---|---|---|---|---|---|
|recommendation_config_id|BIGINT|Không|PK ghép, FK recommendation_config|Không|Phiên bản|
|place_id|BIGINT|Không|PK ghép, FK place|Không|Địa danh|
|rating|NUMERIC(3,2)|Có|CHECK NULL hoặc 0..5|Không|Đánh giá chốt|
|rating_count|INTEGER|Có|CHECK NULL hoặc >=0|Không|Số đánh giá chốt|

Hai quan hệ cha 1:N, DELETE/UPDATE NO ACTION. Index mới trên **bảng cũ** recommendation_config: UNIQUE(status) WHERE status='ACTIVE', bảo đảm một cấu hình active; cần duyệt riêng, có thể chặn dữ liệu vi phạm. Hiện target trống; source không áp dụng.

### place_type_feature — bắt buộc cho web quản trị hiện tại

place_feature/value là đặc trưng theo địa danh, chưa chứa default theo loại; không sửa chúng. Backend Admin và web form địa danh/type dùng mapping này; cần regression đổi loại/giữ custom.

| Cột | Kiểu | NULL | PK/FK | Default | Mô tả |
|---|---|---|---|---|---|
|place_type_id|BIGINT|Không|PK ghép, FK place_type|Không|DELETE CASCADE, UPDATE NO ACTION|
|code|VARCHAR(50)|Không|PK ghép|Không|Mã feature model|

### community_media — bắt buộc nếu giữ chức năng media hiện tại

place_image là ảnh chính thức, không có owner/moderation/private lifecycle. Không dùng bảng này cho nhật ký ảnh riêng. Backend media/thumbnail/explore, Android gallery cộng đồng và web moderation cần bảng này. Giữ media như tính năng cũ; P2 ảnh riêng chưa triển khai.

| Cột | Kiểu | NULL | PK/FK/constraint | Default | Mô tả |
|---|---|---|---|---|---|
|media_id|BIGINT|Không|PK identity|identity|ID|
|user_id|BIGINT|Không|FK app_user|Không|Người tải|
|place_id|BIGINT|Không|FK place|Không|Địa danh|
|caption|VARCHAR(500)|Không|Không|''|Mô tả|
|mime_type|VARCHAR(40)|Không|Không|Không|Loại file|
|media_data|BYTEA|Có|Không|Không|File, nullable sau dọn lifecycle|
|status|VARCHAR(20)|Không|CHECK 6 trạng thái trong SQL|PENDING|Kiểm duyệt|
|review_note|VARCHAR(500)|Có|Không|Không|Lý do|
|reviewed_by|BIGINT|Có|FK app_user|Không|Admin|
|created_at|TIMESTAMPTZ|Không|Không|CURRENT_TIMESTAMP|Tạo|
|reviewed_at|TIMESTAMPTZ|Có|Không|Không|Duyệt|
|first_approved_at|TIMESTAMPTZ|Có|Không|Không|Quest ảnh duyệt lần đầu|
|file_deleted_at|TIMESTAMPTZ|Có|Không|Không|Dọn file|
|delete_after|TIMESTAMPTZ|Có|Không|Không|Hạn lưu|

Cha user/place/admin 1:N, FK DELETE/UPDATE NO ACTION. Hai index(place_id,status), (status,created_at). Bảng mới được tạo trực tiếp ở cấu trúc cuối; không DROP constraint trung gian.

### media_report — bắt buộc nếu giữ chức năng báo cáo media

notification không đủ vì không có unique media/reporter, trạng thái giải quyết và người giải quyết. Backend báo cáo, Android và web moderation cần bảng; regression ẩn/xóa/khôi phục/report trùng.

| Cột | Kiểu | NULL | PK/FK/constraint | Default | Mô tả |
|---|---|---|---|---|---|
|report_id|BIGINT|Không|PK identity|identity|ID|
|media_id|BIGINT|Không|FK community_media DELETE CASCADE|Không|Media|
|reporter_id|BIGINT|Không|FK app_user|Không|Người báo cáo|
|reason|VARCHAR(500)|Không|CHECK trim không rỗng|Không|Lý do|
|status|VARCHAR(12)|Không|CHECK OPEN/RESOLVED/ACCEPTED/DISMISSED|OPEN|Trạng thái|
|created_at|TIMESTAMPTZ|Không|Không|CURRENT_TIMESTAMP|Tạo|
|resolved_at|TIMESTAMPTZ|Có|Không|Không|Giải quyết|
|resolved_by|BIGINT|Có|FK app_user|Không|Admin|

UNIQUE(media_id,reporter_id), index(media_id,status). Các FK còn lại DELETE/UPDATE NO ACTION; mỗi cha 1:N.

### place_video — bắt buộc nếu giữ video chính thức hiện tại

place_image không lưu video/metadata/file; backend video/admin, Android player và web video field cần bảng. Giới hạn 60 giây theo code hiện tại, không thêm dịch vụ lưu trữ ngoài.

| Cột | Kiểu | NULL | PK/FK/constraint | Default | Mô tả |
|---|---|---|---|---|---|
|video_id|BIGINT|Không|PK identity|identity|ID|
|place_id|BIGINT|Có|FK place|Không|Có thể upload trước gắn place|
|created_by|BIGINT|Không|FK app_user|Không|Admin tạo|
|mime_type|VARCHAR(40)|Không|Không|video/mp4|MIME|
|video_data|BYTEA|Không|Không|Không|File|
|duration_seconds|DOUBLE PRECISION|Không|CHECK >0 và <=60|Không|Thời lượng|
|width|INTEGER|Không|CHECK >0|Không|Chiều rộng|
|height|INTEGER|Không|CHECK >0|Không|Chiều cao|
|created_at|TIMESTAMPTZ|Không|Không|CURRENT_TIMESTAMP|Tạo|

Các cha 1:N, DELETE/UPDATE NO ACTION. Index(place_id). Không thay bảng ảnh cũ.

## Migration và ảnh hưởng tài liệu

SQL đầy đủ dự kiến nằm trong [local_schema_compatibility_proposed.sql](local_schema_compatibility_proposed.sql), chỉ là tài liệu, không được tích hợp auto-migration. Chỉ chạy trên v2 sau phê duyệt rõ ràng. Không sửa schema bcv. Các INSERT nhóm sở thích/default feature và snapshot sẽ là seed target được tách khỏi DDL, dùng dữ liệu reference trong repo; không sao chép người dùng thật.

ERD thêm bảy entity/FK nêu trên và sáu cột; DFD bổ sung kho sở thích/model version/media mà source local đang thiếu so với mã. Regression API đăng ký/onboarding/gợi ý/journey/card/quest/admin đọc đã thực thi; media upload/moderation và UI thiết bị còn cần kiểm tra. Trạng thái **ĐÃ TRIỂN KHAI**; kết quả schema ở `schema_comparison_results.md`.
