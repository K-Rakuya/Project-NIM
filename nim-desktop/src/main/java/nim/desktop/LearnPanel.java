package nim.desktop;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nim.core.GameState;
import nim.core.NimTheory;

final class LearnPanel extends VBox {
    private final Label table = new Label();
    private final Label verdict = new Label();
    private final Label note = new Label();

    LearnPanel() {
        super(8);
        getStyleClass().add("side-card");
        Label title = new Label("PHÂN TÍCH NIM-SUM");
        title.getStyleClass().add("field-caption");
        table.getStyleClass().add("mono");
        verdict.getStyleClass().add("verdict");
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
        table.setText(sb.toString());

        if (s.isTerminal()) {
            verdict.setText("Ván đã kết thúc");
            note.setText("");
            return;
        }
        boolean losing = NimTheory.isLosingForCurrentPlayer(s);
        verdict.setText(losing ? "Thế thua cho người đang đi" : "Thế thắng cho người đang đi");
        verdict.getStyleClass().removeAll("good", "bad");
        verdict.getStyleClass().add(losing ? "bad" : "good");

        boolean endgame = s.isMisere() && s.heaps().length > 0 && maxHeap(s) <= 1;
        note.setText(endgame
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
