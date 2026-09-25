package nim.core.ai;

import nim.core.GameSession;
import nim.core.GameState;
import nim.core.Move;
import nim.core.NimTheory;
import nim.core.StateGenerator;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MinimaxAiTest {

    @Test
    void danhGiaKhopVoiDinhLyBoutonTrenTapTheCoNho() {
        MinimaxAi ai = new MinimaxAi();
        for (boolean misere : new boolean[]{false, true}) {
            for (int a = 0; a <= 4; a++)
                for (int b = 0; b <= 4; b++)
                    for (int c = 0; c <= 4; c++) {
                        GameState s = GameState.of(misere, a, b, c);
                        boolean minimaxNoiThang = ai.evaluate(s) > 0;
                        boolean lyThuyetNoiThang = !NimTheory.isLosingForCurrentPlayer(s);
                        assertEquals(lyThuyetNoiThang, minimaxNoiThang, "Lệch tại thế " + s);
                    }
        }
    }

    @Test
    void coCatTiaHayKhongVanChoCungMotKetQua() {
        MinimaxAi coCatTia = new MinimaxAi(true);
        MinimaxAi khongCatTia = new MinimaxAi(false);
        for (boolean misere : new boolean[]{false, true}) {
            for (int a = 0; a <= 4; a++)
                for (int b = 0; b <= 4; b++)
                    for (int c = 0; c <= 4; c++) {
                        GameState s = GameState.of(misere, a, b, c);
                        assertEquals(khongCatTia.evaluate(s), coCatTia.evaluate(s),
                                "Cắt tỉa làm sai kết quả tại " + s);
                    }
        }
    }

    @Test
    void catTiaDuyetItNutHonRoRetTrenBanLon() {
        MinimaxAi coCatTia = new MinimaxAi(true);
        MinimaxAi khongCatTia = new MinimaxAi(false);
        GameState s = GameState.of(false, 4, 5, 6);

        khongCatTia.evaluate(s);
        long fullNodes = khongCatTia.lastNodesVisited();
        coCatTia.evaluate(s);
        long prunedNodes = coCatTia.lastNodesVisited();

        assertTrue(prunedNodes <= fullNodes, "Alpha-beta phải duyệt số nút <= minimax đầy đủ");
        // Số liệu thống kê
        System.out.printf("  Bàn (4,5,6): đầy đủ=%d nút, alpha-beta=%d nút (giảm %.1f lần)%n",
                fullNodes, prunedNodes, (double) fullNodes / prunedNodes);
    }

    @Test
    void nuocDiChonRaLuonDayDoiThuVaoTheThua() {
        MinimaxAi ai = new MinimaxAi();
        for (boolean misere : new boolean[]{false, true}) {
            for (int a = 1; a <= 6; a++)
                for (int b = 1; b <= 6; b++) {
                    GameState s = GameState.of(misere, a, b);
                    if (NimTheory.isLosingForCurrentPlayer(s)) continue;

                    Move m = ai.chooseMove(s);
                    assertTrue(s.isLegal(m));
                    assertTrue(NimTheory.isLosingForCurrentPlayer(s.apply(m)),
                            "Sau nước đi của Minimax, đối thủ phải ở thế thua: " + s);
                }
        }
    }

    @Test
    void tuDauVoiOptimalAiNguoiThangDinhTruocPhaiLuonThang() {
        Random rnd = new Random(123);
        StateGenerator gen = new StateGenerator(rnd);
        MinimaxAi minimax = new MinimaxAi();
        OptimalAi optimal = new OptimalAi();

        for (int i = 0; i < 60; i++) {
            GameState start = gen.random(3, 1, 5, false, Boolean.TRUE);
            GameSession session = new GameSession(start);
            while (!session.state().isTerminal()) {
                AiStrategy toMove = (session.state().currentPlayer() == 0) ? minimax : optimal;
                session.play(toMove.chooseMove(session.state()));
            }
            assertEquals(0, session.state().winner(),
                    "Người chơi 0 (Minimax) phải thắng vì bắt đầu ở thế thắng");
        }
    }
}