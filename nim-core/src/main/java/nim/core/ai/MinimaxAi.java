package nim.core.ai;

import java.util.List;

import nim.core.GameState;
import nim.core.Move;

public final class MinimaxAi implements AiStrategy {

    private final boolean useAlphaBeta;
    private long nodesVisited;

    public MinimaxAi(boolean useAlphaBeta) {
        this.useAlphaBeta = useAlphaBeta;
    }

    public MinimaxAi() {
        this(true);
    }

    @Override
    public String name() {
        return useAlphaBeta ? "Minimax | alpha-beta" : "Minimax";
    }

    /**
     * Số nút đã duyệt ở lần gần nhất
     * 
     * @return
     */
    public long lastNodesVisited() {
        return nodesVisited;
    }

    public int evaluate(GameState s) {
        nodesVisited = 0;
        return negamax(s, -2, 2);
    }

    @Override
    public Move chooseMove(GameState state) {
        List<Move> moves = state.legalMoves();
        if (moves.isEmpty()) {
            throw new IllegalStateException("không còn nước đi");
        }

        nodesVisited = 0;
        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = -2, beta = 2; //đại diện cho các giá trị vô cực

        for (Move m : moves) {
            int value = -negamax(state.apply(m), -beta, -alpha);
            if (value > bestValue) {
                bestValue = value;
                bestMove = m;
            }

            if (useAlphaBeta && bestValue > alpha) {
                alpha = bestValue;
            }
        }

        return bestMove;
    }

    /**
     * Dùng negamax thay cho minimax để tối ưu tính tái sử dụng code
     * @param s
     * @param alpha
     * @param beta
     * @return
     */
    private int negamax(GameState s, int alpha, int beta) {
        nodesVisited++;

        if (s.isTerminal()) {
            return s.isMisere() ? 1 : -1;
        }

        int best = Integer.MIN_VALUE;
        for (Move m : s.legalMoves()) {
            int value = -negamax(s.apply(m), -beta, -alpha);
            if(value > best) best = value;

            if (useAlphaBeta) {
                if (best > alpha) {
                    alpha = best;
                }
                if (alpha >= beta) break;
            }
        }
        return best;
    }
}