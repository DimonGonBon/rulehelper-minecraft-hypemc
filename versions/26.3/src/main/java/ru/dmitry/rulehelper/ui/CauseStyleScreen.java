package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.CauseFormatter;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Shared style selector with live punishment and release previews. */
public final class CauseStyleScreen extends Screen {
   private final Screen parent;
   private int panelX, panelY, panelWidth, panelHeight;

   public CauseStyleScreen(Screen parent) {
      super(Component.literal("Шрифт и стиль причины"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(680, this.width - 24);
      this.panelHeight = Math.min(350, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.addCentered(this.panelY + 12, Component.literal("ШРИФТ И СТИЛЬ ПРИЧИНЫ")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 24, 14);
      this.addCentered(this.panelY + 32, Component.literal("Наказание и снятие · предпросмотр выбранного стиля")
         .withStyle(ChatFormatting.GRAY), this.panelWidth - 24, 12);
      this.addCentered(this.panelY + 51, ReasonComponents.fromCodes(punishmentPreview(RuleHelperConfig.causeStyle())),
         this.panelWidth - 32, 13);
      this.addCentered(this.panelY + 68, ReasonComponents.fromCodes(
         RuleHelperConfig.formatReleaseCause()), this.panelWidth - 32, 13);

      RuleHelperConfig.CauseStyle[] styles = RuleHelperConfig.CauseStyle.values();
      int rows = (styles.length + 1) / 2;
      int rowPitch = Math.min(45, Math.max(20, (this.panelHeight - 126) / rows));
      int colWidth = (this.panelWidth - 42) / 2;
      for (int i = 0; i < styles.length; i++) {
         RuleHelperConfig.CauseStyle style = styles[i];
         int x = this.panelX + 16 + (i % 2) * (colWidth + 10);
         int y = this.panelY + 92 + (i / 2) * rowPitch;
         boolean selected = style == RuleHelperConfig.causeStyle();
         Component label = Component.literal((selected ? "✓ " : "") + style.label())
            .withStyle(selected ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}
               : new ChatFormatting[]{ChatFormatting.WHITE});
         Component tooltip = ReasonComponents.fromCodes(punishmentPreview(style))
            .copy().append(Component.literal("\n"))
            .append(ReasonComponents.fromCodes(CauseFormatter.releaseReason(style.name(), RuleHelperConfig.reasonTemplates())));
         this.addRenderableWidget(new BlueButton(x, y, colWidth, Math.min(27, rowPitch - 3), label, b -> {
            if (style == RuleHelperConfig.CauseStyle.CUSTOM) {
               this.minecraft.gui.setScreen(new CustomReasonScreen(this));
            } else {
               RuleHelperConfig.setCauseStyle(style);
               this.rebuildWidgets();
            }
         }).withTooltip(Tooltip.create(tooltip)));
      }
      this.addRenderableWidget(new BlueButton(this.width / 2 - 78,
         this.panelY + this.panelHeight - 30, 156, 22, Component.literal("Готово · Назад"), b -> this.onClose()));
   }

   private static String punishmentPreview(RuleHelperConfig.CauseStyle style) {
      return CauseFormatter.reason("2.3", "Оскорбительное поведение", null, false, style.name(), RuleHelperConfig.reasonTemplates());
   }

   private void addCentered(int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   @Override public void onClose() { this.minecraft.gui.setScreen(this.parent); }

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
