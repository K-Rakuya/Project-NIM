package nim.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nim.core.GameConfig.Mode;
import nim.core.GameConfig.Opening;
import nim.core.ai.AiLevel;

class MatchTest {

    private static GameConfig vsAi(int humanSeat) {
        return new GameConfig(Mode.VS_AI, false, 3, 9, Opening.RANDOM, AiLevel.HARD, humanSeat);
    }

    private static Match fixed(GameConfig cfg, int... heaps) {
        return Match.resume(cfg, new GameSession(GameState.of(cfg.misere(), heaps)), new Random(1));
    }

    @Test
    void humanCannotPlayOnAiTurn() {
        Match m = fixed(vsAi(1), 3, 4, 5); // máy đi trước
        assertTrue(m.isAiTurn());
        assertThrows(IllegalStateException.class, () -> m.play(new Move(0, 1)));
        m.playAi();
        assertTrue(m.isHumanTurn());
        assertThrows(IllegalStateException.class, m::playAi);
    }

    @Test
    void illegalMoveIsRejectedAndStateUnchanged() {
        Match m = fixed(vsAi(0), 3, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> m.play(new Move(0, 4)));
        assertThrows(IllegalArgumentException.class, () -> m.play(new Move(7, 1)));
        assertEquals(12, m.state().totalItems());
    }

    @Test
    void undoVsAiRollsBackHumanMoveAndReply() {
        Match m = fixed(vsAi(0), 3, 4, 5);
        assertFalse(m.canUndo());
        m.play(new Move(0, 1));
        m.playAi();
        assertTrue(m.canUndo());
        assertTrue(m.undo());
        assertEquals(12, m.state().totalItems());
        assertTrue(m.isHumanTurn());
        assertFalse(m.undo());
    }

    @Test
    void undoWhenAiMovesFirstKeepsOpeningMove() {
        Match m = fixed(vsAi(1), 3, 4, 5);
        m.playAi();                 // máy đi trước
        m.play(new Move(0, 1));     // người đi
        m.playAi();
        assertTrue(m.undo());       // lùi về ngay sau nước đầu của máy
        assertEquals(1, m.session().history().size());
        assertFalse(m.canUndo());   // không còn nước nào của người để lùi
    }

    @Test
    void undoHumanVsHumanIsSingleStep() {
        Match m = fixed(new GameConfig(Mode.VS_HUMAN, false, 3, 9, Opening.RANDOM, AiLevel.EASY, 0), 2, 2);
        m.play(new Move(0, 1));
        assertTrue(m.undo());
        assertEquals(4, m.state().totalItems());
    }

    @Test
    void hintCountsUsageAndIsEmptyOnLostPosition() {
        Match m = fixed(vsAi(0), 1, 1);   // nim-sum = 0 => thế thua
        assertTrue(m.hint().isEmpty());
        assertEquals(1, m.hintsUsed());
        Match w = fixed(vsAi(0), 1, 2);
        assertTrue(w.hint().isPresent());
    }

    @Test
    void hardAiBeatsHumanWhoStartsInLosingPosition() {
        Match m = Match.start(new GameConfig(Mode.VS_AI, false, 4, 9, Opening.FIRST_LOSES, AiLevel.HARD, 0),
                new Random(7));
        while (!m.isOver()) {
            if (m.isHumanTurn())
                m.play(m.state().legalMoves().get(0));
            else
                m.playAi();
        }
        assertEquals(1, m.winner()); // máy thắng
    }

    @Test
    void startRespectsOpeningConstraint() {
        for (boolean misere : new boolean[] { false, true }) {
            Match m = Match.start(new GameConfig(Mode.VS_AI, misere, 5, 12, Opening.FIRST_WINS, AiLevel.EASY, 0),
                    new Random(3));
            assertFalse(NimTheory.isLosingForCurrentPlayer(m.state()));
        }
    }

    @Test
    void configValidation() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.defaults().withHeapCount(0));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.defaults().withHeapCount(9));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.defaults().withMaxItems(21));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.defaults().withHumanSeat(2));
    }

    @Test
    void saveAndLoadMatchKeepsConfigAndHistory(@TempDir Path dir) throws IOException {
        Match m = fixed(vsAi(1).withMisere(true).withLevel(AiLevel.EASY), 3, 4, 5);
        m.playAi();
        m.play(new Move(1, 2));
        Path f = dir.resolve("a/b.nim");
        SaveFile.save(m, f);

        Match back = SaveFile.loadMatch(f, new Random(1));
        assertEquals(Mode.VS_AI, back.config().mode());
        assertEquals(AiLevel.EASY, back.config().level());
        assertEquals(1, back.config().humanSeat());
        assertTrue(back.config().misere());
        assertEquals(m.session().history(), back.session().history());
        assertEquals(m.state().canonicalKey(), back.state().canonicalKey());
    }

    @Test
    void oldSaveWithoutExtrasLoadsAsHumanVsHuman(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("old.nim");
        Files.writeString(f, "nim-save 1\nmisere=false\ninitial=3,5,7\nmoves=0:2,1:3\n");
        Match m = SaveFile.loadMatch(f, new Random(1));
        assertEquals(Mode.VS_HUMAN, m.config().mode());
        assertEquals(2, m.session().history().size());
        assertEquals(1, SaveFile.load(f).state().heap(0));
    }

    @Test
    void newSaveStillReadableByPlainLoad(@TempDir Path dir) throws IOException {
        Match m = fixed(vsAi(0), 3, 4, 5);
        m.play(new Move(0, 3));
        Path f = dir.resolve("n.nim");
        SaveFile.save(m, f);
        assertEquals(9, SaveFile.load(f).state().totalItems());
    }
}
