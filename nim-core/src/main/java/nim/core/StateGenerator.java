package nim.core;

import java.util.Random;

public final class StateGenerator {
    private static final int MAX_ATTEMPTS = 2000;

    private final Random random;

    public StateGenerator(Random random) {
        this.random = random;
    }

    public StateGenerator() {
        this(new Random());
    }

    public GameState random(int heapCount, int minItems, int maxItems, boolean misere, Boolean firstPlayerShouldWin) {
        if (heapCount < 1)
            throw new IllegalArgumentException("ít nhất 1 đống");
        if (minItems < 1)
            throw new IllegalArgumentException("mỗi đống ít nhất 1 item");
        if (maxItems < minItems)
            throw new IllegalArgumentException("maxItems < minItems");

        // Lấy mẫu loại bỏ: các lần thử độc lập nên số lần thử kỳ vọng là 1/p (p = xác suất thế thỏa ràng buộc);
        // MAX_ATTEMPTS chặn vòng lặp vô hạn khi p = 0.
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int[] heaps = new int[heapCount];
            for (int i = 0; i < heapCount; i++) {
                heaps[i] = minItems + random.nextInt(maxItems - minItems + 1);
            }
            GameState s = new GameState(heaps, 0, misere);

            if (firstPlayerShouldWin == null)
                return s;
            if (firstPlayerShouldWin != NimTheory.isLosingForCurrentPlayer(s))
                return s;
        }
        throw new IllegalStateException("Không sinh được thế cờ thỏa ràng buộc sau " + MAX_ATTEMPTS + " lần thử");
    }
}
