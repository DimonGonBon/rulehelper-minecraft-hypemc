package ru.dmitry.rulehelper.chat;

import java.util.Locale;
import java.util.regex.Pattern;

/** A removal announcement is reviewed separately from newly issued punishments. */
public record PunishmentRemovalAnnouncement(Kind kind, String issuer, String target, String reason,
                                            String originalLine, boolean ipTarget) {
   public enum Kind {
      UNBAN("Разбан"), UNMUTE("Размут");
      private final String label;
      Kind(String label) { this.label = label; }
      public String label() { return this.label; }
   }
   private static final String NAME = "[\\p{L}\\p{N}_]{3,16}";
   private static final Pattern ROW = Pattern.compile(
      "(?iu)^" + PunishmentAnnouncement.NOTICE_MARKER + "\\s*Игрок\\s+(" + NAME + ")\\s+(разбанил|размутил)\\s+"
         + "(IP\\s+)?(?:игрок[ау]\\s+)?(" + NAME + ")>?"
         + "(?:\\s+по\\s+причине\\s*:\\s*(.{0,512}))?(?:\\s+(\\([^()]{1,40}\\)))?\\s*$");
   private static final Pattern MODE = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");

   public static PunishmentRemovalAnnouncement parse(String text) {
      if (text == null) return null;
      String normalized = PunishmentAnnouncement.normalizedText(text);
      var match = ROW.matcher(normalized);
      if (!match.matches()) return null;
      Kind kind = match.group(2).toLowerCase(Locale.ROOT).startsWith("разбан") ? Kind.UNBAN : Kind.UNMUTE;
      String reason = match.group(5) == null ? "" : match.group(5).strip();
      if (match.group(6) != null) reason = (reason + " " + match.group(6)).strip();
      return new PunishmentRemovalAnnouncement(kind, match.group(1), match.group(4), reason, normalized, match.group(3) != null);
   }

   public String cleanReason() {
      var match = MODE.matcher(this.reason);
      return match.matches() ? match.group(1).strip() : this.reason;
   }

   public String mode() {
      var match = MODE.matcher(this.reason);
      return match.matches() ? match.group(2).strip() : "не указан";
   }
}
