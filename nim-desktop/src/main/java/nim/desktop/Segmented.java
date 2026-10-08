package nim.desktop;

import java.util.List;
import java.util.function.Function;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

final class Segmented<T> extends HBox {
    private final ToggleGroup group = new ToggleGroup();
    private final ObjectProperty<T> value = new SimpleObjectProperty<>();

    Segmented(List<T> items, Function<T, String> label, T initial) {
        getStyleClass().add("segmented");
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            ToggleButton b = new ToggleButton(label.apply(item));
            b.setToggleGroup(group);
            b.getStyleClass().add("seg-btn");
            if (i == 0)
                b.getStyleClass().add("first");
            if (i == items.size() - 1)
                b.getStyleClass().add("last");
            b.setUserData(item);
            getChildren().add(b);
        }
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null)
                old.setSelected(true);
            else
                value.set(get(now));
        });
        set(initial);
    }

    @SuppressWarnings("unchecked")
    private T get(javafx.scene.control.Toggle t) {
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
