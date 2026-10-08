package nim.core;

public final class NimTheory {
    private NimTheory() {
    }

    private static int countOnes(GameState s) {
        int n = 0;
        for (int i = 0; i < s.heapCount(); i++)
            if (s.heap(i) == 1) {
                n++;
            }
        return n;
    }

    private static int countBigHeaps(GameState s) {
        int n = 0;
        for (int i = 0; i < s.heapCount(); i++)
            if (s.heap(i) >= 2) {
                n++;
            }
        return n;
    }

    private static int firstBigHeaps(GameState s) {
        for (int i = 0; i < s.heapCount(); i++)
            if (s.heap(i) >= 2) {
                return i;
            }
        return -1;
    }

    /*
     * Định lý Bouton (luật thường): người đến lượt thua <=> nim-sum == 0.
     * - Từ nim-sum != 0 luôn tồn tại nước đưa về nim-sum == 0.
     * - Từ nim-sum == 0 mọi nước đi đều làm nim-sum != 0.
     * Luật misère: chiến lược giống luật thường cho tới khi mọi đống <= 1;
     * khi đó thế cờ đảo ngược, người đến lượt thua <=> số đống 1-item là lẻ.
     */
    public static boolean isLosingForCurrentPlayer(GameState s) {
        if (s.isMisere() && countBigHeaps(s) == 0) {
            return countOnes(s) % 2 == 1;
        }
        return s.nimSum() == 0;
    }

    public static Move findingWinningMove(GameState s) {
        if (s.isTerminal() || isLosingForCurrentPlayer(s)) {
            return null;
        }

        // Misère còn đống >= 2: chỉ cần để lại số đống 1-item lẻ cho đối thủ.
        if (s.isMisere()) {
            int big = countBigHeaps(s);

            if (big == 0) {
                for (int i = 0; i < s.heapCount(); i++) {
                    if (s.heap(i) == 1)
                        return new Move(i, 1);
                }
            }

            if (big == 1) {
                int idx = firstBigHeaps(s);
                int keep = (countOnes(s) % 2 == 0) ? 1 : 0;
                return new Move(idx, s.heap(idx) - keep);
            }
        }

        /*
         * x = h0 XOR h1 XOR h2 ..... XOR hi
         * Gọi S la XOR của tất cả trừ đống thứ i
         * x = S XOR hi
         * x XOR hi = S
         * 
         * ta cần x1 mới = 0
         * x1 = S XOR target
         * S XOR target = 0
         * x XOR hi XOR target = 0
         * target = x XOR hi
         * 
         */
        // hợp lệ khi h' < h (chỉ được bớt item).
        int x = s.nimSum();
        for (int i = 0; i < s.heapCount(); i++) {
            int target = s.heap(i) ^ x;
            if (target < s.heap(i)) {
                return new Move(i, s.heap(i) - target);
            }
        }

        throw new IllegalStateException("Không tìm được nước thắng dù nim-sum != 0: " + s);
    }
}
