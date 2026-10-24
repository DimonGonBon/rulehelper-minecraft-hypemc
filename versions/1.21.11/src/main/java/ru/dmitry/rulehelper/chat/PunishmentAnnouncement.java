package ru.dmitry.rulehelper.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A deliberately narrow parser for HypeMC punishment announcements. */
public record PunishmentAnnouncement(Kind kind, String issuer, String target, String duration, String reason) {
   private static final Pattern MESSAGE = Pattern.compile(
      "^❨！❩ Игрок ([A-Za-z0-9_]{3,16}) (забанил|замутил) ([A-Za-z0-9_]{3,16}) на ((?:навсегда|[0-9]{1,4}\\s+(?:секунд(?:у|ы)?|минут(?:у|ы)?|час(?:а|ов)?|день|дня|дней|неделю|недели|месяц|месяца|месяцев))) по причине: (.{1,160})$"
   );

   public enum Kind { BAN, MUTE }

   public static PunishmentAnnouncement parse(String text) {
      if (text == null) return null;
      Matcher matcher = MESSAGE.matcher(text.strip());
      if (!matcher.matches()) return null;
      String duration = matcher.group(4).strip();
      String reason = matcher.group(5).strip();
      if (duration.isEmpty() || reason.isEmpty()) return null;
      return new PunishmentAnnouncement(
         matcher.group(2).equals("забанил") ? Kind.BAN : Kind.MUTE,
         matcher.group(1), matcher.group(3), duration, reason
      );
   }
}
