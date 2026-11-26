package ru.dmitry.rulehelper;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the moderator investigation commands with the exact server syntax. */
public final class ModeratorProofCommands {
   public enum ReleaseType { BAN, MUTE }
   private static final Pattern DURATION_PART = Pattern.compile(
      "(\\d+(?:[.,]\\d+)?)\\s*(weeks?|week|w|нед(?:еля|ели|ель|елю|елью|ель)?|дней|дня|день|дни|дн|д|days?|d|час(?:а|ов|ы)?|ч|hours?|hrs?|h|минут(?:а|ы)?|мин|минута|minutes?|mins?|m)(?![\\p{L}])",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

   private ModeratorProofCommands() {}

   public static String release(ReleaseType type, String player) {
      return (type == ReleaseType.BAN ? "/unban " : "/unmute ") + player + " На время разбирательств";
   }

   public static String requestEvidence(String moderator, String player, String requestedReason, String contact) {
      return "/ban " + moderator + " 3d 3.4 - Предоставьте доказательства на выдачу ограничения игроку \""
         + player + "\" по причине: " + requestedReason.trim() + " | Контакты для связи: " + contact + " -g";
   }

   /** Accepts simple, decimal, and compound Russian/English durations; returns a single server token. */
   public static String normalizeDuration(String input) {
      if (input == null || input.isBlank()) return null;
      Matcher matcher = DURATION_PART.matcher(input.trim());
      int end = 0;
      BigDecimal minutes = BigDecimal.ZERO;
      int parts = 0;
      while (matcher.find()) {
         if (!input.substring(end, matcher.start()).trim().isEmpty()) return null;
         BigDecimal amount;
         try {
            amount = new BigDecimal(matcher.group(1).replace(',', '.'));
         } catch (NumberFormatException ignored) {
            return null;
         }
         String unit = matcher.group(2).toLowerCase(Locale.ROOT);
         int multiplier = durationMultiplier(unit);
         if (multiplier == 0) return null;
         minutes = minutes.add(amount.multiply(BigDecimal.valueOf(multiplier)));
         end = matcher.end();
         parts++;
      }
      if (parts == 0 || !input.substring(end).trim().isEmpty() || minutes.signum() <= 0
         || minutes.stripTrailingZeros().scale() > 0 || minutes.compareTo(BigDecimal.valueOf(9_999_999)) > 0) return null;
      long totalMinutes = minutes.longValueExact();
      // Preserve concise single-part terms that the server commonly accepts.
      if (parts == 1) {
         Matcher one = DURATION_PART.matcher(input.trim());
         if (one.matches() && one.group(1).matches("\\d+")) {
            String unit = one.group(2).toLowerCase(Locale.ROOT);
            char suffix = switch (durationMultiplier(unit)) {
               case 1 -> 'm'; case 60 -> 'h'; case 1440 -> 'd'; default -> 'w';
            };
            return Long.parseLong(one.group(1)) + String.valueOf(suffix);
         }
      }
      return totalMinutes + "m";
   }

   private static int durationMultiplier(String unit) {
      if (unit.startsWith("w") || unit.startsWith("нед")) return 10080;
      if (unit.equals("d") || unit.equals("д") || unit.startsWith("дн") || unit.startsWith("день")
         || unit.startsWith("дня") || unit.startsWith("дней") || unit.startsWith("дни") || unit.startsWith("day")) return 1440;
      if (unit.equals("h") || unit.equals("ч") || unit.startsWith("час") || unit.startsWith("hour") || unit.startsWith("hr")) return 60;
      if (unit.equals("m") || unit.startsWith("мин") || unit.startsWith("minute") || unit.startsWith("min")) return 1;
      return 0;
   }

   public static String humanDuration(String duration) {
      String normalized = normalizeDuration(duration);
      if (normalized == null) return "";
      long remaining = Long.parseLong(normalized.substring(0, normalized.length() - 1));
      char unit = normalized.charAt(normalized.length() - 1);
      long totalMinutes = switch (unit) {
         case 'm' -> remaining;
         case 'h' -> remaining * 60;
         case 'd' -> remaining * 1440;
         default -> remaining * 10080;
      };
      long days = totalMinutes / 1440;
      long hours = (totalMinutes % 1440) / 60;
      long minutes = totalMinutes % 60;
      StringBuilder result = new StringBuilder();
      if (days > 0) result.append(days).append(' ').append(plural(days, "день", "дня", "дней"));
      if (hours > 0) appendPart(result, hours, "час", "часа", "часов");
      if (minutes > 0 || result.isEmpty()) appendPart(result, minutes, "минута", "минуты", "минут");
      return result.toString();
   }

   private static void appendPart(StringBuilder result, long amount, String one, String few, String many) {
      if (!result.isEmpty()) result.append(' ');
      result.append(amount).append(' ').append(plural(amount, one, few, many));
   }

   private static String plural(long amount, String one, String few, String many) {
      long lastTwo = amount % 100;
      long last = amount % 10;
      if (lastTwo >= 11 && lastTwo <= 14) return many;
      if (last == 1) return one;
      if (last >= 2 && last <= 4) return few;
      return many;
   }

}
