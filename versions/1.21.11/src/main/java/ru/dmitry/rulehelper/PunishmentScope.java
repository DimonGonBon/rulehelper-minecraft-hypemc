package ru.dmitry.rulehelper;

import java.util.Locale;

/** Server scope read from the mode suffix in a punishment announcement. */
public enum PunishmentScope {
   RULE_DEFAULT("По умолчанию", ""),
   CURRENT("Текущий режим", ""),
   GLOBAL("Глобально", "-g"),
   SURVIVAL("Выживание", "server:выживание"),
   GRIEF("Гриферский", "server:гриферский"),
   MINIGAMES("Мини-игры", "server:мини-игры");

   private final String label;
   private final String selector;
   PunishmentScope(String label, String selector) { this.label = label; this.selector = selector; }
   public String label() { return label; }
   public String selector() { return selector; }

   public String applyToCommand(String command) {
      String normalized = command.trim();
      String[] selectors = {"-g", "server:выживание", "server:гриферский", "server:мини-игры"};
      for (String existing : selectors) {
         String suffix = " " + existing;
         while (normalized.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
         }
      }
      return this.selector.isEmpty() ? normalized : normalized + " " + this.selector;
   }

   public static PunishmentScope fromAnnouncementMode(String mode) {
      if (mode == null) return null;
      return switch (mode.strip().toLowerCase(Locale.ROOT).replace('ё', 'е')) {
         case "глобальный", "глобальная", "глобально" -> GLOBAL;
         case "выживание", "выживании" -> SURVIVAL;
         case "гриферский", "гриферском" -> GRIEF;
         case "мини-игры", "мини игры", "миниигры", "мини-играх" -> MINIGAMES;
         default -> null;
      };
   }
}
