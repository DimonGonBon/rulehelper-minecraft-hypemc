package ru.dmitry.rulehelper;

import ru.dmitry.rulehelper.chat.PunishmentRemovalAnnouncement;

/** A 3.5 review has one action: ban the remover; never change the freed player's status. */
public final class PunishmentRemovalCommands {
   private PunishmentRemovalCommands() {}

   public static String correction(PunishmentRemovalAnnouncement announcement, String style, ReasonTemplates templates, RuleHelperConfig.DonationTier tier) {
      if (announcement == null || tier == null || !ChatNames.isValid(announcement.issuer())) return null;
      String formatted = CauseFormatter.formatCommand("/ban " + announcement.issuer() + " 1d 3.5",
         "3.5", "Некорректное снятие наказаний", style, templates);
      return DonatorScopePolicy.apply(formatted, "3.5", tier);
   }
}
