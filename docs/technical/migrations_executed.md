# Migration đã chạy trên local v2

Ngày: 10/11/2026. Cả hai đều được chủ dự án phê duyệt rõ ràng trong chat trước khi chạy. Không tạo bảng migration history bổ sung.

| File SQL | SHA256 | Kết quả |
|---|---|---|
| local_schema_compatibility_proposed.sql | 8580445CEB8A61BAC58D2ECE7E119C6EC497F44A3DA7C809A61F366E6D88C00D | COMMIT thành công trên vietnam_explorer_dev_v2 |
| explorer_score_event_approved.sql | 93159A4377F0AC1AA68B885C8F134BCA506C1E4DBAE83E49EE27F17B8F8CA39E | COMMIT thành công trên vietnam_explorer_dev_v2 |

Phương án tương thích thêm 7 bảng, 6 cột, partial UNIQUE trên recommendation_config và CHECK/NOT NULL của cột mới. Ledger thêm 1 bảng với PK/FK/unique và 2 index đã duyệt. Không chạy migration 016; place.rating_count giữ NOT NULL/default cũ. File tương thích giữ nguyên comment đề xuất ban đầu để hash SQL không đổi; phê duyệt/thực thi được ghi tại đây.

Các seed dữ liệu tham chiếu là thao tác dữ liệu riêng trên target; không là migration schema. Không copy user/history thật. Database cũ được read-only recheck, xem schema_comparison_results.md.

## Nhập dữ liệu theo yêu cầu mới

Chủ dự án yêu cầu nhập bcv → v2 sau giai đoạn tạo schema. Đã chạy data-only merge, không thêm bảng/cột/constraint; backup, mapping, các activation giữ theo v2 và kiểm chứng ở data_import_report.md. Dữ liệu cá nhân không được in vào báo cáo hoặc đưa lên cloud.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).
