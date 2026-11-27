package ru.dmitry.rulehelper;

import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement.Kind;

/** Donator correction commands: release the exact mode; respect the saved donor group for 3.4. */
public final class PunishmentCorrectionCommands {
   private PunishmentCorrectionCommands() {}
   public static boolean mayCorrectWithoutRelease(PunishmentAnnouncement announcement) {
      return announcement != null && (announcement.kind() == Kind.KICK || (announcement.kind() == Kind.WARN
         && PunishmentScope.fromAnnouncementMode(announcement.mode()) == null));
   }
   public static String release(PunishmentAnnouncement announcement, String style) {
      return release(announcement, style, null);
   }
   public static String release(PunishmentAnnouncement announcement, String style, ReasonTemplates templates) {
      if (announcement == null || !announcement.kind().canBeRemoved()) return null;
      PunishmentScope scope = PunishmentScope.fromAnnouncementMode(announcement.mode());
      if (scope == null) return null;
      String verb = switch (announcement.kind()) {
         case BAN -> announcement.ipTarget() ? "/unbanip " : "/unban ";
         case MUTE -> announcement.ipTarget() ? "/unmuteip " : "/unmute ";
         case WARN -> "/unwarn ";
         case KICK -> throw new IllegalStateException("Kick has no reversal");
      };
      return scope.applyToCommand(verb + announcement.target() + " " + CauseFormatter.releaseReason(style, templates));
   }
   public static String correction(PunishmentAnnouncement announcement, String duration, String style,
                                   ReasonTemplates templates, RuleHelperConfig.DonationTier tier) {
      if (announcement == null || tier == null || duration == null
         || !(duration.equals("6h") || duration.equals("1d") || duration.equals("3d"))) return null;
      String raw = "/ban " + announcement.issuer() + " " + duration + " 3.4";
      String formatted = CauseFormatter.formatCommand(raw, "3.4", "Некорректное наказание", style, templates);
      return DonatorScopePolicy.apply(formatted, "3.4", tier);
   }
}
