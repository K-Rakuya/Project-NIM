package nim.desktop;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

final class SeatBadge extends HBox {
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private final Label initial = new Label();
    private final Label name = new Label();
    private final Label sub = new Label();
    private final Label score = new Label();

    SeatBadge() {
        getStyleClass().add("seat");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        StackPane avatar = new StackPane(initial);
        avatar.getStyleClass().add("seat-avatar");
        initial.getStyleClass().add("seat-initial");

        name.getStyleClass().add("seat-name");
        sub.getStyleClass().add("seat-sub");
        score.getStyleClass().add("seat-score");
        getChildren().addAll(avatar, new VBox(1, name, sub), score);
    }

    void set(String displayName, String subtitle) {
        name.setText(displayName);
        sub.setText(subtitle);
        String last = displayName.isEmpty() ? "" : displayName.substring(displayName.length() - 1);
        initial.setText(displayName.isEmpty() ? ""
                : Character.isDigit(last.charAt(0)) ? last : displayName.substring(0, 1).toUpperCase());
    }

    void setScore(int wins) {
        score.setText(String.valueOf(wins));
    }

    void setActive(boolean active) {
        pseudoClassStateChanged(ACTIVE, active);
    }
}
