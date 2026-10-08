package nim.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.Scanner;

import nim.core.GameConfig;
import nim.core.GameConfig.Mode;
import nim.core.GameConfig.Opening;
import nim.core.GameSession;
import nim.core.Match;
import nim.core.GameState;
import nim.core.Move;
import nim.core.SaveFile;
import nim.core.StateGenerator;
import nim.core.ai.AiStrategy;
import nim.core.ai.MinimaxAi;
import nim.core.ai.AiLevel;

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

    private static void playVsAi() {
        boolean misere = askMisere();
        AiStrategy ai = askAiStrategy();

        GameConfig cfg = new GameConfig(Mode.VS_AI, misere, 3, 9, Opening.FIRST_WINS, AiLevel.HARD, 0);
        GameState start = new StateGenerator(RND).random(3, 1, 9, misere, Boolean.TRUE);
        run(Match.resume(cfg, new GameSession(start), ai));
    }

    private static void playVsHuman() {
        GameConfig cfg = GameConfig.defaults().withMode(Mode.VS_HUMAN).withMisere(askMisere());
        run(Match.start(cfg.withHeapCount(3).withMaxItems(9), RND));
    }

    private static void loadGame() {
        System.out.print("Đường dẫn file ván: (Enter để dùng đường dẫn mặc định)");
        String input = IN.nextLine().trim();
        if (input.isEmpty()) {
            input = "saves/latest.nim";
        }

        try {
            Match loaded = SaveFile.loadMatch(Path.of(input), RND);

            if (loaded.config().mode() == Mode.VS_HUMAN) {
                System.out.print("Bạn muốn người chơi 2 (đối thủ) là Máy không? [y/N]: ");
                if (IN.nextLine().trim().equalsIgnoreCase("y")) {
                    AiStrategy ai = askAiStrategy();
                    loaded = Match.resume(loaded.config().withMode(Mode.VS_AI).withHumanSeat(0),
                            loaded.session(), ai);
                    System.out.println("Đã thiết lập Máy (" + ai.name() + "). Tiếp tục ván đấu...");
                } else {
                    System.out.println("Tiếp tục ván đấu ở chế độ Người vs Người...");
                }
            }
            run(loaded);
        } catch (IOException | RuntimeException e) {
            System.out.println("Không tải được: " + e.getMessage());
        }
    }

    private static String seatName(Match match, int seat) {
        return match.isAiSeat(seat) ? "Máy" : "Người chơi " + (seat + 1);
    }

    private static void run(Match match) {
        while (true) {
            GameState s = match.state();
            printBoard(match);

            if (match.isOver()) {
                System.out.println("\n>>> " + seatName(match, match.winner()) + " thắng!\n");
                return;
            }

            if (match.isAiTurn()) {
                Move m = match.playAi();
                System.out.println("  Máy đi: " + m);
                if (match.ai() instanceof MinimaxAi mm) {
                    System.out.println("  [debug] Minimax duyệt " + mm.lastNodesVisited() + " nút để chọn nước này");
                }
                continue;
            }

            System.out.print("Nước đi <đống> <số item>, hoặc hint / undo / save <file> / quit: ");
            String line = IN.nextLine().trim();
            if (line.isEmpty())
                continue;
            if (line.equalsIgnoreCase("quit"))
                return;
            if (line.equalsIgnoreCase("hint")) {
                System.out.println(match.hint()
                        .map(h -> "  Gợi ý: " + h)
                        .orElse("  Gợi ý: Không cứu nổi"));
                continue;
            }

            if (line.equalsIgnoreCase("undo")) {
                System.out.println(match.undo() ? "  Đã lùi lại." : "  Không lùi được nữa.");
                continue;
            }

            if (line.toLowerCase().startsWith("save")) {
                String[] parts = line.split("\\s+", 2);
                String target = (parts.length == 2) ? parts[1].trim() : "saves/latest.nim";
                try {
                    SaveFile.save(match, Path.of(target));
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
            match.play(m);
        }
    }

    private static void printBoard(Match match) {
        GameState s = match.state();

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
            String who = match.isAiSeat(s.currentPlayer())
                    ? "Máy (" + match.aiName() + ")"
                    : seatName(match, s.currentPlayer());
            System.out.println("  Lượt đi: " + who);
        }
        System.out.println("===========================================");
    }

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

    private static AiStrategy askAiStrategy() {
        System.out.println("Mức máy:  1 = Dễ (ngẫu nhiên)   2 = Vừa (sai 30%)   3 = Khó (tối ưu)   4 = Minimax (duyệt cây)");
        System.out.print("Chọn: ");
        return switch (IN.nextLine().trim()) {
            case "1" -> AiLevel.EASY.create(RND);
            case "2" -> AiLevel.MEDIUM.create(RND);
            case "4" -> new MinimaxAi();
            default -> AiLevel.HARD.create(RND);
        };
    }
}
