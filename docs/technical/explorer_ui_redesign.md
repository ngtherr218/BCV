# Thiết kế lại các màn khám phá và sưu tập

Ngày: 11/11/2026.

## Phân biệt chức năng

- Bộ sưu tập: album thẻ văn hóa theo chủ đề, được xem trước ảnh của mọi thẻ đang hoạt động, gồm thẻ chưa sở hữu. Nhãn sở hữu/thẻ mới và tiến độ thể hiện phần đã nhận. Xem ảnh không cấp thẻ.
- Hộ chiếu: lịch sử và tiến độ khám phá địa danh theo vùng, dấu mộc và huy hiệu hoàn thành vùng. Không phải bản sao album thẻ. Dấu mộc từng vùng có thể mở/thu gọn, dẫn đến địa danh đã khám phá.
- Bảng xếp hạng: thành tích tuần của người dùng và cộng đồng, có phạm vi toàn quốc/theo vùng; nút chuyển tuần thay ô nhập mã ISO. Hiển thị ngày theo giờ Việt Nam, không lộ timestamp kỹ thuật.

## Giao diện

Hộ chiếu và bảng xếp hạng dùng chung ExplorerPage: nền trắng, tiêu đề và nút quay lại, hero xanh forest, thẻ bo tròn, khoảng cách dp và typography đồng bộ. Bộ sưu tập có hai lối tắt ngắn gọn và phần Tất cả bộ sưu tập. Lưới thẻ chi tiết tăng kích thước ảnh, hiển thị toàn ảnh thay placeholder ổ khóa.

Bộ lọc dùng sheet nền trắng, loại đã chọn màu xanh/dấu tích, bộ đếm, ô giới hạn có nhãn và đơn vị, nhóm thao tác cố định cuối sheet. Mặc định chọn các loại từ hồ sơ; thêm/bỏ chọn tùy ý; bỏ chọn tất cả nghĩa là mọi loại; Đặt lại theo sở thích khôi phục loại và ưu tiên mặc định. Không ghi thay đổi vào hồ sơ. Giữ lựa chọn khi tạo lại Activity.

## API

CollectionRepository trả imageUrl cho mọi thẻ ACTIVE trong bộ, thumbnail bộ lấy từ tất cả ảnh thay vì chỉ ảnh thẻ đã sở hữu. Owned/IsNew, MarkSeen và cơ chế cấp thẻ không thay đổi. Không thay schema hoặc quyền gallery ảnh cộng đồng/nội dung địa danh.

## Xác minh

Backend và Android build thành công. API regression không tạo thưởng synthetic: 33/33 kiểm tra đạt, output/dev-v2/verification/explorer-redesign-api.txt. Unit Android có 52 ca, không lỗi. LocalExplorerScreensTest trên emulator-5554 đạt: tích đúng sở thích; chọn lại và đặt lại; không thay hồ sơ; tìm vùng/empty; chuyển tuần trước và về tuần này; tải hộ chiếu; ảnh thẻ thực sự hiển thị trong trang Bộ sưu tập và album; preview không cấp sở hữu. Thêm kiểm tra không token với album trả 401. Bằng chứng: output/dev-v2/verification/explorer-redesign-emulator.txt và các ảnh redesign-passport.png, redesign-leaderboard.png, redesign-collections.png, redesign-collection-preview.png cùng thư mục. APK cuối đã cài lên emulator, phiên người dùng được khôi phục sau bài test. Chưa thử trên điện thoại vật lý.

## Danh mục độc lập

Local có 10 thẻ ACTIVE, 7 bộ chủ đề DRAFT. Endpoint GET /api/collections/all-cards yêu cầu đăng nhập, trả mọi thẻ ACTIVE không phụ thuộc trạng thái bộ chủ đề. Trang Bộ sưu tập hiển thị trực tiếp lưới mọi thẻ, và có nút mở album riêng. Không kích hoạt bản nháp hoặc thay dữ liệu catalog. Audit chỉ đọc ở output/dev-v2/verification/collection-catalog-audit.sql.
