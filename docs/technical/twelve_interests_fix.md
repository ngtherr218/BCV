# Sở thích: đủ 12 loại địa danh — 11/11/2026

Theo xác nhận của chủ dự án: hiển thị đủ 12 loại địa danh hiện có, giữ giới hạn chọn từ 1 đến 3.

## Nguyên nhân

Màn Android tải API /api/interests; repository đang trả bảng interest_group chỉ có sáu nhóm độc lập 201–206. Màn không giới hạn số thẻ, nhưng dòng hướng dẫn cũng ghi cố định sáu nhóm. Database có 12 loại chuẩn 101–112 và 10 loại nhập cũ 1–10; chỉ dùng 12 loại chuẩn, không trộn thành 22 lựa chọn.

## Cách sửa

- Seed thêm 12 bản ghi PLACE_TYPE_101 đến PLACE_TYPE_112 vào interest_group hiện có, lấy tên từ place_type. Không thêm bảng/cột/index, không đổi model, không xoá nhóm hoặc lựa chọn cũ.
- GET /api/interests và /api/explore/filters trả đúng 12 loại chuẩn. PUT tiếp tục dùng user_preference_interest và giới hạn ba lựa chọn trong transaction.
- Giữ sáu nhóm cũ để tài khoản/client cũ tiếp tục hoạt động. Khi mở editor với lựa chọn cũ, API requiresReselection=true và Android nhắc chọn lại. Không tự đoán chuyển một sở thích rộng sang nhiều loại mới; lựa chọn cũ chỉ bị thay khi người dùng lưu thành công.
- InterestCatalog tái sử dụng mapping 12 loại trong db/014_standard_place_types.sql, gồm bảo tàng/làng nghề/đảo/sông hồ/các nhóm tự nhiên đầy đủ. Recommendation đọc mapping 12 loại hoặc fallback sáu nhóm theo cấu hình phiên bản cũ; không thay K-Means, trọng số hay công thức bước.
- Search dùng cùng mapping cho interestGroupId/matchesInterests. Dòng hướng dẫn Android đổi thành 12 loại; grid vẫn dựng động từ API.

## Danh mục

101 Di tích và bảo tàng; 102 Đền, chùa và nhà thờ; 103 Công trình kiến trúc; 104 Phố cổ và làng nghề; 105 Chợ và ẩm thực; 106 Vui chơi và nghỉ dưỡng; 107 Đồng quê và nông nghiệp; 108 Biển và đảo; 109 Sông, hồ và thác; 110 Núi và đèo; 111 Hang động; 112 Rừng và khu bảo tồn.

## Kiểm thử

API integration 38/38 đạt, gồm catalog12 không trùng, nhắc chọn lại từ nhóm cũ, lưu/tải lại 101/108/112, recommendation, filter ngoài sở thích và từ chối bốn lựa chọn không làm mất dữ liệu đã lưu. Logic C# 27/27 đạt. Dữ liệu test dùng tài khoản example.invalid và bước synthetic trên v2.

Seed dữ liệu: db/seed_dev_v2_twelve_interests.sql có guard database/đủ12loại/xung độtID và transaction. Không chạy migration schema. Bằng chứng API: output/dev-v2/verification/twelve-interests-api.txt.

## Android đã xác minh

APK build thành công,52/52 unit test đạt. Đã cài cập nhật lên emulator-5554 không xoá dữ liệu. LocalExplorerScreensTest đạt trên thiết bị: API đủ12loại, giữ nhắc chọn lại legacy, lưu3loại101/108/112, khôi phục đúng lựa chọn và mở màn sở thích có Rừng và khu bảo tồn/counter3/3; đồng thời search/passport/leaderboard tải được dữ liệu. Log tại output/dev-v2/verification/twelve-interests-emulator.txt. App đã mở lại sau test, phiên người dùng trước test được khôi phục.
