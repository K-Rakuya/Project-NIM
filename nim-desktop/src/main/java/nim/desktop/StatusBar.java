package nim.desktop;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

final class StatusBar extends HBox {
    enum Kind { INFO, TURN, THINKING, HINT, DONE }

    private static final PseudoClass[] STATES = {
        PseudoClass.getPseudoClass("info"), PseudoClass.getPseudoClass("turn"),
        PseudoClass.getPseudoClass("thinking"), PseudoClass.getPseudoClass("hint"),
        PseudoClass.getPseudoClass("done") };

    private final Label text = new Label();
    private final Region dot = new Region();
    private final Region[] beats = { new Region(), new Region(), new Region() };
    private final HBox beatBox = new HBox(4, beats);
    private Timeline pulse;

    StatusBar() {
        super(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("status-bar");
        dot.getStyleClass().add("status-dot");
        text.getStyleClass().add("status");
        text.setWrapText(true);
        text.setMaxWidth(560);
        text.setAlignment(Pos.CENTER);
        for (Region r : beats) {
            r.getStyleClass().add("beat");
            r.setOpacity(0.25);
        }
        beatBox.setAlignment(Pos.CENTER);
        beatBox.setVisible(false);
        beatBox.setManaged(false);
        getChildren().addAll(dot, text, beatBox);
    }

    void set(String message, Kind kind) {
        Motion.swapText(text, message);
        for (int i = 0; i < STATES.length; i++)
            dot.pseudoClassStateChanged(STATES[i], i == kind.ordinal());
        setThinking(kind == Kind.THINKING);
        Motion.pop(dot);
    }

    private void setThinking(boolean on) {
        if (on == beatBox.isVisible())
            return;
        beatBox.setVisible(on);
        beatBox.setManaged(on);
        if (pulse != null) {
            pulse.stop();
            pulse = null;
        }
        if (!on) {
            for (Region r : beats)
                r.setOpacity(0.25);
            return;
        }
        pulse = new Timeline();
        for (int i = 0; i < beats.length; i++) {
            Region r = beats[i];
            Duration t0 = Duration.millis(i * 180);
            pulse.getKeyFrames().addAll(
                    new KeyFrame(t0, new KeyValue(r.opacityProperty(), 0.25)),
                    new KeyFrame(t0.add(Duration.millis(220)), new KeyValue(r.opacityProperty(), 1, Motion.STANDARD)),
                    new KeyFrame(t0.add(Duration.millis(520)), new KeyValue(r.opacityProperty(), 0.25, Motion.STANDARD)),
                    new KeyFrame(Duration.millis(1100), new KeyValue(r.opacityProperty(), 0.25)));
        }
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }
}
