# FitTrack: Ứng Dụng Theo Dõi Cân Nặng & Bài Tập Giảm Cân

**FitTrack** là ứng dụng Android Native (Kotlin + Jetpack Compose + Material Design 3) hiện đại, chuyên biệt cho việc quản lý cân nặng, thực hiện các bài tập thể lực (Workout / Cardio HIIT) và ghi nhận lộ trình chạy bộ (Running tracker).

---

## 🌟 Tính Năng Nổi Bật

### 1. 📊 Theo Dõi Cân Nặng (Weight Tracker)
- **Ghi cân nặng nhanh**: Nhập số cân theo ngày/giờ, thêm ghi chú (trước ăn sáng, sau tập...), % mỡ cơ thể.
- **Tính toán chỉ số sức khỏe tự động**:
  - **BMI**: Phân loại theo chuẩn người châu Á (Thiếu cân, Bình thường, Thừa cân, Béo phì).
  - **BMR (Mifflin-St Jeor)**: Lượng calo tối thiểu cơ thể đốt khi nghỉ ngơi.
  - **TDEE**: Tổng năng lượng tiêu hao hàng ngày theo mức độ vận động.
  - **Đề xuất thâm hụt calo**: Tính toán chính xác mức calo nạp vào hàng ngày để giảm 0.5kg/tuần an toàn.
- **Biểu đồ trực quan (Canvas Curve Chart)**: Đường cong xu hướng cân nặng mượt mà, đường tham chiếu mục tiêu, độ lệch +/- kg so với các lần cân trước.

### 2. 🏋️ Bài Tập Giảm Cân (Workout & Cardio HIIT)
- **Kho giáo án chọn lọc**:
  - *HIIT Đốt Mỡ Thần Tốc (15 phút - 180 kcal)*: Jumping Jacks, Burpees, Squats, Mountain Climbers, High Knees, Plank.
  - *Bụng Phẳng & Eo Thon (12 phút - 120 kcal)*: Plank, Bicycle Crunches, Mountain Climbers, Russian Twists.
  - *Khởi Động Giảm Cân (10 phút - 90 kcal)*: Thích hợp cho người mới bắt đầu.
  - *Sức Bền & Toàn Thân (20 phút - 240 kcal)*: Tăng cơ đốt mỡ toàn diện.
- **Interactive Workout Player (Trình tập tương tác)**:
  - Đếm ngược hiệp tập (Work timer) và thời gian nghỉ (Rest timer).
  - Rung phản hồi haptic / beep 3-2-1 trước khi chuyển bài.
  - Tự động tính toán tổng calo đốt cháy và lưu vào lịch sử khi hoàn thành.

### 3. 🏃 Theo Dõi Chạy Bộ (Running Tracker)
- **GPS Live Run (Chạy ngoài trời)**:
  - Định vị GPS thời gian thực, đo chính xác cự ly (km), tốc độ trung bình (Pace phút/km).
  - Tự động tính toán calo tiêu hao chuẩn xác theo cân nặng: `Weight × Distance × 1.036`.
- **Treadmill Mode (Chạy máy / Thủ công)**:
  - Bấm giờ chạy hoặc nhập cự ly/thời gian tập tại phòng gym.
- **Nhật ký chạy bộ**: Thống kê tổng km tuần/tháng, pace nhanh nhất.

### 4. 📱 Trực Tiếp Trên Termux
- **Tải & Cài APK tự động**: Script `./install-apk.sh` kết nối GitHub Actions build APK và mở trình cài đặt Android (`termux-open`).
- **Termux Companion**: Chạy `./fittrack-companion.py` để mở ngay giao diện Web xem dữ liệu tại `http://localhost:8080`.

---

## 🚀 Cách Cài Đặt Lên Điện Thoại

### Cách 1: Đẩy lên GitHub để tự động build file APK
1. Tạo repo GitHub và push mã nguồn:
   ```bash
   gh repo create fittrack-android --public --source=. --remote=origin --push
   ```
2. Chạy script tải và mở trình cài đặt APK:
   ```bash
   ./install-apk.sh
   ```

### Cách 2: Xem và dùng thử ngay trên trình duyệt điện thoại (Termux Companion)
```bash
python fittrack-companion.py
# Mở trình duyệt điện thoại và truy cập: http://localhost:8080
```
