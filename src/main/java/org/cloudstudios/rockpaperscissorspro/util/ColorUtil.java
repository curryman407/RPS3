package org.cloudstudios.rockpaperscissorspro.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;


public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {}


    public static Component translate(final String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return LEGACY.deserialize(text);
    }


    public static String translateLegacy(final String text) {
        if (text == null) return "";
        return text.replace('&', '\u00a7');
    }
}
