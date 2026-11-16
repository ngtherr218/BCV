# Ảnh hưởng tài liệu đồ án

## Cập nhật 11/11/2026

- Card: bộ demo 10 biểu tượng văn hóa được tự vẽ bằng mã local, lưu JPEG qua API upload/card hiện có; ghi nguồn tác giả của nhóm/mã dự án trong phụ lục hình ảnh. Không dùng ảnh biểu tượng để giả lập khám phá hay ownership. ERD không đổi.
- Vận hành local: health Development công bố tên database để QA kiểm tra đúng target trước thao tác dữ liệu; đây là metadata triển khai, không là quyền truy cập nghiệp vụ mới.

- P1.3: màn Hành trình hiển thị ảnh preview ghép bốn phần theo 25/50/75/100% tiến độ đã xác nhận. Đây là trạng thái UI suy ra, không là kho dữ liệu hoặc giao dịch cấp vật phẩm mới; ERD không đổi. DFD thêm luồng đọc cover preview cho màn Hành trình.
- Đặc tả công thức phải khớp source: kappa=1, L=0,75 m; không dùng số 0,1/0,15 từ tài liệu cũ. Hành trình cũ giữ required_steps đã lưu; không tính lại.
- Use case onboarding ghi 1–3 lựa chọn từ sáu nhóm sở thích; gợi ý không hạn chế quyền tự chọn điểm ngoài sở thích.
- Mốc chu kỳ Quest/ranking cùng ISO week giờ Việt Nam; có test đầu tuần/đầu năm. Bổ sung acceptance criteria về [start,end), vé không sinh điểm đi bộ và chống farm cùng địa danh.
- Hồ sơ kiểm thử tách 53 unit Android, 41 ca instrumentation (hai assumption), 32 integration API, 26 logic và 22 admin API; không thay chúng bằng tuyên bố kiểm chứng Health Connect thực.
- ERD hiện có 8 bảng/6 cột bổ sung theo hồ sơ lịch sử; lượt rà soát này không có entity/quan hệ/index mới. Giữ snapshot nguồn, after và ngoại lệ deparser CHECK trong phụ lục.
- Các file Markdown này là hướng dẫn cập nhật; chưa chỉnh trực tiếp tài liệu Google Docs/Word hay sơ đồ đồ án chính thức.

- Đặc tả/use case: tách xem trước khỏi khám phá/nhận thẻ; tự do chọn địa danh ngoài sở thích; bộ lọc theo vị trí ảo, ước tính ngày theo activity; 2+1 gợi ý không thay model.
- Luồng cũ cần ghi rõ: vé bay mở khóa/thẻ; quyết định điểm đi bộ không tự áp sang vé. Đối chiếu công thức StepConversion và các README đang khác hệ số trước cập nhật đặc tả.
- DFD: bổ sung truy vấn filter/preview/passport và tiến độ; Quest dùng kho hiện tại. Ledger đã được duyệt/triển khai: thêm kho sự kiện điểm, luồng ghi trong transaction và tổng hợp tuần/vùng. Tách luồng vé không chấm điểm đi bộ.
- ERD: bổ sung 7 bảng tương thích đang thiếu ở DB nguồn, explorer_score_event và 6 cột theo phê duyệt. Giữ nguyên các cột/PK/FK/index cũ; xem proposal và schema comparison. Không có bảng badge ownership/period/quiz mới.
- Thuật toán: ngày ước tính = ceil(requiredSteps/dailyTarget), target theo cấu hình active; EASY <=1 ngày, MODERATE <=3, HARD >3. Surprise trong một ngày vận động, không lọc tuyệt đối sở thích. Score hiện hành giữ nguyên, selector 2+1 chạy sau scoring. Công thức bước kappa=1 được giữ nguyên.
- Triển khai: sơ đồ local cho API/PostgreSQL/web/Android; Goong và Health Connect là tích hợp ngoài, không mô tả là dịch vụ nhóm tự host. R2/SMTP tắt ở profile local v2.
- Kiểm thử/nghiệm thu: phân biệt unit/build với tích hợp/live/device; thêm bằng chứng bảo toàn schema, chống trùng thưởng, ranh giới tuần giờ Việt Nam và riêng tư leaderboard.
- Hướng phát triển: P2 chỉ được ghi là hoàn thành nếu có mã, tích hợp và bằng chứng thực thi; hiện giữ là tương lai.

## Quản trị và chuyển dữ liệu local

DFD bổ sung truy vấn quản trị overview/leaderboard/events (chỉ đọc, quyền ADMIN); chỉnh catalog vẫn qua CRUD hiện có. Công cụ nhập local là thao tác chuyển dữ liệu một lần có backup, không là API công khai hoặc tiến trình đồng bộ nền. Mapping ID chỉ lưu ở output, không là entity mới. ERD không đổi trong lần nhập này. Tài khoản cũ giữ hash và lịch sử; nhóm sở thích mới có thể yêu cầu chọn lại theo luồng onboarding hiện có.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).
## Cập nhật cơ chế điểm v3 (12/11/2026)

Tài liệu cơ chế ba mức chung trước đây đã được thay thế. Điểm khám phá gộp thành một giao dịch; mỗi nhiệm vụ có reward_points riêng, điểm thực nhận được chốt theo chu kỳ. Xem ../points/POINT_SYSTEM_DESIGN.md và ../testing/POINT_SYSTEM_TEST_RESULTS.md (bảng F14/F15/F16/F18/F20/F22/F23/F24/F43/F44). Không coi điểm mở rộng là quy tắc vốn có trong Phụ lục A chưa được cung cấp bản gốc.
