package ru.dmitry.rulehelper;

/** Shared validation for server names that may contain Latin or Cyrillic letters. */
public final class PlayerNames {
   private PlayerNames() {}

   public static boolean isValid(String name) {
      if (name == null) return false;
      int length = name.codePointCount(0, name.length());
      if (length < 3 || length > 16) return false;
      return name.codePoints().allMatch(codePoint -> codePoint == '_' || Character.isLetterOrDigit(codePoint));
   }
}
