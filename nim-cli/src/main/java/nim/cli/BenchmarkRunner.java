package nim.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import nim.core.GameSession;
import nim.core.GameState;
import nim.core.StateGenerator;
import nim.core.ai.AiStrategy;
import nim.core.ai.MinimaxAi;
import nim.core.ai.OptimalAi;
import nim.core.ai.RandomAi;

public class BenchmarkRunner {
    static final Path OUT_DIR = Path.of("benchmarks");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUT_DIR);

        System.out.println("=== [1/3] Kiểm chứng áp đảo ===");
        runDominance();

        System.out.println("=== [2/3] Hiệu chỉnh độ khó ===");
        runDifficulty();

        System.out.println("=== [3/3] Hiệu năng: Minimax vs công thức đóng ===");
        runPerformance();

        System.out.println("\nXong. Xem thư mục " + OUT_DIR.toAbsolutePath());
    }

    static void runDominance() throws IOException {
        final int games = 200;
        Random random = new Random(2302);
        StateGenerator generator = new StateGenerator(random);

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve("dominance.csv")))) {
            csv.println("luat,manh,yeu,so_van,manh_thang,ti_le_thang_pt");

            for (boolean misere : new boolean[] { false, true }) {
                List<AiStrategy> strongAIs = List.of(new OptimalAi(), new MinimaxAi());
                List<AiStrategy> weakAIs = List.of(new RandomAi(random), new OptimalAi(0.3, random), new MinimaxAi());

                for (AiStrategy strongAI : strongAIs) {
                    for (AiStrategy weakAI : weakAIs) {
                        int strongWins = 0;
                        for (int i = 0; i < games; i++) {
                            GameState start = generator.random(3, 1, 5, misere, Boolean.TRUE);
                            GameSession session = new GameSession(start);
                            while (!session.state().isTerminal()) {
                                AiStrategy toMove = (session.state().currentPlayer() == 0 ? strongAI : weakAI);
                                session.play(toMove.chooseMove(session.state()));
                            }
                            if (session.state().winner() == 0) {
                                strongWins++;
                            }
                            double rate = 100.0 * strongWins / games;
                            csv.printf("%s,%s,%s,%d,%d,%.1f%n", misere ? "misere" : "normal", strongAI.name(),
                                    weakAI.name(), games, strongWins, rate);
                        }
                    }
                }
            }
        }
    }

    static void runDifficulty() throws IOException {
        final int games = 200;
        double[] rates = { 0.0, 0.1, 0.2, 0.3, 0.5, 0.7, 1.0 };
        Random random = new Random(1507);
        StateGenerator generator = new StateGenerator(random);
        MinimaxAi perfect = new MinimaxAi();

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve("difficulty.csv")))) {
            csv.println("luat,ti_le_sai,so_van,so_van_thang,ti_le_thang_pt");

            for (boolean misere : new boolean[] { false, true }) {
                for (double rate : rates) {
                    OptimalAi imperfect = new OptimalAi(rate, random);
                    int wins = 0;
                    for (int i = 0; i < games; i++) {
                        GameState start = generator.random(3, 1, 5, misere, Boolean.TRUE);
                        GameSession session = new GameSession(start);
                        while (!session.state().isTerminal()) {
                            AiStrategy toMove = (session.state().currentPlayer() == 0) ? imperfect : perfect;
                            session.play(toMove.chooseMove(session.state()));
                        }
                        if (session.state().winner() == 0)
                            wins++;
                    }
                    double pct = 100.0 * wins / games;
                    csv.printf("%s,%.1f,%d,%d,%.1f%n", misere ? "misere" : "normal", rate, games, wins, pct);
                }
            }
        }
    }

    static void runPerformance() throws IOException {
        int[][] boards = { { 1, 2, 3 }, { 2, 3, 4 }, { 3, 4, 5 }, { 4, 5, 6 }, { 5, 6, 7 } };
        final int fullCutoffTotal = 15;

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve("hieu-nang.csv")))) {
            csv.println("ban_co,tong_que,day_du_nut,day_du_ms,alphabeta_nut,alphabeta_ms,optimal_us");

            for (int[] b : boards) {
                int total = b[0] + b[1] + b[2];
                GameState s = GameState.of(false, b[0], b[1], b[2]);

                Long fullNodes = null, fullMs = null;
                if (total <= fullCutoffTotal) {
                    MinimaxAi full = new MinimaxAi(false);
                    long t0 = System.nanoTime();
                    full.evaluate(s);
                    fullMs = (System.nanoTime() - t0) / 1_000_000;
                    fullNodes = full.lastNodesVisited();
                }

                MinimaxAi ab = new MinimaxAi(true);
                long t1 = System.nanoTime();
                ab.evaluate(s);
                long abMs = (System.nanoTime() - t1) / 1_000_000;
                long abNodes = ab.lastNodesVisited();

                OptimalAi opt = new OptimalAi();
                long t2 = System.nanoTime();
                opt.chooseMove(s);
                long optUs = (System.nanoTime() - t2) / 1_000;

                csv.printf("%d-%d-%d,%d,%s,%s,%d,%d,%d%n",
                        b[0], b[1], b[2], total,
                        fullNodes == null ? "" : fullNodes, fullMs == null ? "" : fullMs,
                        abNodes, abMs, optUs);
            }
        }
    }
}
