package nim.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import nim.core.GameState;
import nim.core.Move;

final class BoardView extends StackPane {
    private final SeatBadge[] seats = { new SeatBadge(), new SeatBadge() };
    private final StackPane topSeatHolder = new StackPane();
    private final StackPane bottomSeatHolder = new StackPane();
    private final HBox heapsBox = new HBox(34);
    private final StackPane heapsHolder = new StackPane(heapsBox);
    private final Label status = new Label();
    private final Label pillText = new Label();
    private final HBox pill = new HBox(10);
    private final StackPane infoRow = new StackPane();
    private final StackPane overlay = new StackPane();
    private final List<HeapView> heaps = new ArrayList<>();

    private Consumer<Move> onMove = m -> { };
    private boolean inputEnabled;
    private int selHeap = -1;
    private int selCount;

    BoardView() {
        getStyleClass().add("table-wrap");
        setPadding(new Insets(0));

        Region surface = new Region();
        surface.getStyleClass().add("table");
        Region inset = new Region();
        inset.getStyleClass().add("table-inset");
        inset.setMouseTransparent(true);
        StackPane.setMargin(inset, new Insets(14));

        heapsBox.setAlignment(Pos.BOTTOM_CENTER);
        heapsBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        heapsHolder.setAlignment(Pos.CENTER);
        heapsHolder.setMinHeight(0);
        heapsHolder.layoutBoundsProperty().addListener((o, a, b) -> fitHeaps());
        heapsBox.layoutBoundsProperty().addListener((o, a, b) -> fitHeaps());
        VBox.setVgrow(heapsHolder, Priority.ALWAYS);

        status.getStyleClass().add("status");
        Button confirm = new Button("Bốc");
        confirm.getStyleClass().addAll("btn", "primary");
        confirm.setOnAction(e -> confirm());
        Button cancel = new Button("Hủy");
        cancel.getStyleClass().addAll("btn", "ghost");
        cancel.setOnAction(e -> clearSelection());
        pillText.getStyleClass().add("pill-text");
        pill.getChildren().addAll(pillText, confirm, cancel);
        pill.setAlignment(Pos.CENTER);
        pill.getStyleClass().add("pill");
        pill.setVisible(false);
        pill.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        infoRow.getChildren().addAll(status, pill);
        infoRow.setMinHeight(48);
        infoRow.setPrefHeight(48);

        VBox content = new VBox(10, topSeatHolder, heapsHolder, infoRow, bottomSeatHolder);
        content.setPadding(new Insets(30, 36, 28, 36));
        content.setAlignment(Pos.CENTER);
        topSeatHolder.setAlignment(Pos.CENTER);
        bottomSeatHolder.setAlignment(Pos.CENTER);

        overlay.setVisible(false);
        overlay.getStyleClass().add("overlay");

        getChildren().addAll(surface, inset, content, overlay);
        StackPane.setMargin(surface, new Insets(0));

        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);

