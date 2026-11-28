package ru.dmitry.rulehelper.chat;

import ru.dmitry.rulehelper.PlayerNames;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import ru.dmitry.rulehelper.mixin.ChatHudAccessor;

public final class ChatNameHitTest {
   private ChatNameHitTest() {}

   public static String findName(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      return hit == null ? null : findTokenAt(client.font, hit.line().content(), hit.x());
   }

   /** Prefer the one native Minecraft tooltip already attached to the hovered glyph. */
   public static boolean hasNativeHoverAt(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null) return false;
      int[] offset = {0};
      boolean[] nativeHover = {false};
      hit.line().content().accept((index, style, codePoint) -> {
         int width = Math.max(0, client.font.width(Component.literal(new String(Character.toChars(codePoint))).setStyle(style)));
         if (hit.x() >= offset[0] && hit.x() < offset[0] + width) {
            nativeHover[0] = style.getHoverEvent() != null;
            return false;
         }
         offset[0] += width;
         return true;
      });
      return nativeHover[0];
   }

   public static String findPrivateMessageMenuTarget(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null) return null;
      PrivateMessageMenu.Participants participants = PrivateMessageMenu.parse(hit.line().parent().content().getString(),
         ru.dmitry.rulehelper.RuleHelperConfig.nickname());
      int senderWidth = client.font.width(Component.literal(PrivateMessageMenu.SENDER + " ")
         .withStyle(net.minecraft.ChatFormatting.BLUE));
      int recipientWidth = client.font.width(Component.literal(PrivateMessageMenu.RECIPIENT + " ")
         .withStyle(net.minecraft.ChatFormatting.AQUA));
      return PrivateMessageMenu.targetAt(participants, formattedText(hit.line().content()), hit.x(), senderWidth, recipientWidth);
   }

   /** Direct clipboard target from the visible CoreProtect row; no menu marker is required. */
   public static String findCoreProtectCopyTarget(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null) return null;
      String name = CoreProtectNameCopy.targetAt(hit.line().content(), hit.x(),
         ru.dmitry.rulehelper.RuleHelperConfig.chatMenuPrefix(),
         (style, codePoint) -> client.font.width(Component.literal(new String(Character.toChars(codePoint))).setStyle(style)));
      if (name == null) return null;
      CoreProtectNameCopy.Actor original = CoreProtectNameCopy.actor(hit.line().parent().content().getString(),
         ru.dmitry.rulehelper.RuleHelperConfig.chatMenuPrefix());
      return original == null || original.name().equals(name) ? name : null;
   }

   /** Only the explicit RuleHelper marker opens the punishment menu; the nick text stays inert. */
   public static String findPrefixTarget(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null) return null;
      String prefix = ru.dmitry.rulehelper.RuleHelperConfig.chatMenuPrefix();
      String message = hit.line().parent().content().getString();
      int prefixWidth = client.font.width(Component.literal(prefix + " ")
         .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD));
      if (!ChatMenuPrefix.isPrefixHit(message, formattedText(hit.line().content()), prefix, hit.x(), prefixWidth)) return null;
      return ru.dmitry.rulehelper.chat.ChatMenuPrefix.sender(message.substring(prefix.length() + 1));
   }

   /** The message sender's actual name is a separate copy target; clicking the marker still opens the menu. */
   public static String findMenuNameCopyTarget(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null) return null;
      String prefix = ru.dmitry.rulehelper.RuleHelperConfig.chatMenuPrefix();
      if (prefix.isBlank()) return null;
      String message = hit.line().parent().content().getString();
      if (!message.startsWith(prefix + " ")) return null;
      String sender = ru.dmitry.rulehelper.chat.ChatMenuPrefix.sender(message.substring(prefix.length() + 1));
      if (sender == null) return null;
      if (formattedText(hit.line().content()).startsWith(prefix + " ") && hit.x() < client.font.width(
         Component.literal(prefix + " ").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD))) return null;
      String hoveredName = findTokenAt(client.font, hit.line().content(), hit.x(), true);
      return sender.equalsIgnoreCase(hoveredName) ? sender : null;
   }

   public static PunishmentAnnouncement findPunishment(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null || hit.line().parent().source() != GuiMessageSource.SYSTEM_SERVER) return null;
      // 26.3 exposes the original message for each wrapped row. Parse that exact
      // message rather than reconstructing it from neighboring visible rows.
      return PunishmentAnnouncement.parse(hit.line().parent().content().getString());
   }

   public static PunishmentRemovalAnnouncement findRemoval(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null || hit.line().parent().source() != GuiMessageSource.SYSTEM_SERVER) return null;
      return PunishmentRemovalAnnouncement.parse(hit.line().parent().content().getString());
   }

   /** Resolve the actor or target label at the beginning of a punishment row. */
   public static String findPunishmentCopyTarget(Minecraft client, double mouseX, double mouseY) {
      Hit hit = hit(client, mouseX, mouseY);
      if (hit == null || hit.line().parent().source() != GuiMessageSource.SYSTEM_SERVER) return null;
      PunishmentAnnouncement announcement = PunishmentAnnouncement.parse(hit.line().parent().content().getString());
      PunishmentRemovalAnnouncement removal = PunishmentRemovalAnnouncement.parse(hit.line().parent().content().getString());
      if (announcement == null && removal == null) return null;
      String issuer = announcement != null ? announcement.issuer() : removal.issuer();
      String target = announcement != null ? announcement.target() : removal.target();
      String visiblePrefix = formattedText(hit.line().content());
      if (!visiblePrefix.startsWith(PunishmentCopyPrefixes.ISSUER_LABEL + " " + PunishmentCopyPrefixes.TARGET_LABEL + " ")) return null;
      String part = PunishmentCopyPrefixes.targetAt(hit.x(), client.font);
      if ("issuer".equals(part)) return issuer;
      if ("target".equals(part)) return target;
      return null;
   }

   private static String formattedText(FormattedCharSequence sequence) {
      StringBuilder text = new StringBuilder();
      sequence.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
      return text.toString();
   }

   private static Hit hit(Minecraft client, double mouseX, double mouseY) {
      if (client == null || client.gui == null || client.getWindow() == null) return null;
      ChatComponent chat = client.gui.hud.getChat();
      if (!(chat instanceof ChatHudAccessor accessor)) return null;
      List<Line> lines = accessor.ruleHelper$getVisibleMessages();
      int scroll = Math.max(0, accessor.ruleHelper$getScrolledLines());
      int count = Math.min(chat.getLinesPerPage(), lines.size() - scroll);
      float scale = client.options.chatScale().get().floatValue();
      double spacing = client.options.chatLineSpacing().get();
      int row = ChatRowGeometry.rowAt(mouseY, client.getWindow().getGuiScaledHeight(), scale,
         spacing, client.font.lineHeight, count);
      if (row < 0) return null;
      Line line = lines.get(scroll + row);
      double x = mouseX / scale - 4.0;
      if (x < 0 || x >= client.font.width(line.content())) return null;
      return new Hit(line, x);
   }

   private record Hit(Line line, double x) {}

   private static String findTokenAt(Font var0, FormattedCharSequence var1, double var2) {
      return findTokenAt(var0, var1, var2, false);
   }

   private static String findTokenAt(Font var0, FormattedCharSequence var1, double var2, boolean allowClickable) {
      ArrayList<Glyph> var4 = new ArrayList<>();
      int[] var5 = new int[]{0};
      var1.accept((var3, var4x, var5x) -> {
         String var6x = new String(Character.toChars(var5x));
         int var7x = var0.width(Component.literal(var6x).setStyle(var4x));
         var4.add(new ChatNameHitTest.Glyph(var6x, var5[0], var5[0] + Math.max(0, var7x), var4x.getClickEvent() != null));
         var5[0] += Math.max(0, var7x);
         return true;
      });
      int var6 = -1;

      for (int var7 = 0; var7 < var4.size(); var7++) {
         ChatNameHitTest.Glyph var8 = (ChatNameHitTest.Glyph)var4.get(var7);
         if (var2 >= var8.start && var2 < var8.end && isNameChar(var8.text)) {
            // Respect clickable chat components (e.g. WorldGuard [Add] buttons).
            // RuleHelper should only intercept ordinary player-name clicks.
            if (var8.clickable && !allowClickable) {
               return null;
            }
            var6 = var7;
            break;
         }
      }

      if (var6 < 0) {
         return null;
      }

      int var12 = var6;
      int var13 = var6 + 1;

      while (var12 > 0 && isNameChar(((ChatNameHitTest.Glyph)var4.get(var12 - 1)).text)) {
         var12--;
      }

      while (var13 < var4.size() && isNameChar(((ChatNameHitTest.Glyph)var4.get(var13)).text)) {
         var13++;
      }

      int var9 = var13 - var12;
      if (var9 >= 3 && var9 <= 16) {
         StringBuilder var10 = new StringBuilder();

         for (int var11 = var12; var11 < var13; var11++) {
            var10.append(((ChatNameHitTest.Glyph)var4.get(var11)).text);
         }

         String var14 = var10.toString();
         return PlayerNames.isValid(var14) ? var14 : null;
      } else {
         return null;
      }
   }

   private static boolean isNameChar(String var0) {
      if (var0.isEmpty()) {
         return false;
      }

      int var1 = var0.codePointAt(0);
      return var1 >= 65 && var1 <= 90 || var1 >= 97 && var1 <= 122 || var1 >= 48 && var1 <= 57 || var1 == 95;
   }

   private record Glyph(String text, int start, int end, boolean clickable) {
   }
}
