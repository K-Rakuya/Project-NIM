package nim.core.ai;

import java.util.List;
import java.util.Random;

import nim.core.GameState;
import nim.core.Move;

/**
 * Lớp cung cấp hành vi của RandomAi
 * RandomAi
 */
public final class RandomAi implements AiStrategy {

    private final Random random;

    public RandomAi(Random random) {
        this.random = random;
    }

    public RandomAi() {
        this(new Random());
    }

    @Override
    public String name() {
        return "Random";
    }

    /**
     * Chọn random nước đi trong các nước đi khả dụng ở lượt này
     * @see nim.core.ai.AiStrategy#chooseMove(nim.core.GameState)
     */
    @Override
    public Move chooseMove(GameState state) {
        List<Move> moves = state.legalMoves();
        if (moves.isEmpty())
            throw new IllegalStateException("Không còn nước đi hợp lệ");
        return moves.get(random.nextInt(moves.size()));
    }
}