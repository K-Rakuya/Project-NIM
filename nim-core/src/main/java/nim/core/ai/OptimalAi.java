package nim.core.ai;

import java.util.Random;

import nim.core.GameState;
import nim.core.Move;
import nim.core.NimTheory;

public final class OptimalAi implements AiStrategy {

    // mistakeRate: xác suất AI đi nước lỗi
    private final double mistakeRate;
    private final Random random;
    private final RandomAi fallback;

    public OptimalAi(double mistakeRate, Random random) {
        if (mistakeRate < 0 || mistakeRate > 1)
            throw new IllegalArgumentException("mistakeRate phải nằm trong [0,1]");
        this.mistakeRate = mistakeRate;
        this.random = random;
        this.fallback = new RandomAi(random);
    }

    public OptimalAi() {
        this(0.0, new Random());
    };

    @Override
    public String name() {
        return mistakeRate == 0 ? "Tối ưu" : "Tối ưu với tỉ lệ " + (100 - Math.round(mistakeRate * 100)) + "%";
    }

    @Override 
    public Move chooseMove(GameState state) {
        if (random.nextDouble() < mistakeRate) {
            return fallback.chooseMove(state);
        }
        Move winning = NimTheory.findingWinningMove(state);
        // Đang ở thế thua: đi đại
        return winning != null ? winning : fallback.chooseMove(state);
    }
}