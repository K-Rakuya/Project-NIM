package nim.desktop;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Huy hiệu người chơi. Trạng thái đang đến lượt là một lớp riêng mờ dần vào/ra
 * viền nhấn + nền sáng + avatar màu nhấn thay vì đổi class tức thì
 * điểm số nảy khi thay đổi.
 */
final class SeatBadge extends StackPane {
    private final Label initial = new Label();
    private final Label name = new Label();
    private final Label sub = new Label();
    private final Label score = new Label();
    private final Region activeBg = new Region();
    private final Region avatarOn = new Region();
    private final Label initialOn = new Label();
    private int shownScore = Integer.MIN_VALUE;
    private boolean active;

    SeatBadge() {
        getStyleClass().add("seat");
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        activeBg.getStyleClass().add("seat-active-bg");
        activeBg.setOpacity(0);
        activeBg.setMouseTransparent(true);

        initial.getStyleClass().add("seat-initial");
        initialOn.getStyleClass().add("seat-initial-on");
        initialOn.setOpacity(0);
        avatarOn.getStyleClass().add("seat-avatar-on");
        avatarOn.setOpacity(0);
        StackPane avatar = new StackPane(avatarOn, initial, initialOn);
        avatar.getStyleClass().add("seat-avatar");

        name.getStyleClass().add("seat-name");
        sub.getStyleClass().add("seat-sub");
        score.getStyleClass().add("seat-score");
        HBox row = new HBox(10, avatar, new VBox(1, name, sub), score);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("seat-row");
        getChildren().addAll(activeBg, row);
    }

    void set(String displayName, String subtitle) {
        name.setText(displayName);
        sub.setText(subtitle);
        String last = displayName.isEmpty() ? "" : displayName.substring(displayName.length() - 1);
        String ini = displayName.isEmpty() ? ""
                : Character.isDigit(last.charAt(0)) ? last : displayName.substring(0, 1).toUpperCase();
        initial.setText(ini);
        initialOn.setText(ini);
    }

    void setScore(int wins) {
        boolean changed = shownScore != Integer.MIN_VALUE && shownScore != wins;
        shownScore = wins;
        score.setText(String.valueOf(wins));
        if (changed)
            Motion.pop(score);
    }

    void setActive(boolean on) {
        if (on == active)
            return;
        active = on;
        double v = on ? 1 : 0;
        Motion.to(activeBg, "o", activeBg.opacityProperty(), v, Motion.BASE, Motion.STANDARD);
        Motion.to(avatarOn, "o", avatarOn.opacityProperty(), v, Motion.BASE, Motion.STANDARD);
        Motion.to(initialOn, "o", initialOn.opacityProperty(), v, Motion.BASE, Motion.STANDARD);
        Motion.to(initial, "o", initial.opacityProperty(), on ? 0 : 1, Motion.BASE, Motion.STANDARD);
        if (on)
            Motion.pop(this);
    }
}
