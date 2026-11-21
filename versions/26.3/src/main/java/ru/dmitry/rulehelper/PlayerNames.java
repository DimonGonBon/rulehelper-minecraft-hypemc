package ru.dmitry.rulehelper;

/** Nickname constraints used by clickable chat selection and the saved profile. */
public final class PlayerNames {
   private PlayerNames() {}

   public static boolean isValid(String name) {
      if (name == null) return false;
      int length = name.length();
      if (length < 3 || length > 16) return false;
      for (int i = 0; i < length; i++) {
         char c = name.charAt(i);
         if (!(c == '_' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) return false;
      }
      return true;
   }
}
