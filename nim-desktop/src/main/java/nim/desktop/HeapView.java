package nim.desktop;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Ellipse;
import javafx.util.Duration;

final class HeapView extends VBox {
    interface Listener {
        void hover(int heap, int item);

        void exit(int heap);

        void click(int heap, int item);
    }

    private static final double SIDE_PAD = 14;
    private static final double BOTTOM_PAD = 20;

    private final int index;
    private final int capacity;
    private final Listener listener;
    private final Pane stack = new Pane();
    private final List<ItemNode> items = new ArrayList<>();
    private final Label title = new Label();
    private final Label total = new Label();
    private final Ellipse shadow;
    private final double stackHeight;
    private Animation running;
    private boolean interactive;
    private int count;

    HeapView(int index, int capacity, Listener listener) {
        this.index = index;
        this.capacity = Math.max(1, capacity);
        this.listener = listener;

        double w = 2 * ItemNode.RX + 2 * SIDE_PAD;
        stackHeight = this.capacity * ItemNode.PITCH + 2 * ItemNode.RY + ItemNode.THICKNESS + BOTTOM_PAD + 26;
        stack.setPrefSize(w, stackHeight);
        stack.setMinSize(w, stackHeight);
        stack.setMaxSize(w, stackHeight);

        shadow = new Ellipse(w / 2 + 7, baseY() + ItemNode.THICKNESS + 3, ItemNode.RX + 8, ItemNode.RY + 4);
        shadow.getStyleClass().add("item-shadow");
        shadow.setEffect(new GaussianBlur(9));
        shadow.setMouseTransparent(true);
        stack.getChildren().add(shadow);

        title.setText("ĐỐNG " + (index + 1));
        title.getStyleClass().add("heap-title");
        total.getStyleClass().add("heap-count");

        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(2);
        getChildren().addAll(stack, title, total);

        stack.setPickOnBounds(false);
        setPickOnBounds(false);
    }

    int index() {
        return index;
    }

    int count() {
        return count;
    }

    private double baseY() {
        return stackHeight - BOTTOM_PAD - ItemNode.RY - ItemNode.THICKNESS;
    }

    private double centerY(int k) {
        return baseY() - k * ItemNode.PITCH;
    }

    void setInteractive(boolean on) {
        interactive = on;
        for (ItemNode c : items)
            c.setCursor(on ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
    }

    void setCount(int n, boolean dropIn) {
        stopAnimation();
        int previous = count;
        stack.getChildren().removeAll(items);
        items.clear();
        count = n;
        double cx = stack.getPrefWidth() / 2;
        List<Animation> drops = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            ItemNode c = new ItemNode(k, cx, centerY(k));
            final int itemIndex = k;
            c.setOnMouseEntered(e -> {
                if (interactive)
                    listener.hover(index, itemIndex);
            });
            c.setOnMouseExited(e -> {
                if (interactive)
                    listener.exit(index);
            });
            c.setOnMouseClicked(e -> {
                if (interactive && e.getButton() == MouseButton.PRIMARY) {
                    listener.click(index, itemIndex);
                    e.consume();
                }
            });
            c.setCursor(interactive ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
            items.add(c);
            stack.getChildren().add(c);
            if (dropIn)
                drops.add(c.dropIn(60L * k + 90L * index));
        }
        setText(String.valueOf(n), previous != n && previous > 0 && !dropIn);
        // Đống đã hết: mờ cả bóng lẫn nhãn
        boolean empty = n == 0;
        Motion.to(shadow, "empty", shadow.opacityProperty(), empty ? 0 : 1, Motion.BASE, Motion.STANDARD);
        Motion.to(title, "empty", title.opacityProperty(), empty ? 0.35 : 1, Motion.BASE, Motion.STANDARD);
        Motion.to(total, "empty", total.opacityProperty(), empty ? 0.35 : 1, Motion.BASE, Motion.STANDARD);

        if (!drops.isEmpty()) {
            Animation all = Motion.together(drops.toArray(new Animation[0]));
            running = all;
            all.setOnFinished(e -> running = null);
            all.play();
        }
    }

    private void setText(String text, boolean animate) {
        total.setText(text);
        if (animate)
            Motion.pop(total);
    }

    void mark(int fromItem, ItemNode.State s) {
        for (ItemNode c : items)
            c.setState(c.index() >= fromItem ? s : ItemNode.State.NORMAL);
        int remaining = Math.max(0, Math.min(fromItem, count));
        total.setText(count + "  →  " + remaining);
        if (!total.getStyleClass().contains("heap-count-delta"))
            total.getStyleClass().add("heap-count-delta");
    }

    void unmark() {
        for (ItemNode c : items)
            c.setState(ItemNode.State.NORMAL);
        total.setText(String.valueOf(count));
        total.getStyleClass().remove("heap-count-delta");
    }

    void animateRemove(int n, boolean preview, Runnable done) {
        stopAnimation();
        int from = count - n;
        List<ItemNode> taken = new ArrayList<>(items.subList(from, count));
        for (ItemNode c : taken) {
            c.freeze();
            c.setState(preview ? ItemNode.State.PREVIEW : ItemNode.State.SELECTED);
        }

        SequentialTransition seq = new SequentialTransition();
        // Máy đi: dừng một nhịp để người chơi kịp thấy vật phẩm nào sắp bị bốc.
        if (preview)
            seq.getChildren().add(new PauseTransition(Duration.millis(520)));
        else
            seq.getChildren().add(new PauseTransition(Duration.millis(60)));

        List<Animation> fly = new ArrayList<>();
        for (int i = 0; i < taken.size(); i++) {
            ItemNode c = taken.get(taken.size() - 1 - i);
            c.freeze();
            fly.add(c.flyAway(45L * i));
        }
        seq.getChildren().add(Motion.together(fly.toArray(new Animation[0])));
        running = seq;
        seq.setOnFinished(e -> {
            running = null;
            setCount(from, false);
            done.run();
        });
        seq.play();
    }

    void stopAnimation() {
        if (running != null) {
            running.stop();
            running = null;
        }
    }
}
