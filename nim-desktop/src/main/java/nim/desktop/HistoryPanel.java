package nim.desktop;

import java.util.List;
import java.util.function.IntFunction;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import nim.core.Match;
import nim.core.Move;

/** Lịch sử nước đi: mỗi dòng có số thứ tự, người đi và nước đi; dòng mới nhất trượt vào */
final class HistoryPanel extends VBox {
    /** Một dòng lịch sử. {@code fresh} = vừa phát sinh, cần chạy hiệu ứng xuất hiện một lần. */
    private static final class Entry {
        final int no;
        final int seat;
        final String who;
        final String text;
        boolean fresh;

        Entry(int no, int seat, String who, String text) {
            this.no = no;
            this.seat = seat;
            this.who = who;
            this.text = text;
        }
    }

    private final ListView<Entry> list = new ListView<>();
    private final Label count = new Label("0");
    private final Label footer = new Label();
    private final VBox empty = new VBox(8);

    HistoryPanel() {
        super(12);
        getStyleClass().add("side-card");
        setPrefWidth(236);
        setMinWidth(236);
        setMaxWidth(236);

        Label title = new Label("NƯỚC ĐI");
        title.getStyleClass().add("field-caption");
        count.getStyleClass().add("count-badge");
        javafx.scene.layout.Region grow = new javafx.scene.layout.Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(8, title, grow, count);
        head.setAlignment(Pos.CENTER_LEFT);

        Label e1 = new Label("Chưa có nước đi nào");
        e1.getStyleClass().add("empty-title");
        Label e2 = new Label("Các nước đi sẽ được ghi lại tại đây theo thứ tự.");
        e2.getStyleClass().add("field-hint");
        e2.setWrapText(true);
        e2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        javafx.scene.Node ico = Icons.of(Icons.HISTORY, 28);
        ico.getStyleClass().add("empty-icon");
        empty.getChildren().addAll(ico, e1, e2);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new javafx.geometry.Insets(0, 8, 0, 8));
        list.setPlaceholder(empty);

        list.setFocusTraversable(false);
        list.setSelectionModel(null);
        list.setCellFactory(v -> new Cell());
        VBox.setVgrow(list, Priority.ALWAYS);
        footer.getStyleClass().add("field-hint");
        getChildren().addAll(head, list, footer);
    }

    void update(Match match, IntFunction<String> seatName) {
        List<Move> moves = match.session().history();
        var items = list.getItems();

        boolean append = moves.size() == items.size() + 1;
        if (append) {
            int i = moves.size() - 1;
            items.add(entry(match, seatName, moves, i, true));
        } else if (moves.size() != items.size()) {
            items.clear();
            for (int i = 0; i < moves.size(); i++)
                items.add(entry(match, seatName, moves, i, false));
        }
        if (!moves.isEmpty())
            list.scrollTo(moves.size() - 1);
        String c = String.valueOf(moves.size());
        if (!c.equals(count.getText())) {
            count.setText(c);
            Motion.pop(count);
        }
        Motion.swapText(footer, "Gợi ý đã dùng: " + match.hintsUsed());
    }

    private static Entry entry(Match match, IntFunction<String> seatName, List<Move> moves, int i, boolean fresh) {
        Move m = moves.get(i);
        int seat = match.moverOf(i);
        Entry e = new Entry(i + 1, seat, seatName.apply(seat), "Đống " + (m.heapIndex() + 1) + "  ·  bốc " + m.count());
        e.fresh = fresh;
        return e;
    }

    private static final class Cell extends ListCell<Entry> {
        private final Label no = new Label();
        private final Label who = new Label();
        private final Label what = new Label();
        private final StackPane badge = new StackPane(no);
        private final HBox row;

        Cell() {
            badge.getStyleClass().add("hist-no");
            no.getStyleClass().add("hist-no-text");
            who.getStyleClass().add("hist-who");
            what.getStyleClass().add("hist-what");
            row = new HBox(10, badge, new VBox(1, who, what));
            row.setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("hist-cell");
        }

        @Override
        protected void updateItem(Entry e, boolean empty) {
            super.updateItem(e, empty);
            if (empty || e == null) {
                setGraphic(null);
                return;
            }
            no.setText(String.valueOf(e.no));
            who.setText(e.who);
            what.setText(e.text);
            badge.getStyleClass().removeAll("seat-a", "seat-b");
            badge.getStyleClass().add(e.seat == 0 ? "seat-a" : "seat-b");
            setGraphic(row);
            if (e.fresh) {
                e.fresh = false;
                row.setOpacity(0);
                row.setTranslateX(18);
                javafx.animation.Timeline t = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Motion.SLOW,
                        new javafx.animation.KeyValue(row.opacityProperty(), 1, Motion.EASE_OUT),
                        new javafx.animation.KeyValue(row.translateXProperty(), 0, Motion.EASE_OUT)));
                Motion.play(row, "in", t);
            } else {
                Motion.cancel(row, "in");
                row.setOpacity(1);
                row.setTranslateX(0);
            }
        }
    }
}
