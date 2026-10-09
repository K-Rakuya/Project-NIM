package nim.desktop;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;


final class ItemNode extends Group {
    enum State { NORMAL, PREVIEW, SELECTED, HINT }

    static final double RX = 44;
    static final double RY = 15;
    static final double THICKNESS = 9;
    static final double PITCH = 11;

    private static final PseudoClass PREVIEW = PseudoClass.getPseudoClass("preview");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass HINT = PseudoClass.getPseudoClass("hint");

    private final Path side = new Path();
    private final Ellipse top = new Ellipse(0, 0, RX, RY);
    private final Ellipse ring = new Ellipse(0, 0, RX - 10, RY - 4);

    private final Group overlay = new Group();
    private final Path sideOv = new Path();
    private final Ellipse topOv = new Ellipse(0, 0, RX, RY);
    private final Ellipse ringOv = new Ellipse(0, 0, RX - 10, RY - 4);

    private final int index;
    private State state = State.NORMAL;

    ItemNode(int index, double centerX, double centerY) {
        this.index = index;
        buildSide(side);
        buildSide(sideOv);
        side.getStyleClass().add("item-side");
        top.getStyleClass().add("item-top");
        ring.getStyleClass().add("item-ring");
        sideOv.getStyleClass().add("item-side-ov");
        topOv.getStyleClass().add("item-top-ov");
        ringOv.getStyleClass().add("item-ring-ov");
        ring.setMouseTransparent(true);
        overlay.setMouseTransparent(true);
        overlay.setOpacity(0);
        overlay.getChildren().addAll(sideOv, topOv, ringOv);

        getChildren().addAll(side, top, ring, overlay);
        setLayoutX(centerX);
        setLayoutY(centerY);
    }

    private static void buildSide(Path p) {
        p.getElements().addAll(
                new MoveTo(-RX, 0),
                new LineTo(-RX, THICKNESS),
                new ArcTo(RX, RY, 0, RX, THICKNESS, false, false),
                new LineTo(RX, 0),
                new ClosePath());
    }

    int index() {
        return index;
    }

    State state() {
        return state;
    }

    private static double lift(State s) {
        return switch (s) {
            case NORMAL -> 0;
            case PREVIEW, HINT -> -3;
            case SELECTED -> -6;
        };
    }

    void setState(State s) {
        if (s == state)
            return;
        State prev = state;
        state = s;
        Motion.cancel(this, "hint-pulse");

        if (s != State.NORMAL) {
            for (Node n : new Node[] { sideOv, topOv, ringOv }) {
                n.pseudoClassStateChanged(PREVIEW, s == State.PREVIEW);
                n.pseudoClassStateChanged(SELECTED, s == State.SELECTED);
                n.pseudoClassStateChanged(HINT, s == State.HINT);
            }
            // Chuyển giữa hai trạng thái có màu.
            if (prev != State.NORMAL)
                overlay.setOpacity(Math.min(overlay.getOpacity(), 0.55));
        }
        Motion.to(this, "fill", overlay.opacityProperty(), s == State.NORMAL ? 0 : 1, Motion.FAST, Motion.STANDARD);
        Motion.to(this, "ty", translateYProperty(), lift(s), Motion.FAST, Motion.STANDARD);

        if (s == State.HINT) {
            // pulse nhẹ để thu hút mắt nhưng không gây khó chịu
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.millis(0), new KeyValue(overlay.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(800), new KeyValue(overlay.opacityProperty(), 0.55, Motion.STANDARD)),
                    new KeyFrame(Duration.millis(1600), new KeyValue(overlay.opacityProperty(), 1, Motion.STANDARD)));
            pulse.setDelay(Motion.FAST);
            pulse.setCycleCount(Animation.INDEFINITE);
            Motion.play(this, "hint-pulse", pulse);
        }
    }

    /** Dừng mọi hoạt ảnh của vật phẩm */
    void freeze() {
        Motion.cancel(this, "fill");
        Motion.cancel(this, "ty");
        Motion.cancel(this, "hint-pulse");
    }

    /** Hoạt ảnh rơi xuống bàn, chưa phát. Đặt trạng thái đầu */
    Animation dropIn(long delayMs) {
        setOpacity(0);
        setTranslateY(-34);
        setScaleX(0.96);
        setScaleY(0.96);
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(340),
                        new KeyValue(translateYProperty(), 0, Motion.SETTLE),
                        new KeyValue(scaleXProperty(), 1, Motion.EASE_OUT),
                        new KeyValue(scaleYProperty(), 1, Motion.EASE_OUT)),
                new KeyFrame(Duration.millis(200), new KeyValue(opacityProperty(), 1, Motion.STANDARD)));
        t.setDelay(Duration.millis(delayMs));
        return t;
    }

    /** Hoạt ảnh bay lên và tan đi khi bị bốc, chưa phát  */
    Animation flyAway(long delayMs) {
        double toY = lift(state) - 46;
        Timeline t = new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(translateYProperty(), toY, Motion.EASE_IN),
                new KeyValue(opacityProperty(), 0, Motion.EASE_IN),
                new KeyValue(scaleXProperty(), 0.9, Motion.EASE_IN),
                new KeyValue(scaleYProperty(), 0.9, Motion.EASE_IN)));
        t.setDelay(Duration.millis(delayMs));
        return t;
    }
}
