package nim.desktop;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nim.core.GameState;
import nim.core.NimTheory;

final class LearnPanel extends VBox {
    private final Label table = new Label();
    private final Label verdict = new Label();
    private final Label note = new Label();
    private Boolean lastLosing;

    LearnPanel() {
        super(10);
        getStyleClass().add("side-card");
        Label title = new Label("PHÂN TÍCH NIM-SUM");
        title.getStyleClass().add("field-caption");
        table.getStyleClass().addAll("mono", "code-box");
        table.setMaxWidth(Double.MAX_VALUE);
        verdict.getStyleClass().addAll("verdict");
        verdict.setWrapText(true);
        note.getStyleClass().add("field-hint");
        note.setWrapText(true);
        for (Label l : new Label[] { table, verdict, note })
            l.setMinHeight(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        getChildren().addAll(title, table, verdict, note);
    }

    void update(GameState s) {
        int bits = Math.max(3, 32 - Integer.numberOfLeadingZeros(Math.max(1, maxHeap(s))));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.heapCount(); i++)
            sb.append(String.format("%-5s %s  %2d%n", "#" + (i + 1), binary(s.heap(i), bits), s.heap(i)));
        sb.append("─".repeat(6 + (bits * 2 - 1) + 4)).append('\n');
        sb.append(String.format("%-5s %s  %2d", "XOR", binary(s.nimSum(), bits), s.nimSum()));
        Motion.swapText(table, sb.toString());

        if (s.isTerminal()) {
            Motion.swapText(verdict, "Ván đã kết thúc");
            verdict.getStyleClass().removeAll("good", "bad");
            lastLosing = null;
            Motion.swapText(note, "");
            return;
        }
        boolean losing = NimTheory.isLosingForCurrentPlayer(s);
        Motion.swapText(verdict, losing ? "Thế thua cho người đang đi" : "Thế thắng cho người đang đi");
        verdict.getStyleClass().removeAll("good", "bad");
        verdict.getStyleClass().add(losing ? "bad" : "good");
        if (lastLosing != null && lastLosing != losing)
            Motion.pop(verdict);
        lastLosing = losing;

        boolean endgame = s.isMisere() && s.heaps().length > 0 && maxHeap(s) <= 1;
        Motion.swapText(note, endgame
                ? "Misère, mọi đống ≤ 1: người đi thua khi số đống còn 1 vật phẩm là lẻ."
                : s.isMisere()
                        ? "Misère: chơi như luật thường cho tới khi mọi đống ≤ 1. Nim-sum = 0 là thế thua."
                        : "Nim-sum = 0 là thế thua; khác 0 luôn có nước đưa về 0.");
    }

    private static int maxHeap(GameState s) {
        int m = 0;
        for (int h : s.heaps())
            m = Math.max(m, h);
        return m;
    }

    private static String binary(int v, int bits) {
        StringBuilder b = new StringBuilder();
        for (int i = bits - 1; i >= 0; i--)
            b.append((v >> i & 1) == 1 ? '1' : '0').append(i > 0 ? " " : "");
        return b.toString();
    }
}
