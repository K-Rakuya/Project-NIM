package nim.desktop;

import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

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
    private final int index;
    private State state = State.NORMAL;

    ItemNode(int index, double centerX, double centerY) {
        this.index = index;

        side.getElements().addAll(
                new MoveTo(-RX, 0),
                new LineTo(-RX, THICKNESS),
                new ArcTo(RX, RY, 0, RX, THICKNESS, false, false),
                new LineTo(RX, 0),
                new ClosePath());
        side.getStyleClass().add("item-side");
        top.getStyleClass().add("item-top");
        ring.getStyleClass().add("item-ring");
        ring.setMouseTransparent(true);

        getChildren().addAll(side, top, ring);
        setLayoutX(centerX);
        setLayoutY(centerY);
    }

    int index() {
        return index;
    }

    State state() {
        return state;
    }

    void setState(State s) {
        if (s == state)
            return;
        state = s;
        for (var n : new javafx.scene.Node[] { side, top, ring }) {
            n.pseudoClassStateChanged(PREVIEW, s == State.PREVIEW);
            n.pseudoClassStateChanged(SELECTED, s == State.SELECTED);
            n.pseudoClassStateChanged(HINT, s == State.HINT);
        }

        setTranslateY(switch (s) {
            case NORMAL -> 0;
            case PREVIEW, HINT -> -3;
            case SELECTED -> -6;
        });
    }
}
