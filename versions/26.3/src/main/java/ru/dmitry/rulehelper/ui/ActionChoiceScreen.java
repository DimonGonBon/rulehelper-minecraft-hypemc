package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;

public final class ActionChoiceScreen extends Screen {
   private final String playerName;
   private final RuleEntry rule;
   private final int previousScroll;

   public ActionChoiceScreen(String playerName, RuleEntry rule, int previousScroll) {
      super(Component.literal(rule.code() + " | " + rule.title()));
      this.playerName = playerName;
      this.rule = rule;
      this.previousScroll = previousScroll;
   }

   @Override
   protected void init() {
      Component playerLabel = Component.literal("Игрок: ")
         .withStyle(ChatFormatting.WHITE)
         .append(Component.literal(this.playerName).withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}));
      this.addCenteredText(18, playerLabel, Math.min(this.width - 24, 520));
      this.addCenteredText(
         34,
         Component.literal(this.rule.code() + "  |  " + this.rule.title()).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}),
         Math.min(this.width - 24, 520)
      );
      int buttonWidth = Math.min(500, this.width - 36);
      int x = (this.width - buttonWidth) / 2;
      int top = Math.max(70, this.height / 2 - this.rule.actions().size() * 15);

      for (int i = 0; i < this.rule.actions().size(); i++) {
         RuleAction action = this.rule.actions().get(i);
         Component tip = Component.literal(this.rule.summary())
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("\nНаказание: ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(action.label()).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n" + this.scopedCommand(action)).withStyle(ChatFormatting.AQUA));
         this.addRenderableWidget(
            Button.builder(Component.literal(action.label()), b -> this.execute(action))
               .bounds(x, top + i * 25, buttonWidth, 20)
               .tooltip(Tooltip.create(tip))
               .build()
         );
      }

      this.addRenderableWidget(
         Button.builder(
               Component.literal("← Назад к правилам"), b -> this.minecraft.gui.setScreen(new RuleMenuScreen(this.playerName, this.previousScroll, this))
            )
            .bounds(this.width / 2 - 78, this.height - 34, 156, 20)
            .build()
      );
   }

   private void addCenteredText(int y, Component text, int maxWidth) {
      int textWidth = Math.min(this.font.width(text), maxWidth);
      this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, y, textWidth, 12, text, this.font).setMaxWidth(maxWidth));
   }

   private void execute(RuleAction action) {
      if (this.minecraft != null && this.minecraft.getConnection() != null) {
         String command = this.scopedCommand(action);
         this.minecraft.getConnection().sendCommand(command.startsWith("/") ? command.substring(1) : command);
         this.onClose();
      }
   }

   private String scopedCommand(RuleAction action) {
      String command = action.commandFor(this.playerName);
      return this.rule.code().equals("release") ? command : RuleHelperConfig.applyDonationScope(command, this.rule.code());
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -385217000);
      context.fill(this.width / 2 - 120, 61, this.width / 2 + 120, 62, -13181454);
      super.extractRenderState(context, mouseX, mouseY, delta);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(null);
      }
   }
}
