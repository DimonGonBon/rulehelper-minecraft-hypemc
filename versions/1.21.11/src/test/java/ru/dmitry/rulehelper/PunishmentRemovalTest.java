package ru.dmitry.rulehelper;

import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;
import ru.dmitry.rulehelper.chat.PunishmentCopyPrefixes;
import ru.dmitry.rulehelper.chat.PunishmentRemovalAnnouncement;
import ru.dmitry.rulehelper.chat.PunishmentRemovalAnnouncement.Kind;

public final class PunishmentRemovalTest {
   public static void main(String[] args) {
      var unban = parse("❨！❩ Игрок Vayroneh1k разбанил x_Ver0nika_x по причине: 3.4 (выживание)", Kind.UNBAN);
      require(unban.issuer().equals("Vayroneh1k") && unban.target().equals("x_Ver0nika_x"), "screenshot participants");
      require(unban.cleanReason().equals("3.4") && unban.mode().equals("выживание"), "screenshot reason and mode");
      var unmute = parse("❨！❩ Игрок ECLIPS_0 размутил judoa на 30 минут по причине: [2.2] (глобальный)", Kind.UNMUTE, false);
      // A newly issued mute or an ambiguous hybrid is not a removal announcement.
      require(unmute == null, "reject a removal with an issuing duration");
      var mute = parse("❨！❩ Игрок ECLIPS_0 размутил judoa по причине: test (глобальный)", Kind.UNMUTE);
      for (var removal : new PunishmentRemovalAnnouncement[]{unban, mute,
         parse("❨！❩ Игрок Mod123 разбанил IP Player123 по причине: nekk (гриферский)", Kind.UNBAN),
         parse("❨！❩ Игрок Mod123 размутил игрока Player123 (мини-игры)", Kind.UNMUTE),
         parse("❨！❩ Игрок Mod123 разбанил Player123", Kind.UNBAN)}) {
         String command = PunishmentRemovalCommands.correction(removal, "SHORT", null, RuleHelperConfig.DonationTier.LEADER);
         require(command.equals("/ban " + removal.issuer() + " 1d 3.5 -g"), "only issuer gets a one-day global 3.5 ban");
         require(!command.contains(removal.target()) && !command.contains("server:"), "freed player and removal mode cannot redirect the ban");
         require(PunishmentAnnouncement.parse(removal.originalLine()) == null, "removal is never counted as a newly issued punishment");
         Component decorated = PunishmentCopyPrefixes.add(Component.literal(removal.originalLine()));
         require(decorated.getString().startsWith("🛡 ☠ "), "removal announcements keep the two copy prefixes");
         var reparsed = PunishmentRemovalAnnouncement.parse(decorated.getString());
         require(reparsed != null && reparsed.issuer().equals(removal.issuer()) && reparsed.target().equals(removal.target()), "copy prefixes keep correct participants");
      }
      for (String marker : new String[]{"❨！❩", "❨ ！ ❩", "( ! )", "(!)", "(！)", "(  ！  )"}) {
         String wrapped = marker + " Игрок Veyroneh1k разбанил x_Ver0nika_x по причине: 3.4\n(выживание)";
         var parsed = parse(wrapped, Kind.UNBAN);
         require(parsed.issuer().equals("Veyroneh1k") && parsed.target().equals("x_Ver0nika_x"), "actual spaced-marker screenshot participants");
         require(parsed.cleanReason().equals("3.4") && parsed.mode().equals("выживание"), "wrapped mode survives marker normalization");
         Component prefixed = PunishmentCopyPrefixes.add(Component.literal(wrapped));
         require(prefixed.getString().startsWith("🛡 ☠ "), "actual removal format gets visible copy prefixes");
         var clickable = PunishmentRemovalAnnouncement.parse(prefixed.getString());
         require(clickable != null && PunishmentRemovalCommands.correction(clickable, "SHORT", null, RuleHelperConfig.DonationTier.LEADER)
            .equals("/ban Veyroneh1k 1d 3.5 -g"), "decorated wrapped removal opens 3.5 for the remover");
         require(PunishmentAnnouncement.parse(wrapped) == null, "spaced-marker removal is not a new punishment");
         var issued = PunishmentAnnouncement.parse(marker + " Игрок Issuer123 забанил Victim123 на 1 день по причине: 3.4 (выживание)");
         require(issued != null && issued.issuer().equals("Issuer123"), "issuing notices share the same marker variants");
         require(ru.dmitry.rulehelper.chat.ModerationAlertDetector.incorrectPunishment(
            marker + " Игрок Issuer123 забанил Issuer123 на 1 день по причине: test (выживание)", "Moderator") != null,
            "incorrect-punishment alerts accept the same marker variants");
         require(PunishmentRemovalAnnouncement.parse("Ⓖ Player123 → " + wrapped) == null, "quoted spaced-marker removal is not a server announcement");
      }
      ReasonTemplates templates = new ReasonTemplates();
      templates.setRule("3.5", "&f{rule} &b┃ {reason}");
      String custom = PunishmentRemovalCommands.correction(unban, "CUSTOM", templates, RuleHelperConfig.DonationTier.LEADER);
      require(custom.equals("/ban Vayroneh1k 1d &f3.5 &b┃ Некорректное снятие наказаний&r -g"), "individual 3.5 reason template applies to review");
      require(PunishmentRemovalCommands.correction(unban, "COLORED", null, RuleHelperConfig.DonationTier.LEADER).contains("&f&l&o3.5 &b&l&o"), "ban reason is blue, not the green removal reason");
      require(PunishmentRemovalCommands.correction(null, "SHORT", null, RuleHelperConfig.DonationTier.LEADER) == null, "no command without captured removal");
      for (String invalid : new String[]{"Ⓖ Player123 → " + unban.originalLine(),
         "Player123 → " + unban.originalLine(), "❨！❩ Игрок Mod123 забанил Player123 на 1 день по причине: 3.5",
         "История наказаний игроком Player123", "Player123 был разбанен Console: Покупка разбана"}) {
         require(PunishmentRemovalAnnouncement.parse(invalid) == null, "do not capture ordinary chat/history/ban as a removal");
      }
      System.out.println("PASS: ASCII/spaced/wrapped unban/unmute review, exact 3.5 issuer target, one global ban, copy prefixes and per-rule custom reasons");
   }
   private static PunishmentRemovalAnnouncement parse(String text, Kind kind) { return parse(text, kind, true); }
   private static PunishmentRemovalAnnouncement parse(String text, Kind kind, boolean expected) {
      var result = PunishmentRemovalAnnouncement.parse(text);
      require(expected ? result != null && result.kind() == kind : result == null, "parse " + kind + ": " + text);
      return result;
   }
   private static void require(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
}
