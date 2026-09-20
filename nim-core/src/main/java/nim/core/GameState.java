package nim.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameState {
    private final int[] heaps;
    private final int currentPlayer;
    private final boolean misere;

    public GameState(int[] heaps, int currentPlayer, boolean misere) {
        if (heaps == null || heaps.length == 0) {
            throw new IllegalArgumentException("Ván chơi phải có ít nhất 1 đống");
        }
        for (int h : heaps) {
            if (h < 0)
                throw new IllegalArgumentException("Số vật phẩm không được âm");
        }
        if (currentPlayer != 0 && currentPlayer != 1) {
            throw new IllegalArgumentException("currentPlayer không hợp lệ");
        }

        this.heaps = heaps;
        this.currentPlayer = currentPlayer;
        this.misere = misere;
    }

    public static GameState createInitialState(boolean misere, int... heaps) {
        return new GameState(heaps, 0, misere);
    }

    public int getHeapsCount() {
        return heaps.length;
    }

    public int getHeap(int i) {
        return heaps[i];
    }

    public int[] getHeap() {
        return heaps.clone();
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isMisere() {
        return misere;
    }

    public int getTotalItems() {
        int sum = 0;
        for (int i : heaps) {
            sum += i;
        }
        return sum;
    }

    public int getNimSum() {
        int x = 0;
        for (int i : heaps) {
            x ^= i;
        }
        return x;
    }

    public boolean isTerminal() {
        return getTotalItems() == 0;
    }

    public int getWinner() {
        if (!isTerminal()) {
            throw new IllegalStateException("Ván chưa kết thúc");
        }
        if (misere) {
            return currentPlayer;
        } else {
            return 1 - currentPlayer;
        }
    }

    //lay danh sach tat ca cac nuoc di hop le
    public List<Move> getLegalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < heaps.length; i++) {
            for (int j = 1; j <= heaps[i]; j++) {
                moves.add(new Move(i, j));
            }
        }
        return Collections.unmodifiableList(moves);
    }

    // Kiem tra xem nuoc di co hop le ko
    public boolean isLegal(Move m) {
        return m != null && m.getHeapIndex() < heaps.length && m.getCount() < heaps[m.getHeapIndex()];
    }

    //ap dung nuoc di, tra ve trang thai moi
    public GameState apply(Move m) {
        if(!isLegal(m)) {
            throw new IllegalArgumentException("Nước đi không hợp lệ");
        }

        int[] nexHeaps = heaps.clone();
        nexHeaps[m.getHeapIndex()] -= m.getCount();

        int nextPlayer = 1 - currentPlayer;

        return new GameState(nexHeaps, nextPlayer, misere);
    }

    public String getCanonicalKey() {
        int[] sorted = heaps.clone();
        Arrays.sort(sorted);
        return Arrays.toString(sorted) + (misere ? "|M" : "|N");
    }

    @Override 
    public String toString() {
        return Arrays.toString(heaps)
                + " lượt P" + (currentPlayer + 1)
                + (misere ? " [misère]" : "")
                + " nim-sum = " + getNimSum();
    }
}