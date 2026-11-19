package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;

import java.util.Locale;
import java.util.Set;

/**
 * Keeps the bot from teleporting itself with game commands.
 *
 * <p>The agent can type any command through {@code sendGameCommand} or a "/" chat message. On
 * 2026-11-18 it built a whole lighthouse by sending {@code tp @s X Y Z} before every few blocks,
 * because the builder's own walking kept jumping around. The operator wants the bot to walk like a
 * player, so these commands are refused here, in the one place every bot-sent command passes
 * ({@code PlayerVer.sendChatCommand}). Commands the human types in the chat screen do not come
 * through here and are not affected.
 *
 * <p>The switch is {@code allowTeleportCommands} in {@code altoclef_settings.json}, off by default.
 * It is deliberately not reachable through {@code @set}: the agent is the one being restricted.
 */
public final class GameCommandGuard {

    /** Commands that move the player without walking. Compared without the "minecraft:" prefix. */
    private static final Set<String> TELEPORTS = Set.of("tp", "teleport", "spreadplayers");

    private GameCommandGuard() {}

    /**
     * Why this command must not be sent, or null when it may go. {@code command} is what follows
     * the "/", with or without the slash.
     */
    public static String refusal(String command) {
        if (command == null || teleportsAllowed()) return null;
        String[] words = command.strip().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length == 0) return null;
        boolean teleport = isTeleport(words[0]);
        // "execute ... run tp ..." (nested executes included) moves the player just the same.
        if (!teleport && "execute".equals(bare(words[0]))) {
            for (int i = 1; i + 1 < words.length; i++) {
                if ("run".equals(words[i]) && isTeleport(words[i + 1])) {
                    teleport = true;
                    break;
                }
            }
        }
        if (!teleport) return null;
        return "teleport commands are off for the bot: get there on foot (gotoXYZ, the builder's own"
                + " walking, a pillar or a bridge). Only the operator can allow them.";
    }

    private static boolean isTeleport(String word) {
        return TELEPORTS.contains(bare(word));
    }

    /** "/minecraft:tp" -> "tp". */
    private static String bare(String word) {
        String w = word;
        while (w.startsWith("/")) w = w.substring(1);
        if (w.startsWith("minecraft:")) w = w.substring("minecraft:".length());
        return w;
    }

    private static boolean teleportsAllowed() {
        AltoClef mod = AltoClef.getInstance();
        return mod != null && mod.getModSettings() != null && mod.getModSettings().allowTeleportCommands();
    }
}
