package nim.core;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import nim.core.ai.AiStrategy;

public final class Match {
    private final GameConfig config;
    private final GameSession session;
    private final AiStrategy ai;
    private int hintsUsed;

    private Match(GameConfig config, GameSession session, AiStrategy ai) {
        this.config = config;
        this.session = session;
        this.ai = config.mode() == GameConfig.Mode.VS_AI ? ai : null;
    }

    public static Match start(GameConfig config, Random random) {
        GameState initial = new StateGenerator(random).random(
                config.heapCount(), 1, config.maxItems(), config.misere(),
                config.opening().firstPlayerShouldWin());
        return new Match(config, new GameSession(initial), config.level().create(random));
    }

    public static Match resume(GameConfig config, GameSession session, Random random) {
        return new Match(config, session, config.level().create(random));
    }

    public static Match resume(GameConfig config, GameSession session, AiStrategy ai) {
        return new Match(config, session, ai);
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

    public AiStrategy ai() {
        return ai;
    }

    public String aiName() {
        return ai == null ? null : ai.name();
    }

    public void play(Move move) {
        if (!isHumanTurn())
            throw new IllegalStateException("Chưa đến lượt người chơi");
        if (!state().isLegal(move))
            throw new IllegalArgumentException("Nước đi không hợp lệ: " + move);
        session.play(move);
    }

    public Move playAi() {
        if (!isAiTurn())
            throw new IllegalStateException("Chưa đến lượt máy");
        Move m = ai.chooseMove(state());
        session.play(m);
        return m;
    }

    public Optional<Move> hint() {
        hintsUsed++;
        return Optional.ofNullable(NimTheory.findingWinningMove(state()));
    }

    public int hintsUsed() {
        return hintsUsed;
    }

    public int moverOf(int index) {
        return (session.initialState().currentPlayer() + index) % 2;
    }

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

    public boolean undo() {
        int steps = undoSteps();
        return steps > 0 && session.undo(steps);
    }
}
