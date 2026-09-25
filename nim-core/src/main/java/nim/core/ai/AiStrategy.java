package nim.core.ai;

import nim.core.GameState;
import nim.core.Move;

/**
 * Interface về các chiến lược của AI
 * AiStrategy
 */
public interface AiStrategy {
    String name();
    Move chooseMove(GameState state);
}