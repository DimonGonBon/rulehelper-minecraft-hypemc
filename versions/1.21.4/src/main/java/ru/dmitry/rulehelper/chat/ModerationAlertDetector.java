package ru.dmitry.rulehelper.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects family mentions, project-listed insults and clearly incorrect punishments. */
public final class ModerationAlertDetector {
   private static final String FAMILY_WORD = "(?<![\\p{L}\\p{N}_])(?:мам\\p{L}*|мат(?:ь|ер\\p{L}*)|бат\\p{L}*|пап\\p{L}*|отц\\p{L}*|родител\\p{L}*|родн\\p{L}*|семь\\p{L}*)(?![\\p{L}\\p{N}_])";
   // Family insults and ordinary insults here are based on the supplied HypeMC CSV.
   private static final String ABUSE_WORD = "(?:еб\\p{L}*|ёб\\p{L}*|бля\\p{L}*|сук\\p{L}*|суч\\p{L}*|шлюх\\p{L}*|проститут\\p{L}*|пид\\p{L}*|гомосексуал\\p{L}*|дурак\\p{L}*|туп\\p{L}*|дебил\\p{L}*|дибил\\p{L}*|аутист\\p{L}*|овощ\\p{L}*|конч\\p{L}*|хуесос\\p{L}*|псин\\p{L}*|крыс\\p{L}*|баран\\p{L}*|животн\\p{L}*|клоун\\p{L}*|сдох\\p{L}*|умер\\p{L}*|сволоч\\p{L}*|мраз\\p{L}*|твар\\p{L}*)";
   private static final String INSULT_WORD = "(?:псин\\p{L}*|крыс\\p{L}*|сук\\p{L}*|баран\\p{L}*|животн\\p{L}*|поскули\\p{L}*|клоун\\p{L}*|проститут\\p{L}*|шлюх\\p{L}*|бляд\\p{L}*|блят\\p{L}*|гей\\p{L}*|голуб\\p{L}*|педик\\p{L}*|дурак\\p{L}*|туп\\p{L}*|аутист\\p{L}*|овощ\\p{L}*|конч\\p{L}*|дебил\\p{L}*|дибил\\p{L}*|еблан\\p{L}*|ебанат\\p{L}*|хуесос\\p{L}*|дерьм\\p{L}*|слит\\p{L}*|сосал\\p{L}*|отпор\\p{L}*)";
   private static final Pattern FAMILY_MENTION = Pattern.compile("(?iu)" + FAMILY_WORD);
   private static final Pattern FAMILY_ABUSE = Pattern.compile(
      "(?iu)(?:" + FAMILY_WORD + "(?:\\s+\\p{L}+){0,4}\\s+" + ABUSE_WORD
         + "|" + ABUSE_WORD + "(?:\\s+\\p{L}+){0,3}\\s+" + FAMILY_WORD + ")"
   );
   private static final Pattern GENERAL_INSULT = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])" + INSULT_WORD + "(?![\\p{L}\\p{N}_])");
   private static final Pattern DIRECT_INSULT_PHRASE = Pattern.compile("(?iu)(?:\\bты\\s+(?:сука|сук\\p{L}*|дибил\\p{L}*|дебил\\p{L}*|совсем\\s+туп\\p{L}*|туп\\p{L}*|еблан\\p{L}*|хуесос\\p{L}*)|\\bесли\\s+ты\\s+(?:еблан\\p{L}*|не\\s+с\\s+нами)\\s*(?:то\\s+)?(?:сочувствую|ты\\s+хуесос\\p{L}*))");
   private static final Pattern NON_PUNISHABLE_CSV_PHRASE = Pattern.compile("(?iu)^(?:да[,!\\s]+сук\\p{L}*|вот[,!\\s]+бля\\p{L}*)[.!?]*$");
   private static final Pattern FAMILY_BAIT = Pattern.compile(
      "(?iu)(?:мамк?\\p{L}*\\s+привет|беги\\s+(?:к\\s+)?мамк\\p{L}*|без\\s*мамн\\p{L}*|бат\\p{L}*\\s+твой|(?:мать|мат|маму|мамке?|батю|батя|отца|отец|родителей)\\s+(?:чекни|проверь|глянь|смотри)|маму?\\s+(?:чекни|проверь|глянь|смотри))"
   );
   private static final Pattern PUNISHMENT_START = Pattern.compile("(?iu)^" + PunishmentAnnouncement.NOTICE_MARKER + "\\s*Игрок\\s+");
   private static final Pattern ABSURD = Pattern.compile("(?iu)^(?:x{2,}|test+|lol+|ахаха+|хаха+|123+|qwe+|asdf+|дурак|идиот|клоун|бред|просто так|без причины)$");

   private ModerationAlertDetector() {}

   /** Returns at most one alert. Caps and spam remain ignored; family mentions always alert. */
   public static String familyInsult(String text) {
      return familyInsult(text, true, true);
   }

   public static String notification(String text, String localNickname, ru.dmitry.rulehelper.NotificationPreferences preferences) {
      String alert = familyInsult(text, preferences.family(), preferences.insults());
      return alert != null || !preferences.incorrect() ? alert : incorrectPunishment(text, localNickname);
   }

   private static String familyInsult(String text, boolean familyEnabled, boolean insultsEnabled) {
      if (text == null || text.isBlank()) return null;
      String body = messageBody(text);
      String cleaned = normalize(body);
      String transliterated = normalizeTransliteration(body);
      if (hasFamilyAlert(cleaned) || hasFamilyAlert(transliterated)) return familyEnabled
         ? "Обнаружено возможное упоминание или оскорбление родственников в чате" : null;
      if (insultsEnabled && (isGeneralInsult(cleaned) || isGeneralInsult(transliterated) || isSpecialInsult(body))) return "Обнаружено возможное оскорбление в чате";
      return null;
   }

   private static String messageBody(String text) {
      String body = text.replaceAll("(?i)§[0-9A-FK-OR]", " ").replaceAll("(?i)&[0-9A-FK-OR]", " ");
      int split = -1;
      for (String arrow : new String[]{"→", "➜", "⇨", "->"}) split = Math.max(split, body.lastIndexOf(arrow));
      if (split >= 0) return body.substring(split + (body.startsWith("->", split) ? 2 : 1)).strip();
      Matcher angle = Pattern.compile("(?iu)<[\\p{L}\\p{N}_]{3,16}>\\s*").matcher(body);
      if (angle.find()) return body.substring(angle.end()).strip();
      return body.strip();
   }

   private static boolean hasFamilyAlert(String text) {
      return FAMILY_MENTION.matcher(text).find() || FAMILY_BAIT.matcher(text).find() || FAMILY_ABUSE.matcher(text).find();
   }

   private static boolean isGeneralInsult(String text) {
      if (NON_PUNISHABLE_CSV_PHRASE.matcher(text).matches()) return false;
      return GENERAL_INSULT.matcher(text).find() || DIRECT_INSULT_PHRASE.matcher(text).find()
         || text.matches("(?iu)^(?:бот|нуб|ez|l|0)[.!?]*$")
         || text.matches("(?iu)^клан\\s+дерьм\\p{L}*[.!?]*$")
         || text.matches("(?iu)^туп\\p{L}*\\s+грифер[.!?]*$")
         || text.matches("(?iu)^если\\s+ты\\s+совсем\\s+туп\\p{L}*[.!?]*$");
   }

   private static boolean isSpecialInsult(String original) {
      String value = original.strip().toLowerCase(Locale.ROOT).replace('ё', 'е')
         .replaceAll("(?iu)[.!?]+$", "").strip();
      return value.equals("l") || value.equals("0") || value.equals("ez") || value.equals("bot") || value.equals("nub");
   }

   private static String normalize(String text) {
      return text.replaceAll("(?i)§[0-9A-FK-OR]", " ")
         .replaceAll("(?i)&[0-9A-FK-OR]", " ")
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е')
         // Common Latin lookalikes and simple leetspeak used to disguise Cyrillic words.
         .replace('a', 'а').replace('e', 'е').replace('o', 'о').replace('p', 'р')
         .replace('c', 'с').replace('x', 'х').replace('y', 'у').replace('k', 'к')
         .replace('m', 'м').replace('t', 'т').replace('b', 'в')
         .replace('6', 'б').replace('3', 'з').replace('4', 'ч').replace('0', 'о').replace('@', 'а')
         .replaceAll("(?<=\\p{L})[\\p{Punct}_]+(?=\\p{L})", "")
         .replaceAll("\\s+", " ").strip();
   }

   /** Also recognizes common Latin transliterations ("batya tvoy ebanat", "mat chekni"). */
   private static String normalizeTransliteration(String text) {
      String value = text.replaceAll("(?i)§[0-9A-FK-OR]|&[0-9A-FK-OR]", " ").toLowerCase(Locale.ROOT);
      value = value.replace("shch", "щ").replace("sch", "щ").replace("zh", "ж")
         .replace("kh", "х").replace("ch", "ч").replace("sh", "ш")
         .replace("yu", "ю").replace("ya", "я").replace("yo", "ё").replace("ts", "ц");
      StringBuilder result = new StringBuilder(value.length());
      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         result.append(switch (c) {
            case 'a' -> 'а'; case 'b' -> 'б'; case 'v' -> 'в'; case 'g' -> 'г';
            case 'd' -> 'д'; case 'e' -> 'е'; case 'z' -> 'з'; case 'i' -> 'и';
            case 'k' -> 'к'; case 'l' -> 'л'; case 'm' -> 'м'; case 'n' -> 'н';
            case 'o' -> 'о'; case 'p' -> 'п'; case 'r' -> 'р'; case 's' -> 'с';
            case 't' -> 'т'; case 'u' -> 'у'; case 'f' -> 'ф'; case 'h' -> 'х';
            case 'c' -> 'к'; case 'y' -> 'й'; case 'w' -> 'в'; default -> c;
         });
      }
      return result.toString().replace('ё', 'е')
         .replaceAll("(?<=\\p{L})[\\p{Punct}_]+(?=\\p{L})", "")
         .replaceAll("\\s+", " ").strip();
   }

   /** Reports only obvious 3.4 cases from a server punishment announcement. */
   public static String incorrectPunishment(String text, String moderatorName) {
      if (text == null) return null;
      String normalized = PunishmentAnnouncement.normalizedText(text);
      if (!PUNISHMENT_START.matcher(normalized).find()) return null;

      PunishmentAnnouncement announcement = PunishmentAnnouncement.parse(normalized);
      if (announcement == null) {
         // Recognize an announcement-shaped line with a missing or unparseable reason.
         if (normalized.matches("(?iu)^" + PunishmentAnnouncement.NOTICE_MARKER + "\\s*Игрок\\s+[\\p{L}\\p{N}_]{3,16}\\s+(?:забанил|замутил)\\s+.*")) {
            return "Возможное некорректное наказание: не удалось разобрать причину или срок";
         }
         return null;
      }
      if (same(announcement.issuer(), announcement.target())) {
         return "Возможное некорректное наказание: модератор наказал сам себя";
      }
      String reason = announcement.cleanReason();
      if (reason.isBlank()) return "Возможное некорректное наказание: наказание без причины";
      if (isAbsurd(reason)) return "Возможное некорректное наказание: подозрительная причина";
      return null;
   }

   private static boolean isAbsurd(String reason) {
      String value = reason.strip().replaceAll("(?iu)\\s*\\([^()]*\\)\\s*$", "").strip();
      return value.length() < 2 || ABSURD.matcher(value).matches();
   }

   private static boolean same(String first, String second) {
      return first != null && second != null && first.equalsIgnoreCase(second);
   }
}
