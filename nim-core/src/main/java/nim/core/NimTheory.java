package nim.core;

/**
 * Định lý Bouton cho NIM thường và misère.
 */
public final class NimTheory {

    private NimTheory() {
    }

    // Đống có 1 item
    private static int countOnes(GameState s) {
        int n = 0;
        for (int i = 0; i < s.heapCount(); i++)
            if (s.heap(i) == 1) {
                n++;
            }
        return n;
    }

    // Đống có ít nhất 2 item
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

    /**
     * Người ĐANG đến lượt có thua không, với giả thiết hai bên chơi tối ưu?
     *
     * Luật thường (định lý Bouton): thua <=> nim-sum == 0.
     * Bổ đề 1: từ thế nim-sum != 0 luôn tồn tại nước đi đưa về nim-sum == 0.
     * Bổ đề 2: từ thế nim-sum == 0, mọi nước đi đều làm nim-sum != 0.
     *
     * Luật misère: chừng nào còn ít nhất một đống >= 2 ite,, chiến lược giống
     * luật thường. Khi mọi đống chỉ còn 0 hoặc 1 item, thế cờ đảo ngược:
     * thua <=> số đống 1-item là lẻ.
     */

    public static boolean isLosingForCurrentPlayer(GameState s) {
        if (s.isMisere() && countBigHeaps(s) == 0) {
            return countOnes(s) % 2 == 1;
        }
        return s.nimSum() == 0;
    }

    // Trả về một nước đi thắng, hoặc null nếu đang ở thế thua / ván đã hết.

    public static Move findingWinningMove(GameState s) {
        if (s.isTerminal() || isLosingForCurrentPlayer(s)) {
            return null;
        }

        if (s.isMisere()) {
            int big = countBigHeaps(s);

            // Mọi đống đều <= 1 item, và số đống 1-item đang CHẴN (vì giả định đang ở thế
            // thắng).
            // Bốc 1 item để đối thủ nhận số LẺ đống 1-item.
            if (big == 0) {
                for (int i = 0; i < s.heapCount(); i++) {
                    if (s.heap(i) == 1)
                        return new Move(i, 1);
                }
            }

            // có 1 đống lớn: làm cho số đống 1-item còn lại là lẻ;
            if (big == 1) {
                int idx = firstBigHeaps(s);
                int keep = (countOnes(s) % 2 == 0) ? 1 : 0;
                return new Move(idx, s.heap(idx) - keep);
            }
        }

        // Luật thường, hoặc misère khi còn >= 2 đống lớn: đưa nim-sum về 0.
        int x = s.nimSum();
        for (int i = 0; i < s.heapCount(); i++) {

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
            int target = s.heap(i) ^ x;
            if (target < s.heap(i)) {
                return new Move(i, s.heap(i) - target);
            }
        }

        throw new IllegalStateException("Không tìm được nước thắng dù nim-sum != 0: " + s);
    }
}