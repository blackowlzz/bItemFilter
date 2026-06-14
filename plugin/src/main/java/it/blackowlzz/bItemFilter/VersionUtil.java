package it.blackowlzz.bItemFilter;

import org.bukkit.Bukkit;

public final class VersionUtil {

    public static final boolean ENTITY_PICKUP_SUPPORTED = probe("org.bukkit.event.entity.EntityPickupItemEvent");

    public static final boolean SMALL_CAPS_SUPPORTED;

    static {
        boolean ok = false;
        try {
            String[] parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch  = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            ok = major > 1 || (major == 1 && (minor > 20 || (minor == 20 && patch >= 5)));
        } catch (Exception ignored) {}
        SMALL_CAPS_SUPPORTED = ok;
    }

    private VersionUtil() {}

    private static boolean probe(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Converts a color-coded string to Unicode small caps, stripping bold (&l).
     * Color codes (&a, &c, §7, etc.) are preserved. Non-letter characters are unchanged.
     */
    public static String toSmallCaps(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        StringBuilder sb = new StringBuilder(text.length());
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len) {
                char next = text.charAt(i + 1);
                if (next == 'l' || next == 'L') {
                    i++;
                    continue;
                }
                sb.append(c).append(next);
                i++;
                continue;
            }
            sb.append(smallCapsChar(c));
        }
        return sb.toString();
    }

    private static char smallCapsChar(char c) {
        return switch (Character.toLowerCase(c)) {
            // SMOOOOOOOOOOOLLLL KAAAAAAAAAAAAAAAAAPPPPPSSSSSSSSSS wow
            case 'a' -> 'ᴀ'; case 'b' -> 'ʙ'; case 'c' -> 'ᴄ'; case 'd' -> 'ᴅ';
            case 'e' -> 'ᴇ'; case 'f' -> 'ꜰ'; case 'g' -> 'ɢ'; case 'h' -> 'ʜ';
            case 'i' -> 'ɪ'; case 'j' -> 'ᴊ'; case 'k' -> 'ᴋ'; case 'l' -> 'ʟ';
            case 'm' -> 'ᴍ'; case 'n' -> 'ɴ'; case 'o' -> 'ᴏ'; case 'p' -> 'ᴘ';
            case 'q' -> 'ǫ'; case 'r' -> 'ʀ'; case 's' -> 'ѕ'; case 't' -> 'ᴛ';
            case 'u' -> 'ᴜ'; case 'v' -> 'ᴠ'; case 'w' -> 'ᴡ'; case 'x' -> 'x';
            case 'y' -> 'ʏ'; case 'z' -> 'ᴢ';
            default  -> c;
        };
    }
}
