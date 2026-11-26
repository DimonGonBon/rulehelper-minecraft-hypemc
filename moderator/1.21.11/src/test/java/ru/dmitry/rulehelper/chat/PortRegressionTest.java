package ru.dmitry.rulehelper.chat;

import ru.dmitry.rulehelper.RuleCatalog;
import ru.dmitry.rulehelper.PlayerNames;
import ru.dmitry.rulehelper.PunishmentScope;

public final class PortRegressionTest {
   public static void main(String[] args) {
      require(RuleCatalog.ALL.size() == 28, "Moderator menu should include all 28 rules");
      require(RuleCatalog.ALL.stream().anyMatch(rule -> rule.code().equals("1.3")) && RuleCatalog.ALL.stream().anyMatch(rule -> rule.code().equals("4.1")),
         "Moderator rules 1.3 and 4.1 are present");
      require(RuleCatalog.ALL.stream().allMatch(rule -> !rule.actions().isEmpty()),
         "Every donor-visible rule has at least one punishment choice");
      int clicks = 0;
      for (int height : new int[]{240, 360, 480, 720}) {
         for (float scale : new float[]{0.25f, 0.5f, 0.75f, 1.0f}) {
            for (double spacing : new double[]{0, 0.1, 0.5, 1}) {
               int bottom = (int)Math.floor((float)(height - 40) / scale);
               int entryHeight = (int)(9 * (spacing + 1));
               int textOffset = (int)Math.round(8 * (spacing + 1) - 4 * spacing);
               for (int row = 0; row < 20; row++) {
                  double y = (bottom - row * entryHeight - textOffset + 4.0) * scale;
                  require(ChatRowGeometry.rowAt(y, height, scale, spacing, 9, 20) == row,
                     "Wrong chat row: " + row + ", scale=" + scale + ", spacing=" + spacing);
                  clicks++;
               }
            }
         }
      }
      require(ChatRowGeometry.rowAt(0, 480, 0, 0, 9, 20) == -1, "Zero scale");
      require(ChatRowGeometry.rowAt(479, 480, 1, 0, 9, 20) == -1, "Outside chat");
      PunishmentAnnouncement ban = PunishmentAnnouncement.parse("❨！❩ Игрок LLLLL55555 забанил hohonit на 6 часов по причине: 3.4 (выживание)");
      require(ban != null && ban.issuer().equals("LLLLL55555") && ban.target().equals("hohonit"), "Ban players");
      require(ban.duration().equals("6 часов") && ban.cleanReason().equals("3.4") && ban.mode().equals("выживание"), "Ban fields");
      PunishmentAnnouncement mute = PunishmentAnnouncement.parse("❨！❩ Игрок Moderator замутил Player123 на 30 минут по причине: 2.1 (выживание)");
      require(mute != null && mute.kind() == PunishmentAnnouncement.Kind.MUTE, "Mute");
      require(PunishmentAnnouncement.parse("Ⓖ ArsenyNovikov ⇨ привет") == null, "Adjacent player message");
      require(PunishmentAnnouncement.parse("Player123 ⇨ ❨！❩ Игрок Moderator забанил Player456 на 6 часов по причине: 3.4") == null, "Player-spoofed prefix");
      require(PlayerNames.isValid("Player_123") && !PlayerNames.isValid("Русский"), "Chat/profile names remain Latin-only");
      require(!PlayerNames.isValid("ab") && !PlayerNames.isValid("nick-123"), "Reject too-short or unsupported chat/profile names");
      require(PunishmentScope.fromAnnouncementMode("выживание") == PunishmentScope.SURVIVAL, "survival mode");
      require(PunishmentScope.fromAnnouncementMode("гриферский") == PunishmentScope.GRIEF, "grief mode");
      require(PunishmentScope.fromAnnouncementMode("мини-игры") == PunishmentScope.MINIGAMES, "minigames mode");
      require(PunishmentScope.fromAnnouncementMode("глобальный") == PunishmentScope.GLOBAL, "global mode");
      require(PunishmentScope.fromAnnouncementMode("неизвестный") == null, "unknown mode");
      require(ru.dmitry.rulehelper.PunishmentScopeFormatter.apply("/unban player nekk", null, PunishmentScope.GRIEF, ru.dmitry.rulehelper.RuleHelperConfig.DonationTier.LEADER)
         .equals("/unban player nekk server:гриферский"), "release targets announcement mode");
      require(ru.dmitry.rulehelper.PunishmentScopeFormatter.apply("/unmute player nekk -g", null, PunishmentScope.SURVIVAL, ru.dmitry.rulehelper.RuleHelperConfig.DonationTier.LEADER)
         .equals("/unmute player nekk server:выживание"), "release replaces prior selector");
      require(ModerationAlertDetector.familyInsult("Игрок Steve ⇨ Батя твой ебанат") != null,
         "family insult alert covers father insult");
      require(ModerationAlertDetector.familyInsult("Игрок Steve ⇨ Мать чекни") != null,
         "family alert covers bait phrase");
      var history = new HistoryPunishmentParser();
      var now = java.time.LocalDateTime.of(2026, 11, 26, 21, 4, 59);
      var externalHistory = new HistoryPunishmentParser();
      externalHistory.begin("shll");
      require(!externalHistory.acceptLine("История наказаний игроком otherNick (Записей: 2)", now)
         && !externalHistory.acceptLine("- 2 часов назад -", now),
         "Do not suppress a manually requested /shist for another player");
      history.begin("shll");
      require(history.acceptLine("История наказаний игроком shll (Записей: 2)", now),
         "Recognize own /shist response header for suppression");
      require(history.acceptLine("- 2 часов 14 минут назад -", now),
         "Recognize age separator in own /shist response");
      require(!history.acceptLine("Ⓖ anotherPlayer → hello", now),
         "Keep unrelated chat visible while own /shist response is loading");
      require(history.acceptLine("neadamskidotcom был забанен на 1 день shll: 3.4 (глобальный)", now),
         "Recognize own /shist punishment record for suppression");
      history.acceptLine("- 1 день 8 часов 4 минут назад -", now);
      history.acceptLine("xideZ был забанен на 3 дней shll: 1.8 (глобальный)", now);
      require(history.today().size() == 1, "Daily history excludes punishments from yesterday");
      require(history.today().getFirst().target().equals("neadamskidotcom")
         && history.today().getFirst().issuedAt().equals(java.time.LocalDateTime.of(2026, 11, 26, 18, 50, 59)),
         "Daily history uses each /shist relative timestamp");
      System.out.println("PASS: " + clicks + " chat row clicks, punishment parsing, mode selectors and /shist daily filtering");
   }
   private static void require(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
