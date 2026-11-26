package ru.dmitry.rulehelper;

/** Validation for names typed into the moderator-only evidence request form. */
public final class EvidenceRequestNames {
   private EvidenceRequestNames() {}

   public static boolean isValid(String name) {
      if (name == null) return false;
      int length = name.codePointCount(0, name.length());
      if (length < 3 || length > 16) return false;
      return name.codePoints().allMatch(codePoint -> codePoint == '_' || Character.isLetterOrDigit(codePoint));
   }
}
