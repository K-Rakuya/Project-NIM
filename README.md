# NIM Game

> Niên luận cơ sở — Trò chơi NIM: lý thuyết trò chơi tổ hợp.

![Java](https://img.shields.io/badge/Java-21-orange)
![Build](https://img.shields.io/badge/Build-Gradle-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

## Giới thiệu

Dự án cài đặt trò chơi **NIM** — một trò chơi tổ hợp công bằng hai người chơi.

Sản phẩm cho học phần **Niên luận cơ sở**:
- Giao diện đồ họa cho 2 người chơi hoặc người-máy
- Sinh trạng thái game ngẫu nhiên, lưu/tải trạng thái hiện hành
- Chức năng máy tự chơi với người
- Ứng dụng dạng desktop

## Tính năng

**Đã hoàn thành**
- [x] Luật chơi NIM đầy đủ — luật thường và luật misère
- [x] Định lý Bouton — máy chơi tối ưu
- [x] Minimax + cắt tỉa alpha-beta — máy chơi bằng duyệt cây, dùng để đối chiếu với công thức đóng
- [x] Sinh trạng thái ngẫu nhiên có kiểm soát cho người đi trước
- [x] Lưu / tải ván đang chơi
- [x] Undo nước đi, gợi ý nước đi kèm debug nim-sum hiện tại
- [x] Client console để chơi và kiểm thử thủ công
- [x] Giao diện đồ họa JavaFX: các đống item đặt trên mặt bàn, theme sáng/tối
- [x] Hệ thống chuyển động thống nhất (`Motion`): hover/nhấn nút, chọn vật phẩm, đổi lượt, bật/tắt panel, hộp thoại, kết quả ván
- [x] Quy trình thao tác chuẩn: xác nhận trước khi bỏ ván đang dở, hộp thoại lỗi/hướng dẫn (F1), chú thích phím tắt, trạng thái tự lưu
- [x] Ván mới tùy chỉnh (chế độ, luật, mức máy, thứ tự đi, thế mở màn, số đống/vật phẩm)
- [x] Gợi ý, hoàn tác, lịch sử nước đi, bảng điểm theo phiên
- [x] Chế độ phân tích nim-sum (nhị phân + XOR) để học định lý Bouton
- [x] Lưu/mở ván bằng hộp thoại, tự lưu và khôi phục ván dở
- [x] Bộ test JUnit 5 đối chiếu cài đặt với lý thuyết trên tập thế cờ vét cạn
- [x] Công cụ tự đấu (AI vs AI), xuất số liệu

**Đang phát triển**
- [ ] Đóng gói cài đặt bằng `jpackage`
- [ ] Chế độ 1vs1 online qua server riêng
- [ ] Hình ảnh low-poly dựng từ Blender (Optional)

## Cơ sở lý thuyết

- **Trò chơi tổ hợp công bằng**
- **Định lý Bouton**
- **Luật misère**
- **Minimax + alpha-beta**

## Yêu cầu hệ thống

| Công cụ | Phiên bản |
|---|---|
| JDK | 21 (LTS) trở lên |
| Gradle |

## Cài đặt và chạy

```bash
git clone https://github.com/K-Rakuya/Project-NIM
cd Project-NIM

# Build toàn bộ dự án
./gradlew build

# Chạy ứng dụng desktop (JavaFX)
./gradlew :nim-desktop:run

# Chạy client console
./gradlew :nim-cli:run --console=plain -q
```

> Trên Windows: gõ `chcp 65001` trong terminal trước khi chạy để hiển thị đúng tiếng Việt.

## Chạy test

```bash
./gradlew :nim-core:test
```

Bộ test bao gồm:
- Đối chiếu `NimTheory` với kết quả duyệt vét cạn trên tập thế cờ nhỏ
- Đối chiếu `MinimaxAi` với `NimTheory`, và với chính nó khi bật/tắt cắt tỉa alpha-beta

## Cách chơi (client console)

1. Chọn chế độ: người vs máy, người vs người, hoặc tải ván đã lưu
2. Nhập nước đi theo cú pháp `<số đống> <số items>` — ví dụ `2 3` nghĩa là bốc 3 items ở đống 2
3. Lệnh phụ: `hint` (gợi ý nước đi), `undo` (lùi nước), `save <file>` (lưu ván), `quit` (thoát)

## Cách chơi (ứng dụng desktop)

- Di chuột vào một vật phẩm để xem trước, bấm để chọn, bấm lần nữa (hoặc nút **Bốc** / phím Enter) để xác nhận.
  Chọn một vật phẩm nghĩa là bốc nó **và mọi vật phẩm phía trên nó** trong cùng đống.
- Phím tắt: `←` `→` chọn đống, `↑` `↓` đổi số lượng, `Enter` xác nhận, `Esc` hủy chọn.
- `Ctrl+N` ván mới, `Ctrl+O` mở, `Ctrl+S` lưu, `Ctrl+Z` hoàn tác, `H` gợi ý, `F1` hướng dẫn.
- Ván đang chơi tự lưu tại `~/.nim/autosave.nim` và được khôi phục lần mở sau.

## Cấu trúc module

| Module | Nội dung |
|---|---|
| `nim-core` | Luật chơi, lý thuyết Bouton, AI, `Match`/`GameConfig` (không phụ thuộc giao diện), lưu/tải |
| `nim-cli` | Client console và công cụ benchmark |
| `nim-desktop` | Ứng dụng JavaFX |

## Tài liệu tham khảo

- C. L. Bouton, *Nim, A Game with a Complete Mathematical Theory*, Annals of Mathematics, 1902.
- R. Sprague, P. M. Grundy — lý thuyết Sprague–Grundy cho trò chơi tổ hợp.