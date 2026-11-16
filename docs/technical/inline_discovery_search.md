# Tìm kiếm trong trang Khám phá tiếp — 11/11/2026

## Hành vi hiện tại

- Nút Khám phá tiếp mở RecommendationActivity có ô tìm tên ngay trên trang gợi ý.
- Checkbox Ưu tiên sở thích của tôi được tích mặc định. Khi mở trang, các loại địa danh được tích sẵn theo selectedPlaceTypeIds trong hồ sơ; danh sách lọc theo các loại đó, ưu tiên sở thích. API gợi ý 3 địa danh vẫn giữ nguyên.
- Chọn loại địa danh và bộ lọc mở danh sách12chip có thể tích nhiều loại, khoảng cách tối đa và số ngày tối đa. Đổi từ khóa/checkbox/chip tự cập nhật sau300ms; không cần chuyển màn hay bấm tìm riêng.
- Khi đang lọc, nếu ưu tiên sở thích còn tích thì địa danh khớp sở thích lên trước; đây không phải điều kiện loại bỏ địa danh ngoài sở thích. Bỏ tích dùng thứ tự gần nhất.
- Các loại được tích kết hợp OR; từ khóa/khoảng cách/số ngày kết hợp AND. Có phân trang20kết quả, empty/error/retry và nút đặt lại mặc định.
- Xem chi tiết hoặc chọn điểm đến tiếp tục dùng luồng cũ, gồm xác nhận thay hành trình đang đi. Nút xem bản đồ trả đúng các ID của trang đang hiển thị.
- Màn Khám phá tập trung vào bản đồ và hành trình, không còn ô/nút tìm kiếm. Tìm kiếm nằm trong Khám phá tiếp. Màn PlaceSearchActivity cũ được giữ tương thích.
- Lưu từ khóa, checkbox, loại, trang và trạng thái mở bộ lọc khi Activity được tạo lại.

## Backend

GET /api/explore/places thêm interestGroupIds dạng CSV (tối đa12ID loại chuẩn101–112); validate và bind bigint[] vào SQL tham số hóa. Giữ interestGroupId đơn và mọi query cũ. Không đổi schema, mô hình, công thức bước hoặc cơ chế thưởng.

## Kiểm thử đã thực thi

- Backend build đạt;41/41 API integration checks đạt (đa loại không trùng, ưu tiên đúng thứ tự, loại không hợp lệ trả400 và hồi quy hành trình/thưởng).
- Android build APK và52unit test đạt.
- Test thiết bị LocalExplorerScreensTest đạt: kiểm tra default checkbox, tìm tên ra empty state, đổi loại Biển và đảo có kết quả và bỏ ưu tiên chuyển Gần nhất; các màn sở thích/search/passport/leaderboard cùng fixture vẫn tải được.
- Đã cài APK cập nhật lên emulator-5554, không xoá dữ liệu app. Test dùng tài khoản riêng và khôi phục phiên cũ khi kết thúc; bước synthetic không phải số đo cảm biến.

Bằng chứng: output/dev-v2/verification/inline-discovery-api.txt và inline-discovery-emulator.txt. APK: output/dev-v2/bcv-local-v2-debug.apk. Chưa kiểm tra thao tác tay trên điện thoại vật lý trong lượt này.

## Điều chỉnh bố cục tiếp theo

- Đầu trang Khám phá tiếp giữ vị trí ảo, ô tìm kiếm và checkbox ưu tiên sở thích mặc định bật.
- 12 loại địa danh, khoảng cách và số ngày nằm trong bảng lọc mở từ dưới lên, có nút đặt lại và xem kết quả. Đổi lựa chọn vẫn cập nhật tự động.
- Nút bộ lọc hiển thị số lựa chọn đang dùng; phân trang và xem trên bản đồ nằm dưới danh sách kết quả.
- Trạng thái mở bảng lọc được lưu khi tạo lại Activity; dialog được đóng theo lifecycle.

Xác minh sau điều chỉnh: build APK và APK instrumentation thành công; 52/52 unit test đạt. LocalExplorerScreensTest chạy trên emulator-5554 đạt, kiểm tra checkbox mặc định, empty search, chọn Biển và đảo trong BottomSheetDialog và chuyển sang Gần nhất, đồng thời hồi quy các màn sở thích/search/passport/leaderboard. Đã xem trực quan ảnh kết quả và bảng lọc. Bằng chứng mới: `output/dev-v2/verification/discovery-redesign-emulator.txt`, `discovery-redesign-results.png`, `discovery-redesign-filters.png` trong cùng thư mục. APK local v2 đã cập nhật và cài lên emulator, không xóa dữ liệu app. Backend health xác nhận database v2; chưa kiểm tra trên điện thoại vật lý.
