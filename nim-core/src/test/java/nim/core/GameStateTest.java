package nim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * GameStateTest
 */
public class GameStateTest {

    @Test
    void nimSumLaXorCuaCacDong() {
        assertEquals(3 ^ 5 ^ 7, GameState.of(false, 3, 5, 7).nimSum());
        assertEquals(0, GameState.of(false, 1, 2, 3).nimSum());
    }

    @Test
    void soNuocDiHopLeBangTongSoItem() {
        assertEquals(15, GameState.of(false, 1, 2, 3).legalMoves().size());
    }

    @Test
    void applySinhTrangThaiMoiVaoDoiLuot() {
        GameState s = GameState.of(false, 3, 5, 7);
        GameState t = s.apply(new Move(0, 2));

        assertEquals(3, s.heap(0));
        assertEquals(1, t.heap(0));
        assertEquals(0, s.currentPlayer());
        assertEquals(1, t.currentPlayer());
    }

    @Test
    void nuocDiKhongHopLeBiChan() {
        GameState s = GameState.of(false, 3);
        assertThrows(IllegalArgumentException.class, () -> s.apply(new Move(0, 4)));
        assertThrows(IllegalArgumentException.class, () -> s.apply(new Move(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new Move(0, 0));
    }

    @Test
    void luatThuong() {
        GameState s = GameState.of(false, 1).apply(new Move(0, 1));
        assertTrue(s.isTerminal());
        assertEquals(0, s.winner());
    }

    @Test
    void luatMisere() {
        GameState s = GameState.of(true, 1).apply(new Move(0, 1));
        assertTrue(s.isTerminal());
        assertEquals(1, s.winner());
    }
}