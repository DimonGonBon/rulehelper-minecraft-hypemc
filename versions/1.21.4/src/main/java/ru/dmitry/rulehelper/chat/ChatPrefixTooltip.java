package ru.dmitry.rulehelper.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Draws the player hint over the menu marker without changing chat input. */
public final class ChatPrefixTooltip {
   private ChatPrefixTooltip() {}

   public static void render(GuiGraphics graphics, Minecraft client, int mouseX, int mouseY) {
      if (client == null || client.font == null) return;
      if (ChatNameHitTest.hasNativeHoverAt(client, mouseX, mouseY)) return;
      String player = ChatNameHitTest.findPrivateMessageMenuTarget(client, mouseX, mouseY);
      String hint = "нажми, чтобы открыть меню";
      if (player == null) {
         player = ChatNameHitTest.findCoreProtectCopyTarget(client, mouseX, mouseY);
         hint = "нажми, чтобы скопировать ник";
      }
      if (player == null) {
         player = ChatNameHitTest.findPrefixTarget(client, mouseX, mouseY);
         hint = "нажми, чтобы открыть меню";
      }
      if (player == null) {
         player = ChatNameHitTest.findMenuNameCopyTarget(client, mouseX, mouseY);
         hint = "нажми, чтобы скопировать ник";
      }
      if (player == null) return;
      String label = "Игрок: " + player + " · " + hint;
      int width = client.font.width(label) + 12;
      int x = Math.max(4, Math.min(mouseX + 10, graphics.guiWidth() - width - 4));
      int y = Math.max(4, Math.min(mouseY + 12, graphics.guiHeight() - 18));
      graphics.fill(x, y, x + width, y + 16, 0xEE0A111B);
      graphics.fill(x, y, x + width, y + 1, 0xFF36C5D2);
      graphics.drawString(client.font, Component.literal(label).withStyle(ChatFormatting.AQUA), x + 6, y + 4, 0xFFFFFFFF);
   }
}
