package nim.desktop;

import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;
import javafx.util.Duration;

/** Hộp thoại thống nhất: cùng bố cục tiêu đề – nội dung – nút, cùng hiệu ứng xuất hiện. */
final class Dialogs {
    private Dialogs() {
    }

    /** Áp theme, nút có phản hồi chuyển động và hiệu ứng xuất hiện cho một hộp thoại. */
    static void style(Dialog<?> dlg, Window owner, List<String> sheets) {
        if (owner != null)
            dlg.initOwner(owner);
        dlg.setHeaderText(null);
        dlg.setGraphic(null);
        var pane = dlg.getDialogPane();
        pane.getStylesheets().addAll(sheets);
        pane.getStyleClass().add("dialog");
        for (ButtonType bt : pane.getButtonTypes()) {
            if (pane.lookupButton(bt) instanceof Button b) {
                b.getStyleClass().add("btn");
                b.getStyleClass().add(bt.getButtonData().isDefaultButton() ? "primary" : "ghost");
                Wash.install(b);
                ButtonBar.setButtonUniformSize(b, false);
            }
        }
        pane.setOpacity(0);
        dlg.setOnShown(e -> {
            pane.setScaleX(0.96);
            pane.setScaleY(0.96);
            pane.setTranslateY(10);
            Timeline t = new Timeline(new KeyFrame(Motion.SLOW,
                    new KeyValue(pane.opacityProperty(), 1, Motion.EASE_OUT),
                    new KeyValue(pane.scaleXProperty(), 1, Motion.EASE_OUT),
                    new KeyValue(pane.scaleYProperty(), 1, Motion.EASE_OUT),
                    new KeyValue(pane.translateYProperty(), 0, Motion.EASE_OUT)));
            Motion.play(pane, "in", t);
        });
    }

    /** Tiêu đề + mô tả + biểu tượng tròn, dùng làm phần đầu cho mọi hộp thoại. */
    static Node header(String iconPath, String title, String subtitle, String tone) {
        SVGPath ic = new SVGPath();
        ic.setContent(iconPath);
        ic.getStyleClass().add("dlg-icon-glyph");
        ic.setScaleX(0.85);
        ic.setScaleY(0.85);
        Region bg = new Region();
        bg.getStyleClass().add("dlg-disc");
        StackPane disc = new StackPane(bg, ic);
        disc.getStyleClass().addAll("dlg-icon", tone);
        disc.setMinSize(44, 44);
        disc.setMaxSize(44, 44);
        Label t = new Label(title);
        t.getStyleClass().add("dlg-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("field-hint");
        s.setWrapText(true);
        s.setMinHeight(Region.USE_PREF_SIZE);
        VBox text = new VBox(2, t, s);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox h = new HBox(14, disc, text);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static boolean confirm(Window owner, List<String> sheets, String title, String message, String okText,
            boolean destructive) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(cancel, ok);
        VBox body = new VBox(header(Icons.HELP, title, message, destructive ? "warn" : "info"));
        body.setPadding(new Insets(8, 8, 4, 8));
        body.setPrefWidth(420);
        dlg.getDialogPane().setContent(body);
        style(dlg, owner, sheets);
        return dlg.showAndWait().filter(b -> b == ok).isPresent();
    }

    static void error(Window owner, List<String> sheets, String title, String message) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.getDialogPane().getButtonTypes().add(new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE));
        VBox body = new VBox(header(Icons.HELP, title, message == null ? "Đã xảy ra lỗi không xác định." : message, "bad"));
        body.setPadding(new Insets(8, 8, 4, 8));
        body.setPrefWidth(420);
        dlg.getDialogPane().setContent(body);
        style(dlg, owner, sheets);
        dlg.showAndWait();
    }

    static void help(Window owner, List<String> sheets) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Hướng dẫn");
        dlg.getDialogPane().getButtonTypes().add(new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE));

        Label rules = new Label("""
                Mỗi lượt, người chơi chọn một đống và bốc một hay nhiều vật phẩm từ đỉnh đống đó.
                Luật thường: ai bốc vật phẩm cuối cùng thì thắng.
                Luật misère: ai bốc vật phẩm cuối cùng thì thua.""");
        rules.setWrapText(true);
        rules.setMinHeight(Region.USE_PREF_SIZE);
        rules.getStyleClass().add("help-text");

        GridPane keys = new GridPane();
        keys.setHgap(14);
        keys.setVgap(8);
        String[][] rows = {
            { "← →", "Chọn đống" }, { "↑ ↓", "Đổi số lượng bốc" }, { "Enter", "Xác nhận nước đi" },
            { "Esc", "Hủy lựa chọn" }, { "H", "Gợi ý nước đi" }, { "Ctrl+Z", "Hoàn tác" },
            { "Ctrl+N", "Ván mới" }, { "Ctrl+O", "Mở ván" }, { "Ctrl+S", "Lưu ván" }, { "F1", "Mở hướng dẫn" } };
        for (int i = 0; i < rows.length; i++) {
            Label k = new Label(rows[i][0]);
            k.getStyleClass().add("kbd");
            Label d = new Label(rows[i][1]);
            d.getStyleClass().add("help-text");
            keys.add(k, (i % 2) * 2, i / 2);
            keys.add(d, (i % 2) * 2 + 1, i / 2);
        }

        Label c1 = new Label("LUẬT CHƠI");
        c1.getStyleClass().add("field-caption");
        Label c2 = new Label("THAO TÁC");
        c2.getStyleClass().add("field-caption");
        Label tip = new Label("Rê chuột vào vật phẩm để xem trước, bấm để chọn, bấm lần nữa hoặc nhấn Bốc để xác nhận. "
                + "Chọn một vật phẩm nghĩa là bốc nó và mọi vật phẩm phía trên nó.");
        tip.setWrapText(true);
        tip.setMinHeight(Region.USE_PREF_SIZE);
        tip.getStyleClass().add("help-text");

        VBox body = new VBox(14,
                header(Icons.HELP, "Hướng dẫn chơi NIM", "Trò chơi tổ hợp công bằng cho hai người chơi.", "info"),
                new VBox(6, c1, rules), new VBox(6, c2, tip, keys));
        body.setPadding(new Insets(8, 8, 4, 8));
        body.setPrefWidth(520);
        dlg.getDialogPane().setContent(body);
        style(dlg, owner, sheets);
        dlg.showAndWait();
    }
}
