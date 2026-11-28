package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.NotificationPreferences;
import ru.dmitry.rulehelper.NotificationPreferences.Option;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** A small draft editor for the three alert categories and the shared notification sound. */
public final class NotificationSettingsScreen extends Screen {
   private final Screen parent;
   private NotificationPreferences draft = RuleHelperConfig.notifications();
   private int panelX, panelY, panelWidth, panelHeight;

   public NotificationSettingsScreen(Screen parent) {
      super(Component.literal("Уведомления RuleHelper"));
      this.parent = parent;
   }
   @Override protected void init() {
      this.panelWidth = Math.min(470, this.width - 24);
      this.panelHeight = Math.min(260, this.height - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.text(this.panelY + 13, "УВЕДОМЛЕНИЯ RULEHELPER", ChatFormatting.AQUA);
      this.text(this.panelY + 33, "Выбери, о каких нарушениях сообщать.", ChatFormatting.GRAY);
      int pitch = this.panelHeight < 240 ? 29 : 34;
      int y = this.panelY + 57;
      for (Option option : Option.values()) {
         this.addRenderableWidget(new BlueButton(this.panelX + 16, y, this.panelWidth - 32, 23,
            this.label(option), b -> { this.draft = this.draft.toggle(option); b.setMessage(this.label(option)); }));
         y += pitch;
      }
      this.text(this.panelY + this.panelHeight - 52, "Выключенная категория не показывает текст и не звучит.", ChatFormatting.GRAY);
      int width = (this.panelWidth - 40) / 2;
      this.addRenderableWidget(new BlueButton(this.panelX + 16, this.panelY + this.panelHeight - 29,
         width, 22, Component.literal("Сохранить"), b -> {
            RuleHelperConfig.setNotifications(this.draft);
            this.onClose();
         }));
      this.addRenderableWidget(new BlueButton(this.panelX + 24 + width, this.panelY + this.panelHeight - 29,
         width, 22, Component.literal("Отмена"), b -> this.onClose()));
   }
   private Component label(Option option) {
      boolean enabled = this.draft.enabled(option);
      return Component.literal(option.label() + " · " + (enabled ? "ВКЛ" : "ВЫКЛ"))
         .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY);
   }
   private void text(int y, String value, ChatFormatting color) {
      StringWidget text = new StringWidget(this.panelX + 16, y, this.panelWidth - 32, 13,
         Component.literal(value).withStyle(color), this.font);
      text.setMaxWidth(this.panelWidth - 32);
      this.addRenderableWidget(text);
   }
   @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(this.parent); }
   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1,
         this.panelY + this.panelHeight + 1, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth,
         this.panelY + this.panelHeight, 0xEF172330);
      super.render(graphics, mouseX, mouseY, delta);
   }
}
