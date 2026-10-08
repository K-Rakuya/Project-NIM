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
        int alpha = -2, beta = 2;

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

    /*
     * Negamax: trò chơi tổng bằng 0 nên max(a, b) = -min(-a, -b), dùng một hàm cho cả hai bên.
     * Giá trị +1 / -1 là thắng / thua cho người đến lượt, khoảng (-2, 2) đóng vai trò vô cực.
     * Cắt tỉa alpha-beta bỏ nhánh khi alpha >= beta vì đối thủ sẽ không để ván vào nhánh đó.
     */
    private int negamax(GameState s, int alpha, int beta) {
        nodesVisited++;

        if (s.isTerminal()) {
            // Đối thủ vừa bốc item cuối: người đến lượt thua (luật thường) hoặc thắng (misère).
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
