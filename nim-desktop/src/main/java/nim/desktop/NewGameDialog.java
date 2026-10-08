package nim.desktop;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import nim.core.GameConfig;
import nim.core.GameConfig.Mode;
import nim.core.GameConfig.Opening;
import nim.core.ai.AiLevel;

final class NewGameDialog {
    private NewGameDialog() {
    }

    static Optional<GameConfig> show(Window owner, GameConfig cur, List<String> stylesheets) {
        Dialog<GameConfig> dlg = new Dialog<>();
        dlg.initOwner(owner);
        dlg.setTitle("Ván mới");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getStylesheets().addAll(stylesheets);
        dlg.getDialogPane().getStyleClass().add("dialog");

        ButtonType start = new ButtonType("Bắt đầu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(cancel, start);
        dlg.getDialogPane().lookupButton(start).getStyleClass().addAll("btn", "primary");
        dlg.getDialogPane().lookupButton(cancel).getStyleClass().addAll("btn", "ghost");

        var mode = new Segmented<>(Arrays.asList(Mode.values()), Mode::label, cur.mode());
        var rule = new Segmented<>(List.of(false, true), m -> m ? "Misère" : "Thường", cur.misere());
        var level = new Segmented<>(Arrays.asList(AiLevel.values()), AiLevel::label, cur.level());
        var seat = new Segmented<>(List.of(0, 1), s -> s == 0 ? "Bạn đi trước" : "Máy đi trước", cur.humanSeat());
        var opening = new Segmented<>(Arrays.asList(Opening.values()), Opening::label, cur.opening());

        Slider heaps = slider(2, GameConfig.MAX_HEAPS, Math.max(2, cur.heapCount()));
        Slider items = slider(2, GameConfig.MAX_ITEMS, Math.max(2, cur.maxItems()));
        Label heapsVal = value(heaps);
        Label itemsVal = value(items);

        Label levelHint = new Label();
        levelHint.getStyleClass().add("field-hint");
        level.valueProperty().addListener((o, a, b) -> levelHint.setText(b.description()));
        levelHint.setText(level.get().description());

        VBox aiBox = new VBox(14, field("Mức độ máy", level, levelHint), field("Thứ tự đi", seat));
        aiBox.managedProperty().bind(aiBox.visibleProperty());
        aiBox.visibleProperty().bind(mode.valueProperty().isEqualTo(Mode.VS_AI));

        VBox body = new VBox(16,
                field("Chế độ", mode),
                aiBox,
                field("Luật", rule, hintLabel("Misère: ai bốc vật phẩm cuối cùng thì thua")),
                field("Thế mở màn", opening),
                field("Số đống", heaps, heapsVal),
                field("Số vật phẩm tối đa mỗi đống", items, itemsVal));
        body.setPadding(new Insets(8, 8, 4, 8));
        body.setPrefWidth(480);
        dlg.getDialogPane().setContent(body);

        dlg.setResultConverter(bt -> bt != start ? null
                : new GameConfig(mode.get(), rule.get(), (int) Math.round(heaps.getValue()),
                        (int) Math.round(items.getValue()), opening.get(), level.get(), seat.get()));
        return dlg.showAndWait();
    }

    private static Slider slider(int min, int max, int v) {
        Slider s = new Slider(min, max, v);
        s.setMajorTickUnit(1);
        s.setMinorTickCount(0);
        s.setSnapToTicks(true);
        s.setBlockIncrement(1);
        return s;
    }

    private static Label value(Slider s) {
        Label l = new Label();
        l.getStyleClass().add("field-value");
        l.textProperty().bind(s.valueProperty().asString("%.0f"));
        return l;
    }

    private static Label hintLabel(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("field-hint");
        return l;
    }

    private static VBox field(String caption, javafx.scene.Node... nodes) {
        Label c = new Label(caption.toUpperCase());
        c.getStyleClass().add("field-caption");
        VBox box = new VBox(6, c);
        if (nodes.length == 2 && nodes[0] instanceof Slider) {
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12, nodes[0], nodes[1]);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(nodes[0], javafx.scene.layout.Priority.ALWAYS);
            box.getChildren().add(row);
        } else {
            box.getChildren().addAll(nodes);
        }
        return box;
    }
}
