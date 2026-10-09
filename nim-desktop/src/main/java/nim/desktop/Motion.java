package nim.desktop;

import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Hệ thống chuyển động dùng chung
 *
 * <ul>
 *   <li>{@link #FAST}: phản hồi tức thời </li>
 *   <li>{@link #BASE}: chuyển trạng thái thông thường </li>
 *   <li>{@link #SLOW}: chuyển cảnh lớn </li>
 * </ul>
 *
 * Mỗi Animation được gắn khóa theo (node, key) bắt đầu Animation mới trên cùng khóa sẽ hủy Animation cũ
 */
final class Motion {
    static final Duration FAST = Duration.millis(140);
    static final Duration BASE = Duration.millis(240);
    static final Duration SLOW = Duration.millis(380);

    static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);
    static final Interpolator STANDARD = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
    static final Interpolator EASE_IN = Interpolator.SPLINE(0.4, 0.0, 1.0, 1.0);
    static final Interpolator SETTLE = back(1.25);
    static final Interpolator POP = back(2.4);

    private static final String KEY_PREFIX = "nim.motion.";

    private Motion() {
    }

    static Interpolator back(double overshoot) {
        return new Interpolator() {
            @Override
            protected double curve(double t) {
                double u = t - 1;
                return u * u * ((overshoot + 1) * u + overshoot) + 1;
            }
        };
    }

    // ------------------------------------------------------------------ nền tảng

    /** Animation một thuộc tính đến giá trị đích, hủy Animation cũ cùng khóa trên node */
    static void to(Node owner, String key, WritableValue<Number> prop, double to, Duration d, Interpolator ip) {
        to(owner, key, prop, to, d, ip, Duration.ZERO);
    }

    static void to(Node owner, String key, WritableValue<Number> prop, double to, Duration d, Interpolator ip,
            Duration delay) {
        Timeline t = new Timeline(new KeyFrame(d, new KeyValue(prop, to, ip)));
        t.setDelay(delay);
        play(owner, key, t);
    }

    /** Phát Animation và gắn vào khóa; Animation trước đó cùng khóa bị dừng. */
    static void play(Node owner, String key, Animation a) {
        cancel(owner, key);
        Map<Object, Object> props = owner.getProperties();
        props.put(KEY_PREFIX + key, a);
        a.statusProperty().addListener((o, was, now) -> {
            if (now == Animation.Status.STOPPED && props.get(KEY_PREFIX + key) == a)
                props.remove(KEY_PREFIX + key);
        });
        a.play();
    }

    static void cancel(Node owner, String key) {
        Object old = owner.getProperties().remove(KEY_PREFIX + key);
        if (old instanceof Animation an)
            an.stop();
    }

    // ------------------------------------------------------------------ hiện / ẩn

    /** Hiện node: mờ dần vào, trượt nhẹ từ dưới lên và phóng từ 97% lên 100% */
    static void show(Node n) {
        n.setVisible(true);
        n.setMouseTransparent(false);
        n.setScaleX(Math.min(n.getScaleX(), 0.97));
        n.setScaleY(Math.min(n.getScaleY(), 0.97));
        if (n.getOpacity() >= 1)
            n.setOpacity(0);
        if (n.getTranslateY() == 0)
            n.setTranslateY(6);
        Timeline t = new Timeline(new KeyFrame(BASE,
                new KeyValue(n.opacityProperty(), 1, EASE_OUT),
                new KeyValue(n.translateYProperty(), 0, EASE_OUT),
                new KeyValue(n.scaleXProperty(), 1, EASE_OUT),
                new KeyValue(n.scaleYProperty(), 1, EASE_OUT)));
        play(n, "visibility", t);
    }

    /** Ẩn node: mờ dần, trượt nhẹ lên trên, rồi mới tắt visible. */
    static void hide(Node n) {
        if (!n.isVisible())
            return;
        n.setMouseTransparent(true);
        Timeline t = new Timeline(new KeyFrame(FAST,
                new KeyValue(n.opacityProperty(), 0, EASE_IN),
                new KeyValue(n.translateYProperty(), -4, EASE_IN),
                new KeyValue(n.scaleXProperty(), 0.98, EASE_IN),
                new KeyValue(n.scaleYProperty(), 0.98, EASE_IN)));
        t.setOnFinished(e -> n.setVisible(false));
        play(n, "visibility", t);
    }

    /** Mờ dần vào, không dịch chuyển. */
    static void fadeIn(Node n, Duration d) {
        n.setVisible(true);
        if (n.getOpacity() >= 1)
            n.setOpacity(0);
        to(n, "fade", n.opacityProperty(), 1, d, STANDARD);
    }

    /** Hiện/ẩn theo kiểu panel bên, trượt ngang + mờ, đồng thời bật/tắt managed để bố cục co giãn */
    static void reveal(Node n, boolean on, double slide) {
        if (on) {
            n.setVisible(true);
            n.setOpacity(0);
            n.setTranslateX(slide);
            Timeline t = new Timeline(new KeyFrame(SLOW,
                    new KeyValue(n.opacityProperty(), 1, EASE_OUT),
                    new KeyValue(n.translateXProperty(), 0, EASE_OUT)));
            play(n, "reveal", t);
        } else {
            Timeline t = new Timeline(new KeyFrame(BASE,
                    new KeyValue(n.opacityProperty(), 0, EASE_IN),
                    new KeyValue(n.translateXProperty(), slide, EASE_IN)));
            t.setOnFinished(e -> n.setVisible(false));
            play(n, "reveal", t);
        }
    }

    // ------------------------------------------------------------------ đổi nội dung

    /** Đổi chữ bằng chuyển mờ chéo, chữ cũ mờ đi, chữ mới trượt lên. Bỏ qua nếu không đổi */
    static void swapText(Labeled l, String text) {
        if (text == null)
            text = "";
        if (text.equals(l.getText()))
            return;
        final String next = text;
        if (!l.isVisible() || l.getScene() == null) {
            l.setText(next);
            return;
        }
        Timeline out = new Timeline(new KeyFrame(Duration.millis(90),
                new KeyValue(l.opacityProperty(), 0, EASE_IN),
                new KeyValue(l.translateYProperty(), -5, EASE_IN)));
        out.setOnFinished(e -> {
            l.setText(next);
            l.setTranslateY(6);
            Timeline in = new Timeline(new KeyFrame(BASE,
                    new KeyValue(l.opacityProperty(), 1, EASE_OUT),
                    new KeyValue(l.translateYProperty(), 0, EASE_OUT)));
            play(l, "swap", in);
        });
        play(l, "swap", out);
    }

    /** Nhấn mạnh nhanh: phóng lên rồi về, dùng cho điểm số và huy hiệu vừa đổi */
    static void pop(Node n) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(n.scaleXProperty(), 0.82), new KeyValue(n.scaleYProperty(), 0.82)),
                new KeyFrame(Duration.millis(380), new KeyValue(n.scaleXProperty(), 1, POP),
                        new KeyValue(n.scaleYProperty(), 1, POP)));
        play(n, "pop", t);
    }

    // ------------------------------------------------------------------ thu gọn / mở rộng

    /**
     * Thu gọn hoặc mở rộng một vùng theo chiều cao, dùng cho các khối tùy chọn trong hộp thoại
     * {@code onFrame} được gọi mỗi khung hình để cửa sổ chứa có thể co giãn theo.
     */
    static void collapse(Region box, boolean show, boolean animate, Runnable onFrame) {
        double width = box.getWidth() > 0 ? box.getWidth()
                : box.getParent() instanceof Region r ? r.getWidth() - r.getInsets().getLeft() - r.getInsets().getRight() : 0;
        if (!animate || width <= 0 || box.getScene() == null) {
            box.setVisible(show);
            box.setManaged(show);
            box.setMaxHeight(Region.USE_COMPUTED_SIZE);
            box.setOpacity(1);
            box.setClip(null);
            if (onFrame != null)
                onFrame.run();
            return;
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(box.widthProperty());
        clip.heightProperty().bind(box.heightProperty());

        final double oldMin = box.getMinHeight();
        double full = box.prefHeight(width);
        if (show) {
            box.setMinHeight(0);
            box.setMaxHeight(0);
            box.setOpacity(0);
            box.setManaged(true);
            box.setVisible(true);
        } else {
            full = Math.max(box.getHeight(), 1);
            box.setMinHeight(0);
            box.setMaxHeight(full);
        }
        box.setClip(clip);
        javafx.beans.value.ChangeListener<Number> follow = (o, a, b) -> {
            if (onFrame != null)
                onFrame.run();
        };
        box.heightProperty().addListener(follow);
        Timeline t = new Timeline(new KeyFrame(BASE,
                new KeyValue(box.maxHeightProperty(), show ? full : 0, STANDARD),
                new KeyValue(box.opacityProperty(), show ? 1 : 0, STANDARD)));
        t.setOnFinished(e -> {
            box.heightProperty().removeListener(follow);
            box.setClip(null);
            box.setMinHeight(oldMin);
            box.setMaxHeight(Region.USE_COMPUTED_SIZE);
            if (!show) {
                box.setVisible(false);
                box.setManaged(false);
            }
            if (onFrame != null)
                onFrame.run();
        });
        play(box, "collapse", t);
    }

    /** Gộp nhiều Animation chạy song song. */
    static ParallelTransition together(Animation... animations) {
        return new ParallelTransition(animations);
    }
}
