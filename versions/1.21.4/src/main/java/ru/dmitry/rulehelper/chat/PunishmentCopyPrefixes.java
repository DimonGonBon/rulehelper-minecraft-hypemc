package ru.dmitry.rulehelper.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Clickable issuer/target labels prepended to punishment announcement rows. */
public final class PunishmentCopyPrefixes {
   public static final String ISSUER_LABEL = "🛡";
   public static final String TARGET_LABEL = "☠";
   private static final String SEPARATOR = " ";

   private PunishmentCopyPrefixes() {}

   public static Component add(Component original) {
      PunishmentAnnouncement parsed = PunishmentAnnouncement.parse(original.getString());
      PunishmentRemovalAnnouncement removal = PunishmentRemovalAnnouncement.parse(original.getString());
      if (parsed == null && removal == null) return original;
      if (original.getString().stripLeading().startsWith(ISSUER_LABEL + " " + TARGET_LABEL + " ")) return original;
      String issuerName = parsed != null ? parsed.issuer() : removal.issuer();
      String targetName = parsed != null ? parsed.target() : removal.target();
      MutableComponent issuer = label(ISSUER_LABEL, issuerName, ChatFormatting.BLUE,
         removal == null ? "Скопировать ник выдавшего наказание" : "Скопировать ник снявшего наказание");
      MutableComponent target = label(TARGET_LABEL, targetName, ChatFormatting.RED,
         removal == null ? "Скопировать ник наказанного игрока" : "Скопировать ник освобождённого игрока");
      // Use an unstyled root so the issuer label's style cannot leak into the
      // target label or the server's original announcement text.
      return Component.empty().append(issuer).append(Component.literal(SEPARATOR))
         .append(target).append(Component.literal(SEPARATOR)).append(original);
   }

   private static MutableComponent label(String text, String nickname, ChatFormatting color, String hover) {
      return Component.literal(text).setStyle(Style.EMPTY.withColor(color).withBold(false)
         .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, nickname))
         .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover + ": " + nickname))));
   }

   public static String targetAt(double x, net.minecraft.client.gui.Font font) {
      int issuerWidth = font.width(Component.literal(ISSUER_LABEL + SEPARATOR));
      int targetWidth = font.width(Component.literal(TARGET_LABEL + SEPARATOR));
      if (x >= 0 && x < issuerWidth) return "issuer";
      if (x >= issuerWidth && x < issuerWidth + targetWidth) return "target";
      return null;
   }

   public static int prefixWidth(net.minecraft.client.gui.Font font) {
      return font.width(Component.literal(ISSUER_LABEL + SEPARATOR + TARGET_LABEL + SEPARATOR));
   }
}
