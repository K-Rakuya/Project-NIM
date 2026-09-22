package nim.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.Scanner;

import nim.core.GameSession;
import nim.core.GameState;
import nim.core.Move;
import nim.core.NimTheory;
import nim.core.SaveFile;
import nim.core.StateGenerator;
import nim.core.ai.AiStrategy;
import nim.core.ai.OptimalAi;
import nim.core.ai.RandomAi;

public class Main {

    private static final Scanner IN = new Scanner(System.in);
    private static final Random RND = new Random();

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("   TRÒ CHƠI NIM  -  v0.1 - CLI - Prototype");
        System.out.println("===============================================");

        while (true) {
            System.out.println("""

                    1. Người vs Máy
                    2. Người vs Người
                    3. Tải ván đã lưu
                    0. Thoát""");
            System.out.print("Chọn: ");

            switch (IN.nextLine().trim()) {
                case "1" -> playVsAi();
                case "2" -> playVsHuman();
                case "3" -> loadGame();
                case "0" -> {
                    System.out.println("Tạm biệt!");
                    return;
                }
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    // ---------- khởi tạo ván ----------

    private static void playVsAi() {
        boolean misere = askMisere();
        AiStrategy ai = askAiLevel();

        // thế thắng cho người đi trước
        GameState start = new StateGenerator(RND).random(3, 1, 9, misere, Boolean.TRUE);
        run(new GameSession(start), ai);
    }

    private static void playVsHuman() {
        boolean misere = askMisere();
        GameState start = new StateGenerator(RND).random(3, 1, 9, misere, null);
        run(new GameSession(start), null);
    }

    private static void loadGame() {
        System.out.print("Đường dẫn file ván: ");
        Path path = Path.of(IN.nextLine().trim());
        try {
            GameSession session = SaveFile.load(path);
            System.out.println("Đã tải ván. Tiếp tục ở chế độ Người vs Người.");
            run(session, null);
        } catch (IOException | RuntimeException e) {
            System.out.println("Không tải được: " + e.getMessage());
        }
    }

    // ---------- vòng lặp chính ----------

    private static void run(GameSession session, AiStrategy ai) {
        while (true) {
            GameState s = session.state();
            printBoard(s, ai);

            if (s.isTerminal()) {
                int w = s.winner();
                String name = (ai != null && w == 1) ? "Máy" : "Người chơi " + (w + 1);
                System.out.println("\n>>> " + name + " thắng!\n");
                return;
            }

            if (ai != null && s.currentPlayer() == 1) {
                Move m = ai.chooseMove(s);
                System.out.println("  Máy đi: " + m);
                session.play(m);
                continue;
            }

            System.out.print("Nước đi <đống> <số item>, hoặc hint / undo / save <file> / quit: ");
            String line = IN.nextLine().trim();
            if (line.isEmpty())
                continue;
            if (line.equalsIgnoreCase("quit"))
                return;
            if (line.equalsIgnoreCase("hint")) {
                Move hint =  NimTheory.findingWinningMove(s);
                System.out.println(hint == null
                        ? "  Gợi ý: Không cứu nổi"
                        : "  Gợi ý: " + hint );
                continue;
            }

            if (line.equalsIgnoreCase("undo")) {
                int steps = (ai != null) ? 2 : 1;
                System.out.println(session.undo(steps) ? "  Đã lùi lại." : "  Không lùi được nữa.");
                continue;
            }

            if (line.toLowerCase().startsWith("save")) {
                String[] parts = line.split("\\s+", 2);
                String target = (parts.length == 2) ? parts[1].trim() : "saves/latest.nim";
                try {
                    SaveFile.save(session, Path.of(target));
                } catch (IOException e) {
                    System.out.println("  Lưu thất bại: " + e.getMessage());
                }
                continue;
            }

            Move m = parseMove(line);
            if (m == null) {
                System.out.println("  Sai cú pháp. Ví dụ: 2 3  (bốc 3 items ở đống 2)");
                continue;
            }
            if (!s.isLegal(m)) {
                System.out.println("  Nước đi không hợp lệ.");
                continue;
            }
            session.play(m);
        }
    }

    // ---------- hiển thị và nhập liệu ----------

    private static void printBoard(GameState s, AiStrategy ai) {
        // In vài dòng trống để tạo cảm giác "làm mới" màn hình console
        System.out.println("\n".repeat(2));
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│            TRẠNG THÁI BÀN CỜ            │");
        System.out.println("└─────────────────────────────────────────┘");

        for (int i = 0; i < s.heapCount(); i++) {
            System.out.printf("  Đống %d: %-15s (%d)%n", i + 1, "I".repeat(s.heap(i)), s.heap(i));
        }

        System.out.println("-------------------------------------------");
        System.out.printf("  [Debug] Nim-sum: %d | Luật: %s%n", 
                s.nimSum(), 
                s.isMisere() ? "Misère" : "Thường");

        if (!s.isTerminal()) {
            String who = (ai != null && s.currentPlayer() == 1)
                    ? "Máy (" + ai.name() + ")"
                    : "Người chơi " + (s.currentPlayer() + 1);
            System.out.println("  Lượt đi: " + who);
        }
        System.out.println("===========================================");
    }

    /* Chuyển chuỗi thành Move */
    private static Move parseMove(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2)
            return null;
        try {
            int heap = Integer.parseInt(parts[0]) - 1;
            int count = Integer.parseInt(parts[1]);
            if (heap < 0 || count < 1)
                return null;
            return new Move(heap, count);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean askMisere() {
        System.out.print("Luật misère? [y/N]: ");
        return IN.nextLine().trim().equalsIgnoreCase("y");
    }

    private static AiStrategy askAiLevel() {
        System.out.println("Mức máy:  1 = Dễ (ngẫu nhiên)   2 = Vừa (sai 30%)   3 = Khó (tối ưu)");
        System.out.print("Chọn: ");
        return switch (IN.nextLine().trim()) {
            case "1" -> new RandomAi(RND);
            case "2" -> new OptimalAi(0.30, RND);
            default  -> new OptimalAi(0.0, RND);
        };
    }
}