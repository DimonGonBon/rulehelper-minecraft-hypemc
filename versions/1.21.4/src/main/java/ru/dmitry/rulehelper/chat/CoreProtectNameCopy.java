package ru.dmitry.rulehelper.chat;

import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import ru.dmitry.rulehelper.ChatNames;

/** Copies the actor column directly, independent of menu-marker insertion and color palettes. */
public final class CoreProtectNameCopy {
   // The time unit is display text: accept a localized/custom token without interpreting it.
   private static final Pattern ROW = Pattern.compile(
      "(?iu)^\\s*[0-9]+(?:[.,][0-9]+)?\\S*\\s+(?:назад|ago)\\s+[+\\-−]\\s+([\\p{L}\\p{N}_]{3,16})(?=\\s|$)");
   public record Actor(String name, int start, int end) {}
   @FunctionalInterface public interface GlyphWidth { int measure(Style style, int codePoint); }
   private CoreProtectNameCopy() {}

   public static Actor actor(String line, String prefix) {
      if (line == null) return null;
      int offset = prefix != null && !prefix.isBlank() && line.startsWith(prefix + " ") ? prefix.length() + 1 : 0;
      var match = ROW.matcher(line.substring(offset).replace('\u00a0', ' '));
      if (!match.find() || !ChatNames.isValid(match.group(1))) return null;
      return new Actor(match.group(1), offset + match.start(1), offset + match.end(1));
   }

   /** Use the actually rendered row and its styled glyph widths, without requiring a marker. */
   public static String targetAt(FormattedCharSequence row, double x, String prefix, GlyphWidth width) {
      if (row == null || !Double.isFinite(x) || x < 0) return null;
      StringBuilder text = new StringBuilder();
      int[] pixels = {0}, hovered = {-1};
      row.accept((index, style, codePoint) -> {
         int advance = Math.max(0, width.measure(style, codePoint));
         if (x >= pixels[0] && x < pixels[0] + advance) hovered[0] = text.length();
         text.appendCodePoint(codePoint);
         pixels[0] += advance;
         return true;
      });
      Actor actor = actor(text.toString(), prefix);
      return actor != null && hovered[0] >= actor.start() && hovered[0] < actor.end() ? actor.name() : null;
   }

   /** Also attach Minecraft's own clipboard action so native chat clicks need no custom hit test. */
   public static Component addNativeCopy(Component message) {
      Actor actor = actor(message.getString(), "");
      if (actor == null) return message;
      var result = Component.empty();
      int[] offset = {0};
      message.visit((style, part) -> {
         int start = offset[0], end = start + part.length();
         int from = Math.max(start, actor.start()), to = Math.min(end, actor.end());
         if (from < to) {
            if (from > start) result.append(Component.literal(part.substring(0, from - start)).setStyle(style));
            Style copyStyle = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, actor.name()))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Скопировать ник: " + actor.name())));
            result.append(Component.literal(part.substring(from - start, to - start)).setStyle(copyStyle));
            if (to < end) result.append(Component.literal(part.substring(to - start)).setStyle(style));
         } else {
            result.append(Component.literal(part).setStyle(style));
         }
         offset[0] = end;
         return Optional.<Void>empty();
      }, Style.EMPTY);
      return result;
   }
}
