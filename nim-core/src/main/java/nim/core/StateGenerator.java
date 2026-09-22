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

    /**
     * @param firstPlayerShouldWin TRUE -> thế mở màn là thế thắng
     *                             FALSE -> thế mở màn là thế thua
     *                             null -> không ràng buộc
     */

    // Sinh ngẫu nhiên bằng phương pháp thử và sai: tạo bàn cờ ngẫu nhiên nếu đúng ý
    // thì lấy ko thì thôi
    public GameState random(int heapCount, int minItems, int maxItems, boolean misere, Boolean firstPlayerShouldWin) {
        if (heapCount < 1)
            throw new IllegalArgumentException("ít nhất 1 đống");
        if (minItems < 1)
            throw new IllegalArgumentException("mỗi đống ít nhất 1 item");
        if (maxItems < minItems)
            throw new IllegalArgumentException("maxItems < minItems");

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

    //Sau này đổi thành giải thuật tạo n-1 đống rồi dựa trên nim-sum tạo ra đống cuối để tối ưu
}