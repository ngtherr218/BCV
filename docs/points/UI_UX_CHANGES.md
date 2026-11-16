# Thay đổi UI/UX điểm và nhiệm vụ

## Web quản trị

- Cấu hình có hai vùng: một form Điểm khám phá địa danh và một khối giải thích Điểm thưởng nhiệm vụ với liên kết #quests. Bỏ ba ô chung cũ. Đơn vị nằm cạnh input, lỗi hiển thị tại input, lưu/đang lưu/thành công/thất bại rõ ràng, có hoàn tác và giữ dữ liệu khi API lỗi.
- Biểu mẫu nhiệm vụ chia Thông tin cơ bản, Điều kiện hoàn thành, Phần thưởng. Chỉ có một lựa chọn chu kỳ, tự đồng bộ quest_type. Đơn vị mục tiêu thay theo điều kiện. Điểm riêng và vật phẩm tách rõ; ẩn số lượng nếu không chọn vật phẩm. Có gợi ý 10/25/50/100/150 và tóm tắt phần thưởng.
- Danh sách có cột Điểm thưởng, chu kỳ, vật phẩm; giữ tìm kiếm, trạng thái, phân trang và thêm lọc chu kỳ ở backend trước phân trang. Drawer giữ focus bàn phím và dữ liệu lỗi; khóa đồng bộ bằng ref và disabled để tránh gửi lặp khi bấm nhanh.
- Lịch sử điểm phân biệt DISCOVERY với các loại cũ có nhãn lịch sử; hiển thị tên nhiệm vụ, nguồn và phiên bản quy tắc.
- Tận dụng React, Material hiện có trên Android, CSS và icon sẵn có; không thêm thư viện UI mới. Hai cột desktop chuyển thành một trên màn hình nhỏ.

## Android

- Mỗi thẻ nhiệm vụ có nhãn ngày/tuần, mục tiêu, tiến độ, điểm riêng nổi rõ. Vật phẩm nằm dòng riêng, có trạng thái Đã nhận. Điểm hoàn thành ghi Đã cộng; trường hợp thực nhận 0 không hiển thị mức mới như còn được nhận.
- Footer nhiệm vụ xếp dọc, tránh ép phần thưởng dài và trạng thái cạnh nhau trên điện thoại. Tên không giới hạn một dòng; điểm khác biệt với số bước.
- Popup Khám phá thành công dùng đúng rankingPoints backend trả về và chỉ hiện một dòng điểm khám phá; có thông tin thẻ nếu đã được cấp, hoặc trạng thái chưa tải/chưa có thẻ. Không hardcode 150.
- Leaderboard giữ vị trí của bạn, highlight dòng cá nhân và lọc tuần/vùng; ghi chú đổi sang điểm khám phá và mức riêng từng nhiệm vụ.

Ảnh web: output/playwright/points-v3-settings-desktop.png, points-v3-settings-mobile.png, points-v3-quest-create.png, points-v3-quest-list.png, points-v3-quest-mobile.png.
Ảnh Android: output/dev-v2/verification/quest-ranking-points.png, quest-weekly-points-v3.png, discovery-reward-v3.png, ranking-my-position.png.
