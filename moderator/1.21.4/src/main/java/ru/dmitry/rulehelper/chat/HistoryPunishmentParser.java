package ru.dmitry.rulehelper.chat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a /shist response and keeps only punishments issued on the current date. */
public final class HistoryPunishmentParser {
   public enum Kind { BAN, MUTE, WARN }
   public record Entry(LocalDateTime issuedAt, Kind kind, String target, String duration, String reason, String mode) {}

   private static final Pattern HEADER = Pattern.compile("(?iu)^История наказаний игроком\\s+([\\p{L}\\p{N}_]{3,16})\\s*\\(\\s*Записей:\\s*(\\d+)\\s*\\).*$");
   private static final Pattern AGE = Pattern.compile("(?iu)^[-—]?\\s*(?:(\\d+)\\s*(?:день|дня|дней|дн\\p{L}*)\\s*)?(?:(\\d+)\\s*(?:час|часа|часов|ч\\p{L}*)\\s*)?(?:(\\d+)\\s*(?:минут\\p{L}*|мин\\p{L}*)\\s*)?(?:(\\d+)\\s*(?:секунд\\p{L}*|сек\\p{L}*)\\s*)?назад\\s*[-—]?$");
   private static final Pattern RECORD = Pattern.compile("(?iu)^([\\p{L}\\p{N}_]{3,16})\\s+(?:был[ао]?\\s+)?(забанен\\p{L}*|замучен\\p{L}*|заглушен\\p{L}*|предупрежден\\p{L}*|получил\\s+предупреждение)\\s+(?:на\\s+)?(.+?)\\s+([\\p{L}\\p{N}_]{3,16})\\s*:\\s*(.*?)\\s*$");
   private static final Pattern MODE = Pattern.compile("(?iu)\\s*\\((глобальн\\p{L}*|выживан\\p{L}*|гриферск\\p{L}*|мини[- ]?игр\\p{L}*)\\)\\s*$");
   private static final Pattern EXPIRED = Pattern.compile("(?iu)\\s*\\[(?:истек|истекло|снят|снято)]\\s*$");

   private final List<Entry> today = new ArrayList<>();
   private String moderator = "";
   private Duration pendingAge;
   private int expectedRecords = -1;
   private int readRecords;
   private boolean active;
   private boolean responseMatched;

   public synchronized void begin(String nickname) {
      this.moderator = nickname == null ? "" : nickname.strip();
      this.today.clear();
      this.pendingAge = null;
      this.expectedRecords = -1;
      this.readRecords = 0;
      this.active = !this.moderator.isBlank();
      this.responseMatched = false;
   }

   /** Returns true if the line belonged to the matching /shist response. */
   public synchronized boolean acceptLine(String raw, LocalDateTime now) {
      if (raw == null || now == null) return false;
      String line = normalize(raw);
      Matcher header = HEADER.matcher(line);
      if (header.matches()) {
         if (!this.moderator.isBlank() && !header.group(1).equalsIgnoreCase(this.moderator)) {
            this.active = false;
            this.responseMatched = false;
            return false;
         }
         this.moderator = header.group(1);
         this.today.clear();
         this.pendingAge = null;
         this.expectedRecords = parseInt(header.group(2));
         this.readRecords = 0;
         this.active = this.expectedRecords != 0;
         this.responseMatched = true;
         return true;
      }
      if (!this.active || !this.responseMatched) return false;

      Matcher age = AGE.matcher(line);
      if (age.matches()) {
         this.pendingAge = ageDuration(age);
         return true;
      }

      Matcher record = RECORD.matcher(line);
      if (!record.matches()) return false;
      if (this.pendingAge != null && record.group(4).equalsIgnoreCase(this.moderator)) {
         LocalDateTime issuedAt = now.minus(this.pendingAge);
         if (issuedAt.toLocalDate().equals(now.toLocalDate())) {
            String verb = record.group(2).toLowerCase(Locale.ROOT);
            Kind kind = verb.startsWith("забан") ? Kind.BAN
               : verb.startsWith("предупреж") || verb.startsWith("получил") ? Kind.WARN : Kind.MUTE;
            String detail = EXPIRED.matcher(record.group(5)).replaceFirst("").strip();
            String mode = "—";
            Matcher modeMatcher = MODE.matcher(detail);
            if (modeMatcher.find()) {
               mode = modeMatcher.group(1);
               detail = detail.substring(0, modeMatcher.start()).strip();
            }
            if (detail.isBlank()) detail = "причина не указана";
            this.today.add(new Entry(issuedAt, kind, record.group(1), record.group(3).strip(), detail, mode));
         }
         this.readRecords++;
      }
      this.pendingAge = null;
      if (this.expectedRecords >= 0 && this.readRecords >= this.expectedRecords) this.active = false;
      return true;
   }

   public synchronized List<Entry> today() { return List.copyOf(this.today); }
   public synchronized boolean active() { return this.active; }
   public synchronized int expectedRecords() { return this.expectedRecords; }
   public synchronized int readRecords() { return this.readRecords; }

   private static String normalize(String raw) {
      return raw.replaceAll("(?i)§[0-9A-FK-OR]", "").replaceAll("(?i)&[0-9A-FK-OR]", "")
         .replace('\u00a0', ' ').strip().replaceAll("\\s+", " ");
   }

   private static Duration ageDuration(Matcher age) {
      long days = number(age.group(1));
      long hours = number(age.group(2));
      long minutes = number(age.group(3));
      long seconds = number(age.group(4));
      return Duration.ofDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
   }

   private static long number(String value) { try { return value == null ? 0 : Long.parseLong(value); } catch (NumberFormatException ignored) { return 0; } }
   private static int parseInt(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; } }
}
