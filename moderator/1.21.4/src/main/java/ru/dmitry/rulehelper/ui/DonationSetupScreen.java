package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.PlayerNames;

/** Nickname-only setup for the moderator build; it has no donation selector. */
public final class DonationSetupScreen extends Screen {
   private final String selectedPlayer;
   private EditBox nicknameInput;

   public DonationSetupScreen(String selectedPlayer) {
      super(Component.literal("Настройка Rule Helper"));
      this.selectedPlayer = selectedPlayer;
   }

   @Override
   protected void init() {
      this.addCenteredText(34, Component.literal("ПРОФИЛЬ МОДЕРАТОРА").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), 360);
      this.addCenteredText(59, Component.literal("Укажи свой ник на сервере"), 320);
      this.nicknameInput = new EditBox(this.font, this.width / 2 - 120, 87, 240, 22, Component.literal("Ник на сервере"));
      this.nicknameInput.setMaxLength(16);
      if (!RuleHelperConfig.nickname().isBlank()) this.nicknameInput.setValue(RuleHelperConfig.nickname());
      this.addRenderableWidget(this.nicknameInput);
      this.addCenteredText(120, Component.literal("Донат для этой версии фиксирован как «Версия для модераторов».").withStyle(ChatFormatting.GRAY), 560);
      this.addRenderableWidget(Button.builder(Component.literal("Сохранить ник"), b -> this.save())
         .bounds(this.width / 2 - 92, 163, 184, 22).build());
      this.addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.minecraft.setScreen(new RuleMenuScreen(this.selectedPlayer)))
         .bounds(this.width / 2 - 48, this.height - 32, 96, 20).build());
   }

   private void save() {
      String nickname = this.nicknameInput.getValue().trim();
      if (!PlayerNames.isValid(nickname)) return;
      RuleHelperConfig.saveModeratorName(nickname);
      if (this.minecraft != null) this.minecraft.setScreen(new RuleMenuScreen(this.selectedPlayer));
   }

   private void addCenteredText(int y, Component text, int maxWidth) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 12, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(0, 0, this.width, this.height, -653652456);
      super.render(graphics, mouseX, mouseY, delta);
   }
}
