package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.StringWidget;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.RuleHelperConfig.DonationTier;

/** First-use setup: save the user's own server nickname and declared tier. */
public final class DonationSetupScreen extends Screen {
   private final String selectedPlayer;
   private EditBox nicknameInput;

   public DonationSetupScreen(String selectedPlayer) {
      super(Component.literal("Настройка Rule Helper"));
      this.selectedPlayer = selectedPlayer;
   }

   @Override
   protected void init() {
      this.addCenteredText(22, Component.literal("НАСТРОЙКА RULE HELPER").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), Math.min(this.width - 24, 420));
      this.addCenteredText(43, Component.literal("Введите ваш ник на сервере — это нужно один раз"), Math.min(this.width - 24, 500));
      this.nicknameInput = new EditBox(this.font, this.width / 2 - 110, 64, 220, 20, Component.literal("Ник на сервере"));
      this.nicknameInput.deleteWords(16);
      if (RuleHelperConfig.isConfigured()) {
         this.nicknameInput.setValue(RuleHelperConfig.nickname());
      }
      this.addRenderableWidget(this.nicknameInput);
      this.addCenteredText(91, Component.literal("Ник: 3–16 символов, латинские буквы, цифры и _"), Math.min(this.width - 24, 540));
      this.addCenteredText(117, Component.literal("Выбирайте только свой уровень доната." ).withStyle(ChatFormatting.YELLOW), Math.min(this.width - 24, 520));
      this.addCenteredText(132, Component.literal("Выбор чужого уровня нарушает правило 3.4 и может привести к наказанию.").withStyle(ChatFormatting.YELLOW), Math.min(this.width - 24, 620));
      this.addCenteredText(165, Component.literal("Какой у вас донат?"), Math.min(this.width - 24, 320));
      int buttonWidth = Math.min(230, (this.width - 48) / 2);
      int leftX = this.width / 2 - buttonWidth - 6;
      int rightX = this.width / 2 + 6;
      this.addRenderableWidget(Button.builder(Component.literal("Правитель+"), b -> this.save(DonationTier.LEADER)).bounds(leftX, 191, buttonWidth, 22).build());
      this.addRenderableWidget(Button.builder(Component.literal("Staff-"), b -> this.save(DonationTier.STAFF)).bounds(rightX, 191, buttonWidth, 22).build());
      this.addCenteredText(229, Component.literal("Выбор сохраняется на этом компьютере."), Math.min(this.width - 24, 420));
   }

   private void save(DonationTier selectedTier) {
      String nickname = this.nicknameInput.getValue().trim();
      if (!nickname.matches("[A-Za-z0-9_]{3,16}")) {
         return;
      }
      RuleHelperConfig.save(nickname, selectedTier);
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(new RuleMenuScreen(this.selectedPlayer));
      }
   }

   private void addCenteredText(int y, Component text, int maxWidth) {
      int textWidth = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - textWidth / 2, y, textWidth, 12, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -653652456);
      super.extractRenderState(context, mouseX, mouseY, delta);
   }
}
