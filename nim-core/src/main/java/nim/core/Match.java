package nim.core;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import nim.core.ai.AiStrategy;

/**
 * Một ván đang diễn ra: gom {@link GameSession} (trạng thái + lịch sử) với
 * {@link GameConfig} (ai là người, ai là máy) và các quy tắc lượt đi.
 */
public final class Match {

    private final GameConfig config;
    private final GameSession session;
    private final AiStrategy ai; // null khi người vs người
    private int hintsUsed;

    private Match(GameConfig config, GameSession session, Random random) {
        this.config = config;
        this.session = session;
        this.ai = config.mode() == GameConfig.Mode.VS_AI ? config.level().create(random) : null;
    }

    /** Bắt đầu ván mới, thế mở màn sinh theo {@link GameConfig#opening()}. */
    public static Match start(GameConfig config, Random random) {
        GameState initial = new StateGenerator(random).random(
                config.heapCount(), 1, config.maxItems(), config.misere(),
                config.opening().firstPlayerShouldWin());
        return new Match(config, new GameSession(initial), random);
    }

    /** Tiếp tục một phiên có sẵn. */
    public static Match resume(GameConfig config, GameSession session, Random random) {
        return new Match(config, session, random);
    }

    public GameConfig config() {
        return config;
    }

    public GameSession session() {
        return session;
    }

    public GameState state() {
        return session.state();
    }

    public boolean isOver() {
        return state().isTerminal();
    }

    public int winner() {
        return state().winner();
    }

    public boolean isAiSeat(int seat) {
        return config.mode() == GameConfig.Mode.VS_AI && seat != config.humanSeat();
    }

    public boolean isAiTurn() {
        return !isOver() && isAiSeat(state().currentPlayer());
    }

    public boolean isHumanTurn() {
        return !isOver() && !isAiSeat(state().currentPlayer());
    }

    public String aiName() {
        return ai == null ? null : ai.name();
    }

    /** Người chơi đi một nước. */
    public void play(Move move) {
        if (!isHumanTurn())
            throw new IllegalStateException("Chưa đến lượt người chơi");
        if (!state().isLegal(move))
            throw new IllegalArgumentException("Nước đi không hợp lệ: " + move);
        session.play(move);
    }

    /** Máy chọn và đi một nước; trả về nước đã đi. */
    public Move playAi() {
        if (!isAiTurn())
            throw new IllegalStateException("Chưa đến lượt máy");
        Move m = ai.chooseMove(state());
        session.play(m);
        return m;
    }

    /** Gợi ý nước thắng; rỗng nếu đang ở thế thua. Mỗi lần gọi được tính là một lần dùng gợi ý. */
    public Optional<Move> hint() {
        hintsUsed++;
        return Optional.ofNullable(NimTheory.findingWinningMove(state()));
    }

    public int hintsUsed() {
        return hintsUsed;
    }

    /** Ghế của người đã đi nước thứ {@code index} trong lịch sử. */
    public int moverOf(int index) {
        return (session.initialState().currentPlayer() + index) % 2;
    }

    /** Số nước cần lùi để về lại lượt người chơi gần nhất; 0 nếu không lùi được. */
    private int undoSteps() {
        List<Move> history = session.history();
        if (history.isEmpty())
            return 0;
        if (config.mode() == GameConfig.Mode.VS_HUMAN)
            return 1;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (moverOf(i) == config.humanSeat())
                return history.size() - i;
        }
        return 0;
    }

    public boolean canUndo() {
        return undoSteps() > 0;
    }

    /** Lùi lại nước đi gần nhất của người chơi. */
    public boolean undo() {
        int steps = undoSteps();
        return steps > 0 && session.undo(steps);
    }
}
