package nim.desktop;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.control.skin.ToggleButtonSkin;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Gắn phản hồi chuyển động cho nút bấm mà không cần tạo lớp con của từng loại nút
 *
 * <p>CSS của JavaFX không có transition, nên màu nền khi hover/nhấn/chọn sẽ nhảy tức thì. Ở đây mỗi
 * nút được thêm các lớp phủ nằm dưới chữ ({@code .wash}, {@code .press}, {@code .on}); CSS chỉ quy định
 * màu của lớp phủ, còn độ mờ của chúng được chạy bằng hoạt ảnh
 */
final class Wash {
    private static final double DISABLED_OPACITY = 0.38;

    private Wash() {
    }

    /** Cài phản hồi cho nút; trả về chính nút để dùng trong biểu thức. */
    static <T extends ButtonBase> T install(T b) {
        if (b instanceof ToggleButton t)
            t.setSkin(new ToggleSkin(t));
        else if (b instanceof Button x)
            x.setSkin(new PlainSkin(x));

        b.setOpacity(b.isDisabled() ? DISABLED_OPACITY : 1);
        b.disabledProperty().addListener((o, was, now) ->
                Motion.to(b, "enabled", b.opacityProperty(), now ? DISABLED_OPACITY : 1, Motion.BASE, Motion.STANDARD));
        b.pressedProperty().addListener((o, was, now) -> {
            double s = now ? 0.965 : 1;
            Motion.to(b, "press", b.scaleXProperty(), s, now ? Duration.millis(90) : Motion.BASE, now ? Motion.STANDARD : Motion.SETTLE);
            Motion.to(b, "pressY", b.scaleYProperty(), s, now ? Duration.millis(90) : Motion.BASE, now ? Motion.STANDARD : Motion.SETTLE);
        });
        return b;
    }

    /** Nút biểu tượng tròn kèm chú thích. */
    static <T extends ButtonBase> T tip(T b, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(450));
        t.setHideDelay(Duration.millis(60));
        b.setTooltip(t);
        return b;
    }

    private static final class Layers {
        final Region wash = layer("wash");
        final Region press = layer("press");
        final Region on = layer("on");

        private static Region layer(String style) {
            Region r = new Region();
            r.getStyleClass().add(style);
            r.setManaged(false);
            r.setMouseTransparent(true);
            r.setOpacity(0);
            return r;
        }

        void attach(javafx.collections.ObservableList<javafx.scene.Node> children, ButtonBase c) {
            children.add(0, on);
            children.add(1, wash);
            children.add(2, press);
            c.hoverProperty().addListener((o, was, now) ->
                    Motion.to(wash, "o", wash.opacityProperty(), now ? 1 : 0, Motion.FAST, Motion.STANDARD));
            c.pressedProperty().addListener((o, was, now) ->
                    Motion.to(press, "o", press.opacityProperty(), now ? 1 : 0, now ? Duration.millis(60) : Motion.BASE, Motion.STANDARD));
        }

        void size(double w, double h) {
            for (Region r : new Region[] { on, wash, press })
                r.resizeRelocate(0, 0, w, h);
        }
    }

    private static final class PlainSkin extends ButtonSkin {
        private final Layers layers = new Layers();

        PlainSkin(Button c) {
            super(c);
            layers.attach(getChildren(), c);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            layers.size(getSkinnable().getWidth(), getSkinnable().getHeight());
            super.layoutChildren(x, y, w, h);
        }
    }

    private static final class ToggleSkin extends ToggleButtonSkin {
        private final Layers layers = new Layers();

        ToggleSkin(ToggleButton c) {
            super(c);
            layers.attach(getChildren(), c);
            layers.on.setOpacity(c.isSelected() ? 1 : 0);
            c.selectedProperty().addListener((o, was, now) ->
                    Motion.to(layers.on, "o", layers.on.opacityProperty(), now ? 1 : 0, Motion.BASE, Motion.STANDARD));
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            layers.size(getSkinnable().getWidth(), getSkinnable().getHeight());
            super.layoutChildren(x, y, w, h);
        }
    }
}
