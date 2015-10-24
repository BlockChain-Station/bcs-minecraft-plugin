package com.enjin.officialplugin;

import java.util.regex.Pattern;

import org.bukkit.ChatColor;

/**
 * Handling misc chat/console functions
 */
public class EnjinConsole {
    private static Pattern chatColorPattern = Pattern.compile("(?i)&([0-9A-FK-R])");

    public static String[] header() {
        return new String[]{ChatColor.GREEN + "=== Enjin Minecraft Plugin ==="};
    }

    public static String translateColorCodes(String string) {
        if (string == null) {
            return "";
        }

        return chatColorPattern.matcher(string).replaceAll("\u00A7$1");
    }
}
