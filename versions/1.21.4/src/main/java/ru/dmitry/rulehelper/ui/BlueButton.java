package ru.dmitry.rulehelper.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Compact RuleHelper button with a cyan outline and navy fill. */
public final class BlueButton extends AbstractWidget {
   @FunctionalInterface public interface OnPress { void onPress(BlueButton button); }
   private final OnPress onPress;

   public BlueButton(int x, int y, int width, int height, Component label, OnPress onPress) {
      super(x, y, width, height, label);
      this.onPress = onPress;
   }

   public BlueButton withTooltip(Tooltip tooltip) {
      this.setTooltip(tooltip);
      return this;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      int x = this.getX();
      int y = this.getY();
      boolean hover = this.active && this.isHoveredOrFocused();
      int border = hover ? 0xFF65E4F3 : 0xFF379BC2;
      int fill = hover ? 0xF02C6481 : 0xE91B344A;
      graphics.fill(x, y, x + this.width, y + this.height, fill);
      graphics.fill(x, y, x + this.width, y + 1, border);
      graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, border);
      graphics.fill(x, y, x + 1, y + this.height, border);
      graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, border);
      int color = this.active ? 0xFFF3F8FC : 0xFF8996A5;
      graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + this.width / 2,
         y + (this.height - 8) / 2, color);
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      this.playDownSound(Minecraft.getInstance().getSoundManager());
      this.onPress.onPress(this);
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
