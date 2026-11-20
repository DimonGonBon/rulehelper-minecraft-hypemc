package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Test-only cause formatting preview and selector. */
public final class CauseStyleScreen extends Screen {
   private final Screen parent;
   public CauseStyleScreen(Screen parent) {
      super(Component.literal("Варианты причины"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.addCentered(14, Component.literal("СТИЛЬ ПРИЧИНЫ — ТЕСТ").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), 250);
      this.addCentered(31, Component.literal("Короткий, расширенный или с символами").withStyle(ChatFormatting.GRAY), 330);
      RuleHelperConfig.CauseStyle[] styles = RuleHelperConfig.CauseStyle.values();
      int gap = 8;
      int width = Math.min(244, (this.width - 32 - gap) / 2);
      int left = (this.width - (width * 2 + gap)) / 2;
      int top = 58;
      for (int i = 0; i < styles.length; i++) {
         RuleHelperConfig.CauseStyle style = styles[i];
         int x = left + (i % 2) * (width + gap);
         int y = top + (i / 2) * 56;
         Component label = Component.literal((style == RuleHelperConfig.causeStyle() ? "✓ " : "") + style.label())
            .withStyle(style == RuleHelperConfig.causeStyle() ? ChatFormatting.AQUA : ChatFormatting.WHITE);
         this.addRenderableWidget(Button.builder(label, b -> {
            RuleHelperConfig.setCauseStyle(style);
            this.minecraft.gui.setScreen(this.parent);
         }).bounds(x, y, width, 20).build());
         this.addCentered(y + 24, Component.literal(preview(style)).withStyle(ChatFormatting.WHITE), width + gap);
      }
      int bottom = this.height - 30;
      this.addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> this.minecraft.gui.setScreen(this.parent))
         .bounds(this.width / 2 - 52, bottom, 104, 20).build());
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

   private void addCentered(int y, Component text, int maxWidth) {
      int w = Math.min(this.font.width(text), maxWidth);
      this.addRenderableWidget(new StringWidget(this.width / 2 - w / 2, y, w, 12, text, this.font).setMaxWidth(maxWidth));
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(0, 0, this.width, this.height, -653652456);
      graphics.fill(this.width / 2 - 155, 48, this.width / 2 + 155, 49, -13246990);
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }
}
