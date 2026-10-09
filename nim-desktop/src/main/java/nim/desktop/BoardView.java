package nim.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;
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
    private final StatusBar status = new StatusBar();
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

        Button confirm = Wash.install(new Button("Bốc"));
        confirm.getStyleClass().addAll("btn", "primary");
        confirm.setOnAction(e -> confirm());
        Button cancel = Wash.install(new Button("Hủy"));
        cancel.getStyleClass().addAll("btn", "ghost");
        cancel.setOnAction(e -> clearSelection());
        pillText.getStyleClass().add("pill-text");
        pill.getChildren().addAll(pillText, confirm, cancel);
        pill.setAlignment(Pos.CENTER);
        pill.getStyleClass().add("pill");
        pill.setVisible(false);
        pill.setOpacity(0);
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
        overlay.setOpacity(0);
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
        hideResult(false);
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
        status.set(text, StatusBar.Kind.INFO);
    }

    void setStatus(String text, StatusBar.Kind kind) {
        status.set(text, kind);
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
        Region disc = new Region();
        disc.getStyleClass().add("result-disc");
        SVGPath check = new SVGPath();
        check.setContent(Icons.CHECK);
        check.getStyleClass().add("result-check");
        StackPane badge = new StackPane(disc, check);
        badge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label kicker = new Label("KẾT THÚC VÁN");
        kicker.getStyleClass().add("field-caption");
        Label t = new Label(title);
        t.getStyleClass().add("result-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("result-sub");
        s.setWrapText(true);
        s.setMaxWidth(340);
        s.setAlignment(Pos.CENTER);
        s.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button again = Wash.install(new Button("Ván mới"));
        again.getStyleClass().addAll("btn", "primary");
        again.setOnAction(e -> onNewGame.run());
        Button close = Wash.install(new Button("Xem lại bàn"));
        close.getStyleClass().addAll("btn", "ghost");
        close.setOnAction(e -> hideResult(true));
        HBox actions = new HBox(10, again, close);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(16, 0, 0, 0));
        VBox card = new VBox(6, badge, kicker, t, s, actions);
        VBox.setMargin(kicker, new Insets(12, 0, 0, 0));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("result-card");
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        overlay.getChildren().setAll(card);
        overlay.setVisible(true);
        overlay.setMouseTransparent(false);

        // Nền mờ vào trước, thẻ kết quả nảy lên sau, huy hiệu nảy cuối cùng.
        Motion.to(overlay, "o", overlay.opacityProperty(), 1, Motion.SLOW, Motion.STANDARD);
        card.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);
        card.setTranslateY(14);
        javafx.animation.Timeline in = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Motion.SLOW.add(javafx.util.Duration.millis(80)),
                new javafx.animation.KeyValue(card.opacityProperty(), 1, Motion.EASE_OUT),
                new javafx.animation.KeyValue(card.scaleXProperty(), 1, Motion.SETTLE),
                new javafx.animation.KeyValue(card.scaleYProperty(), 1, Motion.SETTLE),
                new javafx.animation.KeyValue(card.translateYProperty(), 0, Motion.EASE_OUT)));
        in.setDelay(javafx.util.Duration.millis(120));
        Motion.play(card, "in", in);
        badge.setScaleX(0);
        badge.setScaleY(0);
        javafx.animation.Timeline b = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(420),
                new javafx.animation.KeyValue(badge.scaleXProperty(), 1, Motion.POP),
                new javafx.animation.KeyValue(badge.scaleYProperty(), 1, Motion.POP)));
        b.setDelay(javafx.util.Duration.millis(380));
        Motion.play(badge, "in", b);
    }

    void hideResult() {
        hideResult(true);
    }

    private void hideResult(boolean animated) {
        if (!overlay.isVisible())
            return;
        if (!animated) {
            Motion.cancel(overlay, "o");
            overlay.setVisible(false);
            overlay.setOpacity(0);
            overlay.getChildren().clear();
            return;
        }
        overlay.setMouseTransparent(true);
        javafx.animation.Timeline t = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Motion.BASE,
                new javafx.animation.KeyValue(overlay.opacityProperty(), 0, Motion.EASE_IN)));
        t.setOnFinished(e -> {
            overlay.setVisible(false);
            overlay.getChildren().clear();
        });
        Motion.play(overlay, "o", t);
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
        if (!pill.isVisible() || pill.getOpacity() < 1)
            Motion.show(pill);
        else
            Motion.pop(pillText);
        Motion.to(status, "o", status.opacityProperty(), 0, Motion.FAST, Motion.STANDARD);
    }

    private void clearSelection() {
        selHeap = -1;
        selCount = 0;
        Motion.hide(pill);
        Motion.to(status, "o", status.opacityProperty(), 1, Motion.BASE, Motion.STANDARD);
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
