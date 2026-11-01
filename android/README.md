# App Android Bước Chân Việt (*android*)

App Kotlin cho người dùng: đọc số bước, đi hành trình ảo trên bản đồ Goong và mở khóa địa danh.

App gọi Backend API qua HTTPS, không truy cập database trực tiếp. Mặc định dùng cùng backend/database với web quản trị tại `https://bcv-admin-test.onrender.com/`. Mã nguồn chia theo nghiệp vụ trong `feature/`; phần dùng chung nằm trong `core/`.

## Mục lục

- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Luồng sử dụng](#luồng-sử-dụng)
- [Ghi nhận bước chân](#ghi-nhận-bước-chân)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Kiểm thử](#kiểm-thử)
- [Xử lý sự cố](#xử-lý-sự-cố)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Cài đặt

### Yêu cầu

- Android Studio bản mới (kèm JDK) và Android SDK.
- Thiết bị hoặc emulator Android 8.1 trở lên (minSdk 27), có Health Connect để đọc số bước.
- Backend đang chạy (xem [backend/README.md](../backend/README.md#cài-đặt)).

### Cấu hình `dev.properties`

Tạo `android/dev.properties` (Git bỏ qua file này):

```properties
bcvApiBaseUrl=https://bcv-admin-test.onrender.com/
bcvGoongMaptilesKey=<Goong Maptiles Key>
bcvGoongApiKey=<Goong REST API Key>
```

| Khóa | Ý nghĩa | Nếu bỏ trống |
| --- | --- | --- |
| `bcvApiBaseUrl` | Địa chỉ Backend, kết thúc bằng `/` | `https://bcv-admin-test.onrender.com/` |
| `bcvGoongMaptilesKey` | Hiển thị bản đồ Goong | Bản đồ trống |
| `bcvGoongApiKey` | Tra địa chỉ từ tọa độ và chỉ đường theo đường phố | Không hiện địa chỉ, không vẽ đường đi |

- **Bản deploy:** chỉ cần Internet, không cần backend local, cùng Wi-Fi với máy tính hay kết nối USB.
- **Chạy local:** ghi đè bằng `bcvApiBaseUrl=http://10.0.2.2:5081/` cho emulator, hoặc IP Wi-Fi máy tính cho điện thoại thật. Khi đó máy tính phải chạy backend và điện thoại cùng mạng; có thể dùng `127.0.0.1:5081` với `adb reverse` qua USB.
- Giá trị được đóng vào app lúc build, nên **sửa xong phải build lại**.
- Bản release nhận giá trị qua Gradle property, ví dụ `-PbcvApiBaseUrl=https://... -PbcvGoongMaptilesKey=...`, và cần URL HTTPS.
- Phiên đăng nhập, bộ đếm cảm biến, Recording API và mốc đối chiếu bước được tách theo địa chỉ backend. Đổi từ local sang deploy không gửi token hoặc tổng bước của local sang database deploy; dữ liệu local được giữ để có thể quay lại cấu hình local. App yêu cầu đăng nhập bằng tài khoản thuộc database đang chọn.
- Tài khoản tự điền cho kiểm thử chỉ dùng ở backend được chỉ định bởi `bcvPreviewLoginApiBaseUrl` (mặc định `http://127.0.0.1:5081/`). Chuyển sang Render không tự điền tài khoản local.

### Build và cài

```powershell
cd android
.\gradlew.bat installDebug
```

Hoặc mở thư mục `android/` bằng Android Studio rồi bấm **Run**. Một số điện thoại (ví dụ vivo) hỏi xác nhận cài qua USB; nếu Gradle báo `INSTALL_FAILED_ABORTED`, bấm **Cài đặt** trên điện thoại rồi chạy lại.

## Sử dụng

Mở app, đăng ký hoặc đăng nhập. Phiên được giữ 30 ngày và tự gia hạn, nên lần sau mở app sẽ vào thẳng màn đang dở (thiết lập hoặc Khám phá); chỉ khi phiên bị từ chối (đăng xuất, đổi mật khẩu, bị khóa) mới phải đăng nhập lại.

Khi trình diễn trên thiết bị không có bước thật, bấm nút icon người đi bộ có dấu "+" cạnh chữ **Khám phá** để ghi 2.000 bước mẫu vào Health Connect, rồi bấm **Đồng bộ** (hoặc chờ tự đồng bộ).

## Luồng sử dụng

1. **Đăng nhập / Đăng ký** bằng email và mật khẩu; có chức năng quên mật khẩu (mã gửi qua email nếu backend đã cấu hình SMTP).
2. **Chọn sở thích:** 1–3 trong 12 loại địa danh.
3. **Mức vận động:** Ít vận động, Trung bình hoặc Năng động.
4. **Health Connect:** cấp quyền đọc số bước; có thể chọn "Để sau".
5. **Điểm xuất phát:** dùng điểm hệ thống đề xuất hoặc vị trí hiện tại. Backend chọn địa danh `ACTIVE` gần nhất trong bán kính 75 km. Mỗi tài khoản chỉ lưu điểm xuất phát một lần.
6. **Khám phá:** bản đồ hiển thị vị trí ảo, hành trình đang đi và đường đi theo đường phố tới điểm đến. Icon người đi bộ quay mặt theo hướng đi. Số bước được tự động đồng bộ lên backend mỗi phút khi màn này mở (xem [Ghi nhận bước chân](#ghi-nhận-bước-chân)).
7. **Gợi ý điểm đến:** 3 địa danh chưa mở khóa phù hợp nhất, kèm khoảng cách và số bước. Chọn một địa danh để bắt đầu; nếu đang có hành trình khác, app hỏi có chuyển sang hành trình mới không.
8. **Hành trình:** tiến độ, bản đồ tuyến đường và lịch sử. Khi đã hoàn thành, bấm thẻ điểm đến để xem ảnh và nội dung địa danh vừa mở khóa.
9. Các tab và màn khác: **Bộ sưu tập** thẻ địa danh, **Nhiệm vụ** ngày/tuần kèm vật phẩm (Giày tăng tốc, Vé máy bay), **Hồ sơ** với thống kê, **Thông báo**.

Mọi màn tải dữ liệu từ server hiện skeleton có hình dạng giống nội dung thật trong lúc chờ.

## Ghi nhận bước chân

Health Connect là nguồn bước chính, vì nó đã gộp mọi ứng dụng ghi bước (Google Fit, đồng hồ, tính năng tự đếm của Health Connect…) và không tính hai lần những khoảng thời gian chồng nhau. Mã: `feature/discovery/data/HealthConnectStepSource.kt`.

| Tình huống | Nguồn bước của hành trình |
| --- | --- |
| Trong 24 giờ qua có ứng dụng **khác BCV** ghi bước vào Health Connect | Tổng bước của Health Connect |
| Không có ứng dụng nào ghi bước, máy có Google Play services **và** người dùng đã cho phép **Hoạt động thể chất** | Recording API của Google Play services (`RecordingStepCounter`), cộng với bước thử của BCV |
| Không dùng được Recording API, hoặc nguồn đọc lỗi/quá hạn; máy có cảm biến và đã cấp quyền Hoạt động thể chất | Cảm biến trực tiếp `TYPE_STEP_COUNTER`, dự phòng `TYPE_STEP_DETECTOR` |
| Chưa cấp quyền đọc và cũng chưa cho phép Hoạt động thể chất | Yêu cầu cấp quyền; giữ nguyên tiến độ đã lưu |

- **Bước từ Recording API chỉ lưu trong app, không ghi vào Health Connect**, nên không bị đọc lại và cộng trùng. BCV chỉ ghi vào Health Connect bước thử (+2.000).
- Quyền Hoạt động thể chất dùng cho cảm biến dự phòng và phát hiện nguồn Health Connect bị đứng, kể cả khi Health Connect đang hoạt động. Quyền thông báo giúp hiển thị trạng thái đếm bước nền; từ chối thông báo không tự cấp hoặc thay thế quyền cảm biến.
- Khi đã có nguồn bước dùng được (kể cả cảm biến), mở lại Khám phá không tự bật hộp xin quyền Health Connect nữa. Người dùng vẫn có thể kết nối Health Connect trong Hồ sơ.
- Recording API bắt đầu ghi khi app đăng ký lần đầu cho hành trình (lúc tạo hành trình hoặc mở màn Khám phá); bước đi trước đó không có.
- Sau khi đăng ký, Google Play services tự đếm cả khi app tắt hay điện thoại khởi động lại, và giữ dữ liệu 10 ngày trên máy. Không mở app đồng bộ quá 10 ngày thì phần bước cũ hơn bị mất.
- `JourneyStepService` là foreground service loại `health`, giữ cảm biến chạy khi chuyển ứng dụng, khóa màn hình hoặc vuốt Activity khỏi danh sách gần đây. Dịch vụ được mở lúc app đang hiển thị và đã có quyền Hoạt động thể chất, kèm thông báo "Bước Chân Việt đang đếm bước". Sensor listener thuộc dịch vụ, không bị dừng khi đóng một màn hình. Hành trình kết thúc hoặc đăng xuất thì dịch vụ dừng và bỏ mốc phần cứng để không tính khoảng đã ngừng theo dõi.
- Khi dùng Health Connect/Recording, dịch vụ vẫn lưu bước cảm biến làm nguồn dự phòng nhưng không cộng vào tổng của nguồn chính. Nếu nguồn chính đứng ít nhất **120 giây**, trong khi cảm biến tăng ít nhất **20 bước**, lần đồng bộ tiếp theo chuyển sang cảm biến: giữ tổng đã xác nhận và cộng đúng phần cảm biến kể từ mốc nguồn chính cuối cùng tiến lên. Sau thời gian ở nền, nếu nguồn chính chỉ cập nhật một phần và còn thiếu ít nhất 20 bước so với cửa sổ cảm biến, dùng tổng lớn hơn của hai nguồn trong cửa sổ đó, không cộng hai nguồn với nhau. Lỗi hoặc quá hạn 5 giây cũng chuyển nguồn nếu có số đo cảm biến. Trạng thái được lưu trong `bcv_journey_step_state`, kể cả sau khi process được tạo lại.
- Sau khi chuyển sang cảm biến, hành trình giữ nguồn đó tới khi kết thúc. Health Connect cập nhật bù muộn không được cộng lần nữa; bước thử Health Connect cũng không cộng thêm ở chế độ này. Các ngưỡng 120 giây/20 bước là quy tắc dự phòng, không phải thông số học máy.
- Dịch vụ chỉ đọc cảm biến, không đọc Health Connect trong nền và không xin quyền đọc sức khỏe nền. Bước được giữ trên máy khi offline; khi mở Khám phá hoặc Hành trình, app đối chiếu nguồn và đồng bộ với backend. Hành trình cũng đồng bộ khi được mở trực tiếp từ thông báo.
- `TYPE_STEP_COUNTER` giữ tổng trong phần cứng và không cần giữ CPU thức liên tục. Chỉ máy dùng `TYPE_STEP_DETECTOR` không đánh thức CPU mới dùng partial wake lock có thời hạn, giải phóng khi dịch vụ dừng. Mốc vẫn xử lý an toàn trường hợp khởi động lại hoặc reset cảm biến.
- Android vẫn dừng dịch vụ nếu người dùng chọn **Buộc dừng** hoặc dừng ứng dụng từ trình quản lý dịch vụ đang chạy. Khi đó cần mở lại app để tiếp tục; app không cố vượt quyết định dừng của người dùng và không tự khởi động dịch vụ sức khỏe sau khi bật máy.
- **Tự động đồng bộ:** khi màn Khám phá đang mở, app tự gửi bước mới lúc mở màn và mỗi phút một lần (chỉ gửi khi tổng bước tăng), không hiện trạng thái "Đang đồng bộ" và lỗi thì giữ nguyên tiến độ để lần sau thử lại. Nút **Đồng bộ** vẫn dùng được để cập nhật ngay.
- Nút "+2.000 bước thử" ghi bản ghi thử vào Health Connect; mỗi bản ghi bắt đầu sau bản ghi trước. Bước thử chỉ được tính khi hành trình đang dùng Health Connect/Recording, không dùng để tăng tổng của nguồn cảm biến đã chuyển sang.

Tham khảo Android: [foreground service loại health](https://developer.android.com/develop/background-work/services/fgs/service-types#health), [điều kiện khởi chạy dịch vụ](https://developer.android.com/develop/background-work/services/fgs/launch), [cảm biến đếm bước](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion#sensors-motion-stepcounter).

## Cấu trúc mã nguồn

```text
app/src/main/java/com/project/bcv/
├── core/        # Dùng chung: network, map, ui (skeleton, popup...), presentation, di
└── feature/     # Theo nghiệp vụ: account, location, discovery, journey,
                 # collection, questitem, statsnotify
```

Mỗi tính năng chia `presentation` (Activity + ViewModel), `domain` (model, interface repository) và `data` (cài đặt repository, nguồn dữ liệu).

## Kiểm thử

Kiểm thử đơn vị cho khôi phục phiên (thành công, hết hạn, offline, onboarding và tái tạo màn hình) và bộ đếm cảm biến (mốc ban đầu, không cộng trùng, khởi động lại/reset):

```powershell
.\gradlew.bat testDebugUnitTest
```

Khi có phiên lưu, màn đăng nhập hiện trạng thái **Đang khôi phục phiên đăng nhập**. Activity được tạo lại vẫn kiểm tra phiên với ViewModel mới, đồng thời không gửi yêu cầu lặp khi ViewModel cũ còn sống.

Khi backend trả `journey.awardedCard` cho hành trình hoàn thành và thẻ được xác nhận đã sở hữu, app hiện hộp thoại nhận thẻ với nút **Xem Bộ sưu tập**. Mỗi cặp hành trình/thẻ chỉ hiện một lần trên máy, dùng chung cho Khám phá và Hành trình. Backend cũ chưa trả trường này vẫn tương thích nhưng không hiện hộp thoại.

Test giao diện và tích hợp nằm trong `app/src/androidTest/`, chạy trên thiết bị hoặc emulator đang kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

`CardReceiptTest` kiểm tra hộp thoại nhận thẻ trên thiết bị: không hiện khi chưa hoàn thành/chưa có xác nhận thẻ, mở đúng Bộ sưu tập và không hiện lặp. Test dùng thẻ giả chỉ trong giao diện, không thêm bước hay thẻ vào database.

## Xử lý sự cố

**App báo "Chưa thể kết nối":**

1. Trên điện thoại, mở trình duyệt vào `http://<IP-máy-tính>:5081/health`. Kết quả đúng là `{"status":"ok"}`.
2. Nếu bị timeout, Windows Firewall đang chặn. Mở PowerShell bằng quyền Administrator và mở cổng 5081 cho mạng nội bộ (thay dải IP cho đúng):

   ```powershell
   New-NetFirewallRule -DisplayName 'BCV API for Android phone' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5081 -RemoteAddress 192.168.1.0/24 -Profile Private,Public
   ```

   Xóa khi không cần nữa: `Remove-NetFirewallRule -DisplayName 'BCV API for Android phone'`.
3. Nếu IP máy tính thay đổi, sửa `bcvApiBaseUrl` rồi build lại.
4. Giữ Wi-Fi bật. Bản debug ưu tiên Wi-Fi khi gọi API có IP nội bộ, vì dữ liệu di động không truy cập được địa chỉ `192.168.x.x`.

**Bản đồ trống hoặc không có đường đi:** kiểm tra hai khóa Goong trong `dev.properties` rồi build lại.

**Không có điểm đề xuất, bộ sưu tập hoặc nhiệm vụ:** nội dung trong database còn `DRAFT`. Xem [db/README.md](../db/README.md#trạng-thái-nội-dung).

## Đóng góp

Xem [Đóng góp](../README.md#đóng-góp) ở README gốc.

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703. Biểu tượng Material Symbols dùng trong app theo giấy phép Apache 2.0, xem [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
