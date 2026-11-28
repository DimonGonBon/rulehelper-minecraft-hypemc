package ru.dmitry.rulehelper.chat;

import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.dmitry.rulehelper.PlayerNames;

/** The actor column in a CoreProtect inspector/lookup row, never the block or action. */
public final class CoreProtectLog {
   private static final Pattern ROW = Pattern.compile(
      "(?iu)^\\s*[0-9]+(?:[.,][0-9]+)?\\s*/[smhdwyсмчднг]\\s+(?:назад|ago)\\s+[+\\-−]\\s+([A-Za-z0-9_]{3,16})\\s+\\S.*$");

   private CoreProtectLog() {}

   public static String actor(String line) {
      if (line == null) return null;
      var match = ROW.matcher(line.replace('\u00a0', ' '));
      return match.matches() && PlayerNames.isValid(match.group(1)) ? match.group(1) : null;
   }

   /** Check the actor's inherited component color, not a blue block later in the row. */
   public static boolean hasPlayerActor(Component message) {
      if (message == null) return false;
      var match = ROW.matcher(message.getString().replace('\u00a0', ' '));
      if (!match.matches() || !PlayerNames.isValid(match.group(1))) return false;
      int start = match.start(1), end = match.end(1);
      int[] offset = {0}, colored = {0};
      boolean[] valid = {true};
      message.visit((style, part) -> {
         int from = Math.max(start, offset[0]), to = Math.min(end, offset[0] + part.length());
         if (from < to) {
            if (!isBlue(style)) valid[0] = false;
            colored[0] += to - from;
         }
         offset[0] += part.length();
         return Optional.<Void>empty();
      }, Style.EMPTY);
      return valid[0] && colored[0] == end - start;
   }

   private static boolean isBlue(Style style) {
      if (style.getColor() == null) return false;
      int rgb = style.getColor().getValue();
      // Servers and resource packs can use custom RGB blues (e.g. #31B0E8),
      // so check the hue rather than four exact legacy palette entries.
      int red = (rgb >>> 16) & 255, green = (rgb >>> 8) & 255, blue = rgb & 255;
      return blue >= 64 && blue >= red + 24 && blue >= green - 24;
   }
}
