package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Renders a reason preview without displaying the command's literal color codes. */
public final class ReasonComponents {
   private ReasonComponents() {}

   public static Component fromCodes(String text) {
      MutableComponent result = Component.empty();
      StringBuilder part = new StringBuilder();
      List<ChatFormatting> formats = new ArrayList<>();
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         ChatFormatting format = (c == '&' || c == '§') && i + 1 < text.length()
            ? ChatFormatting.getByCode(text.charAt(i + 1)) : null;
         if (format == null) {
            part.append(c);
            continue;
         }
         append(result, part, formats);
         if (format == ChatFormatting.RESET || "0123456789abcdef".indexOf(Character.toLowerCase(text.charAt(i + 1))) >= 0) formats.clear();
         if (format != ChatFormatting.RESET) formats.add(format);
         i++;
      }
      append(result, part, formats);
      return result;
   }

   private static void append(MutableComponent result, StringBuilder part, List<ChatFormatting> formats) {
      if (part.isEmpty()) return;
      result.append(Component.literal(part.toString()).withStyle(formats.toArray(ChatFormatting[]::new)));
      part.setLength(0);
   }
}
