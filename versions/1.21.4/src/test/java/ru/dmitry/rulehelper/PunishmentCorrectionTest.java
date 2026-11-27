package ru.dmitry.rulehelper;

import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.chat.ModerationAlertDetector;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement.Kind;
import ru.dmitry.rulehelper.chat.PunishmentCopyPrefixes;

public final class PunishmentCorrectionTest {
   public static void main(String[] args) {
      // Legacy HoverEvent initializes item registries even for a text-only tooltip.
      net.minecraft.SharedConstants.tryDetectVersion();
      net.minecraft.server.Bootstrap.bootStrap();
      var warn = parse("❨！❩ Игрок Moderator выдал предупреждение Player123 по причине: 4.2 (выживание)", Kind.WARN);
      require(warn.issuer().equals("Moderator") && warn.target().equals("Player123"), "warning participants");
      require(warn.cleanReason().equals("4.2") && warn.mode().equals("выживание"), "warning fields");
      equal(PunishmentCorrectionCommands.release(warn, "SHORT"),
         "/unwarn Player123 nekk server:выживание", "unwarn addresses the announcement's mode");
      require(!PunishmentCorrectionCommands.mayCorrectWithoutRelease(warn), "a mode-aware warning is removed first");
      equal(PunishmentCorrectionCommands.correction(warn, "1d", "SHORT", null, RuleHelperConfig.DonationTier.LEADER),
         "/ban Moderator 1d 3.4 -g", "warning correction targets issuer globally");
      String green = "&f&l&o3.4 &a&l&o━━➤ 「 Некорректное наказание 」&r";
      String blue = "&f&l&o3.4 &b&l&o━━➤ 「 Некорректное наказание 」&r";
      equal(PunishmentCorrectionCommands.release(warn, "COLORED"),
         "/unwarn Player123 " + green + " server:выживание", "warning removal uses the selected green style");
      equal(PunishmentCorrectionCommands.correction(warn, "3d", "COLORED", null, RuleHelperConfig.DonationTier.LEADER),
         "/ban Moderator 3d " + blue + " -g", "warning correction uses the selected blue style");
      equal(CauseFormatter.formatCommand("/unwarn Player123 nekk server:гриферский", null, null, "COLORED"),
         "/unwarn Player123 " + green + " server:гриферский", "standalone unwarn formatting");

      for (String[] mode : new String[][]{{"глобальный", "-g"}, {"выживание", "server:выживание"},
            {"гриферский", "server:гриферский"}, {"мини-игры", "server:мини-игры"}}) {
         var warning = parse("❨！❩ Игрок Mod123 предупредил игрока Player123 по причине: 4.2 (" + mode[0] + ")", Kind.WARN);
         equal(PunishmentCorrectionCommands.release(warning, "SHORT"),
            "/unwarn Player123 nekk " + mode[1], "warning removal selector " + mode[0]);
      }
      parse("❨！❩ Игрок Mod123 выдал варн игроку Player123 по причине: 4.2 (гриферский)", Kind.WARN);
      var unknownWarn = parse("❨！❩ Игрок Moderator выдал предупреждение Player123 по причине: 4.2", Kind.WARN);
      require(PunishmentCorrectionCommands.release(unknownWarn, "SHORT") == null, "do not guess a warning's mode");
      require(PunishmentCorrectionCommands.mayCorrectWithoutRelease(unknownWarn), "warning remains reviewable when its mode is missing");
      var emptyWarn = parse("❨！❩ Игрок Moderator предупредил Player123 (гриферский)", Kind.WARN);
      require(emptyWarn.cleanReason().isBlank() && emptyWarn.mode().equals("гриферский"), "warning without reason retains mode");
      require(ModerationAlertDetector.incorrectPunishment(emptyWarn.originalLine(), "Moderator") != null,
         "a warning without reason triggers the existing alert");

      for (String verb : new String[]{"кикнул", "выгнал", "выкинул"}) {
         var kick = parse("❨！❩ Игрок Moderator " + verb + " Player123 с сервера по причине: test (выживание)", Kind.KICK);
         require(PunishmentCorrectionCommands.release(kick, "COLORED") == null, "a completed kick never generates a reversal");
         require(PunishmentCorrectionCommands.mayCorrectWithoutRelease(kick), "kick can prepare 3.4 directly");
         equal(PunishmentCorrectionCommands.correction(kick, "1d", "SHORT", null, RuleHelperConfig.DonationTier.LEADER),
            "/ban Moderator 1d 3.4 -g", "kick correction targets issuer, not victim");
         require(ModerationAlertDetector.incorrectPunishment(kick.originalLine(), "Moderator") != null, "suspicious kick alert");
      }
      var emptyKick = parse("❨！❩ Игрок Moderator кикнул Player123", Kind.KICK);
      require(emptyKick.cleanReason().isBlank(), "a kick without reason is still clickable");
      require(ModerationAlertDetector.incorrectPunishment(emptyKick.originalLine(), "Moderator") != null, "kick without reason alert");
      var validKick = parse("❨！❩ Игрок Moderator кикнул Player123 по причине: 3.2", Kind.KICK);
      require(ModerationAlertDetector.incorrectPunishment(validKick.originalLine(), "Moderator") == null,
         "ordinary kicks are not automatically marked incorrect");
      var self = parse("❨！❩ Игрок Moderator выдал предупреждение Moderator по причине: test (глобальный)", Kind.WARN);
      equal(PunishmentCorrectionCommands.correction(self, "6h", "SHORT", null, RuleHelperConfig.DonationTier.LEADER), "/ban Moderator 6h 3.4 -g", "self-punishment duration stays available");
      require(ModerationAlertDetector.incorrectPunishment(self.originalLine(), "Moderator") != null, "self warning alert");

      var ban = parse("❨！❩ Игрок Moderator забанил IP Player123 на 1 день по причине: 3.4 (выживание)", Kind.BAN);
      equal(PunishmentCorrectionCommands.release(ban, "SHORT"), "/unbanip Player123 nekk server:выживание", "IP ban reversal retained");
      var mute = parse("❨！❩ Игрок Moderator замутил Player123 на 3 часа по причине: 2.3 (глобальный)", Kind.MUTE);
      equal(PunishmentCorrectionCommands.release(mute, "SHORT"), "/unmute Player123 nekk -g", "mute reversal retained");
      var ipMute = parse("❨！❩ Игрок Moderator замутил IP Player123 на 1 день по причине: 2.3 (мини-игры)", Kind.MUTE);
      equal(PunishmentCorrectionCommands.release(ipMute, "SHORT"), "/unmuteip Player123 nekk server:мини-игры", "IP mute uses its matching command");
      var unknownBan = parse("❨！❩ Игрок Moderator забанил Player123 на 1 день по причине: 3.4", Kind.BAN);
      require(PunishmentCorrectionCommands.release(unknownBan, "SHORT") == null
         && !PunishmentCorrectionCommands.mayCorrectWithoutRelease(unknownBan), "unscoped bans keep the previous restriction");

      for (var punishment : new PunishmentAnnouncement[]{warn, emptyKick, ban, mute}) {
         Component withPrefixes = PunishmentCopyPrefixes.add(Component.literal(punishment.originalLine()));
         require(withPrefixes.getString().startsWith("🛡 ☠ "), "all four types get copy prefixes");
         var reparsed = parse(withPrefixes.getString(), punishment.kind());
         require(reparsed.issuer().equals(punishment.issuer()) && reparsed.target().equals(punishment.target()), "copy prefixes preserve participants");
         require(withPrefixes.getSiblings().get(0).getStyle().getClickEvent() != null
            && withPrefixes.getSiblings().get(2).getStyle().getClickEvent() != null, "both copy prefixes remain interactive");
      }
      parse("&9🛡 &c☠ &f❨！❩ Игрок Moderator выдал предупреждение Player123 по причине: &f&l&o4.2 &b&l&o━━➤ 「 Помеха игрокам 」&r (выживание)", Kind.WARN);
      require(PunishmentAnnouncement.parse("Ⓖ Player123 → " + warn.originalLine()) == null, "player chat cannot masquerade as an announcement");
      require(PunishmentAnnouncement.parse("❨！❩ Игрок Moderator снял предупреждение Player123 по причине: 3.4") == null, "removal is not a new punishment");
      require(PunishmentCorrectionCommands.correction(warn, "0d", "SHORT", null, RuleHelperConfig.DonationTier.LEADER) == null
         && PunishmentCorrectionCommands.correction(warn, null, "SHORT", null, RuleHelperConfig.DonationTier.LEADER) == null, "unprepared or unsupported durations produce no command");
      System.out.println("PASS: warning/kick parsing, copy prefixes, safe reversal planning, mode selectors and global 3.4 commands");
   }

   private static PunishmentAnnouncement parse(String line, Kind expected) {
      var result = PunishmentAnnouncement.parse(line);
      require(result != null && result.kind() == expected, "parse " + expected + ": " + line);
      return result;
   }
   private static void equal(String actual, String expected, String label) {
      if (!expected.equals(actual)) throw new AssertionError(label + "\nexpected: " + expected + "\nactual: " + actual);
   }
   private static void require(boolean value, String label) {
      if (!value) throw new AssertionError(label);
   }
}
