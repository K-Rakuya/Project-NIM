package nim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Phiên chơi và lưu ván
 * Một ván đang diễn ra: trạng thái ban đầu + lịch sử nước đi.
 * Giữ lịch sử cho phép undo và replay.
 */

public final class GameSession {

    private final GameState initial;
    private final List<Move> history = new ArrayList<>();
    private GameState current;

    public GameSession(GameState initial) {
        this.initial = initial;
        this.current = initial;
    }

    public GameState state() {
        return current;
    }

    public GameState initialState() {
        return initial;
    }

    public List<Move> history() {
        return List.copyOf(history);
    }

    public void play(Move m) {
        current = current.apply(m);
        history.add(m);
    }

    /** Lùi lại {@code steps} nước, người vs máy thì lùi 2 */
    public boolean undo(int steps) {
        if (steps <= 0 || steps > history.size())
            return false;
        for (int i = 0; i < steps; i++) {
            history.remove(history.size() - 1);
        }
        GameState s = initial;
        for (Move m : history)
            s = s.apply(m);
        current = s;
        return true;
    }

    //Sau này đổi sang cơ chế đảo ngược nước đi để tối ưu giải thuật
}