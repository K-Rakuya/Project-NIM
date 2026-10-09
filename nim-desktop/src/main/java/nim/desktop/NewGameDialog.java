package nim.desktop;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        dlg.setTitle("Ván mới");

        ButtonType start = new ButtonType("Bắt đầu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(cancel, start);

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
        levelHint.setWrapText(true);
        levelHint.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        level.valueProperty().addListener((o, a, b) -> Motion.swapText(levelHint, b.description()));
        levelHint.setText(level.get().description());

        VBox aiBox = new VBox(16, field("Mức độ máy", level, levelHint), field("Thứ tự đi", seat));
        boolean vsAi = cur.mode() == Mode.VS_AI;
        aiBox.setVisible(vsAi);
        aiBox.setManaged(vsAi);

        VBox body = new VBox(18,
                Dialogs.header(Icons.ADD, "Thiết lập ván mới", "Chọn chế độ, luật chơi và kích thước bàn.", "info"),
                field("Chế độ", mode),
                aiBox,
                field("Luật", rule, hintLabel("Misère: ai bốc vật phẩm cuối cùng thì thua")),
                field("Thế mở màn", opening),
                field("Số đống", heaps, heapsVal),
                field("Số vật phẩm tối đa mỗi đống", items, itemsVal));
        body.setPadding(new Insets(8, 8, 4, 8));
        body.setPrefWidth(500);
        dlg.getDialogPane().setContent(body);
        Dialogs.style(dlg, owner, stylesheets);

        // Khối tùy chọn máy co/giãn mượt; cửa sổ chạy theo từng khung hình để không bị chừa trống hay cắt nội dung.
        Runnable fit = () -> {
            var w = dlg.getDialogPane().getScene() == null ? null : dlg.getDialogPane().getScene().getWindow();
            if (w != null && w.isShowing())
                w.sizeToScene();
        };
        mode.valueProperty().addListener((o, a, b) -> {
            if (a != null)
                Motion.collapse(aiBox, b == Mode.VS_AI, true, fit);
        });

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
        // Phần đã đi qua của thanh trượt được tô màu
        Runnable paint = () -> {
            Node track = s.lookup(".track");
            if (track == null)
                return;
            double pct = (s.getValue() - s.getMin()) / (s.getMax() - s.getMin()) * 100;
            track.setStyle(String.format(java.util.Locale.ROOT,
                    "-fx-background-color: linear-gradient(to right, -c-accent 0%%, -c-accent %.1f%%, -c-border %.1f%%, -c-border 100%%);",
                    pct, pct));
        };
        s.valueProperty().addListener((o, a, b) -> paint.run());
        s.skinProperty().addListener((o, a, b) -> Platform.runLater(paint));
        s.sceneProperty().addListener((o, a, b) -> Platform.runLater(paint));
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

    private static VBox field(String caption, Node... nodes) {
        Label c = new Label(caption.toUpperCase());
        c.getStyleClass().add("field-caption");
        VBox box = new VBox(8, c);
        if (nodes.length == 2 && nodes[0] instanceof Slider) {
            HBox row = new HBox(14, nodes[0], nodes[1]);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nodes[0], Priority.ALWAYS);
            box.getChildren().add(row);
        } else {
            box.getChildren().addAll(nodes);
        }
        return box;
    }
}
