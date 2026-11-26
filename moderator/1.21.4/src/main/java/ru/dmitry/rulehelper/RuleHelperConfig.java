package ru.dmitry.rulehelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;

/** Small per-install configuration recovered from the distributed JARs. */
public final class RuleHelperConfig {
   public enum DonationTier {
      LEADER,
      STAFF
   }

   public enum CauseStyle {
      SHORT("Укороченное"),
      EXTENDED("Расширенное"),
      CORNER("「 」"),
      DOUBLE_CORNER("『 』"),
      SPARKLE("✦「 」✦"),
      BOX("【 】");

      private final String label;
      CauseStyle(String label) { this.label = label; }
      public String label() { return label; }
   }

   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rulehelper.properties");
   private static String nickname = "";
   private static DonationTier tier;
   private static CauseStyle causeStyle = CauseStyle.SHORT;
   private static String moderatorVkContact = "";
   private static String moderatorTelegramContact = "";
   private static String moderatorDiscordContact = "";
   private static int proofRequestKey = InputConstants.KEY_J;
   private static int dailyHistoryKey = InputConstants.KEY_F9;
   private static int settingsKey = InputConstants.KEY_HOME;
   private static String alertPrefix = "⚠ RuleHelper » ";
   private static String chatMenuPrefix = "»";
   private static final java.util.Set<String> excludedPlayers = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);

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

   public static String moderatorVkContact() { return moderatorVkContact; }
   public static String moderatorTelegramContact() { return moderatorTelegramContact; }
   public static String moderatorDiscordContact() { return moderatorDiscordContact; }
   public static int proofRequestKey() { return proofRequestKey; }
   public static int dailyHistoryKey() { return dailyHistoryKey; }
   public static int settingsKey() { return settingsKey; }
   public static String alertPrefix() { return alertPrefix; }
   public static String chatMenuPrefix() { return chatMenuPrefix; }
   public static java.util.Set<String> excludedPlayers() { return java.util.Set.copyOf(excludedPlayers); }
   public static boolean isExcluded(String name) { return name != null && excludedPlayers.contains(name.trim()); }
   public static boolean containsExcludedName(String message) {
      if (message == null || message.isBlank()) return false;
      for (String name : excludedPlayers) {
         if (java.util.regex.Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])" + java.util.regex.Pattern.quote(name)
            + "(?![\\p{L}\\p{N}_])").matcher(message).find()) return true;
      }
      return false;
   }
   public static boolean addExcludedPlayer(String name) {
      String cleaned = name == null ? "" : name.trim();
      if (!EvidenceRequestNames.isValid(cleaned) || !excludedPlayers.add(cleaned)) return false;
      persist();
      return true;
   }
   public static void setAlertPrefix(String prefix) {
      alertPrefix = prefix == null ? "" : prefix.substring(0, Math.min(32, prefix.length()));
      persist();
   }
   public static void setChatMenuPrefix(String prefix) {
      String value = prefix == null ? "" : prefix.strip();
      chatMenuPrefix = value.substring(0, Math.min(8, value.length()));
      persist();
   }
   public static void saveModerationKeys(int proof, int history) {
      int[] requested = {proof, history};
      int[] defaults = {InputConstants.KEY_J, InputConstants.KEY_F9};
      java.util.Set<Integer> used = new java.util.HashSet<>();
      used.add(InputConstants.KEY_HOME);
      int[] resolved = new int[2];
      for (int i = 0; i < requested.length; i++) {
         int candidate = validKey(requested[i], defaults[i]);
         if (!used.add(candidate)) {
            candidate = defaults[i];
            if (!used.add(candidate)) {
               candidate = InputConstants.KEY_F6 + i;
               while (!used.add(candidate)) candidate++;
            }
         }
         resolved[i] = candidate;
      }
      proofRequestKey = resolved[0];
      dailyHistoryKey = resolved[1];
      settingsKey = InputConstants.KEY_HOME;
      persist();
   }
   private static int validKey(int key, int fallback) {
      return key == InputConstants.KEY_ESCAPE ? fallback : key;
   }

   public static void saveProofRequestKey(int key) {
      if (key == InputConstants.KEY_HOME || key == dailyHistoryKey) {
         key = InputConstants.KEY_J;
      }
      proofRequestKey = validKey(key, InputConstants.KEY_J);
      persist();
   }

   public static void saveModeratorContacts(String vk, String telegram, String discord) {
      moderatorVkContact = vk.trim();
      moderatorTelegramContact = telegram.trim();
      moderatorDiscordContact = discord.trim();
      persist();
   }

   public static String tierLabel() {
      return tier == DonationTier.LEADER ? "Правитель+" : "Staff-";
   }

   public static CauseStyle causeStyle() { return causeStyle; }

   public static void setCauseStyle(CauseStyle style) {
      causeStyle = style == null ? CauseStyle.SHORT : style;
      persist();
   }

   public static String formatCause(String command, String ruleCode, String title) {
      CauseStyle style = causeStyle;
      if (ruleCode == null || title == null) return command;
      Pattern pattern = Pattern.compile("(?<![0-9.])" + Pattern.quote(ruleCode) + "(?:\\[([^\\]]*)\\])?(?![0-9.])");
      Matcher matcher = pattern.matcher(command);
      if (!matcher.find()) return command;
      String extra = matcher.group(1);
      if (style == CauseStyle.SHORT) {
         String shortReason = ruleCode + (extra == null || extra.isBlank() ? "" : "[" + extra.trim() + "]");
         return matcher.replaceFirst(Matcher.quoteReplacement(shortReason));
      }
      String causeTitle = ruleCode.equals("3.4") ? "Некорректные наказания" : title;
      String text = ruleCode + " | " + causeTitle + (extra == null || extra.isBlank() ? "" : " · " + extra.trim());
      String replacement = switch (style) {
         case CORNER -> "「 " + text + " 」";
         case DOUBLE_CORNER -> "『 " + text + " 』";
         case SPARKLE -> "✦「 " + text + " 」✦";
         case BOX -> "【 " + text + " 】";
         default -> ruleCode + " | " + causeTitle + (extra == null || extra.isBlank() ? "" : " · " + extra.trim());
      };
      return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
   }

   public static void save(String name, DonationTier selectedTier) {
      nickname = name.trim();
      tier = selectedTier;
      persist();
   }

   /** Moderator profile stores the server nickname without exposing donation selection. */
   public static void saveModeratorName(String name) {
      nickname = name.trim();
      tier = DonationTier.LEADER;
      persist();
   }

   private static void persist() {
      Properties properties = new Properties();
      properties.setProperty("nickname", nickname);
      if (tier != null) properties.setProperty("donation", tier.name());
      properties.setProperty("moderator_vk_contact", moderatorVkContact);
      properties.setProperty("moderator_telegram_contact", moderatorTelegramContact);
      properties.setProperty("moderator_discord_contact", moderatorDiscordContact);
      properties.setProperty("proof_request_key", Integer.toString(proofRequestKey));
      properties.setProperty("daily_history_key", Integer.toString(dailyHistoryKey));
      properties.setProperty("moderation_settings_key", Integer.toString(settingsKey));
      properties.setProperty("alert_prefix", alertPrefix);
      properties.setProperty("chat_menu_prefix", chatMenuPrefix);
      properties.setProperty("excluded_players", String.join(",", excludedPlayers));
      properties.setProperty("causeStyle", causeStyle.name());
      try {
         Files.createDirectories(FILE.getParent());
         try (OutputStream output = Files.newOutputStream(FILE)) {
            properties.store(output, "Rule Helper settings");
         }
      } catch (IOException exception) {
         throw new IllegalStateException("Could not save Rule Helper settings", exception);
      }
   }

   /** Keeps donation scope for standalone extra commands that are not tied to a numbered rule. */
   public static String applyDonationScope(String command) {
      return applyDonationScope(command, null);
   }

   /** Rule sections 1–3 use -g; Survival and Minigame rules 4–5 are local and omit it. */
   public static String applyDonationScope(String command, String ruleCode) {
      String normalized = command.trim();
      while (normalized.endsWith(" -g")) {
         normalized = normalized.substring(0, normalized.length() - 3).trim();
      }
      boolean globalRule = ruleCode != null && (ruleCode.startsWith("1.") || ruleCode.startsWith("2.") || ruleCode.startsWith("3."));
      boolean shouldUseGlobal = ruleCode == null ? tier == DonationTier.LEADER : globalRule;
      if (shouldUseGlobal) {
         normalized += " -g";
      }
      return normalized;
   }

   /** Appends Interdict-compatible cross-server selectors, or keeps the rule's normal scope. */
   public static String applyPunishmentScope(String command, String ruleCode, PunishmentScope scope) {
      return PunishmentScopeFormatter.apply(command, ruleCode, scope, tier);
   }

   private static void load() {
      if (!Files.isRegularFile(FILE)) {
         return;
      }
      Properties properties = new Properties();
      try (InputStream input = Files.newInputStream(FILE)) {
         properties.load(input);
         try { causeStyle = CauseStyle.valueOf(properties.getProperty("causeStyle", "SHORT")); }
         catch (IllegalArgumentException ignored) { causeStyle = CauseStyle.SHORT; }
         String loadedName = properties.getProperty("nickname", "").trim();
         String loadedTier = properties.getProperty("donation", "");
         moderatorVkContact = properties.getProperty("moderator_vk_contact", properties.getProperty("moderator_contact", "")).trim();
         moderatorTelegramContact = properties.getProperty("moderator_telegram_contact", "").trim();
         moderatorDiscordContact = properties.getProperty("moderator_discord_contact", "").trim();
         try {
            proofRequestKey = Integer.parseInt(properties.getProperty("proof_request_key", Integer.toString(InputConstants.KEY_J)));
         } catch (NumberFormatException ignored) {
            proofRequestKey = InputConstants.KEY_J;
         }
         dailyHistoryKey = intProperty(properties, "daily_history_key", InputConstants.KEY_F9);
         settingsKey = InputConstants.KEY_HOME;
         alertPrefix = properties.getProperty("alert_prefix", "⚠ RuleHelper » ");
         chatMenuPrefix = properties.getProperty("chat_menu_prefix", "»");
         for (String excluded : properties.getProperty("excluded_players", "").split(",")) {
            if (EvidenceRequestNames.isValid(excluded.trim())) excludedPlayers.add(excluded.trim());
         }
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

   private static int intProperty(Properties properties, String key, int fallback) {
      try { return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback))); }
      catch (NumberFormatException ignored) { return fallback; }
   }
}
