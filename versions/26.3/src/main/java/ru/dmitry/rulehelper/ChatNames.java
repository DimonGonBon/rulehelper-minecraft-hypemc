package ru.dmitry.rulehelper;

/** Names displayed by server chat, including Unicode names in private-message headers. */
public final class ChatNames {
   private ChatNames() {}
   public static boolean isValid(String name) {
      if (name == null) return false;
      int length = name.codePointCount(0, name.length());
      return length >= 3 && length <= 16 && name.codePoints().allMatch(c -> c == '_' || Character.isLetterOrDigit(c));
   }
}
