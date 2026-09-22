package nim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * NimTheoryTest
 */
public class NimTheoryTest {
    /** Duyệt vét cạn: người đến lượt có chắc chắn thua? */
    private boolean bruteForceLosing(GameState s, Map<String, Boolean> memo) {
        List<Move> moves = s.legalMoves();
        if (moves.isEmpty()) {
            // Hết item. Người vừa đi đã bốc item cuối.
            // Luật thường: người đó thắng
            // Luật misère: người đó thua
            return !s.isMisere();
        }

        String key = s.canonicalKey();
        Boolean cached = memo.get(key);
        if (cached != null)
            return cached;

        boolean losing = true;
        for (Move m : moves) {
            if (bruteForceLosing(s.apply(m), memo)) {
                losing = false;
                break;
            }
        }
        memo.put(key, losing);
        return losing;
    }

    @Test
    void kiemTraNimTheoryVsBruteForce() {
        for (boolean misere : new boolean[] { false, true }) {
            Map<String, Boolean> memo = new HashMap<>();
            for (int a = 0; a <= 5; a++)
                for (int b = 0; b <= 5; b++)
                    for (int c = 0; c <= 5; c++)
                        for (int d = 0; d <= 5; d++) {
                            GameState s = GameState.of(misere, a, b, c, d);
                            assertEquals(bruteForceLosing(s, memo),
                                    NimTheory.isLosingForCurrentPlayer(s),
                                    "Lệch tại thế " + s);
                        }
        }
    }

    @Test
    void nuocDiToiUu() {
        for (boolean misere : new boolean[] { false, true }) {
            for (int a = 0; a <= 6; a++)
                for (int b = 0; b <= 6; b++)
                    for (int c = 0; c <= 6; c++) {
                        GameState s = GameState.of(misere, a, b, c);
                        Move m = NimTheory.findingWinningMove(s);

                        if (s.isTerminal() || NimTheory.isLosingForCurrentPlayer(s)) {
                            assertNull(m, "Thế thua không được có nước thắng: " + s);
                        } else {
                            assertNotNull(m, "Thế thắng phải có nước thắng: " + s);
                            assertTrue(s.isLegal(m), "Nước thắng phải hợp lệ: " + s);
                            assertTrue(NimTheory.isLosingForCurrentPlayer(s.apply(m)),
                                    "Sau " + m + " đối thủ phải ở thế thua: " + s);
                        }
                    }
        }
    }
}