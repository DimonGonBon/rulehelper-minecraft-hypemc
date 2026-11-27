package ru.dmitry.rulehelper;

import ru.dmitry.rulehelper.ui.ReasonComponents;

public final class ReasonStyleTest {
   public static void main(String[] args) {
      String colored = "&f&l&o3.4 &b&l&o━━➤ 「 Некорректное наказание 」&r";
      String release = "&f&l&o3.4 &a&l&o━━➤ 「 Некорректное наказание 」&r";
      equal(CauseFormatter.formatCommand("/ban Staff123 3d 3.4 -g", "3.4", "Некорректные наказания", "COLORED"),
         "/ban Staff123 3d " + colored + " -g", "global correction ban");
      equal(CauseFormatter.formatCommand("/unban Player123 nekk server:выживание", "release", "Снятие", "COLORED"),
         "/unban Player123 " + release + " server:выживание", "mode-aware unban");
      equal(CauseFormatter.formatCommand("unmute Player123 nekk server:гриферский", "release", "Снятие", "COLORED"),
         "unmute Player123 " + release + " server:гриферский", "mode-aware unmute");
      equal(CauseFormatter.formatCommand("/unbanip Player123 nekk server:мини-игры", null, null, "COLORED"),
         "/unbanip Player123 " + release + " server:мини-игры", "IP release");
      equal(CauseFormatter.formatCommand("/mute Nick2_3 3h 2.3[Родня] -g", "2.3", "Оскорбительное поведение", "COLORED"),
         "/mute Nick2_3 3h &f&l&o2.3 &b&l&o━━➤ 「 Оскорбительное поведение · Родня 」&r -g", "preserve reason qualifiers");
      equal(CauseFormatter.formatCommand("/mute Player123 3.4h 3.4", "3.4", "x", "COLORED"),
         "/mute Player123 3.4h " + colored, "do not rewrite a decimal duration");
      equal(CauseFormatter.formatCommand("/ban Player123 3d 3.40", "3.4", "x", "COLORED"),
         "/ban Player123 3d 3.40", "do not match partial rules");
      equal(CauseFormatter.formatCommand("/unban Player123 На время разбирательств", "release", "x", "COLORED"),
         "/unban Player123 На время разбирательств", "preserve investigation reason");
      equal(CauseFormatter.formatCommand("/pex user Nick2_3", "2.3", "x", "COLORED"),
         "/pex user Nick2_3", "unrelated command");
      equal(CauseFormatter.formatCommand("/ban Player123 1d 2.3[Родня] -g", "2.3", "x", "SHORT"),
         "/ban Player123 1d 2.3[Родня] -g", "legacy short style");
      equal(CauseFormatter.formatCommand("/unmute Player123 nekk -g", "release", "x", "SHORT"),
         "/unmute Player123 nekk -g", "legacy unmute reason");
      equal(CauseFormatter.reason("3.4", "x", null, false, "CORNER"),
         "「 3.4 | Некорректные наказания 」", "legacy decorative style");
      equal(CauseFormatter.reason("2.3", "Первая\nвторая", null, false, "COLORED"),
         "&f&l&o2.3 &b&l&o━━➤ 「 Первая вторая 」&r", "single-line output");

      var preview = ReasonComponents.fromCodes(release + " tail");
      equal(preview.getString(), "3.4 ━━➤ 「 Некорректное наказание 」 tail", "rendered preview has no raw codes");
      var number = preview.getSiblings().get(0).getStyle();
      var caption = preview.getSiblings().get(1).getStyle();
      var reset = preview.getSiblings().get(2).getStyle();
      require(number.getColor().getValue() == 0xFFFFFF && number.isBold() && number.isItalic(), "white bold italic rule");
      require(caption.getColor().getValue() == 0x55FF55 && caption.isBold() && caption.isItalic(), "green bold italic release");
      require(reset.getColor() == null && !reset.isBold() && !reset.isItalic(), "formatting ends at the reason");
      var punishment = ReasonComponents.fromCodes(colored);
      require(punishment.getSiblings().get(1).getStyle().getColor().getValue() == 0x55FFFF, "aqua punishment text");
      equal(ReasonComponents.fromCodes("unknown &z code").getString(), "unknown &z code", "unknown codes stay visible");
      System.out.println("PASS: colored reasons, command arguments, release modes, legacy styles and preview colors");
   }

   private static void equal(String actual, String expected, String label) {
      if (!expected.equals(actual)) throw new AssertionError(label + "\nexpected: " + expected + "\nactual: " + actual);
   }
   private static void require(boolean value, String label) {
      if (!value) throw new AssertionError(label);
   }
}
