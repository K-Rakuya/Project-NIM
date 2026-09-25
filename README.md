# NIM Game

> Niên luận cơ sở — Trò chơi NIM: lý thuyết trò chơi tổ hợp.

![Java](https://img.shields.io/badge/Java-21-orange)
![Build](https://img.shields.io/badge/Build-Gradle-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

## Giới thiệus

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
- [x] Bộ test JUnit 5 đối chiếu cài đặt với lý thuyết trên tập thế cờ vét cạn

**Đang phát triển**
- [ ] Công cụ tự đấu (AI vs AI), xuất số liệu
- [ ] Giao diện đồ họa JavaFX
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

## Tài liệu tham khảo

- C. L. Bouton, *Nim, A Game with a Complete Mathematical Theory*, Annals of Mathematics, 1902.
- R. Sprague, P. M. Grundy — lý thuyết Sprague–Grundy cho trò chơi tổ hợp.