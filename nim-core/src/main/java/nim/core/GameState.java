package nim.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GameState {
    private final int[] heaps;
    private final int currentPlayer; // 0 hoac 1, player 0 di truoc
    private final boolean misere; // true = ai boc que cuoi cung thi thua

    public GameState(int[] heaps, int currentPlayer, boolean misere) {
        if (heaps == null || heaps.length == 0) {
            throw new IllegalArgumentException("Phải có ít nhất 1 đống");
        }
        for (int h : heaps) {
            if (h < 0)
                throw new IllegalArgumentException("Số vật phẩm ko đc âm");
        }
        if (currentPlayer != 0 && currentPlayer != 1) {
            throw new IllegalArgumentException("currentPlayer chỉ chấp nhận 0 và 1");
        }
        this.heaps = heaps;
        this.currentPlayer = currentPlayer;
        this.misere = misere;
    }

    // Init game
    public static GameState of(boolean misere, int... heaps) {
        return new GameState(heaps, 0, misere);
    }

    public int heapCount() {
        return heaps.length;
    }

    public int heap(int i) {
        return heaps[i];
    }

    public int[] heaps() {
        return heaps.clone();
    }

    public int currentPlayer() {
        return currentPlayer;
    }

    public boolean isMisere() {
        return misere;
    }

    public int totalItems() {
        int sum = 0;
        for (int i : heaps)
            sum += i;
        return sum;
    }

    /**
     * Nim-sum = XOR của tất cả các đống.
     * Định lý Bouton.
     */

    public int nimSum() {
        int x = 0;
        for (int i : heaps)
            x ^= i;
        return x;
    }

    public boolean isTerminal() {
        return totalItems() == 0;
    }

    // Ket thuc van
    public int winner() {
        if (!isTerminal()) {
            throw new IllegalStateException("Van chua ket thuc");
        }
        return misere ? currentPlayer : 1 - currentPlayer;
    }

    // lay tat ca nuoc di hop le
    public List<Move> legalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < heaps.length; i++) {
            for (int j = 1; j <= heaps[i]; j++) {
                moves.add(new Move(i, j));
            }
        }
        return Collections.unmodifiableList(moves);
    }

    public boolean isLegal(Move m) {
        return m != null
                && m.heapIndex() < heaps.length
                && m.count() <= heaps[m.heapIndex()];
    }

    // Ap dung nuoc di, tra ve trang thai moi
    public GameState apply(Move m) {
        if (!isLegal(m)) {
            throw new IllegalArgumentException("Nước đi không hợp lệ");
        }
        int[] next = heaps.clone();
        next[m.heapIndex()] -= m.count();
        return new GameState(next, 1 - currentPlayer, misere);
    }

    // key cho cache
    public String canonicalKey() {
        int[] sorted = heaps.clone();
        Arrays.sort(sorted);
        return Arrays.toString(sorted) + (misere ? "|M" : "|N");
    }

    @Override
    public String toString() {
        return Arrays.toString(heaps)
                + " lượt P" + (currentPlayer + 1)
                + (misere ? " [misère]" : "")
                + " nim-sum=" + nimSum();
    }
}