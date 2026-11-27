package ru.dmitry.rulehelper;

import ru.dmitry.rulehelper.RuleHelperConfig.DonationTier;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

public final class DonatorScopeTest {
   public static void main(String[] args) {
      require(RuleCatalog.ALL.size() == 26 && RuleCatalog.ALL.stream().noneMatch(r -> r.code().equals("1.3") || r.code().equals("4.1")),
         "donator catalog keeps official-team-only rules excluded");
      for (var rule : RuleCatalog.ALL) {
         for (var action : rule.actions()) {
            String raw = action.commandFor("Player123");
            String leader = DonatorScopePolicy.apply(raw, rule.code(), DonationTier.LEADER);
            String staff = DonatorScopePolicy.apply(raw, rule.code(), DonationTier.STAFF);
            require(!staff.endsWith(" -g"), "Staff- never gets global scope by default");
            require(leader.endsWith(" -g") == (rule.code().charAt(0) <= '3'), "Правитель+ retains rule-section scope");
         }
      }
      require(DonatorScopePolicy.apply("/ban Player123 1d 3.4 -g", "3.4", DonationTier.STAFF).equals("/ban Player123 1d 3.4"),
         "group change removes a prior global selector");
      for (String verb : new String[]{"забанил", "замутил", "выдал предупреждение", "кикнул"}) {
         var announcement = PunishmentAnnouncement.parse("❨！❩ Игрок Issuer123 " + verb + " Victim123 по причине: 3.4 (выживание)");
         require(announcement != null, "supported announcement " + verb);
         String leader = PunishmentCorrectionCommands.correction(announcement, "1d", "SHORT", null, DonationTier.LEADER);
         String staff = PunishmentCorrectionCommands.correction(announcement, "1d", "SHORT", null, DonationTier.STAFF);
         require(leader.equals("/ban Issuer123 1d 3.4 -g"), "Правитель+ correction addresses issuer globally");
         require(staff.equals("/ban Issuer123 1d 3.4"), "Staff- correction keeps its existing local scope");
         require(PunishmentCorrectionCommands.correction(announcement, "1d", "SHORT", null, null) == null,
            "an unconfigured group cannot prepare a punishment");
      }
      System.out.println("PASS: donor-only catalog and Правитель+/Staff- scope restrictions across all four punishment types");
   }
   private static void require(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
}
