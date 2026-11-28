package ru.dmitry.rulehelper;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import ru.dmitry.rulehelper.NotificationPreferences.Option;
import ru.dmitry.rulehelper.chat.ModerationAlertDetector;

public final class NotificationPreferencesTest {
   public static void main(String[] args) throws Exception {
      String family = "Ⓖ Player123 → мать чекни";
      String insult = "Ⓖ Player123 → Ты сука";
      String incorrect = "❨！❩ Игрок Issuer123 забанил Issuer123 на 3 дня по причине: test (выживание)";
      for (int mask = 0; mask < 16; mask++) {
         var prefs = new NotificationPreferences((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0);
         require((ModerationAlertDetector.notification(family, "Moderator", prefs) != null) == prefs.family(), "independent family category " + mask);
         require((ModerationAlertDetector.notification(insult, "Moderator", prefs) != null) == prefs.insults(), "independent insult category " + mask);
         require((ModerationAlertDetector.notification(incorrect, "Moderator", prefs) != null) == prefs.incorrect(), "independent incorrect-punishment category " + mask);
         var properties = new Properties();
         properties.setProperty("nickname", "Moderator"); prefs.writeTo(properties);
         var writer = new StringWriter(); properties.store(writer, "notification test");
         var loaded = new Properties(); loaded.load(new StringReader(writer.toString()));
         require(NotificationPreferences.readFrom(loaded).equals(prefs), "all flags survive restart " + mask);
         require(loaded.getProperty("nickname").equals("Moderator"), "existing profile stays intact");
         for (Option option : Option.values()) require(prefs.toggle(option).toggle(option).equals(prefs), "toggle reverses only its option");
      }
      var onlyIncorrect = new NotificationPreferences(false, false, true, true);
      require(ModerationAlertDetector.notification("Батя твой ебанат", "Moderator", onlyIncorrect) == null, "the requested incorrect-only setup disables family insults too");
      require(NotificationPreferences.readFrom(new Properties()).equals(NotificationPreferences.defaults()), "older settings keep existing alerts enabled");
      require(ModerationAlertDetector.notification("КАПС капс спам спам", "Moderator", NotificationPreferences.defaults()) == null, "caps/spam remain ignored");
      System.out.println("PASS: all 16 alert combinations, incorrect-only mode, sound independence and settings persistence");
   }
   private static void require(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
}
