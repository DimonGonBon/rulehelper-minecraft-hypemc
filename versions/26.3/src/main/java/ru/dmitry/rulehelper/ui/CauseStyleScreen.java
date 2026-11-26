package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Cause formatting preview and selector. */
public final class CauseStyleScreen extends Screen {
   private final Screen parent;
   private int panelX, panelY, panelWidth, panelHeight;

   public CauseStyleScreen(Screen parent) {
      super(Component.literal("Варианты причины"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(670, this.width - 24);
      this.panelHeight = Math.min(365, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.addCentered(this.panelY + 16, Component.literal("СТИЛЬ ПРИЧИНЫ")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 32, 16);
      this.addCentered(this.panelY + 38, Component.literal("Выбери, как RuleHelper будет оформлять причину в команде")
         .withStyle(ChatFormatting.GRAY), this.panelWidth - 32, 14);
      this.addCentered(this.panelY + 60, Component.literal("Пример: 3.4 | Некорректные наказания")
         .withStyle(ChatFormatting.WHITE), this.panelWidth - 32, 14);

      RuleHelperConfig.CauseStyle[] styles = RuleHelperConfig.CauseStyle.values();
      int gapX = 10;
      int colWidth = (this.panelWidth - 40 - gapX) / 2;
      int left = this.panelX + 15;
      int top = this.panelY + 91;
      int rowPitch = 69;
      for (int i = 0; i < styles.length; i++) {
         RuleHelperConfig.CauseStyle style = styles[i];
         int x = left + (i % 2) * (colWidth + gapX);
         int y = top + (i / 2) * rowPitch;
         boolean selected = style == RuleHelperConfig.causeStyle();
         Component label = Component.literal((selected ? "✓  " : "") + style.label())
            .withStyle(selected ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}
               : new ChatFormatting[]{ChatFormatting.WHITE});
         this.addRenderableWidget(new BlueButton(x, y, colWidth, 24, label, b -> {
            RuleHelperConfig.setCauseStyle(style);
            this.minecraft.gui.setScreen(this.parent);
         }));
         this.addCentered(y + 32, Component.literal(preview(style)).withStyle(
            selected ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}
               : new ChatFormatting[]{ChatFormatting.GRAY}), colWidth, 14);
      }
      this.addRenderableWidget(new BlueButton(this.width / 2 - 64,
         this.panelY + this.panelHeight - 32, 128, 22, Component.literal("← Назад"),
         b -> this.minecraft.gui.setScreen(this.parent)));
   }

   private static String preview(RuleHelperConfig.CauseStyle style) {
      return switch (style) {
         case SHORT -> "3.4";
         case EXTENDED -> "3.4 | Некорректные наказания";
         case CORNER -> "「 3.4 | Некорректные наказания 」";
         case DOUBLE_CORNER -> "『 3.4 | Некорректные наказания 』";
         case SPARKLE -> "✦「 3.4 | Некорректные наказания 」✦";
         case BOX -> "【 3.4 | Некорректные наказания 】";
      };
   }

   private void addCentered(int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }
}
