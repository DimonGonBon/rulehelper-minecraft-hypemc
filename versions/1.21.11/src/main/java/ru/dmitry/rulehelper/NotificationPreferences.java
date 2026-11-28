package ru.dmitry.rulehelper;

import java.util.Properties;

/** Independent saved notification categories; changing sound never changes the text categories. */
public record NotificationPreferences(boolean family, boolean insults, boolean incorrect, boolean sound) {
   public enum Option {
      FAMILY("Упоминания родни"), INSULTS("Оскорбления"), INCORRECT("Некорректные наказания"), SOUND("Звук уведомлений");
      private final String label;
      Option(String label) { this.label = label; }
      public String label() { return this.label; }
   }
   public static NotificationPreferences defaults() { return new NotificationPreferences(true, true, true, true); }
   public boolean enabled(Option option) {
      return switch (option) { case FAMILY -> family; case INSULTS -> insults; case INCORRECT -> incorrect; case SOUND -> sound; };
   }
   public NotificationPreferences toggle(Option option) {
      return switch (option) {
         case FAMILY -> new NotificationPreferences(!family, insults, incorrect, sound);
         case INSULTS -> new NotificationPreferences(family, !insults, incorrect, sound);
         case INCORRECT -> new NotificationPreferences(family, insults, !incorrect, sound);
         case SOUND -> new NotificationPreferences(family, insults, incorrect, !sound);
      };
   }
   public void writeTo(Properties properties) {
      for (Option option : Option.values()) properties.setProperty(key(option), Boolean.toString(enabled(option)));
   }
   public static NotificationPreferences readFrom(Properties properties) {
      return new NotificationPreferences(read(properties, Option.FAMILY), read(properties, Option.INSULTS),
         read(properties, Option.INCORRECT), read(properties, Option.SOUND));
   }
   private static boolean read(Properties properties, Option option) {
      return !"false".equalsIgnoreCase(properties.getProperty(key(option), "true").strip());
   }
   private static String key(Option option) { return "notification." + option.name().toLowerCase(java.util.Locale.ROOT); }
}
