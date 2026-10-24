package ru.dmitry.rulehelper.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the supported HypeMC punishment announcement formats. */
public record PunishmentAnnouncement(Kind kind, String issuer, String target, String duration, String reason, String originalLine, boolean ipTarget) {
   private static final String DURATION = "(?:навсегда|[0-9]{1,4}\\s+(?:секунд(?:у|ы)?|минут(?:у|ы)?|час(?:а|ов)?|день|дня|дней|неделю|недели|месяц|месяца|месяцев))";
   private static final Pattern MESSAGE = Pattern.compile(
      "^❨[！!]❩\\s*Игрок\\s+([\\p{L}\\p{N}_]{3,16})\\s+(забанил|замутил)\\s+(IP\\s+)?([\\p{L}\\p{N}_]{3,16})>?\\s+(?:на\\s+(" + DURATION + ")\\s+)?по\\s+причине:\\s*(.{1,160})$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
   );
   private static final Pattern MODE = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");

   public enum Kind { BAN, MUTE }

   /** True when the visible announcement row has the expected prefix. */
   public static boolean startsAnnouncement(String text) {
      return text != null && text.stripLeading().matches("^❨[！!]❩\\s*Игрок\\b.*");
   }

   public static PunishmentAnnouncement parse(String text) {
      if (text == null) return null;
      String normalized = text.strip().replaceAll("\\s+", " ");
      Matcher matcher = MESSAGE.matcher(normalized);
      if (!matcher.matches()) return null;
      String duration = matcher.group(5) == null ? "не указан" : matcher.group(5).strip();
      String reason = matcher.group(6).strip();
      if (reason.isEmpty()) return null;
      return new PunishmentAnnouncement(
         matcher.group(2).equalsIgnoreCase("забанил") ? Kind.BAN : Kind.MUTE,
         matcher.group(1), matcher.group(4), duration, reason, normalized, matcher.group(3) != null
      );
   }

   public String cleanReason() {
      Matcher mode = MODE.matcher(this.reason);
      return mode.matches() && !mode.group(1).isBlank() ? mode.group(1).strip() : this.reason;
   }

   public String mode() {
      Matcher mode = MODE.matcher(this.reason);
      return mode.matches() && !mode.group(1).isBlank() ? mode.group(2).strip() : "не указан";
   }
}
