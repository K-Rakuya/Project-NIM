package nim.desktop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import nim.core.GameConfig;
import nim.core.Match;
import nim.core.Move;
import nim.core.SaveFile;

public class NimApp extends Application {
    private static final Path AUTOSAVE = Path.of(System.getProperty("user.home"), ".nim", "autosave.nim");

    private final Random random = new Random();
    private final BoardView board = new BoardView();
    private final HistoryPanel history = new HistoryPanel();
    private final Label subtitle = new Label();
    private final Button undoBtn = button("Hoàn tác", "ghost");
    private final Button hintBtn = button("Gợi ý", "ghost");
    private final Button saveBtn = button("Lưu", "ghost");
    private Stage stage;
    private Scene scene;
    private final LearnPanel learn = new LearnPanel();
    private final ToggleButton learnBtn = new ToggleButton("Phân tích");
    private final Button themeBtn = button("", "ghost");
    private final int[] wins = new int[2];
    private int scored = -2;
    private boolean dark = Prefs.dark();
    private GameConfig config = Prefs.config();
    private Match match;
    private boolean inputOpen;
    private int token;
    private PauseTransition aiDelay;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        Label logo = new Label("NIM");
        logo.getStyleClass().add("logo");
        subtitle.getStyleClass().add("subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newGame = button("Ván mới", "primary");
        newGame.setOnAction(e -> promptNewGame());
        Button open = button("Mở", "ghost");
        open.setOnAction(e -> openGame());
        saveBtn.setOnAction(e -> saveGame());
        undoBtn.setOnAction(e -> undo());
        hintBtn.setOnAction(e -> hint());

        learnBtn.getStyleClass().addAll("btn", "ghost");
        learnBtn.setSelected(Prefs.learn());
        learnBtn.setFocusTraversable(false);
        learnBtn.setOnAction(e -> {
            Prefs.setLearn(learnBtn.isSelected());
            learn.setVisible(learnBtn.isSelected());
        });
        learn.managedProperty().bind(learn.visibleProperty());
        learn.setVisible(learnBtn.isSelected());
        themeBtn.setOnAction(e -> {
            dark = !dark;
            Prefs.setDark(dark);
            applyTheme();
        });

        HBox header = new HBox(6, logo, subtitle, spacer, hintBtn, undoBtn, saveBtn, open, learnBtn, themeBtn, newGame);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 10, 24));
        subtitle.setPadding(new Insets(0, 0, 0, 6));

        BorderPane root = new BorderPane();
        root.setTop(header);
        VBox side = new VBox(14, learn, history);
        side.setPrefWidth(236);
        VBox.setVgrow(history, Priority.ALWAYS);
        HBox center = new HBox(18, board, side);
        HBox.setHgrow(board, Priority.ALWAYS);
        center.setPadding(new Insets(8, 24, 24, 24));
        root.setCenter(center);

        scene = new Scene(root, 1180, 760);
        applyTheme();
        installShortcuts();

        stage.setTitle("NIM");
        stage.setMinWidth(1060);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();

        board.setOnMove(this::onHumanMove);
        if (!restoreAutosave())
            newGame();
    }

    private void applyTheme() {
        scene.getStylesheets().setAll(stylesheets());
        themeBtn.setText(dark ? "Sáng" : "Tối");
    }

    private List<String> stylesheets() {
        return List.of(NimApp.class.getResource(dark ? "theme-dark.css" : "theme-light.css").toExternalForm(),
                NimApp.class.getResource("app.css").toExternalForm());
    }

    private static Button button(String text, String style) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", style);
        return b;
    }

    private void installShortcuts() {
        var acc = scene.getAccelerators();
        acc.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::promptNewGame);
        acc.put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::openGame);
        acc.put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::saveGame);
        acc.put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::undo);
        acc.put(new KeyCodeCombination(KeyCode.H), this::hint);
    }

    private void promptNewGame() {
        Optional<GameConfig> picked = NewGameDialog.show(stage, config, stylesheets());
        picked.ifPresent(c -> {
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
        refreshSubtitle();
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

    private void refreshSubtitle() {
        GameConfig c = match.config();
        subtitle.setText(c.mode().label() + "  ·  " + (c.misere() ? "Luật misère" : "Luật thường")
                + (c.mode() == GameConfig.Mode.VS_AI ? "  ·  Máy: " + c.level().label() : ""));
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
            board.setStatus("Ván đã kết thúc");
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
            board.setStatus("Máy đang suy nghĩ…");
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
            board.setStatus("Lượt của " + seatName(cur) + " — chọn một vật phẩm, bạn sẽ bốc nó và mọi vật phẩm phía trên");
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
            board.setStatus("Gợi ý: đống " + (h.get().heapIndex() + 1) + ", bốc " + h.get().count());
        } else {
            board.clearHint();
            board.setStatus("Thế này đang bất lợi: nếu đối thủ chơi tối ưu thì không còn nước thắng. Hãy kéo dài ván.");
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
            board.setStatus("Đã lưu: " + p.getFileName());
        } catch (IOException e) {
            error("Không lưu được", e.getMessage());
        }
    }

    private void openGame() {
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
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.initOwner(stage);
        a.setTitle(title);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().addAll(stylesheets());
        a.showAndWait();
    }

    private void autosave() {
        try {
            if (match.isOver()) {
                Files.deleteIfExists(AUTOSAVE);
            } else {
                SaveFile.save(match, AUTOSAVE);
            }
        } catch (IOException ignored) {
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
            board.setStatus("Đã khôi phục ván đang chơi dở");
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
