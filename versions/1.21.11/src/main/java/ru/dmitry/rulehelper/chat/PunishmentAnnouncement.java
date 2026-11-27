package ru.dmitry.rulehelper.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses server announcements for bans, mutes, warnings and kicks. */
public record PunishmentAnnouncement(Kind kind, String issuer, String target, String duration, String reason, String originalLine, boolean ipTarget) {
   private static final String NAME = "[\\p{L}\\p{N}_]{3,16}";
   private static final String DURATION = "(?:навсегда|[0-9]{1,4}\\s+(?:секунд(?:у|ы)?|минут(?:у|ы)?|час(?:а|ов)?|день|дня|дней|неделю|недели|месяц|месяца|месяцев))";
   private static final Pattern MESSAGE = Pattern.compile(
      "^❨[！!]❩\\s*Игрок\\s+(" + NAME + ")\\s+"
         + "(забанил|замутил|предупредил|выдал\\s+(?:предупреждение|варн)|кикнул|выгнал|выкинул)\\s+"
         + "(IP\\s+)?(?:игрок[ау]\\s+)?(" + NAME + ")>?"
         + "(?:\\s+с\\s+сервера)?(?:\\s+на\\s+(" + DURATION + "))?"
         + "(?:\\s+по\\s+причине\\s*:\\s*(.{0,512}))?(?:\\s+(\\([^()]{1,40}\\)))?\\s*$",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
   );
   private static final Pattern MODE = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");
   private static final Pattern ANNOUNCEMENT_START = Pattern.compile("(?iu)^❨[！!]❩\\s*Игрок\\s+");
   private static final Pattern UI_PREFIXES = Pattern.compile("(?iu)^🛡\\s+☠\\s*");

   public enum Kind {
      BAN("Бан"), MUTE("Мут"), WARN("Предупреждение"), KICK("Кик");
      private final String label;
      Kind(String label) { this.label = label; }
      public String label() { return this.label; }
      public boolean canBeRemoved() { return this != KICK; }
   }

   /** True when the visible announcement row has the expected server prefix. */
   public static boolean startsAnnouncement(String text) {
      return text != null && ANNOUNCEMENT_START.matcher(normalizedText(text)).find();
   }

   /** Remove the two clickable copy-nickname labels added by RuleHelper. */
   public static String stripUiPrefixes(String text) {
      return text == null ? null : UI_PREFIXES.matcher(text.stripLeading()).replaceFirst("");
   }

   public static String normalizedText(String text) {
      if (text == null) return "";
      String plain = text.replaceAll("(?i)[§&][0-9A-FK-OR]", "").replace('\u00a0', ' ');
      return stripUiPrefixes(plain).strip().replaceAll("\\s+", " ");
   }

   public static PunishmentAnnouncement parse(String text) {
      if (text == null) return null;
      String normalized = normalizedText(text);
      Matcher matcher = MESSAGE.matcher(normalized);
      if (!matcher.matches()) return null;
      String verb = matcher.group(2).toLowerCase(Locale.ROOT);
      Kind kind = verb.startsWith("забан") ? Kind.BAN : verb.startsWith("замут") ? Kind.MUTE
         : verb.startsWith("предупред") || verb.startsWith("выдал") ? Kind.WARN : Kind.KICK;
      boolean ipTarget = matcher.group(3) != null;
      if (ipTarget && (kind == Kind.WARN || kind == Kind.KICK)) return null;
      String duration = matcher.group(5) == null
         ? (kind == Kind.WARN || kind == Kind.KICK ? "не применяется" : "не указан")
         : matcher.group(5).strip();
      String reason = matcher.group(6) == null ? "" : matcher.group(6).strip();
      // Keep the existing strict ban/mute behavior. A missing warn/kick reason
      // is still reviewable and can itself explain why a correction is needed.
      if ((kind == Kind.BAN || kind == Kind.MUTE) && reason.isBlank()) return null;
      if (matcher.group(7) != null) reason = (reason + " " + matcher.group(7)).strip();
      return new PunishmentAnnouncement(kind, matcher.group(1), matcher.group(4), duration,
         reason, normalized, ipTarget);
   }

   public String cleanReason() {
      Matcher mode = MODE.matcher(this.reason);
      return mode.matches() ? mode.group(1).strip() : this.reason;
   }

   public String mode() {
      Matcher mode = MODE.matcher(this.reason);
      return mode.matches() ? mode.group(2).strip() : "не указан";
   }
}
