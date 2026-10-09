package nim.desktop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Ellipse;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import nim.core.GameConfig;
import nim.core.Match;
import nim.core.Move;
import nim.core.SaveFile;

public class NimApp extends Application {
    private static final Path AUTOSAVE = Path.of(System.getProperty("user.home"), ".nim", "autosave.nim");
    private static final PseudoClass KEYBOARD = PseudoClass.getPseudoClass("kb");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final Random random = new Random();
    private final BoardView board = new BoardView();
    private final HistoryPanel history = new HistoryPanel();
    private final HBox chips = new HBox(6);
    private final Button undoBtn = icon(Icons.UNDO, "Hoàn tác  (Ctrl+Z)");
    private final Button hintBtn = icon(Icons.HINT, "Gợi ý nước đi  (H)");
    private final Button saveBtn = icon(Icons.SAVE, "Lưu ván  (Ctrl+S)");
    private final Button openBtn = icon(Icons.OPEN, "Mở ván  (Ctrl+O)");
    private final Button helpBtn = icon(Icons.HELP, "Hướng dẫn  (F1)");
    private final Button themeBtn = icon(Icons.MOON, "");
    private final ToggleButton learnBtn = Wash.tip(Wash.install(new ToggleButton()), "Phân tích nim-sum");
    private final LearnPanel learn = new LearnPanel();
    private final Label saveState = new Label();
    private final Region saveDot = new Region();

    private Stage stage;
    private Scene scene;
    private StackPane rootStack;
    private BorderPane shell;
    private HBox header;
    private VBox side;
    private HBox footer;

    private final int[] wins = new int[2];
    private int scored = -2;
    private boolean dark = Prefs.dark();
    private GameConfig config = Prefs.config();
    private Match match;
    private boolean inputOpen;
    private int token;
    private PauseTransition aiDelay;

    private static Button icon(String path, String tip) {
        Button b = Wash.install(new Button());
        b.setGraphic(Icons.of(path));
        b.getStyleClass().addAll("btn", "ghost", "icon-btn");
        b.setFocusTraversable(true);
        if (!tip.isEmpty())
            Wash.tip(b, tip);
        return b;
    }

    private static Button button(String text, String style) {
        Button b = Wash.install(new Button(text));
        b.getStyleClass().addAll("btn", style);
        return b;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // ---- thanh tiêu đề: thương hiệu + thông tin ván bên trái, thao tác bên phải
        Node brand = brand();
        chips.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newGame = button("Ván mới", "primary");
        newGame.setGraphic(Icons.of(Icons.ADD, 16));
        newGame.getStyleClass().add("with-icon");
        Wash.tip(newGame, "Tạo ván mới  (Ctrl+N)");
        newGame.setOnAction(e -> promptNewGame());
        openBtn.setOnAction(e -> openGame());
        saveBtn.setOnAction(e -> saveGame());
        undoBtn.setOnAction(e -> undo());
        hintBtn.setOnAction(e -> hint());
        helpBtn.setOnAction(e -> Dialogs.help(stage, stylesheets()));

        learnBtn.getStyleClass().addAll("btn", "ghost", "icon-btn");
        learnBtn.setGraphic(Icons.of(Icons.CHART));
        learnBtn.setFocusTraversable(true);
        learnBtn.setSelected(Prefs.learn());
        learnBtn.setOnAction(e -> {
            Prefs.setLearn(learnBtn.isSelected());
            Motion.collapse(learn, learnBtn.isSelected(), true, null);
        });
        learn.setVisible(learnBtn.isSelected());
        learn.setManaged(learnBtn.isSelected());
        themeBtn.setOnAction(e -> {
            dark = !dark;
            Prefs.setDark(dark);
            applyTheme(true);
        });

        header = new HBox(10, brand, chips, spacer,
                group(hintBtn, undoBtn), sep(), group(saveBtn, openBtn), sep(), group(learnBtn, themeBtn, helpBtn), sep(), newGame);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // ---- vùng chơi + thanh bên
        side = new VBox(14, learn, history);
        side.setPrefWidth(236);
        VBox.setVgrow(history, Priority.ALWAYS);
        HBox center = new HBox(18, board, side);
        HBox.setHgrow(board, Priority.ALWAYS);
        center.setPadding(new Insets(18, 24, 14, 24));

        // ---- thanh chân trang: gợi ý phím + trạng thái tự lưu
        footer = buildFooter();

        shell = new BorderPane();
        shell.setTop(header);
        shell.setCenter(center);
        shell.setBottom(footer);
        rootStack = new StackPane(shell);
        rootStack.getStyleClass().add("app-root");

        scene = new Scene(rootStack, 1180, 780);
        applyTheme(false);
        installShortcuts();
        installFocusRing();

        stage.setTitle("NIM");
        stage.setMinWidth(1060);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        board.setOnMove(this::onHumanMove);
        if (!restoreAutosave())
            newGame();
        playIntro();
    }

