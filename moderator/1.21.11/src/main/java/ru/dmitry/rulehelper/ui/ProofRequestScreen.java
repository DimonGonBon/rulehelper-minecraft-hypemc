package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Moderator form for temporarily releasing a player while requesting evidence. */
public final class ProofRequestScreen extends Screen {
   private EditBox moderatorInput;
   private EditBox releasedPlayerInput;
   private EditBox requestedReasonInput;
   private EditBox vkContactInput;
   private EditBox telegramContactInput;
   private EditBox discordContactInput;
   private StringWidget formStatus;
   private boolean banTarget;
   private final String initialModerator;
   private final String initialPlayer;
   private final String initialVk;
   private final String initialTelegram;
   private final String initialDiscord;
   private final String initialRequestedReason;
   private int panelX, panelY, panelWidth, panelHeight;

   public ProofRequestScreen() {
      this("", "", true, RuleHelperConfig.moderatorVkContact(),
         RuleHelperConfig.moderatorTelegramContact(), RuleHelperConfig.moderatorDiscordContact(), "");
   }

   public ProofRequestScreen(String moderator, String player, boolean banTarget, String vk, String telegram, String discord,
                             String requestedReason) {
      super(Component.literal("Запрос доказательств"));
      this.initialModerator = moderator;
      this.initialPlayer = player;
      this.banTarget = banTarget;
      this.initialVk = vk;
      this.initialTelegram = telegram;
      this.initialDiscord = discord;
      this.initialRequestedReason = requestedReason;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(650, this.width - 24);
      this.panelHeight = Math.min(435, this.height - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.addCentered(this.panelY + 12, Component.literal("ЗАПРОС ДОКАЗАТЕЛЬСТВ")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 32, 16);
      this.addCentered(this.panelY + 32, Component.literal("Временно освободи игрока и запроси доказательства у модератора")
         .withStyle(ChatFormatting.GRAY), this.panelWidth - 32, 14);

      int inputWidth = Math.min(420, this.panelWidth - 56);
      int inputX = this.width / 2 - inputWidth / 2;
      this.addCentered(this.panelY + 56, Component.literal("У кого запросить доказательства:")
         .withStyle(ChatFormatting.WHITE), this.panelWidth - 32, 14);
      this.moderatorInput = this.addInput(inputX, this.panelY + 72, inputWidth, "Ник модератора", this.initialModerator, 16);
      this.addCentered(this.panelY + 101, Component.literal("Кого освободить на время разбирательств:")
         .withStyle(ChatFormatting.WHITE), this.panelWidth - 32, 14);
      this.releasedPlayerInput = this.addInput(inputX, this.panelY + 117, inputWidth, "Ник игрока", this.initialPlayer, 16);

      this.addCentered(this.panelY + 150, Component.literal("Какое наказание временно снять?")
         .withStyle(ChatFormatting.WHITE), this.panelWidth - 32, 14);
      int buttonGap = 8;
      int choiceWidth = Math.min(190, (this.panelWidth - 48 - buttonGap) / 2);
      int choiceX = this.width / 2 - choiceWidth - buttonGap / 2;
      this.addRenderableWidget(new BlueButton(choiceX, this.panelY + 167, choiceWidth, 22,
         this.typeLabel(true), b -> this.setType(true)));
      this.addRenderableWidget(new BlueButton(this.width / 2 + buttonGap / 2, this.panelY + 167, choiceWidth, 22,
         this.typeLabel(false), b -> this.setType(false)));

      this.addCentered(this.panelY + 197, Component.literal("Запрашиваемая причина наказания:")
         .withStyle(ChatFormatting.WHITE), this.panelWidth - 32, 14);
      this.requestedReasonInput = this.addInput(inputX, this.panelY + 213, inputWidth, "Например: 3.2",
         this.initialRequestedReason, 80);

      this.addCentered(this.panelY + 245, Component.literal("Контакты для ответа · укажи хотя бы один")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 32, 14);
      this.addContactRow(this.panelY + 263, "VK:", this.initialVk, 1);
      this.addContactRow(this.panelY + 287, "Telegram:", this.initialTelegram, 2);
      this.addContactRow(this.panelY + 311, "Discord:", this.initialDiscord, 3);
      this.addCentered(this.panelY + 339, Component.literal("Контакты сохраняются на этом компьютере.")
         .withStyle(ChatFormatting.GRAY), this.panelWidth - 32, 14);
      this.formStatus = this.addCentered(this.panelY + 358, Component.literal("")
         .withStyle(ChatFormatting.RED), this.panelWidth - 32, 14);

      this.addRenderableWidget(new BlueButton(this.width / 2 - 153, this.panelY + this.panelHeight - 31,
         146, 22, Component.literal("Проверить команды"), b -> this.preview()));
      this.addRenderableWidget(new BlueButton(this.width / 2 + 7, this.panelY + this.panelHeight - 31,
         106, 22, Component.literal("Закрыть"), b -> this.onClose()));
   }

   private EditBox addInput(int x, int y, int width, String hint, String value, int maxLength) {
      EditBox input = new EditBox(this.font, x, y, width, 21, Component.literal(hint));
      input.setMaxLength(maxLength);
      input.setValue(value);
      this.addRenderableWidget(input);
      return input;
   }

   private void addContactRow(int y, String label, String value, int field) {
      int totalWidth = Math.min(500, this.panelWidth - 50);
      int x = (this.width - totalWidth) / 2;
      int labelWidth = 92;
      StringWidget labelWidget = new StringWidget(x, y + 5, labelWidth, 12,
         Component.literal("Контакт " + label).withStyle(ChatFormatting.WHITE), this.font);
      this.addRenderableWidget(labelWidget);
      EditBox input = new EditBox(this.font, x + labelWidth + 6, y, totalWidth - labelWidth - 6, 21,
         Component.literal(label));
      input.setMaxLength(100);
      input.setValue(value);
      this.addRenderableWidget(input);
      switch (field) {
         case 1 -> this.vkContactInput = input;
         case 2 -> this.telegramContactInput = input;
         case 3 -> this.discordContactInput = input;
      }
   }

   private Component typeLabel(boolean ban) {
      String label = ban ? "БАН" : "МУТ";
      if (this.banTarget == ban) label += "  ✓";
      return Component.literal(label).withStyle(this.banTarget == ban
         ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.WHITE});
   }

