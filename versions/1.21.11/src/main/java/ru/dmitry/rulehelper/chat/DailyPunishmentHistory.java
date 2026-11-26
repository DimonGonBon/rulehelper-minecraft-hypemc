package ru.dmitry.rulehelper.chat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Daily counts come from /shist and its per-record relative timestamps. */
public final class DailyPunishmentHistory {
   public enum Kind { BAN, MUTE, WARN }
   public record Entry(LocalTime time, Kind kind, String target, String duration, String reason, String mode) {}

   private static final HistoryPunishmentParser PARSER = new HistoryPunishmentParser();
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
   private static LocalDate currentDate = LocalDate.now();
   private static long version;
   private static long requestStartedAt;
   private static String requestedNickname = "";

   private DailyPunishmentHistory() {}

   public static synchronized void beginQuery(String nickname) {
      refreshDay();
      requestedNickname = nickname == null ? "" : nickname.strip();
      PARSER.begin(requestedNickname);
      requestStartedAt = System.currentTimeMillis();
      version++;
   }

   /** Called for each incoming chat component while /shist output is arriving. */
   public static synchronized boolean consumeHistoryLine(String line, String nickname) {
      refreshDay();
      if (nickname == null || nickname.isBlank() || !nickname.equalsIgnoreCase(requestedNickname)) return false;
      boolean accepted = PARSER.acceptLine(line, LocalDateTime.now());
      if (accepted) version++;
      return accepted;
   }

   public static synchronized List<Entry> entries() {
      refreshDay();
      return PARSER.today().stream().map(entry -> new Entry(entry.issuedAt().toLocalTime(),
         Kind.valueOf(entry.kind().name()), entry.target(), entry.duration(), entry.reason(), entry.mode())).toList();
   }

   public static synchronized int count(Kind kind) { return (int)entries().stream().filter(entry -> entry.kind() == kind).count(); }
   public static synchronized int totalCount() { return entries().size(); }
   public static synchronized String dateLabel() { refreshDay(); return currentDate.format(DATE); }
   public static synchronized long version() { return version; }
   public static synchronized boolean loading() {
      if (requestStartedAt == 0 || System.currentTimeMillis() - requestStartedAt > 15_000L) return false;
      return PARSER.active();
   }
   public static String formatTime(Entry entry) { return entry.time().format(DateTimeFormatter.ofPattern("HH:mm:ss")); }

   private static void refreshDay() {
      LocalDate today = LocalDate.now();
      if (!today.equals(currentDate)) {
         currentDate = today;
         PARSER.begin(requestedNickname);
         requestStartedAt = 0;
         version++;
      }
   }
}
