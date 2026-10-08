package nim.desktop;

import java.util.List;
import java.util.function.IntFunction;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import nim.core.Match;
import nim.core.Move;

final class HistoryPanel extends VBox {
    private final ListView<String> list = new ListView<>();
    private final Label footer = new Label();

    HistoryPanel() {
        super(10);
        getStyleClass().add("side-card");
        setPrefWidth(236);
        setMinWidth(236);
        setMaxWidth(236);
        Label title = new Label("NƯỚC ĐI");
        title.getStyleClass().add("field-caption");
        list.setPlaceholder(new Label("Chưa có nước đi nào"));
        list.setFocusTraversable(false);
        VBox.setVgrow(list, Priority.ALWAYS);
        footer.getStyleClass().add("field-hint");
        getChildren().addAll(title, list, footer);
    }

    void update(Match match, IntFunction<String> seatName) {
        List<Move> moves = match.session().history();
        list.getItems().clear();
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            list.getItems().add((i + 1) + ".  " + seatName.apply(match.moverOf(i))
                    + "  ·  đống " + (m.heapIndex() + 1) + ", bốc " + m.count());
        }
        if (!moves.isEmpty())
            list.scrollTo(moves.size() - 1);
        footer.setText("Gợi ý đã dùng: " + match.hintsUsed());
    }
}
