package ru.dmitry.rulehelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Small per-install configuration recovered from the distributed JARs. */
public final class RuleHelperConfig {
   public enum DonationTier {
      LEADER,
      STAFF
   }

   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rulehelper.properties");
   private static String nickname = "";
   private static DonationTier tier;

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

   public static void save(String name, DonationTier selectedTier) {
      nickname = name.trim();
      tier = selectedTier;
      Properties properties = new Properties();
      properties.setProperty("nickname", nickname);
      properties.setProperty("donation", tier.name());
      try {
         Files.createDirectories(FILE.getParent());
         try (OutputStream output = Files.newOutputStream(FILE)) {
            properties.store(output, "Rule Helper settings");
         }
      } catch (IOException exception) {
         throw new IllegalStateException("Could not save Rule Helper settings", exception);
      }
   }

   /** Adds -g for Правитель+ only to rules 1–3. The rule code is passed explicitly. */
   public static String applyDonationScope(String command, String ruleCode) {
      String normalized = command.trim();
      while (normalized.endsWith(" -g")) {
         normalized = normalized.substring(0, normalized.length() - 3).trim();
      }
      boolean ruleFour = ruleCode != null && ruleCode.startsWith("4.");
      if (tier == DonationTier.LEADER && !ruleFour) {
         normalized += " -g";
      }
      return normalized;
   }

   private static void load() {
      if (!Files.isRegularFile(FILE)) {
         return;
      }
      Properties properties = new Properties();
      try (InputStream input = Files.newInputStream(FILE)) {
         properties.load(input);
         String loadedName = properties.getProperty("nickname", "").trim();
         String loadedTier = properties.getProperty("donation", "");
         if (loadedName.matches("[A-Za-z0-9_]{3,16}")) {
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
}
