package ru.dmitry.rulehelper;

/** Existing donator-group scope policy shared by menu and correction commands. */
public final class DonatorScopePolicy {
   private DonatorScopePolicy() {}
   public static boolean usesGlobal(String ruleCode, RuleHelperConfig.DonationTier tier) {
      boolean globalRule = ruleCode != null && (ruleCode.startsWith("1.") || ruleCode.startsWith("2.") || ruleCode.startsWith("3."));
      return tier == RuleHelperConfig.DonationTier.LEADER && (ruleCode == null || globalRule);
   }
   public static String apply(String command, String ruleCode, RuleHelperConfig.DonationTier tier) {
      String result = command.trim();
      while (result.endsWith(" -g")) result = result.substring(0, result.length() - 3).trim();
      return usesGlobal(ruleCode, tier) ? result + " -g" : result;
   }
}
