package nim.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nim.core.ai.AiLevel;

/**
 * Định dạng văn bản phiên bản 1, không phụ thuộc thư viện ngoài.
 * Sang giai đoạn client-server sẽ thay bằng JSON.
 *
 * nim-save 1
 * misere=false
 * initial=3,5,7
 * moves=0:2,1:3
 */

/**
 * SaveFile
 */
public final class SaveFile {

    private SaveFile() {
    }

    /**
     * Save session game dưới dạng String vào một file
     * @param session
     * @param file
     * @throws IOException
     */
    public static void save(GameSession session, Path file) throws IOException {
        write(session, "", file);
    }

    /**
     * Lưu cả thông tin cấu hình (chế độ, mức máy, ghế người chơi) dưới dạng các
     * khóa phụ. Header vẫn là {@code nim-save 1} và các khóa lạ
     */
    public static void save(Match match, Path file) throws IOException {
        GameConfig c = match.config();
        String extras = "mode=" + c.mode().name() + "\n"
                + "ai=" + c.level().name() + "\n"
                + "human=" + c.humanSeat() + "\n";
        write(match.session(), extras, file);
    }

    private static void write(GameSession session, String extras, Path file) throws IOException {
        // Tạo đối tượng StringBuilder do chuỗi hay thay đổi
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
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0)
                sb.append(',');
            sb.append(moves.get(i).heapIndex()).append(':').append(moves.get(i).count());
        }
        sb.append('\n');
        sb.append(extras);

        // Check thư mục cha chưa tồn tại thì tạo luôn tránh gây lỗi
        if (file.getParent() != null)
            Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    //load session
    public static GameSession load(Path file) throws IOException {
        return parse(file).session();
    }

    /** Tải ván kèm cấu hình; thiếu khóa phụ được hiểu là người vs người. */
    public static Match loadMatch(Path file, Random random) throws IOException {
        Parsed p = parse(file);
        GameConfig.Mode mode = GameConfig.Mode.VS_HUMAN;
        AiLevel level = AiLevel.MEDIUM;
        int human = 0;
        try {
            if (p.extras().containsKey("mode"))
                mode = GameConfig.Mode.valueOf(p.extras().get("mode"));
            if (p.extras().containsKey("ai"))
                level = AiLevel.valueOf(p.extras().get("ai"));
            if (p.extras().containsKey("human"))
                human = Integer.parseInt(p.extras().get("human"));
        } catch (IllegalArgumentException e) {
            throw new IOException("Cấu hình trong file không hợp lệ: " + e.getMessage());
        }

        GameState init = p.session().initialState();
        int max = 1;
        for (int h : init.heaps())
            max = Math.max(max, h);
        try {
            GameConfig cfg = new GameConfig(mode, init.isMisere(), init.heapCount(),
                    Math.min(max, GameConfig.MAX_ITEMS), GameConfig.Opening.RANDOM, level, human);
            return Match.resume(cfg, p.session(), random);
        } catch (IllegalArgumentException e) {
            throw new IOException("Ván lưu vượt giới hạn giao diện: " + e.getMessage());
        }
    }

    private record Parsed(GameSession session, Map<String, String> extras) {
    }

    private static Parsed parse(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if(lines.isEmpty() || !lines.get(0).startsWith("nim-save 1"))
            throw new IOException("File không hợp lệ");

        
        boolean misere = false;
        int[] initial = null;
        String movesLine = "";
        Map<String, String> extras = new HashMap<>();
        
        //cắt string từng dòng
        for (String line : lines.subList(1, lines.size())) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            switch (key) {
                case "misere"   ->  misere = Boolean.parseBoolean(value);
                case "initial"  ->  initial = parseInts(value);
                case "moves"    ->  movesLine = value;
                default         ->  extras.put(key, value);
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
        
        return new Parsed(session, extras);
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