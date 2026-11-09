package ru.dmitry.rulehelper;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
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

   public static Button makeUnpunishButton(String var0, int var1, int var2) {
      int var3 = buttonWidth(var1);
      Builder var4 = Button.builder(
         Component.literal("Разбан / размут"),
         var1x -> {
            Minecraft var2x = Minecraft.getInstance();
            if (var2x != null) {
               RuleEntry var3x = new RuleEntry(
                  "release",
                  "Разбан и размут",
                  "Выбери команду для игрока " + var0,
                  "Команда будет выполнена для выбранного ника",
                  List.of(
                     new RuleAction("/unban " + var0 + " nekk", "/unban {player} nekk"),
                     new RuleAction("/unmute " + var0 + " nekk", "/unmute {player} nekk")
                  )
               );
               var2x.gui.setScreen(new ActionChoiceScreen(var0, var3x, 0));
            }
         }
      );
      int var5 = var1 / 2 + 4;
      return var4.bounds(var5, buttonY(var2), var3, 20).build();
   }

   public static Button makePexButton(String target, int screenWidth, int screenHeight) {
      int width = buttonWidth(screenWidth);
      int x = screenWidth / 2 - width - 4;
      String label = "/pex user " + target;
      return Button.builder(Component.literal(label), button -> {
         Minecraft client = Minecraft.getInstance();
         if (client == null || client.getConnection() == null || !RuleHelperConfig.isConfigured()) {
            return;
         }
         client.getConnection().sendCommand("op " + RuleHelperConfig.nickname());
         client.getConnection().sendCommand("pex user " + target);
         client.gui.setScreen(null);
      }).bounds(x, buttonY(screenHeight), width, 20).build();
   }

   public static Button makePlayerCommandButton(String command, String target, int screenWidth, int screenHeight, int row) {
      int width = Math.min(220, Math.max(140, screenWidth / 4 - 20));
      int y = screenHeight / 2 - 36 + row * 26;
      String label = command + " " + target;
      return Button.builder(Component.literal(label), button -> {
         Minecraft client = Minecraft.getInstance();
         if (client == null || client.getConnection() == null || !RuleHelperConfig.isConfigured()) {
            return;
         }
         String scopedCommand = RuleHelperConfig.applyDonationScope(label);
         client.getConnection().sendCommand(scopedCommand.startsWith("/") ? scopedCommand.substring(1) : scopedCommand);
         client.gui.setScreen(null);
      }).bounds(12, y, width, 20).build();
   }

   public static Button makeCopyNameButton(String target, int screenWidth, int screenHeight, int x) {
      int width = Math.min(164, Math.max(110, screenWidth / 4 - 12));
      return Button.builder(Component.literal("Копировать ник"), button -> {
         Minecraft client = Minecraft.getInstance();
         if (client == null) return;
         client.keyboardHandler.setClipboard(target);
         client.gui.hud.getChat().addClientSystemMessage(Component.literal("Скопирован ник: " + target));
         client.gui.setScreen(null);
      }).bounds(x, buttonY(screenHeight), width, 20).build();
   }

}
