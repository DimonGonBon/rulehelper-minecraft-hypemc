package ru.dmitry.rulehelper.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hides only the server response to /shist for the configured local player. */
public final class SelfShistSuppressor {
   private static final Pattern HEADER = Pattern.compile(
      "(?iu)^История наказаний игроком\\s+([\\p{L}\\p{N}_]{3,16})\\s*\\(\\s*Записей:\\s*(\\d+)\\s*\\).*$");
   private static final Pattern AGE = Pattern.compile(
      "(?iu)^[-—]?\\s*(?:(\\d+)\\s*(?:день|дня|дней|дн\\p{L}*)\\s*)?"
         + "(?:(\\d+)\\s*(?:час|часа|часов|ч\\p{L}*)\\s*)?"
         + "(?:(\\d+)\\s*(?:минут\\p{L}*|мин\\p{L}*)\\s*)?"
         + "(?:(\\d+)\\s*(?:секунд\\p{L}*|сек\\p{L}*)\\s*)?назад\\s*[-—]?$");
   private static final Pattern ROW = Pattern.compile(
      "(?iu)^[\\p{L}\\p{N}_]{3,16}\\s+(?:был[ао]?\\s+)?"
         + "(?:забанен\\p{L}*|замучен\\p{L}*|заглушен\\p{L}*|разбанен\\p{L}*|размучен\\p{L}*|"
         + "предупрежден\\p{L}*|кикнут\\p{L}*|получил\\s+предупреждение)\\b.*$");
   private static final long TIMEOUT_MILLIS = 20_000L;

   private static String requestedNickname = "";
   private static boolean awaitingHeader;
   private static boolean readingResponse;
   private static int expectedRows;
   private static int readRows;
   private static long startedAt;

   private SelfShistSuppressor() {}

   public static synchronized void onOutgoingCommand(String rawCommand, String localNickname) {
      if (rawCommand == null || localNickname == null || localNickname.isBlank()) return;
      String command = rawCommand.strip();
      if (command.startsWith("/")) command = command.substring(1).strip();
      String[] parts = command.split("\\s+");
      if (parts.length == 0 || !parts[0].equalsIgnoreCase("shist")) return;
      if (parts.length < 2 || !parts[1].equalsIgnoreCase(localNickname.strip())) {
         reset();
         return;
      }
      requestedNickname = localNickname.strip();
      awaitingHeader = true;
      readingResponse = false;
      expectedRows = -1;
      readRows = 0;
      startedAt = System.currentTimeMillis();
   }

   /** Returns true only for a line belonging to the matching self-history response. */
   public static synchronized boolean suppress(String rawLine) {
      if (rawLine == null || (!awaitingHeader && !readingResponse)) return false;
      if (System.currentTimeMillis() - startedAt > TIMEOUT_MILLIS) {
         reset();
         return false;
      }
      String line = rawLine.replace('\u00a0', ' ').strip().replaceAll("\\s+", " ");
      Matcher header = HEADER.matcher(line);
      if (header.matches()) {
         if (!header.group(1).equalsIgnoreCase(requestedNickname)) {
            reset();
            return false;
         }
         expectedRows = parseCount(header.group(2));
         readRows = 0;
         awaitingHeader = false;
         readingResponse = expectedRows > 0;
         if (!readingResponse) reset();
         return true;
      }
      if (awaitingHeader) return false;

      Matcher age = AGE.matcher(line);
      if (age.matches()) return true;
      if (ROW.matcher(line).matches()) {
         readRows++;
         if (expectedRows >= 0 && readRows >= expectedRows) reset();
         return true;
      }
      return false;
   }

   private static int parseCount(String value) {
      try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; }
   }

   private static void reset() {
      requestedNickname = "";
      awaitingHeader = false;
      readingResponse = false;
      expectedRows = -1;
      readRows = 0;
      startedAt = 0;
   }
}
