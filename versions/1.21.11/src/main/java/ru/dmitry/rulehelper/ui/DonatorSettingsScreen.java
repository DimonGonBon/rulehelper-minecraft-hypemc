package ru.dmitry.rulehelper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Donator settings: editable chat prefixes and a configurable daily-history shortcut. */
public final class DonatorSettingsScreen extends Screen {
   private final Screen parent;
   private EditBox menuPrefix;
   private EditBox alertPrefix;
   private BlueButton historyKeyButton;
   private int historyKey;
   private boolean capturing;
   private int panelX, panelY, panelWidth, panelHeight;

   public DonatorSettingsScreen(Screen parent) {
      super(Component.literal("Настройки RuleHelper"));
      this.parent = parent;
      this.historyKey = RuleHelperConfig.dailyHistoryKey();
   }

   @Override protected void init() {
      this.panelWidth = Math.min(540, this.width - 24);
      this.panelHeight = Math.min(350, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.addCentered(this.panelY + 16, "НАСТРОЙКИ RULEHELPER", ChatFormatting.AQUA, true);
      this.addCentered(this.panelY + 38, "Префикс перед ником открывает меню; сам ник копируется.", ChatFormatting.GRAY, false);
      this.addLabel(this.panelX + 22, this.panelY + 70, "Префикс меню в чате");
      this.menuPrefix = this.input(this.panelX + 22, this.panelY + 87, this.panelWidth - 44,
         RuleHelperConfig.chatMenuPrefix(), 8);
      this.addLabel(this.panelX + 22, this.panelY + 120, "Префикс уведомлений");
      this.alertPrefix = this.input(this.panelX + 22, this.panelY + 137, this.panelWidth - 44,
         RuleHelperConfig.alertPrefix(), 32);
      this.addCentered(this.panelY + 178, "ГОРЯЧИЕ КЛАВИШИ", ChatFormatting.GOLD, true);
      this.addCentered(this.panelY + 199, "Home открывает настройки", ChatFormatting.GRAY, false);
      this.addLabel(this.panelX + 22, this.panelY + 234, "История наказаний за день");
      this.historyKeyButton = new BlueButton(this.panelX + this.panelWidth - 190, this.panelY + 228, 168, 24,
         this.historyKeyLabel(), b -> { this.capturing = true; b.setMessage(Component.literal("Нажми клавишу… (Esc — отмена)")); });
      this.addRenderableWidget(this.historyKeyButton);
      this.addRenderableWidget(new BlueButton(this.width / 2 - 112, this.panelY + this.panelHeight - 35,
         104, 22, Component.literal("Сохранить"), b -> this.save()));
      this.addRenderableWidget(new BlueButton(this.width / 2 + 8, this.panelY + this.panelHeight - 35,
         104, 22, Component.literal("Назад"), b -> this.back()));
   }

   private EditBox input(int x, int y, int width, String value, int max) {
      EditBox box = new EditBox(this.font, x, y, width, 22, Component.empty());
      box.setMaxLength(max);
      box.setValue(value);
      return this.addRenderableWidget(box);
   }
   private void addLabel(int x, int y, String text) {
      StringWidget widget = new StringWidget(x, y, this.panelWidth - 44, 14, Component.literal(text), this.font);
      widget.setMaxWidth(this.panelWidth - 44);
      this.addRenderableWidget(widget);
   }
   private void addCentered(int y, String text, ChatFormatting color, boolean bold) {
      Component component = Component.literal(text).withStyle(bold ? new ChatFormatting[]{color, ChatFormatting.BOLD} : new ChatFormatting[]{color});
      int width = Math.min(this.font.width(component), this.panelWidth - 32);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 14, component, this.font);
      widget.setMaxWidth(this.panelWidth - 32);
      this.addRenderableWidget(widget);
   }
   private Component historyKeyLabel() {
      return Component.literal(this.capturing ? "Нажми клавишу… (Esc — отмена)"
         : InputConstants.Type.KEYSYM.getOrCreate(this.historyKey).getDisplayName().getString())
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   }
   @Override public boolean keyPressed(KeyEvent event) {
      if (!this.capturing) return super.keyPressed(event);
      if (event.key() != InputConstants.KEY_ESCAPE && event.key() != InputConstants.KEY_HOME) this.historyKey = event.key();
      this.capturing = false;
      this.historyKeyButton.setMessage(this.historyKeyLabel());
      return true;
   }
   private void save() {
      RuleHelperConfig.setChatMenuPrefix(this.menuPrefix.getValue());
      RuleHelperConfig.setAlertPrefix(this.alertPrefix.getValue());
      RuleHelperConfig.setDailyHistoryKey(this.historyKey);
      this.back();
   }
   private void back() { if (this.minecraft != null) this.minecraft.setScreen(this.parent); }
   @Override public void onClose() { this.back(); }
   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1, this.panelY + this.panelHeight + 1, 0xE809111C);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xD61A2432);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF38C5D2);
      super.render(graphics, mouseX, mouseY, delta);
   }
}