        surface.setOnMouseClicked(e -> {
            requestFocus();
            clearSelection();
        });
        heapsHolder.setOnMouseClicked(e -> {
            requestFocus();
            clearSelection();
        });
    }

    void setOnMove(Consumer<Move> c) {
        onMove = c;
    }

    void setup(GameState s, int capacity, boolean dropIn) {
        clearSelection();
        hideResult();
        heaps.forEach(HeapView::stopAnimation);
        heaps.clear();
        heapsBox.getChildren().clear();
        HeapView.Listener l = new HeapView.Listener() {
            @Override
            public void hover(int heap, int item) {
                if (selHeap < 0 && inputEnabled)
                    heaps.get(heap).mark(item, ItemNode.State.PREVIEW);
            }

            @Override
            public void exit(int heap) {
                if (selHeap < 0)
                    heaps.get(heap).unmark();
            }

            @Override
            public void click(int heap, int item) {
                requestFocus();
                int c = heaps.get(heap).count() - item;
                if (selHeap == heap && selCount == c)
                    confirm();
                else
                    select(heap, c);
            }
        };
        for (int i = 0; i < s.heapCount(); i++) {
            HeapView h = new HeapView(i, capacity, l);
            h.setCount(s.heap(i), dropIn);
            h.setInteractive(inputEnabled);
            heaps.add(h);
            heapsBox.getChildren().add(h);
        }
    }

    void sync(GameState s) {
        clearSelection();
        for (int i = 0; i < heaps.size(); i++) {
            heaps.get(i).stopAnimation();
            heaps.get(i).setCount(s.heap(i), false);
        }
    }

    void setSeat(int seat, String name, String sub) {
        seats[seat].set(name, sub);
    }

    void setBottomSeat(int seat) {
        topSeatHolder.getChildren().setAll(seats[1 - seat]);
        bottomSeatHolder.getChildren().setAll(seats[seat]);
    }

    void setScore(int seat, int wins) {
        seats[seat].setScore(wins);
    }

    void setActiveSeat(int seat) {
        for (int i = 0; i < 2; i++)
            seats[i].setActive(i == seat);
    }

    void setStatus(String text) {
        status.setText(text);
    }

    void setInputEnabled(boolean on) {
        inputEnabled = on;
        heaps.forEach(h -> h.setInteractive(on));
        if (!on)
            clearSelection();
    }

    void playMove(Move m, boolean preview, Runnable done) {
        clearSelection();
        heaps.get(m.heapIndex()).animateRemove(m.count(), preview, done);
    }

    void showHint(Move m) {
        clearSelection();
        HeapView h = heaps.get(m.heapIndex());
        h.mark(h.count() - m.count(), ItemNode.State.HINT);
    }

    void clearHint() {
        if (selHeap < 0)
            heaps.forEach(HeapView::unmark);
    }

    void showResult(String title, String subtitle, Runnable onNewGame) {
        Label t = new Label(title);
        t.getStyleClass().add("result-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("result-sub");
        Button again = new Button("Ván mới");
        again.getStyleClass().addAll("btn", "primary");
        again.setOnAction(e -> onNewGame.run());
        Button close = new Button("Đóng");
        close.getStyleClass().addAll("btn", "ghost");
        close.setOnAction(e -> hideResult());
        VBox card = new VBox(6, t, s, new HBox(10, again, close) {{
            setAlignment(Pos.CENTER);
            setPadding(new Insets(14, 0, 0, 0));
        }});
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("result-card");
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        overlay.getChildren().setAll(card);
        overlay.setVisible(true);
    }

    void hideResult() {
        overlay.setVisible(false);
        overlay.getChildren().clear();
    }

    private void select(int heap, int count) {
        if (!inputEnabled || count < 1 || count > heaps.get(heap).count())
            return;
        heaps.forEach(HeapView::unmark);
        selHeap = heap;
        selCount = count;
        HeapView h = heaps.get(heap);
        h.mark(h.count() - count, ItemNode.State.SELECTED);
        pillText.setText("Đống " + (heap + 1) + "  ·  bốc " + count);
        pill.setVisible(true);
        status.setVisible(false);
    }

    private void clearSelection() {
        selHeap = -1;
        selCount = 0;
        pill.setVisible(false);
        status.setVisible(true);
        heaps.forEach(HeapView::unmark);
    }

    private void confirm() {
        if (selHeap < 0 || !inputEnabled)
            return;
        Move m = new Move(selHeap, selCount);
        clearSelection();
        onMove.accept(m);
    }

    private void onKey(KeyEvent e) {
        if (!inputEnabled)
            return;
        KeyCode k = e.getCode();
        switch (k) {
            case LEFT, RIGHT -> {
                int dir = k == KeyCode.LEFT ? -1 : 1;
                int h = selHeap < 0 ? (dir > 0 ? -1 : heaps.size()) : selHeap;
                for (int i = 0; i < heaps.size(); i++) {
                    h += dir;
                    if (h < 0 || h >= heaps.size())
                        break;
                    if (heaps.get(h).count() > 0) {
                        select(h, Math.min(Math.max(selCount, 1), heaps.get(h).count()));
                        break;
                    }
                }
                e.consume();
            }
            case UP, DOWN -> {
                if (selHeap >= 0)
                    select(selHeap, selCount + (k == KeyCode.DOWN ? 1 : -1));
                e.consume();
            }
            case ENTER -> {
                confirm();
                e.consume();
            }
            case ESCAPE -> {
                clearSelection();
                e.consume();
            }
            default -> { }
        }
    }

    private void fitHeaps() {
        double w = heapsBox.getLayoutBounds().getWidth();
        double h = heapsBox.getLayoutBounds().getHeight();
        if (w <= 0 || h <= 0)
            return;
        double s = Math.min(1.5, Math.min((heapsHolder.getWidth() - 8) / w, (heapsHolder.getHeight() - 4) / h));
        s = Math.max(0.4, s);
        heapsBox.setScaleX(s);
        heapsBox.setScaleY(s);
    }
}
