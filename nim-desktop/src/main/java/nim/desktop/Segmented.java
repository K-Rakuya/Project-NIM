package nim.desktop;

import java.util.List;
import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Bộ chọn dạng phân đoạn: thanh nền màu nhấn trượt mượt từ lựa chọn cũ sang lựa chọn mới. */
final class Segmented<T> extends StackPane {
    private final ToggleGroup group = new ToggleGroup();
    private final ObjectProperty<T> value = new SimpleObjectProperty<>();
    private final HBox buttons = new HBox();
    private final Region thumb = new Region();
    private final DoubleProperty thumbX = new SimpleDoubleProperty();
    private final DoubleProperty thumbW = new SimpleDoubleProperty();
    private boolean placed;

    Segmented(List<T> items, Function<T, String> label, T initial) {
        getStyleClass().add("segmented");
        setMaxWidth(USE_PREF_SIZE);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        buttons.getStyleClass().add("segmented-row");
        thumb.getStyleClass().add("seg-thumb");
        thumb.setManaged(false);
        thumb.setMouseTransparent(true);

        for (T item : items) {
            ToggleButton b = new ToggleButton(label.apply(item));
            b.setToggleGroup(group);
            b.getStyleClass().add("seg-btn");
            b.setUserData(item);
            b.setFocusTraversable(true);
            buttons.getChildren().add(b);
        }
        getChildren().addAll(thumb, buttons);

        thumbX.addListener((o, a, b) -> layoutThumb());
        thumbW.addListener((o, a, b) -> layoutThumb());
        // Khi bố cục đổi đặt lại thanh trượt ngay, không hoạt ảnh
        buttons.layoutBoundsProperty().addListener((o, a, b) -> moveThumb(false));
        for (var n : buttons.getChildren())
            n.layoutBoundsProperty().addListener((o, a, b) -> moveThumb(false));
        heightProperty().addListener((o, a, b) -> layoutThumb());

        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null)
                old.setSelected(true);
            else {
                value.set(get(now));
                moveThumb(placed);
            }
        });
        set(initial);
    }

    private void layoutThumb() {
        thumb.resizeRelocate(thumbX.get(), 0, thumbW.get(), getHeight());
    }

    private void moveThumb(boolean animate) {
        Toggle sel = group.getSelectedToggle();
        if (!(sel instanceof ToggleButton b) || b.getWidth() <= 0)
            return;
        double x = b.getBoundsInParent().getMinX();
        double w = b.getWidth();
        if (!animate) {
            thumbX.set(x);
            thumbW.set(w);
            placed = true;
            return;
        }
        Timeline t = new Timeline(new KeyFrame(Motion.BASE,
                new KeyValue(thumbX, x, Motion.EASE_OUT),
                new KeyValue(thumbW, w, Motion.EASE_OUT)));
        Motion.play(this, "thumb", t);
    }

    @SuppressWarnings("unchecked")
    private T get(Toggle t) {
        return (T) t.getUserData();
    }

    T get() {
        return value.get();
    }

    void set(T v) {
        for (var t : group.getToggles())
            if (t.getUserData().equals(v))
                t.setSelected(true);
    }

    ObjectProperty<T> valueProperty() {
        return value;
    }
}
