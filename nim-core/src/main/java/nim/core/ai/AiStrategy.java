package nim.core.ai;

import nim.core.GameState;
import nim.core.Move;

public interface AiStrategy {
    String name();
    Move chooseMove(GameState state);
}