   private void setType(boolean ban) {
      String moderator = this.moderatorInput.getValue();
      String player = this.releasedPlayerInput.getValue();
      String vk = this.vkContactInput.getValue();
      String telegram = this.telegramContactInput.getValue();
      String discord = this.discordContactInput.getValue();
      String requestedReason = this.requestedReasonInput.getValue();
      this.banTarget = ban;
      this.minecraft.setScreen(new ProofRequestScreen(moderator, player, ban, vk, telegram, discord, requestedReason));
   }

   private void preview() {
      String moderator = this.moderatorInput.getValue().trim();
      String player = this.releasedPlayerInput.getValue().trim();
      String vk = this.vkContactInput.getValue().trim();
      String telegram = this.telegramContactInput.getValue().trim();
      String discord = this.discordContactInput.getValue().trim();
      String requestedReason = this.requestedReasonInput.getValue().trim();
      List<String> contacts = new ArrayList<>();
      if (!vk.isBlank()) contacts.add("VK - " + vk);
      if (!telegram.isBlank()) contacts.add("Telegram - " + telegram);
      if (!discord.isBlank()) contacts.add("Discord - " + discord);
      if (!ru.dmitry.rulehelper.EvidenceRequestNames.isValid(moderator) || !ru.dmitry.rulehelper.EvidenceRequestNames.isValid(player)) {
         this.showFormError("Укажи корректные ники (3–16 символов: буквы, цифры и _; русские буквы допускаются в этой форме)."); return;
      }
      if (moderator.equalsIgnoreCase(player)) { this.showFormError("Ники модератора и игрока должны отличаться."); return; }
      if (requestedReason.isBlank() || requestedReason.contains("\n") || requestedReason.contains("\r") || requestedReason.contains("|")) {
         this.showFormError("Укажи запрашиваемую причину в одну строку, без символа |."); return;
      }
      if (contacts.isEmpty()) { this.showFormError("Укажи хотя бы один контакт: VK, Telegram или Discord."); return; }
      if (vk.contains("\n") || telegram.contains("\n") || discord.contains("\n")
         || vk.contains("\r") || telegram.contains("\r") || discord.contains("\r")) {
         this.showFormError("Контакт должен быть в одной строке."); return;
      }
      this.minecraft.setScreen(new ProofRequestConfirmScreen(moderator, player, this.banTarget, vk, telegram, discord, requestedReason));
   }

   private void showFormError(String message) {
      if (this.formStatus != null) this.formStatus.setMessage(Component.literal(message).withStyle(ChatFormatting.RED));
   }

   private StringWidget addCentered(int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      return this.addRenderableWidget(widget);
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
      super.render(graphics, mouseX, mouseY, delta);
   }

   @Override
   public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(null); }
}
