package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.ModeratorProofCommands;
import ru.dmitry.rulehelper.ModeratorProofCommands.ReleaseType;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Shows the exact release and evidence-request commands before sending. */
public final class ProofRequestConfirmScreen extends Screen {
   private final String moderator;
   private final String releasedPlayer;
   private final boolean banTarget;
   private final String vkContact;
   private final String telegramContact;
   private final String discordContact;
   private final String requestedReason;
   private int panelX, panelY, panelWidth, panelHeight;
   private int firstCardTop, firstCardBottom, secondCardTop, secondCardBottom, warningY;

   public ProofRequestConfirmScreen(String moderator, String releasedPlayer, boolean banTarget, String vkContact,
                                    String telegramContact, String discordContact, String requestedReason) {
      super(Component.literal("Подтверждение запроса доказательств"));
      this.moderator = moderator;
      this.releasedPlayer = releasedPlayer;
      this.banTarget = banTarget;
      this.vkContact = vkContact;
      this.telegramContact = telegramContact;
      this.discordContact = discordContact;
      this.requestedReason = requestedReason;
   }

   private String releaseCommand() {
      return ModeratorProofCommands.release(this.banTarget ? ReleaseType.BAN : ReleaseType.MUTE, this.releasedPlayer);
   }

   private String proofCommand() {
      List<String> contacts = new ArrayList<>();
      if (!this.vkContact.isBlank()) contacts.add("VK - " + this.vkContact);
      if (!this.telegramContact.isBlank()) contacts.add("Telegram - " + this.telegramContact);
      if (!this.discordContact.isBlank()) contacts.add("Discord - " + this.discordContact);
      return ModeratorProofCommands.requestEvidence(this.moderator, this.releasedPlayer, this.requestedReason,
         String.join("; ", contacts));
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(1080, this.width - 28);
      this.panelHeight = Math.min(500, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int contentX = this.panelX + 20;
      int contentWidth = this.panelWidth - 40;
      this.addCentered(this.panelY + 14, Component.literal("ПРОВЕРЬ ПЕРЕД ОТПРАВКОЙ")
         .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), contentWidth, 16);
      this.addCentered(this.panelY + 37, Component.literal("Команды отправятся по очереди. Сверь ники, наказание и причину.")
         .withStyle(ChatFormatting.GRAY), contentWidth, 14);

      this.firstCardTop = this.panelY + 65;
      this.addText(contentX + 12, this.firstCardTop + 10, Component.literal("ШАГ 1 · ВРЕМЕННО СНЯТЬ НАКАЗАНИЕ")
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), contentWidth - 24, 14);
      int firstTextBottom = this.addWrapped(this.releaseCommand(), contentX + 12, this.firstCardTop + 34,
         contentWidth - 24, ChatFormatting.AQUA, 18, 3);
      this.firstCardBottom = firstTextBottom + 9;

      this.secondCardTop = this.firstCardBottom + 10;
      this.addText(contentX + 12, this.secondCardTop + 10, Component.literal("ШАГ 2 · ЗАПРОСИТЬ ДОКАЗАТЕЛЬСТВА")
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), contentWidth - 24, 14);
      int secondTextBottom = this.addWrapped(this.proofCommand(), contentX + 12, this.secondCardTop + 34,
         contentWidth - 24, ChatFormatting.AQUA, 18, 6);
      this.secondCardBottom = secondTextBottom + 9;
      this.warningY = Math.min(this.panelY + this.panelHeight - 65, this.secondCardBottom + 10);
      this.addCentered(this.warningY, Component.literal("Проверь ник, тип наказания и запрашиваемую причину.")
         .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), contentWidth, 14);

      this.addRenderableWidget(new BlueButton(this.width / 2 - 187, this.panelY + this.panelHeight - 34,
         180, 22, Component.literal("Отправить обе команды").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), b -> this.send()));
      this.addRenderableWidget(new BlueButton(this.width / 2 + 7, this.panelY + this.panelHeight - 34,
         180, 22, Component.literal("← Исправить данные"), b -> this.edit()));
   }

   private int addWrapped(String value, int x, int y, int width, ChatFormatting color, int lineHeight, int maxLines) {
      String remaining = value == null ? "" : value;
      int line = 0;
      while (!remaining.isEmpty() && line < maxLines) {
         int end = 0;
         while (end < remaining.length() && this.font.width(remaining.substring(0, end + 1)) <= width) end++;
         if (end <= 0) break;
         if (end < remaining.length()) {
            int space = remaining.lastIndexOf(' ', end);
            if (space > 0) end = space;
         }
         String text = remaining.substring(0, end).strip();
         this.addText(x, y + line * lineHeight, Component.literal(text).withStyle(color), width, lineHeight);
         remaining = remaining.substring(end).stripLeading();
         line++;
      }
      return y + line * lineHeight;
   }

   private void addText(int x, int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(x, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addCentered(int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void edit() {
      this.minecraft.setScreen(new ProofRequestScreen(this.moderator, this.releasedPlayer, this.banTarget,
         this.vkContact, this.telegramContact, this.discordContact, this.requestedReason));
   }

   private void send() {
      Minecraft client = this.minecraft;
      if (client == null || client.getConnection() == null) return;
      RuleHelperConfig.saveModeratorContacts(this.vkContact, this.telegramContact, this.discordContact);
      client.getConnection().sendCommand(this.releaseCommand().substring(1));
      client.getConnection().sendCommand(this.proofCommand().substring(1));
      client.gui.getChat().addMessage(Component.literal("Запрос доказательств отправлен."));
      client.setScreen(null);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1,
         this.panelY + this.panelHeight + 1, 0xD908101A);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth,
         this.panelY + this.panelHeight, 0xD51A2432);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth,
         this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth,
         this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX + 10, this.firstCardTop, this.panelX + this.panelWidth - 10,
         this.firstCardBottom, 0xB3213548);
      graphics.fill(this.panelX + 10, this.firstCardTop, this.panelX + 12,
         this.firstCardBottom, 0xFFFFB938);
      graphics.fill(this.panelX + 10, this.secondCardTop, this.panelX + this.panelWidth - 10,
         this.secondCardBottom, 0xB3213548);
      graphics.fill(this.panelX + 10, this.secondCardTop, this.panelX + 12,
         this.secondCardBottom, 0xFF38C5D2);
      super.render(graphics, mouseX, mouseY, delta);
   }

   @Override public void onClose() { this.edit(); }
}