    // ------------------------------------------------------------------ dựng giao diện

    private Node brand() {
        Group mark = new Group();
        for (int i = 0; i < 3; i++) {
            Ellipse e = new Ellipse(0, 0, 11, 4.2);
            e.setLayoutY(-5 + i * 5.2);
            e.getStyleClass().add(i == 0 ? "brand-disc-top" : "brand-disc");
            mark.getChildren().add(e);
        }
        StackPane tile = new StackPane(mark);
        tile.getStyleClass().add("brand-tile");
        tile.setMinSize(34, 34);
        tile.setMaxSize(34, 34);
        Label logo = new Label("NIM");
        logo.getStyleClass().add("logo");
        HBox b = new HBox(10, tile, logo);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(0, 10, 0, 0));
        return b;
    }

    private static Node group(Node... n) {
        HBox g = new HBox(2, n);
        g.setAlignment(Pos.CENTER);
        return g;
    }

    private static Node sep() {
        Region r = new Region();
        r.getStyleClass().add("vsep");
        r.setMinSize(1, 22);
        r.setMaxSize(1, 22);
        return r;
    }

    private HBox buildFooter() {
        Label hints = new Label("Rê chuột để xem trước  ·  Bấm để chọn  ·  Enter xác nhận  ·  Esc hủy chọn");
        hints.getStyleClass().add("footer-text");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        saveDot.getStyleClass().add("save-dot");
        saveState.getStyleClass().add("footer-text");
        saveState.setText("Chưa có ván đang chơi");
        HBox f = new HBox(8, hints, grow, saveDot, saveState);
        f.setAlignment(Pos.CENTER_LEFT);
        f.getStyleClass().add("footer");
        return f;
    }

    /** Vào màn hình: các khối trượt vào lần lượt thay vì hiện ra cùng lúc. */
    private void playIntro() {
        Node[] parts = { header, board, side, footer };
        double[] dy = { -12, 14, 14, 8 };
        for (int i = 0; i < parts.length; i++) {
            Node n = parts[i];
            n.setOpacity(0);
            n.setTranslateY(dy[i]);
            Timeline t = new Timeline(new KeyFrame(Duration.millis(520),
                    new KeyValue(n.opacityProperty(), 1, Motion.EASE_OUT),
                    new KeyValue(n.translateYProperty(), 0, Motion.EASE_OUT)));
            t.setDelay(Duration.millis(70L * i));
            Motion.play(n, "intro", t);
        }
    }

    // ------------------------------------------------------------------ giao diện sáng / tối

    private void applyTheme(boolean animate) {
        ImageView frozen = null;
        if (animate && rootStack.getWidth() > 0) {
            // Chụp khung hình hiện tại, đổi theme bên dưới, rồi mờ ảnh chụp đi => chuyển theme mờ chéo.
            double sx = stage.getOutputScaleX();
            double sy = stage.getOutputScaleY();
            SnapshotParameters sp = new SnapshotParameters();
            sp.setTransform(Transform.scale(sx, sy));
            WritableImage img = rootStack.snapshot(sp, null);
            frozen = new ImageView(img);
            frozen.setFitWidth(rootStack.getWidth());
            frozen.setFitHeight(rootStack.getHeight());
            frozen.setMouseTransparent(true);
            rootStack.getChildren().add(frozen);
        }
        scene.getStylesheets().setAll(stylesheets());
        themeBtn.setGraphic(Icons.of(dark ? Icons.SUN : Icons.MOON));
        Wash.tip(themeBtn, dark ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối");
        if (frozen != null) {
            final ImageView f = frozen;
            Timeline t = new Timeline(new KeyFrame(Duration.millis(420), new KeyValue(f.opacityProperty(), 0, Motion.STANDARD)));
            t.setOnFinished(e -> rootStack.getChildren().remove(f));
            t.play();
            Motion.pop(themeBtn.getGraphic());
        }
    }

    private List<String> stylesheets() {
        return List.of(NimApp.class.getResource(dark ? "theme-dark.css" : "theme-light.css").toExternalForm(),
                NimApp.class.getResource("app.css").toExternalForm());
    }

    /** Viền focus chỉ hiện khi người dùng đang thao tác bằng bàn phím, không hiện sau mỗi cú nhấp chuột. */
    private void installFocusRing() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB)
                rootStack.pseudoClassStateChanged(KEYBOARD, true);
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> rootStack.pseudoClassStateChanged(KEYBOARD, false));
    }

    private void installShortcuts() {
        var acc = scene.getAccelerators();
        acc.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::promptNewGame);
        acc.put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::openGame);
        acc.put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::saveGame);
        acc.put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::undo);
        acc.put(new KeyCodeCombination(KeyCode.H), this::hint);
        acc.put(new KeyCodeCombination(KeyCode.F1), () -> Dialogs.help(stage, stylesheets()));
    }

    // ------------------------------------------------------------------ quy trình ván chơi

    private boolean inProgress() {
        return match != null && !match.isOver() && !match.session().history().isEmpty();
    }

    private boolean confirmDiscard(String action) {
        if (!inProgress())
            return true;
        return Dialogs.confirm(stage, stylesheets(), "Bỏ ván đang chơi?",
                "Ván hiện tại chưa kết thúc. " + action + " sẽ thay thế ván này; bạn có thể lưu trước bằng Ctrl+S.",
                "Bỏ ván", true);
    }

    private void promptNewGame() {
        Optional<GameConfig> picked = NewGameDialog.show(stage, config, stylesheets());
        picked.ifPresent(c -> {
            if (!confirmDiscard("Ván mới"))
                return;
            config = c;
            Prefs.setConfig(c);
            newGame();
        });
    }

    private void newGame() {
        try {
            begin(Match.start(config, random));
        } catch (IllegalStateException e) {
            begin(Match.start(config.withOpening(GameConfig.Opening.RANDOM), random));
        }
    }

    private void begin(Match m) {
        cancelPending();
        if (match != null && match.config().mode() != m.config().mode())
            wins[0] = wins[1] = 0;
        match = m;
        scored = m.isOver() ? -1 : -2;
        board.setup(match.state(), match.config().maxItems(), true);
        configureSeats();
        refreshChips();
        refreshScores();
        afterMove();
    }

    private void configureSeats() {
        GameConfig c = match.config();
        if (c.mode() == GameConfig.Mode.VS_AI) {
            board.setSeat(c.humanSeat(), "Bạn", "Người chơi");
            board.setSeat(1 - c.humanSeat(), "Máy", "Mức " + c.level().label());
            board.setBottomSeat(c.humanSeat());
        } else {
            board.setSeat(0, "Người chơi 1", "Đi trước");
            board.setSeat(1, "Người chơi 2", "Đi sau");
            board.setBottomSeat(0);
        }
    }

    private int scoreIndex(int seat) {
        GameConfig c = match.config();
        return c.mode() == GameConfig.Mode.VS_AI ? (seat == c.humanSeat() ? 0 : 1) : seat;
    }

    private void refreshScores() {
        board.setScore(0, wins[scoreIndex(0)]);
        board.setScore(1, wins[scoreIndex(1)]);
    }

    private String seatName(int seat) {
        GameConfig c = match.config();
        if (c.mode() == GameConfig.Mode.VS_AI)
            return seat == c.humanSeat() ? "Bạn" : "Máy";
        return "Người chơi " + (seat + 1);
    }

    private void refreshChips() {
        GameConfig c = match.config();
        String[] texts = {
            c.mode().label(),
            c.misere() ? "Luật misère" : "Luật thường",
            c.mode() == GameConfig.Mode.VS_AI ? "Máy: " + c.level().label() : null };
        chips.getChildren().clear();
        for (int i = 0; i < texts.length; i++) {
            if (texts[i] == null)
                continue;
            Label chip = new Label(texts[i]);
            chip.getStyleClass().add("chip");
            chips.getChildren().add(chip);
            chip.setOpacity(0);
            Timeline t = new Timeline(new KeyFrame(Motion.BASE, new KeyValue(chip.opacityProperty(), 1, Motion.STANDARD)));
            t.setDelay(Duration.millis(60L * i));
            Motion.play(chip, "in", t);
        }
        stage.setTitle("NIM  —  " + c.mode().label() + (c.misere() ? "  ·  misère" : ""));
    }

    private void onHumanMove(Move m) {
        if (!match.isHumanTurn())
            return;
        setInputOpen(false);
        int t = token;
        match.play(m);
        board.playMove(m, false, () -> {
            if (t == token)
                afterMove();
        });
    }

    private void afterMove() {
        history.update(match, this::seatName);
        learn.update(match.state());
        autosave();
        if (match.isOver()) {
            if (scored == -2) {
                scored = scoreIndex(match.winner());
                wins[scored]++;
            }
            refreshScores();
            setInputOpen(false);
            board.setActiveSeat(-1);
            board.setStatus("Ván đã kết thúc", StatusBar.Kind.DONE);
            board.showResult(seatName(match.winner()) + " thắng",
                    match.config().misere() ? "Luật misère: ai bốc vật phẩm cuối cùng thì thua"
                            : "Luật thường: ai bốc vật phẩm cuối cùng thì thắng",
                    this::promptNewGame);
            return;
        }
        int cur = match.state().currentPlayer();
        board.setActiveSeat(cur);
        if (match.isAiTurn()) {
            setInputOpen(false);
            board.setStatus("Máy đang suy nghĩ", StatusBar.Kind.THINKING);
            int t = token;
            aiDelay = new PauseTransition(Duration.millis(650));
            aiDelay.setOnFinished(e -> {
                if (t != token)
                    return;
                Move m = match.playAi();
                board.playMove(m, true, () -> {
                    if (t == token)
                        afterMove();
                });
            });
            aiDelay.play();
        } else {
            setInputOpen(true);
            board.setStatus("Lượt của " + seatName(cur) + " — chọn một vật phẩm, bạn sẽ bốc nó và mọi vật phẩm phía trên",
                    StatusBar.Kind.TURN);
        }
    }

    private void setInputOpen(boolean open) {
        inputOpen = open;
        board.setInputEnabled(open);
        undoBtn.setDisable(!(open || match.isOver()) || !match.canUndo());
        hintBtn.setDisable(!open);
        saveBtn.setDisable(match == null);
    }

    private void cancelPending() {
        token++;
        if (aiDelay != null) {
            aiDelay.stop();
            aiDelay = null;
        }
    }

    private void hint() {
        if (!inputOpen)
            return;
        Optional<Move> h = match.hint();
        history.update(match, this::seatName);
        if (h.isPresent()) {
            board.showHint(h.get());
            board.setStatus("Gợi ý: đống " + (h.get().heapIndex() + 1) + ", bốc " + h.get().count(), StatusBar.Kind.HINT);
        } else {
            board.clearHint();
            board.setStatus("Thế này đang bất lợi: nếu đối thủ chơi tối ưu thì không còn nước thắng. Hãy kéo dài ván.",
                    StatusBar.Kind.HINT);
        }
    }

    private void undo() {
        if (!inputOpen && !(match != null && match.isOver()))
            return;
        if (!match.canUndo())
            return;
        cancelPending();
        if (scored >= 0) {
            wins[scored]--;
            scored = -2;
        }
        match.undo();
        board.setup(match.state(), match.config().maxItems(), false);
        afterMove();
    }

    // ------------------------------------------------------------------ lưu / mở

    private FileChooser chooser(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ván NIM (*.nim)", "*.nim"));
        File dir = new File("saves");
        if (dir.isDirectory())
            fc.setInitialDirectory(dir);
        return fc;
    }

    private void saveGame() {
        if (match == null || !(inputOpen || match.isOver()))
            return;
        FileChooser fc = chooser("Lưu ván");
        fc.setInitialFileName("van-nim.nim");
        File f = fc.showSaveDialog(stage);
        if (f == null)
            return;
        Path p = f.getName().endsWith(".nim") ? f.toPath() : Path.of(f.getPath() + ".nim");
        try {
            SaveFile.save(match, p);
            board.setStatus("Đã lưu: " + p.getFileName(), StatusBar.Kind.INFO);
        } catch (IOException e) {
            error("Không lưu được", e.getMessage());
        }
    }

    private void openGame() {
        if (!confirmDiscard("Mở ván khác"))
            return;
        File f = chooser("Mở ván").showOpenDialog(stage);
        if (f == null)
            return;
        try {
            Match loaded = SaveFile.loadMatch(f.toPath(), random);
            config = loaded.config();
            begin(loaded);
        } catch (IOException | RuntimeException e) {
            error("Không mở được ván", e.getMessage());
        }
    }

    private void error(String title, String msg) {
        Dialogs.error(stage, stylesheets(), title, msg);
    }

    private void autosave() {
        try {
            if (match.isOver()) {
                Files.deleteIfExists(AUTOSAVE);
                Motion.swapText(saveState, "Ván đã kết thúc");
                saveDot.getStyleClass().remove("on");
            } else {
                SaveFile.save(match, AUTOSAVE);
                Motion.swapText(saveState, "Đã tự lưu lúc " + LocalTime.now().format(CLOCK));
                if (!saveDot.getStyleClass().contains("on"))
                    saveDot.getStyleClass().add("on");
                Motion.pop(saveDot);
            }
        } catch (IOException ignored) {
            Motion.swapText(saveState, "Không thể tự lưu");
        }
    }

    private boolean restoreAutosave() {
        if (!Files.exists(AUTOSAVE))
            return false;
        try {
            Match m = SaveFile.loadMatch(AUTOSAVE, random);
            if (m.isOver() || m.session().history().isEmpty())
                return false;
            config = m.config();
            begin(m);
            board.setStatus("Đã khôi phục ván đang chơi dở", StatusBar.Kind.INFO);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
