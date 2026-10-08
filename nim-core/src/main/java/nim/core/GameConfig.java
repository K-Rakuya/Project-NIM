package nim.core;

import nim.core.ai.AiLevel;

public record GameConfig(Mode mode, boolean misere, int heapCount, int maxItems,
        Opening opening, AiLevel level, int humanSeat) {
    public enum Mode {
        VS_AI("Người vs Máy"), VS_HUMAN("Người vs Người");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Opening {
        RANDOM("Ngẫu nhiên", null),
        FIRST_WINS("Đi trước có lợi", Boolean.TRUE),
        FIRST_LOSES("Đi trước bất lợi", Boolean.FALSE);

        private final String label;
        private final Boolean firstPlayerShouldWin;

        Opening(String label, Boolean firstPlayerShouldWin) {
            this.label = label;
            this.firstPlayerShouldWin = firstPlayerShouldWin;
        }

        public String label() {
            return label;
        }

        public Boolean firstPlayerShouldWin() {
            return firstPlayerShouldWin;
        }
    }

    public static final int MAX_HEAPS = 8;
    public static final int MAX_ITEMS = 20;

    public GameConfig {
        if (mode == null || opening == null || level == null)
            throw new IllegalArgumentException("mode/opening/level không được null");
        if (heapCount < 1 || heapCount > MAX_HEAPS)
            throw new IllegalArgumentException("heapCount phải trong [1," + MAX_HEAPS + "]");
        if (maxItems < 1 || maxItems > MAX_ITEMS)
            throw new IllegalArgumentException("maxItems phải trong [1," + MAX_ITEMS + "]");
        if (humanSeat != 0 && humanSeat != 1)
            throw new IllegalArgumentException("humanSeat chỉ nhận 0 hoặc 1");
    }

    public static GameConfig defaults() {
        return new GameConfig(Mode.VS_AI, false, 3, 9, Opening.RANDOM, AiLevel.MEDIUM, 0);
    }

    public GameConfig withMode(Mode m) {
        return new GameConfig(m, misere, heapCount, maxItems, opening, level, humanSeat);
    }

    public GameConfig withMisere(boolean v) {
        return new GameConfig(mode, v, heapCount, maxItems, opening, level, humanSeat);
    }

    public GameConfig withHeapCount(int v) {
        return new GameConfig(mode, misere, v, maxItems, opening, level, humanSeat);
    }

    public GameConfig withMaxItems(int v) {
        return new GameConfig(mode, misere, heapCount, v, opening, level, humanSeat);
    }

    public GameConfig withOpening(Opening v) {
        return new GameConfig(mode, misere, heapCount, maxItems, v, level, humanSeat);
    }

    public GameConfig withLevel(AiLevel v) {
        return new GameConfig(mode, misere, heapCount, maxItems, opening, v, humanSeat);
    }

    public GameConfig withHumanSeat(int v) {
        return new GameConfig(mode, misere, heapCount, maxItems, opening, level, v);
    }
}
