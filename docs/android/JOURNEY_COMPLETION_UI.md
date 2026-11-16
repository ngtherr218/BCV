# Màn hình hoàn thành hành trình Android

## Kiến trúc và phạm vi

Ứng dụng dùng XML Views, AppCompatActivity, ViewBinding, ViewModel/LiveData, repository và coroutine. Màn hình mới dùng cùng kiến trúc/màu Material hiện tại, không thêm thư viện. JourneyRewardController được giữ làm bộ điều phối mở màn hình, thay toàn bộ dialog cũ. JourneyCelebrationView cũ có animation lặp đã được loại bỏ vì không còn được tham chiếu.

Không thay công thức bước, điều kiện hoàn thành, mở khóa, cấp thẻ, vật phẩm, nhiệm vụ hoặc ghi điểm. Không đổi schema. Không thêm XP, cấp độ, video, âm thanh hay rung.

## Bố cục

1. Thanh quay lại và tiêu đề Thành quả hành trình.
2. Dấu tích, Chinh phục thành công!, lời chúc có tên địa danh đầy đủ.
3. Ảnh địa danh (156dp, bo góc trong MaterialCardView), tên và vùng. Placeholder nhỏ màu dịu khi thiếu ảnh/tải lỗi; khi có bitmap dùng centerCrop, không kéo méo.
4. Thẻ thành tựu: ảnh fitCenter, tên, trạng thái và thông tin đã lưu vào bộ sưu tập. Chỉ hiện khi backend xác nhận user_card tồn tại ở thời điểm hoàn thành. newlyAwarded=true mới dùng nhãn Đã mở khóa thẻ mới; thẻ đã sở hữu có nhãn trung tính.
5. Hai ô: recordedSteps và rankingPoints thực nhận. Điểm 0 hiện +0. Đây là điểm khám phá; điểm nhiệm vụ được ghi chú xem riêng, không tự cộng vào số này.
6. Thời điểm hoàn thành, Xem địa danh và Tiếp tục khám phá. Nội dung trong ScrollView, nút cao tối thiểu 52dp, hỗ trợ font scaling và tên dài xuống dòng. App hiện chỉ có theme sáng, không tự tạo theme tối mới.

## Nguồn dữ liệu

Thêm GET /api/journeys/{journeyId}/completion vì /api/explore chỉ trả hành trình đang đi hoặc mới nhất, không đủ để mở lại một Journey ID cũ khi có hành trình khác. Endpoint mới chỉ SELECT, dùng phiên hiện tại để lọc user_id; không có mutation. Trả 401 khi chưa đăng nhập, JOURNEY_NOT_FOUND cho hành trình không thuộc người dùng, JOURNEY_NOT_COMPLETED khi chưa hoàn thành.

Một SELECT trả journey/place ID, tên, vùng, ảnh, trạng thái, recordedSteps/requiredSteps, completedAt, isFirstDiscovery, card và rankingPoints. Thẻ được đọc từ user_card và received_at, không suy từ việc hiện có thẻ ACTIVE ở địa danh. Cờ mới dựa trên received_at bằng completed_at trong cùng transaction cấp thẻ. Thẻ thêm sau hành trình không bị nhận nhầm là phần thưởng lịch sử.

Điểm đọc từ ledger đã lưu: DISCOVERY của lần khám phá đầu tiên và các định dạng JOURNEY/FIRST_PLACE lịch sử có thể đối chiếu chắc chắn. Không áp mức cấu hình hiện tại vào quá khứ. Không backfill. Android không tính lại điểm, không gọi endpoint sync/hoàn thành/cấp thẻ khi mở hoặc thử lại màn hình.

Repository parse receipt; JourneyCompletionViewModel chỉ chuyển Ready khi ID khớp, place hợp lệ, status COMPLETED, recordedSteps>=requiredSteps>0, điểm không âm và có thời điểm hoàn thành. State Loading/Failed/SessionExpired dùng component hiện có. Lỗi có nút Thử lại; không lộ thành công hoặc các nút hành động khi chưa xác minh.

## Chống mở lặp và phục hồi

- Controller bỏ qua IN_PROGRESS, progress chưa đủ và ID không hợp lệ; trạng thái completed của model phải đến từ backend.
- Cờ opening chặn hai lệnh mở ngay trong cùng vòng lặp trước onPause. Controller chỉ mở lúc host resumed, và dùng SINGLE_TOP.
- Đánh dấu đã trình bày sau khi receipt được xác minh; đánh dấu khi người dùng chủ động đóng để không buộc mở lại lỗi liên tục. Không đánh dấu chỉ vì bắt đầu load. Nếu process chết trước xác minh, lần khôi phục có thể tải lại kết quả.
- Mốc đã xem completion được scope bằng backend + user ID, sống qua đóng app/đổi token; đọc chuyển tiếp mốc dialog cũ cho phiên hiện tại. Các marker thẻ khác giữ nguyên để tương thích.
- Xoay màn hình giữ ViewModel, không phát sinh lại GET hoặc nghiệp vụ thưởng. Mở lại từ Lịch sử cho phép đọc receipt theo ID một lần nữa, kể cả đang có hành trình mới.
- Lưu ý: thành quả đã xem sẽ không tự ép mở mỗi lần khởi động; vẫn có thể chủ động xem lại ở Lịch sử hành trình.

## Animation và điều hướng

Chỉ có fade toàn bộ nội dung 240ms và scale dấu tích 95%→100% trong 200ms, một lần. Kiểm tra ValueAnimator.areAnimatorsEnabled; lưu entrancePlayed khi recreation, cancel khi pause và đưa alpha/scale về 1. Theme riêng tắt transition toàn cục cho màn này. Không có callback animation liên quan dữ liệu, điều hướng hay thưởng.

Xem địa danh dùng PlaceDetailActivity với đúng ID và discovered=true, CLEAR_TOP; Tiếp tục khám phá dùng ExploreActivity với CLEAR_TOP|SINGLE_TOP. Cờ leaving và disabled hai nút ngay lập tức chặn bấm lặp; completion Activity kết thúc sau điều hướng. Lịch sử các hành trình COMPLETED mở receipt; hành trình khác giữ luồng xem địa danh hiện tại.

## Các file chính

- android/app/src/main/java/com/project/bcv/feature/journey/completion/JourneyCompletion.kt
- android/app/src/main/java/com/project/bcv/feature/journey/completion/JourneyCompletionActivity.kt
- android/app/src/main/res/layout/activity_journey_completion.xml
- android/app/src/main/java/com/project/bcv/feature/collection/presentation/JourneyRewardController.kt
- android/app/src/main/java/com/project/bcv/feature/collection/data/CardInbox.kt
- android/app/src/main/java/com/project/bcv/feature/journey/presentation/JourneyHistoryActivity.kt
- android/app/src/main/AndroidManifest.xml và res/values/themes.xml
- backend/Bcv.Api/Modules/Journey/Infrastructure/JourneyCompletion.cs và Api/JourneyEndpoints.cs

Kiểm thử và bằng chứng: ../testing/JOURNEY_COMPLETION_TESTS.md.
