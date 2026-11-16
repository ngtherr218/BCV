# Hồ sơ kỹ thuật local v2

## Tám tài liệu nghiệm thu

1. [Khảo sát hệ thống](system_audit.md)
2. [Schema nguồn ban đầu](database_schema_before.md)
3. [Phương án và lịch sử thay đổi database](database_changes_proposal.md)
4. [Kế hoạch ưu tiên và việc còn lại](implementation_plan.md)
5. [API bổ sung/thay đổi](api_changes.md)
6. [Schema v2 sau triển khai](database_schema_after.md)
7. [Báo cáo triển khai, trạng thái và kiểm thử](implementation_report.md)
8. [Ảnh hưởng đặc tả, DFD, ERD và tài liệu đồ án](thesis_documentation_impact.md)

## Chạy và đối chiếu

- [Hướng dẫn chạy/demo local](local_v2_runbook.md)
- [Đối chiếu schema](schema_comparison_results.md)
- [Manifest source/config](repository_inventory.md)
- [Kiểm thử Android/web](android_web_verification_2026-11-11.md)
- [Nhập dữ liệu đã thực hiện ở lượt trước](data_import_report.md)

Không có schema mới được đề xuất/thực thi trong lượt rà soát 11/11. Database v2 đang tồn tại; không tạo lại hoặc chạy lại migration. Các trạng thái phê duyệt lịch sử không thay cho phê duyệt DDL mới.

Bằng chứng mới nhất ở output/dev-v2/verification (Git bỏ qua); APK ở output/dev-v2/bcv-local-v2-debug.apk. Mười artwork tự tạo và backup ảnh thẻ cũ ở output/dev-v2/card-art; tạo lại bằng backend/configure-cultural-card-art-dev-v2.ps1. Backend/web/database chạy local; Goong/Health Connect/cảm biến giữ tích hợp hiện có.

## Gỡ video theo yêu cầu sau cùng

Xem video_removal_report.md: v2 còn 33 bảng, chỉ giữ ảnh và kiểm duyệt ảnh. SQL gỡ đã thực thi là remove_video_approved.sql. Các mô tả video trong tài liệu trước đây là lịch sử; không chạy lại migration video cũ.
