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

/** Compact settings panel with the RuleHelper navy-and-cyan visual style. */
public final class ModerationSettingsScreen extends Screen {
   private final Screen parent;
   private EditBox menuPrefix;
   private EditBox alertPrefix;
   private BlueButton[] keyButtons;
   private int capturing = -1;
   private int proofKey;
   private int historyKey;
   private int panelX, panelY, panelWidth, panelHeight;

   public ModerationSettingsScreen(Screen parent) {
      super(Component.literal("Настройки RuleHelper"));
      this.parent = parent;
      this.proofKey = RuleHelperConfig.proofRequestKey();
      this.historyKey = RuleHelperConfig.dailyHistoryKey();
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(540, this.width - 24);
      this.panelHeight = Math.min(420, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;

      this.addCentered(this.panelY + 15, Component.literal("НАСТРОЙКИ RULEHELPER")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
      this.addCentered(this.panelY + 36, Component.literal("Префикс ставится перед именем в сообщениях игроков.")
         .withStyle(ChatFormatting.GRAY));
      this.addLabel(this.panelX + 22, this.panelY + 67, "Префикс меню в чате");
      this.menuPrefix = this.input(this.panelX + 22, this.panelY + 83, this.panelWidth - 44,
         RuleHelperConfig.chatMenuPrefix(), 8);
      this.addLabel(this.panelX + 22, this.panelY + 115, "Префикс уведомлений");
      this.alertPrefix = this.input(this.panelX + 22, this.panelY + 131, this.panelWidth - 44,
         RuleHelperConfig.alertPrefix(), 32);

      this.addCentered(this.panelY + 171, Component.literal("ГОРЯЧИЕ КЛАВИШИ")
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      this.addCentered(this.panelY + 191, Component.literal("Home всегда открывает это меню настроек.")
         .withStyle(ChatFormatting.GRAY));
      this.keyButtons = new BlueButton[2];
      String[] labels = {"Запрос доказательств", "История наказаний за день"};
      for (int i = 0; i < labels.length; i++) {
         final int keyIndex = i;
         int y = this.panelY + 220 + i * 34;
         this.addLabel(this.panelX + 22, y + 5, labels[i]);
         this.keyButtons[i] = new BlueButton(this.panelX + this.panelWidth - 190, y, 168, 24,
            this.keyLabel(i), b -> {
               this.capturing = keyIndex;
               b.setMessage(Component.literal("Нажми клавишу… (Esc — отмена)"));
            });
         this.addRenderableWidget(this.keyButtons[i]);
      }

      this.addRenderableWidget(new BlueButton(this.width / 2 - 112, this.panelY + this.panelHeight - 34,
         104, 22, Component.literal("Сохранить"), b -> this.save()));
      this.addRenderableWidget(new BlueButton(this.width / 2 + 8, this.panelY + this.panelHeight - 34,
         104, 22, Component.literal("Назад"), b -> this.back()));
   }

   private EditBox input(int x, int y, int width, String value, int maxLength) {
      EditBox box = new EditBox(this.font, x, y, width, 22, Component.empty());
      box.setMaxLength(maxLength);
      box.setValue(value);
      return this.addRenderableWidget(box);
   }

   private void addLabel(int x, int y, String label) {
      StringWidget widget = new StringWidget(x, y, this.panelWidth - 44, 14,
         Component.literal(label).withStyle(ChatFormatting.WHITE), this.font);
      widget.setMaxWidth(this.panelWidth - 44);
      this.addRenderableWidget(widget);
   }

   private Component keyLabel(int index) {
      int key = index == 0 ? this.proofKey : this.historyKey;
      String label = this.capturing == index ? "Нажми клавишу… (Esc — отмена)"
         : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
      return Component.literal(label).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.capturing < 0) return super.keyPressed(event);
      if (event.key() != InputConstants.KEY_ESCAPE) {
         if (this.capturing == 0) this.proofKey = event.key();
         else this.historyKey = event.key();
      }
      int changed = this.capturing;
      this.capturing = -1;
      this.keyButtons[changed].setMessage(this.keyLabel(changed));
      return true;
   }

   private void save() {
      RuleHelperConfig.setChatMenuPrefix(this.menuPrefix.getValue());
      RuleHelperConfig.setAlertPrefix(this.alertPrefix.getValue());
      RuleHelperConfig.saveModerationKeys(this.proofKey, this.historyKey);
      this.back();
   }

   private void back() { if (this.minecraft != null) this.minecraft.setScreen(this.parent); }

   private void addCentered(int y, Component text) {
      int width = Math.min(this.font.width(text), this.panelWidth - 32);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 14, text, this.font);
      widget.setMaxWidth(this.panelWidth - 32);
      this.addRenderableWidget(widget);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1, this.panelY + this.panelHeight + 1, 0xE809111C);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xD61A2432);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF38C5D2);
      graphics.fill(this.panelX + 22, this.panelY + 58, this.panelX + this.panelWidth - 22, this.panelY + 59, 0xFF354D63);
      graphics.fill(this.panelX + 22, this.panelY + 161, this.panelX + this.panelWidth - 22, this.panelY + 162, 0xFF354D63);
      super.render(graphics, mouseX, mouseY, delta);
   }

   @Override public void onClose() { this.back(); }
}
