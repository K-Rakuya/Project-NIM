package nim.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class StateGeneratorTest {

    @Test
    void sinhDungRangBuocThangThua() {
        StateGenerator gen = new StateGenerator(new Random(42));
        for (int i = 0; i < 300; i++) {
            assertFalse(NimTheory.isLosingForCurrentPlayer(
                    gen.random(3, 1, 9, false, Boolean.TRUE)));
            assertTrue(NimTheory.isLosingForCurrentPlayer(
                    gen.random(3, 1, 9, false, Boolean.FALSE)));
        }
    }

    @Test
    void ketQuaLapLaiDuocVoiCungSeed() {
        GameState a = new StateGenerator(new Random(7)).random(4, 1, 9, false, null);
        GameState b = new StateGenerator(new Random(7)).random(4, 1, 9, false, null);
        assertArrayEquals(a.heaps(), b.heaps());
    }
}