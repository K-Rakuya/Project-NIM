package nim.core.ai;

import java.util.Random;

import nim.core.GameState;
import nim.core.Move;
import nim.core.NimTheory;

/**
 * Hành vi của AI sử dụng lý thuyết trò chơi NIM
 * OptimalAi
 */
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

    /**
     * Chọn nước đi tiếp theo dựa trên tỉ lệ cho trước
     * Fallback về hành vi của RandomAI nếu rơi vào tỉ lệ lỗi
     * 
     * @see nim.core.ai.AiStrategy#chooseMove(nim.core.GameState)
     */
    @Override
    public Move chooseMove(GameState state) {

        Move winning = NimTheory.findingWinningMove(state);

        // Rơi vào tỉ lệ lỗi
        if (random.nextDouble() < mistakeRate) {
            // return fallback.chooseMove(state);

            // Code mới để đảm bảo tỉ lệ không lệch do fallback về chọn random
            if (winning != null && state.legalMoves().size() > 1) {
                Move randomMove;
                do {
                    randomMove = fallback.chooseMove(state);
                } while (randomMove.equals(winning));

                return randomMove;
            }
        }

        // Chơi nghiêm túc, đang ở thế thua, còn 1 nước đi
        return winning != null ? winning : fallback.chooseMove(state);
    }
}