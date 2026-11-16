# Kiểm thử Android và web — 11/11/2026

## Cập nhật sau lượt hoàn thiện

Đã bổ sung ghép ảnh milestone và test render: runner mới nhất `OK (41 tests)`, gồm 39 bài đạt và hai assumption không thực thi như mô tả bên dưới. Đã kiểm tra 26/26 logic recommendation/chu kỳ, 32/32 integration API, 22/22 admin API và 10/10 cấu hình ảnh thẻ văn hóa. Có một lượt ExploreSheetTest không mở được Activity; đã sửa bằng startActivitySync và chạy lại thành công, giữ XML lỗi trước ở output/dev-v2/verification/android-instrumentation-before-sheet-fix.xml. XML hiện tại chỉ còn hai assumption, log runner trực tiếp được lưu nguyên trạng.

Mười biểu tượng văn hóa đã được tự vẽ local và lưu qua API admin, ảnh cũ có backup; editor web đã hiển thị ảnh mới (output/playwright/cultural-card-editor.png). Health ở Development trả thêm database thực tế để kiểm tra đúng v2 trước fixture writes; ngoài Development giữ response cũ. Các kết quả 40 test và ảnh cover tham chiếu bên dưới là lịch sử của lượt kiểm thử trước khi bổ sung milestone/artwork. Xem implementation_report.md cho trạng thái hiện hành.

## Môi trường và kết quả

- Backend ASP.NET Core local: `http://127.0.0.1:5082`, database `vietnam_explorer_dev_v2`.
- Web React/Vite: `http://127.0.0.1:5173`, trình duyệt Chromium qua Playwright CLI.
- Android Studio mở dự án; emulator `Medium_Phone_API_37.0`, Android 17/API 37, serial `emulator-5554`. Build/test được thực hiện bằng Gradle với JDK đi kèm Android Studio, không phải bấm Run/Test trong IDE.
- Không chạy migration/import lại. Integration test tạo tài khoản riêng `example.invalid` và gửi bước mô phỏng trên database v2; các bước này không phải dữ liệu vận động thật.

| Hạng mục | Kết quả |
| --- | --- |
| Web unit test | 11/11 đạt |
| Web production build | Thành công |
| Backend integration, có ExerciseAwards | 32/32 đạt |
| Android debug APK + unit test | Build thành công; 53/53 unit test đạt |
| Android instrumentation qua runner trực tiếp | `OK (40 tests)`; 38 bài đạt và 2 bài không thực thi do assumption |
| Android ba màn mới qua API thật | Đạt: danh sách tìm kiếm, trạng thái không có kết quả, hộ chiếu và xếp hạng người mới |

Hai assumption: `DeviceLocationSmokeTest` cần đối số tọa độ GPS kỳ vọng; `emulatorNetworkRoutingDiagnostics` chỉ chạy với host `10.0.2.2`, không áp dụng cho APK localhost. XML của Android Gradle Plugin ghi hai assumption thành failure, trong khi AndroidJUnitRunner trực tiếp kết thúc `OK (40 tests)`. Không xem nhãn BUILD SUCCESSFUL của Gradle là bằng chứng toàn bộ test đạt.

## Quy trình và vấn đề đã xử lý

1. Kiểm tra working tree, runbook và cấu hình local v2. Khởi động backend/web ngoài sandbox để browser và emulator truy cập được socket host.
2. Chạy `node --test tests/*.test.mjs` và `npm run build` trong `web/`.
3. Chạy `./backend/test-dev-v2.ps1 -ExerciseAwards`: phân quyền, tìm kiếm/phân trang/bộ lọc, preview không cấp thưởng, tạo hành trình, gửi bước lặp, hoàn thành đồng thời, thẻ/hộ chiếu/điểm chỉ cấp một lần, nhiệm vụ tuần và chống farm điểm đều đạt.
4. Build Android và chạy unit test. Phát hiện init script đặt build directory quá muộn: APK vẫn nằm ở đường dẫn repository có dấu, làm Gradle/ADB không cài được nhưng Gradle vẫn báo thành công. Đổi helper sang `gradle.beforeProject` và sao chép APK từ build root ASCII.
5. Với `10.0.2.2:5082`, cả kết nối mặc định và explicit Wi-Fi trên emulator timeout. Không thay firewall; dùng `adb reverse tcp:5082 tcp:5082`, build lại với `http://127.0.0.1:5082/`.
6. Chạy instrumentation trên emulator; bổ sung `LocalExplorerScreensTest` để đăng ký fixture, thiết lập tài khoản rồi mở ba Activity thật. Test khôi phục session cũ khi kết thúc.
7. Đọc XML, kiểm tra assumption và chạy AndroidJUnitRunner trực tiếp để đối chiếu kết quả. APK được cài lại sau khi Gradle tự gỡ bộ test.
8. Playwright đăng nhập tài khoản quản trị có sẵn, kiểm tra dashboard; bốn tab Thẻ đặc trưng/Hộ chiếu vùng/Thử thách tuần/Xếp hạng & điểm; chọn Hà Nội và Áp dụng; mở biểu mẫu chỉnh sửa thẻ với tên/mô tả/ảnh ban đầu. Chưa lưu thay đổi nội dung qua biểu mẫu.
9. Chụp và xem bố cục web ở kích thước desktop và 390×844. Ảnh bằng chứng: `output/playwright/web-ranking.png`, `output/playwright/web-mobile.png`.

## Lệnh chạy lại Android

```powershell
& 'C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe' reverse tcp:5082 tcp:5082
./backend/build-android-dev-v2.ps1 -Api http://127.0.0.1:5082/
cd android
& 'E:/AndroidStudio/jbr/bin/java.exe' -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:connectedDebugAndroidTest '-PbcvApiBaseUrl=http://127.0.0.1:5082/' --offline -I ../output/dev-v2/android-v2.init.gradle
```

APK bàn giao: `output/dev-v2/bcv-local-v2-debug.apk`, dùng localhost:5082; phải giữ ADB reverse và backend đang chạy. Sau khi ngắt emulator/khởi động lại ADB cần thiết lập reverse lại. Không dùng APK này như một bản kết nối Internet độc lập.

Bằng chứng Android được lưu tại `output/dev-v2/verification/`: XML unit test, `android-instrumentation.xml` và `android-runner.txt`. XML gốc được giữ nguyên, gồm cách AGP biểu diễn assumption.

## Giới hạn còn lại

- Chưa xác minh bước chân/cảm biến/Health Connect và GPS thật trên điện thoại vật lý.
- Đường truy cập emulator `10.0.2.2:5082` vẫn timeout trên máy hiện tại; localhost qua ADB reverse hoạt động.
- Browser smoke test chưa bao phủ toàn bộ CRUD, upload media và quy trình kiểm duyệt.
- Console web có một lỗi phụ `favicon.ico` 404; không ghi nhận lỗi JavaScript ứng dụng trong những màn đã đi qua.
- Build có cảnh báo Kotlin deprecated/điều kiện luôn false và NuGet không truy vấn được dữ liệu vulnerability; build vẫn thành công, chưa xác nhận được kiểm tra vulnerability online.
- Computer Use mở được Android Studio và liệt kê emulator; bước chụp cửa sổ emulator sau test bị timeout phê duyệt công cụ. Bằng chứng Android dựa trên instrumentation/runner, không dựa vào ảnh UI thủ công.
