package ru.dmitry.rulehelper;

import java.util.Locale;

/** Applies the server selector syntax supported by HypeMC moderation commands. */
public final class PunishmentScopeFormatter {
   private PunishmentScopeFormatter() {}

   public static String apply(String command, String ruleCode, PunishmentScope scope,
                              RuleHelperConfig.DonationTier tier) {
      PunishmentScope selected = scope == null ? PunishmentScope.RULE_DEFAULT : scope;
      if (selected == PunishmentScope.RULE_DEFAULT) {
         String normalized = command.trim();
         while (normalized.endsWith(" -g")) normalized = normalized.substring(0, normalized.length() - 3).trim();
         boolean globalRule = ruleCode != null && (ruleCode.startsWith("1.") || ruleCode.startsWith("2.") || ruleCode.startsWith("3."));
         boolean shouldUseGlobal = ruleCode == null ? tier == RuleHelperConfig.DonationTier.LEADER : globalRule;
         return shouldUseGlobal ? normalized + " -g" : normalized;
      }

      String normalized = command.trim();
      String[] selectors = {"-g", "server:выживание", "server:гриферский", "server:мини-игры"};
      for (String selector : selectors) {
         String suffix = " " + selector;
         while (normalized.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
         }
      }
      String selector = switch (selected) {
         case GLOBAL -> "-g";
         case SURVIVAL -> "server:выживание";
         case GRIEF -> "server:гриферский";
         case MINIGAMES -> "server:мини-игры";
         case CURRENT, RULE_DEFAULT -> "";
      };
      return selector.isEmpty() ? normalized : normalized + " " + selector;
   }
}
