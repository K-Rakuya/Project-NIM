package nim.desktop;

import java.util.prefs.Preferences;

import nim.core.GameConfig;
import nim.core.ai.AiLevel;

final class Prefs {
    private static final Preferences P = Preferences.userNodeForPackage(Prefs.class);

    private Prefs() {
    }

    static boolean dark() {
        return P.getBoolean("dark", false);
    }

    static void setDark(boolean v) {
        P.putBoolean("dark", v);
    }

    static boolean learn() {
        return P.getBoolean("learn", false);
    }

    static void setLearn(boolean v) {
        P.putBoolean("learn", v);
    }

    static GameConfig config() {
        GameConfig d = GameConfig.defaults();
        try {
            return new GameConfig(
                    GameConfig.Mode.valueOf(P.get("mode", d.mode().name())),
                    P.getBoolean("misere", d.misere()),
                    P.getInt("heaps", d.heapCount()),
                    P.getInt("items", d.maxItems()),
                    GameConfig.Opening.valueOf(P.get("opening", d.opening().name())),
                    AiLevel.valueOf(P.get("level", d.level().name())),
                    P.getInt("seat", d.humanSeat()));
        } catch (IllegalArgumentException e) {
            return d;
        }
    }

    static void setConfig(GameConfig c) {
        P.put("mode", c.mode().name());
        P.putBoolean("misere", c.misere());
        P.putInt("heaps", c.heapCount());
        P.putInt("items", c.maxItems());
        P.put("opening", c.opening().name());
        P.put("level", c.level().name());
        P.putInt("seat", c.humanSeat());
    }
}
