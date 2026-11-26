package ru.dmitry.rulehelper;

import java.util.Locale;

/** Cross-server targets supported by the HypeMC punishment command syntax. */
public enum PunishmentScope {
   RULE_DEFAULT("Текущий режим"),
   GLOBAL("Глобально (-g)"),
   SURVIVAL("Выживание"),
   GRIEF("Гриферский"),
   MINIGAMES("Мини-игры"),
   CURRENT("Текущий режим");

   private final String label;
   PunishmentScope(String label) { this.label = label; }
   public String label() { return label; }

   /** Converts the server label from a HypeMC punishment announcement to its command selector. */
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
