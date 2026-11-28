package ru.dmitry.rulehelper.chat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class CoreProtectNameCopyTest {
   public static void main(String[] args) {
      net.minecraft.SharedConstants.tryDetectVersion();
      net.minecraft.server.Bootstrap.bootStrap();
      String[][] examples = {{"4.95/ч назад + ", "WASAP", " поставил light_gray_carpet."},
         {"4.96/ч назад - ", "Exclusive", " сломал white_carpet."},
         {"5.56/ч назад + ", "diva52_42", " поставил white_carpet."},
         {"2.48/любая-единица назад + ", "motaxps", " поставил dirt."},
         {"0.01/m ago - ", "Player123", " broke stone."},
         {"1.20/д назад + ", "Русский", " поставил stone."}};
      for (String[] example : examples) {
         for (String prefix : new String[]{"", "»", "[RH]"}) {
            String marker = prefix.isEmpty() ? "" : prefix + " ";
            String line = marker + example[0] + example[1] + example[2];
            FormattedCharSequence row = FormattedCharSequence.forward(line, Style.EMPTY);
            int start = marker.length() + example[0].length(), end = start + example[1].length();
            for (int i = start; i < end; i++) {
               require(example[1].equals(CoreProtectNameCopy.targetAt(row, i * 6 + 3, prefix, (s,c) -> 6)), "every actor glyph copies the entire exact name without a marker");
            }
            require(CoreProtectNameCopy.targetAt(row, start * 6 - 1, prefix, (s,c) -> 6) == null, "separator is not the nickname");
            require(CoreProtectNameCopy.targetAt(row, end * 6 + 3, prefix, (s,c) -> 6) == null, "action is not the nickname");
            require(CoreProtectNameCopy.targetAt(row, (line.length() - 3) * 6, prefix, (s,c) -> 6) == null, "block name is not the nickname");
         }
         Component original = Component.literal(example[0]).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(example[1]).withStyle(s -> s.withColor(0x31B0E8)))
            .append(Component.literal(example[2]).withStyle(ChatFormatting.WHITE));
         Component copyable = CoreProtectNameCopy.addNativeCopy(original);
         require(copyable.getString().equals(original.getString()), "native clipboard action preserves the visible log text");
         List<Span> spans = spans(copyable);
         int position = 0;
         for (Span span : spans) {
            for (int i = 0; i < span.text().length(); i++, position++) {
               boolean actor = position >= example[0].length() && position < example[0].length() + example[1].length();
               require((span.style().getClickEvent() != null) == actor, "only actor characters have a native copy action");
               if (actor) require(span.style().getColor().getValue() == 0x31B0E8 && span.style().getHoverEvent() != null, "keep custom actor color and provide the copy hint");
            }
         }
      }
      for (String unrelated : new String[]{"CoreProtect - Режим инспектора теперь включён.", "----- CoreProtect ----- (x19/y71/z-197)",
         "Игрок WASAP → 4.95/ч назад + Exclusive поставил stone.", "4.95/ч назад + #water поставил stone."}) {
         require(CoreProtectNameCopy.actor(unrelated, "") == null, "only a CoreProtect actor column is copied");
      }
      require(CoreProtectNameCopy.targetAt(FormattedCharSequence.forward("4.95/ч назад + WASAP поставил stone.", Style.EMPTY), -1, "", (s,c) -> 6) == null, "outside row");
      System.out.println("PASS: direct CoreProtect nickname copying without prefixes, latest screenshot actors, native clipboard events and block-name isolation");
   }
   private static List<Span> spans(Component component) {
      List<Span> spans = new ArrayList<>();
      component.visit((style, text) -> { spans.add(new Span(text, style)); return java.util.Optional.<Void>empty(); }, Style.EMPTY);
      return spans;
   }
   private record Span(String text, Style style) {}
   private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
