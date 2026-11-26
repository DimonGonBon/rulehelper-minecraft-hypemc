package ru.dmitry.rulehelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.InputConstants;

/** Small per-install configuration recovered from the distributed JARs. */
public final class RuleHelperConfig {
   public enum DonationTier {
      LEADER,
      STAFF
   }

   public enum CauseStyle {
      SHORT("Укороченное"), EXTENDED("Расширенное"), CORNER("「 」"),
      DOUBLE_CORNER("『 』"), SPARKLE("✦「 」✦"), BOX("【 】");
      private final String label;
      CauseStyle(String label) { this.label = label; }
      public String label() { return label; }
   }

   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rulehelper.properties");
   private static String nickname = "";
   private static DonationTier tier;
   private static CauseStyle causeStyle = CauseStyle.SHORT;
   private static String alertPrefix = "⚠ RuleHelper » ";
   private static String chatMenuPrefix = "»";
   private static int dailyHistoryKey = InputConstants.KEY_F9;

   static {
      load();
   }

   private RuleHelperConfig() {}

   public static boolean isConfigured() {
      return !nickname.isBlank() && tier != null;
   }

   public static String nickname() {
      return nickname;
   }

   public static DonationTier tier() {
      return tier;
   }

   public static String tierLabel() {
      return tier == DonationTier.LEADER ? "Правитель+" : "Staff-";
   }

   public static CauseStyle causeStyle() { return causeStyle; }
   public static String alertPrefix() { return alertPrefix; }
   public static String chatMenuPrefix() { return chatMenuPrefix; }
   public static int dailyHistoryKey() { return dailyHistoryKey; }

   public static void setAlertPrefix(String value) {
      String prefix = value == null ? "" : value;
      alertPrefix = prefix.substring(0, Math.min(32, prefix.length()));
      persist();
   }

   public static void setChatMenuPrefix(String value) {
      String prefix = value == null ? "" : value.strip();
      chatMenuPrefix = prefix.substring(0, Math.min(8, prefix.length()));
      persist();
   }

   public static void setDailyHistoryKey(int key) {
      dailyHistoryKey = key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_HOME
         ? InputConstants.KEY_F9 : key;
      persist();
   }

   public static void setCauseStyle(CauseStyle style) {
      causeStyle = style == null ? CauseStyle.SHORT : style;
      persist();
   }

   public static String formatCause(String command, String ruleCode, String title) {
      if (ruleCode == null || title == null) return command;
      Matcher matcher = Pattern.compile("(?<![0-9.])" + Pattern.quote(ruleCode) + "(?:\\[([^\\]]*)\\])?(?![0-9.])").matcher(command);
      if (!matcher.find()) return command;
      String extra = matcher.group(1);
      if (causeStyle == CauseStyle.SHORT) {
         String shortReason = ruleCode + (extra == null || extra.isBlank() ? "" : "[" + extra.trim() + "]");
         return matcher.replaceFirst(Matcher.quoteReplacement(shortReason));
      }
      String label = ruleCode.equals("3.4") ? "Некорректные наказания" : title;
      String text = ruleCode + " | " + label + (extra == null || extra.isBlank() ? "" : " · " + extra.trim());
      String styled = switch (causeStyle) {
         case CORNER -> "「 " + text + " 」";
         case DOUBLE_CORNER -> "『 " + text + " 』";
         case SPARKLE -> "✦「 " + text + " 」✦";
         case BOX -> "【 " + text + " 】";
         default -> text;
      };
      return matcher.replaceFirst(Matcher.quoteReplacement(styled));
   }

   public static void save(String name, DonationTier selectedTier) {
      nickname = name.trim();
      tier = selectedTier;
      persist();
   }

   /** Keeps donation scope for standalone extra commands that are not tied to a numbered rule. */
   public static String applyDonationScope(String command) {
      return applyDonationScope(command, null);
   }

   /** Adds -g for Правитель+ only to rules in sections 1–3; survival and minigames are local. */
   public static String applyDonationScope(String command, String ruleCode) {
      String normalized = command.trim();
      while (normalized.endsWith(" -g")) {
         normalized = normalized.substring(0, normalized.length() - 3).trim();
      }
      boolean globalRule = ruleCode != null && (ruleCode.startsWith("1.") || ruleCode.startsWith("2.") || ruleCode.startsWith("3."));
      boolean shouldUseGlobal = ruleCode == null ? tier == DonationTier.LEADER : globalRule && tier == DonationTier.LEADER;
      if (shouldUseGlobal) {
         normalized += " -g";
      }
      return normalized;
   }

   /** Uses an explicitly detected server when releasing an announced punishment. */
   public static String applyPunishmentScope(String command, PunishmentScope scope) {
      String ruleCode = null;
      return applyPunishmentScope(command, ruleCode, scope);
   }

   public static String applyPunishmentScope(String command, String ruleCode, PunishmentScope scope) {
      if (scope == null || scope == PunishmentScope.RULE_DEFAULT) {
         return applyDonationScope(command, ruleCode);
      }
      if (scope == PunishmentScope.CURRENT) return command.trim();
      return scope.applyToCommand(command);
   }

   private static void load() {
      if (!Files.isRegularFile(FILE)) {
         return;
      }
      Properties properties = new Properties();
      try (InputStream input = Files.newInputStream(FILE)) {
         properties.load(input);
         alertPrefix = properties.getProperty("alert_prefix", alertPrefix);
         chatMenuPrefix = properties.getProperty("chat_menu_prefix", chatMenuPrefix);
         try { dailyHistoryKey = Integer.parseInt(properties.getProperty("daily_history_key", Integer.toString(dailyHistoryKey))); }
         catch (NumberFormatException ignored) { dailyHistoryKey = InputConstants.KEY_F9; }
         try { causeStyle = CauseStyle.valueOf(properties.getProperty("causeStyle", "SHORT")); }
         catch (IllegalArgumentException ignored) { causeStyle = CauseStyle.SHORT; }
         String loadedName = properties.getProperty("nickname", "").trim();
         String loadedTier = properties.getProperty("donation", "");
         if (PlayerNames.isValid(loadedName)) {
            try {
               nickname = loadedName;
               tier = DonationTier.valueOf(loadedTier);
            } catch (IllegalArgumentException ignored) {
               nickname = "";
               tier = null;
            }
         }
      } catch (IOException ignored) {
         nickname = "";
         tier = null;
      }
   }

   private static void persist() {
      Properties properties = new Properties();
      properties.setProperty("nickname", nickname);
      if (tier != null) properties.setProperty("donation", tier.name());
      properties.setProperty("causeStyle", causeStyle.name());
      properties.setProperty("alert_prefix", alertPrefix);
      properties.setProperty("chat_menu_prefix", chatMenuPrefix);
      properties.setProperty("daily_history_key", Integer.toString(dailyHistoryKey));
      try {
         Files.createDirectories(FILE.getParent());
         try (OutputStream output = Files.newOutputStream(FILE)) { properties.store(output, "Rule Helper settings"); }
      } catch (IOException exception) {
         throw new IllegalStateException("Could not save Rule Helper settings", exception);
      }
   }
}
