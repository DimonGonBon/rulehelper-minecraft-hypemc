package ru.dmitry.rulehelper;

import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_4185;
import net.minecraft.class_4185.class_7840;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.ui.ActionChoiceScreen;

public final class RuleHelperExtra {
   private RuleHelperExtra() {
   }

   private static int buttonWidth(int var0) {
      return Math.min(220, Math.max(120, (var0 - 24) / 2));
   }

   private static int buttonY(int var0) {
      return Math.max(61, var0 - 82);
   }

   public static class_4185 makeUnpunishButton(String var0, int var1, int var2) {
      int var3 = buttonWidth(var1);
      class_7840 var4 = class_4185.method_46430(
         class_2561.method_43470("Разбан / размут"),
         var1x -> {
            class_310 var2x = class_310.method_1551();
            if (var2x != null) {
               RuleEntry var3x = new RuleEntry(
                  "release",
                  "Разбан и размут",
                  "Выбери команду для игрока " + var0,
                  "Команда будет выполнена для выбранного ника",
                  List.of(
                     new RuleAction("/unban " + var0 + " nekk", RuleHelperConfig.applyDonationScope("/unban {player} nekk")),
                     new RuleAction("/unmute " + var0 + " nekk", RuleHelperConfig.applyDonationScope("/unmute {player} nekk"))
                  )
               );
               var2x.method_1507(new ActionChoiceScreen(var0, var3x, 0));
            }
         }
      );
      int var5 = var1 / 2 + 4;
      return var4.method_46434(var5, buttonY(var2), var3, 20).method_46431();
   }

   public static class_4185 makePexButton(String target, int screenWidth, int screenHeight) {
      int width = buttonWidth(screenWidth);
      int x = screenWidth / 2 - width - 4;
      String label = "/pex user " + target;
      return class_4185.method_46430(class_2561.method_43470(label), button -> {
         class_310 client = class_310.method_1551();
         if (client == null || client.method_1562() == null || !RuleHelperConfig.isConfigured()) {
            return;
         }
         client.method_1562().method_45730("op " + RuleHelperConfig.nickname());
         client.method_1562().method_45730("pex user " + target);
         client.method_1507(null);
      }).method_46434(x, buttonY(screenHeight), width, 20).method_46431();
   }

   public static class_4185 makePlayerCommandButton(String command, String target, int screenWidth, int screenHeight, int row) {
      int width = Math.min(220, Math.max(140, screenWidth / 4 - 20));
      int y = screenHeight / 2 - 36 + row * 26;
      String label = command + " " + target;
      return class_4185.method_46430(class_2561.method_43470(label), button -> {
         class_310 client = class_310.method_1551();
         if (client == null || client.method_1562() == null || !RuleHelperConfig.isConfigured()) {
            return;
         }
         String scopedCommand = RuleHelperConfig.applyDonationScope(label);
         client.method_1562().method_45730(scopedCommand.startsWith("/") ? scopedCommand.substring(1) : scopedCommand);
         client.method_1507(null);
      }).method_46434(12, y, width, 20).method_46431();
   }

}
