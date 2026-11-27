package ru.dmitry.rulehelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formats only a command's reason; player, duration and server flags stay intact. */
public final class CauseFormatter {
   private static final Pattern RELEASE = Pattern.compile(
      "(?i)^(\\s*/?(?:unbanip|unban|unmuteip|unmute|unwarn)\\s+\\S+\\s+)nekk(?=\\s|$)");
   private static final Pattern PUNISHMENT = Pattern.compile(
      "(?i)^(\\s*/?(?:banip|ban|muteip|mute|warn|kick)\\s+\\S+\\s+)(.*)$");

   private CauseFormatter() {}

   public static String formatCommand(String command, String ruleCode, String title, String style) {
      return formatCommand(command, ruleCode, title, style, null);
   }

   public static String formatCommand(String command, String ruleCode, String title, String style, ReasonTemplates templates) {
      if (command == null) return null;
      Matcher release = RELEASE.matcher(command);
      if (release.find()) {
         return release.replaceFirst(Matcher.quoteReplacement(release.group(1) + releaseReason(style, templates)));
      }
      if (ruleCode == null || title == null) return command;
      Matcher punishment = PUNISHMENT.matcher(command);
      if (!punishment.matches()) return command;
      String tail = punishment.group(2);
      Pattern rule = Pattern.compile("(?<![\\p{L}\\p{N}_.])" + Pattern.quote(ruleCode)
         + "(?:\\[([^\\]]*)\\])?(?![\\p{L}\\p{N}_.])");
      Matcher reason = rule.matcher(tail);
      if (!reason.find()) return command;
      String formatted = reason(ruleCode, title, reason.group(1), false, style, templates);
      return punishment.group(1) + reason.replaceFirst(Matcher.quoteReplacement(formatted));
   }

   public static String releaseReason(String style) {
      return releaseReason(style, null);
   }

   public static String releaseReason(String style, ReasonTemplates templates) {
      return "SHORT".equals(style) ? "nekk"
         : reason("3.4", "Некорректное наказание", null, true, style, templates);
   }

   public static String reason(String code, String title, String extra, boolean release, String style) {
      return reason(code, title, extra, release, style, null);
   }

   public static String reason(String code, String title, String extra, boolean release, String style, ReasonTemplates templates) {
      code = oneLine(code);
      extra = oneLine(extra);
      if ("CUSTOM".equals(style)) {
         ReasonTemplates selected = templates == null ? new ReasonTemplates() : templates;
         String caption = "3.4".equals(code) ? "Некорректное наказание" : oneLine(title);
         return selected.render(code, caption, extra, release);
      }
      if ("SHORT".equals(style)) return code + (extra.isEmpty() ? "" : "[" + extra + "]");
      String caption = "3.4".equals(code)
         ? ("COLORED".equals(style) || release ? "Некорректное наказание" : "Некорректные наказания")
         : oneLine(title);
      if (!extra.isEmpty()) caption += " · " + extra;
      if ("COLORED".equals(style)) {
         return "&f&l&o" + code + (release ? " &a&l&o" : " &b&l&o")
            + "━━➤ 「 " + caption + " 」&r";
      }
      String text = code + " | " + caption;
      return switch (style) {
         case "CORNER" -> "「 " + text + " 」";
         case "DOUBLE_CORNER" -> "『 " + text + " 』";
         case "SPARKLE" -> "✦「 " + text + " 」✦";
         case "BOX" -> "【 " + text + " 】";
         default -> text;
      };
   }

   private static String oneLine(String text) {
      return text == null ? "" : text.replaceAll("[\\r\\n\\t]+", " ").strip();
   }
}
