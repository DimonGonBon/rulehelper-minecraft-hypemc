package ru.dmitry.rulehelper.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CoreProtectLogTest {
   public static void main(String[] args) {
      String[][] rows = {{"4.90/d назад + ", "FestivMC", "поставил verdant_froglight."},
         {"4.90/d назад - ", "FestivMC", "сломал grass_block."},
         {"5.55/d назад + ", "Hertix", "поставил grass_block."},
         {"5.55/d назад − ", "Hertix", "сломал grass_block."},
         {"0.01/m ago + ", "Player_123", "placed stone."},
         {"2,50/h назад - ", "Player_456", "забрал 64 stone."},
         {"2.48/м назад + ", "motaxps", "поставил dirt."},
         {"2.63/м назад - ", "motaxps", "сломал dirt."},
         {"2.50/м назад + ", "motaxps", "поставил dirt."},
         {"2.50/м назад - ", "motaxps", "сломал dirt."},
         {"1.25/ч назад + ", "Hertix", "поставил stone."},
         {"4.90/д назад - ", "FestivMC", "сломал grass_block."},
         {"30.00/с назад + ", "Player_789", "поставил dirt."}};
      for (String[] row : rows) {
         Component log = log(row[0], row[1], row[2], ChatFormatting.DARK_AQUA);
         require(CoreProtectLog.hasPlayerActor(log), "blue actor in " + log.getString());
         equal(CoreProtectLog.actor(log.getString()), row[1], "actor column, not action or block");
         Component decorated = ChatMenuPrefix.add(log, "»");
         equal(decorated.getString(), "» " + log.getString(), "same menu marker as normal player chat");
         equal(ChatMenuPrefix.sender(decorated.getString().substring(2)), row[1], "click/copy target");
         require(decorated.getSiblings().getLast() == log, "preserve the original component, colors and native click events");
         require(!decorated.getStyle().isBold() && decorated.getStyle().getColor() == null, "marker does not color or bold the log");
      }
      for (String unit : new String[]{"s", "m", "h", "d", "w", "y", "с", "м", "ч", "д", "н", "г"}) {
         equal(CoreProtectLog.actor("2.48/" + unit + " назад + motaxps поставил dirt."), "motaxps", "localized time unit " + unit);
      }
      for (int color : new int[]{0x31B0E8, 0x169BD5, 0x2A5ADD, 0xA0DCFF}) {
         Component rgbLog = Component.literal("2.48/м назад + ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("motaxps").withStyle(style -> style.withColor(color)))
            .append(Component.literal(" поставил dirt.").withStyle(ChatFormatting.WHITE));
         require(CoreProtectLog.hasPlayerActor(rgbLog), "custom blue actor color " + Integer.toHexString(color));
         Component decorated = ChatMenuPrefix.add(rgbLog, "»");
         equal(decorated.getString(), "» 2.48/м назад + motaxps поставил dirt.", "new screenshot gets the actual marker");
         equal(ChatMenuPrefix.sender(decorated.getString().substring(2)), "motaxps", "new screenshot hover/click actor");
      }
      Component splitName = Component.literal("1.25/h ago + ").withStyle(ChatFormatting.GRAY)
         .append(Component.empty().withStyle(ChatFormatting.BLUE)
            .append(Component.literal("Fest")).append(Component.literal("ivMC")))
         .append(Component.literal(" placed stone.").withStyle(ChatFormatting.WHITE));
      require(CoreProtectLog.hasPlayerActor(splitName), "inherited actor color across nested components");
      Component noBlueName = log("4.90/d назад + ", "FestivMC", "поставил stone.", ChatFormatting.WHITE);
      require(ChatMenuPrefix.add(noBlueName, "»") == noBlueName, "a blue block cannot turn a white actor into a player target");
      require(ChatMenuPrefix.add(splitName, "") == splitName, "empty prefix disables menu markers");
      for (String invalid : new String[]{"----- CoreProtect ----- (x13/y70/z-207)",
         "CoreProtect - Режим инспектора теперь включён.", "CoreProtect - Данные не найдены в light_gray_concrete.",
         "4.90/d назад + #water поставил stone.", "4.90/d назад + Player1234567890123 поставил stone.",
         "4.90/d назад + stone", "Player123 → 4.90/d назад + Victim123 поставил stone."}) {
         require(CoreProtectLog.actor(invalid) == null, "not an inspector actor: " + invalid);
      }
      equal(ChatMenuPrefix.sender("Ⓖ Player123 → 4.90/d назад + Victim123 поставил stone."), "Player123", "quoted log stays ordinary player chat");
      String full = "» 4.90/d назад + FestivMC поставил stone.";
      require(ChatMenuPrefix.isPrefixHit(full, full, "»", 17.9, 18), "include the entire bold marker");
      require(!ChatMenuPrefix.isPrefixHit(full, full, "»", 18, 18), "timestamp is not the marker");
      require(!ChatMenuPrefix.isPrefixHit(full, "поставил stone.", "»", 1, 18), "wrapped continuation cannot open another row's menu");
      require(!ChatMenuPrefix.isPrefixHit(full, full, "»", -1, 18), "left margin is not clickable");
      boolean[] endFlags = {true, false, false, true, false, true, false};
      int[] expected = {0, 0, 0, 1, 1, 2, 2};
      for (int i = 0; i < expected.length; i++) {
         require(ChatRowGeometry.originalMessageIndex(i, endFlags.length, row -> endFlags[row]) == expected[i],
            "wrapped/scrolled row maps to its exact original message: " + i);
      }
      require(ChatRowGeometry.originalMessageIndex(-1, endFlags.length, row -> endFlags[row]) == -1, "invalid original row");
      System.out.println("PASS: CoreProtect Russian/English time units, motaxps screenshot RGB, blue actor inheritance, wrapped rows and normal-chat isolation");
   }
   private static Component log(String age, String name, String action, ChatFormatting actorColor) {
      return Component.literal(age).withStyle(ChatFormatting.GRAY)
         .append(Component.literal(name).withStyle(actorColor))
         .append(Component.literal(" " + action).withStyle(ChatFormatting.WHITE))
         .append(Component.literal(" stone").withStyle(ChatFormatting.DARK_AQUA));
   }
   private static void equal(String actual, String expected, String label) { require(expected.equals(actual), label + ": " + actual); }
   private static void require(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
}
