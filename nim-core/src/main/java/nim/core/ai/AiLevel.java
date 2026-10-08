package nim.core.ai;

import java.util.Random;

/**
 * Các mức độ khó dành cho người chơi
 */
public enum AiLevel {
    EASY("Dễ", "Đi ngẫu nhiên"),
    MEDIUM("Vừa", "Chơi tối ưu nhưng sai khoảng 30% số lượt"),
    HARD("Khó", "Luôn đi nước tối ưu theo định lý Bouton");

    private final String label;
    private final String description;

    AiLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public AiStrategy create(Random random) {
        return switch (this) {
            case EASY -> new RandomAi(random);
            case MEDIUM -> new OptimalAi(0.30, random);
            case HARD -> new OptimalAi(0.0, random);
        };
    }
}
