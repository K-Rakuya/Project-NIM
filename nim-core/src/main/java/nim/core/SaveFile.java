package nim.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Định dạng văn bản phiên bản 1, không phụ thuộc thư viện ngoài.
 * Sang giai đoạn client-server sẽ thay bằng JSON.
 *
 * nim-save 1
 * misere=false
 * initial=3,5,7
 * moves=0:2,1:3
 */

public final class SaveFile {

    private SaveFile() {
    }

    //save session
    public static void save(GameSession session, Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("nim-save 1\n");
        sb.append("misere=").append(session.initialState().isMisere()).append('\n');

        /// lưu heaps
        int[] heaps = session.initialState().heaps();
        sb.append("initial=");
        for (int i = 0; i < heaps.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(heaps[i]);
        }
        sb.append('\n');

        // lưu move
        List<Move> moves = session.history();
        sb.append("moves=");
        for (int i = 0; i < heaps.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(moves.get(i).heapIndex()).append(':').append(moves.get(i).count());
        }
        sb.append('\n');

        if (file.getParent() != null)
            Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    //load session
    public static GameSession load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if(lines.isEmpty() || !lines.get(0).startsWith("nim-save 1"))
            throw new IOException("File không hợp lệ");

        //cắt string từng dòng
        boolean misere = false;
        int[] initial = null;
        String movesLine = "";

        for (String line : lines.subList(1, lines.size())) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            switch (key) {
                case "misere"   ->  misere = Boolean.parseBoolean(value);
                case "initial"  ->  initial = parseInts(value);
                case "moves"    ->  movesLine = value;
                default         ->  {}
            }
        }

        // quăng lỗi do null ko thể làm giá trị mặc định như các biến khác
        if (initial == null) throw new IOException("File thiếu dòng initial=");

        //cắt string lấy move
        GameSession session = new GameSession(new GameState(initial, 0, misere));
        if (!movesLine.isEmpty()) {
            for (String token : movesLine.split(",")) {
                String[] p = token.split(":");
                if (p.length != 2) throw new IOException("Nước đi lỗi: " + token);
                session.play(new Move(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())));
            }
        }
        
        return session;
    }

    // ParseInt nè
    private static int[] parseInts(String csv) {
        String[] parts = csv.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }
}