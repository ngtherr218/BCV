# Đã thay thế bằng v3

Nội dung dưới đây là lịch sử triển khai ba mức chung. Cơ chế hiện hành: [thiết kế v3](../points/POINT_SYSTEM_DESIGN.md), [migration](../points/POINT_MIGRATION.md) và [kết quả kiểm thử](../testing/POINT_SYSTEM_TEST_RESULTS.md). Không dùng quy tắc cũ dưới đây để vận hành.

# Điểm xếp hạng và cấu hình phần thưởng

## Hành vi

- Web quản trị: **Hệ thống → Cấu hình điểm thưởng**, hoặc liên kết tại trang Nhiệm vụ.
- Ba mức điểm chung: hoàn thành nhiệm vụ (mặc định 20), mỗi hành trình đi bộ hoàn thành (50), lần đầu đi bộ khám phá địa danh (thưởng thêm 100).
- Nhập số nguyên từ 0 đến 100000. Giá trị 0 tắt thưởng cho loại hoạt động tương ứng. Đây là cấu hình chung theo loại hoạt động, chưa phải mức riêng cho từng nhiệm vụ.
- Áp dụng cho hoạt động hoàn thành sau khi lưu; điểm lịch sử giữ nguyên. Nhiệm vụ hoàn thành hiển thị điểm thực nhận; nhiệm vụ chưa hoàn thành hiển thị mức hiện tại.
- Mỗi journey ID chỉ nhận điểm một lần, kể cả gửi đồng thời hoặc đồng bộ lại. Địa danh đã khám phá (đi bộ hoặc vé bay) không thể chọn lại làm điểm đến. Điểm hành trình và thưởng khám phá chỉ áp dụng khi hoàn thành đi bộ đến địa danh chưa khám phá. Backend chặn cả tạo mới, thay điểm đến và hoàn thành hành trình cũ không còn hợp lệ. Vé bay không cộng điểm xếp hạng.
- Android hiển thị điểm trong nhiệm vụ và thông báo hoàn thành hành trình. Bảng xếp hạng đặt vị trí của người dùng ở đầu, đánh dấu dòng của người dùng và bổ sung dòng đó nếu nằm ngoài top 20.

## Dữ liệu và API

Migration `db/027_explorer_reward_config.sql` thêm bảng singleton `explorer_reward_config`. Đã áp dụng vào `vietnam_explorer_dev_v2`; không áp dụng vào database nguồn `bcv`. Môi trường khác cần áp dụng migration trước khi dùng trang cấu hình.

GET/PUT `/api/admin/exploration/reward-settings` chỉ dành cho ADMIN đang ACTIVE. PUT kiểm tra revision để chặn ghi đè bản chỉnh sửa mới hơn. Giao dịch hoàn thành hoạt động giữ khóa đọc cấu hình đến khi ghi điểm xong. Sổ điểm giữ rule version và số điểm thực tế; không backfill các hành trình cũ.

## Ảnh địa danh

Đã bổ sung 7 ảnh cho Bãi Dài Cam Ranh, Thành cổ Diên Khánh, Vườn quốc gia U Minh Thượng và Thánh Địa Mỹ Sơn. Không thay ảnh hiện có. Manifest tác giả, giấy phép và liên kết nguồn: `backend/Bcv.Api/Modules/Discovery/Domain/place-photo-credits.json`. Web và Android có hiển thị ghi nguồn. Chrome đã tải thành công ảnh mới của Thành cổ Diên Khánh và xác nhận phần ghi nguồn; chưa dùng kết quả này để khẳng định mọi ảnh đều tải được trên mọi mạng.

## Kiểm chứng

- 15 kiểm tra API cấu hình: phân quyền, biên dữ liệu, revision, cộng điểm theo cấu hình, mức 0, giữ điểm lịch sử và khôi phục cấu hình gốc.
- 52 kiểm tra API khám phá/xếp hạng: hành trình, nhiệm vụ, gửi đồng thời, không cộng trùng, chặn chọn lại địa danh, giữ hành trình hiện tại khi thay điểm đến không hợp lệ và vé bay.
- 7 kiểm tra web qua Chrome với API thật, gồm lưu/tải lại/hoàn tác và màn hình 390px; không lỗi JavaScript.
- Android: build APK và 52 unit tests thành công. Kiểm thử riêng tính năng điểm thưởng trên thiết bị đạt `OK (1 test)`, xác nhận điểm hành trình, vị trí xếp hạng và điểm nhiệm vụ hằng tuần; log `output/dev-v2/verification/ranking-rewards-emulator-final.txt`. Bộ kiểm thử toàn bộ màn hình bị hộp thoại hệ thống/Pixel Launcher không phản hồi chặn focus, nên không được tính là đạt. Dữ liệu local hiện không có nhiệm vụ hằng ngày ACTIVE; kiểm thử điểm chuyển sang tab hằng tuần.
- Vite production build thành công. Kiểm tra schema xác nhận cấu trúc và số dòng từng bảng database nguồn vẫn khớp snapshot gốc.

Các kiểm thử hành trình dùng bước chân giả lập gửi vào API local, không xác nhận cảm biến hoặc Health Connect thực tế. Tài khoản kiểm thử riêng dùng miền `example.invalid`. Log API tại `output/dev-v2/verification/reward-settings-api.txt` và `ranking-rewards-api.txt`; ảnh web tại `output/playwright`.

APK bàn giao: `output/dev-v2/bcv-local-v2-debug.apk`, API base `http://127.0.0.1:5082/`. Backend và web kiểm thử dùng 5084/5174 để không chiếm 5082/5173 của phiên chạy VS Code.

## Hiệu chỉnh quy tắc không chọn lại địa danh

Đã bỏ hành vi cho phép quay lại địa danh cũ để nhận thêm điểm. API tạo hành trình trả PLACE_ALREADY_DISCOVERED trước khi thay thế hành trình hiện tại. Unlock đi bộ không phải lần khám phá đầu tiên trả lỗi và rollback toàn bộ giao dịch; sổ điểm cũng có kiểm tra firstDiscovery riêng. Kiểm thử mô phỏng hành trình cũ sai quy tắc chỉ sửa dữ liệu của tài khoản QA example.invalid, xác nhận lỗi 400 và không đổi tiến độ/điểm. Log mới: output/dev-v2/verification/discovered-destination-api.txt. Không tự sửa điểm lịch sử và không thay schema. Điểm hành trình và điểm khám phá là hai khoản cộng cho cùng lần hoàn thành đến địa danh mới, tổng mặc định 150; nhiệm vụ tính riêng.
