package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.PunishmentScope;

/** Compact styled action selector for rules with selectable server scope. */
public final class ActionChoiceScreen extends Screen {
   private final String playerName;
   private final RuleEntry rule;
   private final int previousScroll;
   private int scopeIndex;
   private int panelX, panelY, panelWidth, panelHeight;

   public ActionChoiceScreen(String playerName, RuleEntry rule, int previousScroll) {
      super(Component.literal(rule.code() + " | " + rule.title()));
      this.playerName = playerName;
      this.rule = rule;
      this.previousScroll = previousScroll;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(540, this.width - 24);
      this.panelHeight = Math.min(490, Math.max(290, 162 + this.rule.actions().size() * 29));
      this.panelHeight = Math.min(this.panelHeight, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.addText(this.panelX + 18, this.panelY + 14,
         Component.literal("ВЫБОР НАКАЗАНИЯ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 36, 16);
      this.addText(this.panelX + 18, this.panelY + 37,
         Component.literal("Игрок: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(this.playerName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)), this.panelWidth - 36, 14);
      this.addText(this.panelX + 18, this.panelY + 57,
         Component.literal(this.rule.code() + "  ·  " + this.rule.title()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), this.panelWidth - 36, 14);

      int contentY = this.panelY + 82;
      int contentWidth = this.panelWidth - 36;
      int summaryHeight = this.addWrapped(this.rule.summary(), this.panelX + 18, contentY, contentWidth,
         ChatFormatting.GRAY, 12, 4);
      int cursorY = Math.max(contentY + 30, summaryHeight + 8);
      if (this.isLocalRuleOrRelease()) {
         this.addText(this.panelX + 18, cursorY, Component.literal("РЕЖИМ ВЫДАЧИ")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), contentWidth, 12);
         cursorY += 17;
         PunishmentScope scope = this.scopes()[this.scopeIndex];
         this.addRenderableWidget(new BlueButton(this.panelX + 18, cursorY, contentWidth, 24,
            Component.literal(scope.label()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), b -> {
               this.scopeIndex = (this.scopeIndex + 1) % this.scopes().length;
               this.rebuildWidgets();
            }));
         cursorY += 34;
      } else {
         this.addText(this.panelX + 18, cursorY, Component.literal("РЕЖИМ ВЫДАЧИ · ГЛОБАЛЬНО (-g)")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), contentWidth, 12);
         cursorY += 26;
      }

      this.addText(this.panelX + 18, cursorY, Component.literal("ВАРИАНТЫ НАКАЗАНИЯ")
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), contentWidth, 12);
      cursorY += 17;
      int buttonWidth = contentWidth;
      int availableBottom = this.panelY + this.panelHeight - 51;
      for (RuleAction action : this.rule.actions()) {
         if (cursorY + 23 > availableBottom) break;
         String command = this.scopedCommand(action);
         Component tip = Component.literal(this.rule.punishment()).withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("\n" + command).withStyle(ChatFormatting.AQUA));
         this.addRenderableWidget(new BlueButton(this.panelX + 18, cursorY, buttonWidth, 22,
            Component.literal(action.label()).withStyle(ChatFormatting.WHITE), b -> this.execute(action))
            .withTooltip(Tooltip.create(tip)));
         cursorY += 27;
      }
      this.addRenderableWidget(new BlueButton(this.panelX + this.panelWidth / 2 - 82,
         this.panelY + this.panelHeight - 34, 164, 22, Component.literal("← Назад к правилам"), b -> this.back()));
   }

   private int addWrapped(String value, int x, int y, int width, ChatFormatting color, int lineHeight, int maxLines) {
      String remaining = value == null ? "" : value;
      int line = 0;
      while (!remaining.isEmpty() && line < maxLines) {
         int end = 0;
         while (end < remaining.length() && this.font.width(remaining.substring(0, end + 1)) <= width) end++;
         if (end <= 0) break;
         if (end < remaining.length()) {
            int space = remaining.lastIndexOf(' ', end);
            if (space > 0) end = space;
         }
         this.addText(x, y + line * lineHeight, Component.literal(remaining.substring(0, end).strip())
            .withStyle(color), width, lineHeight);
         remaining = remaining.substring(end).stripLeading();
         line++;
      }
      return y + line * lineHeight;
   }

   private void addText(int x, int y, Component text, int maxWidth, int height) {
      int textWidth = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(x, y, textWidth, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
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
      if (this.rule.code().equals("release")) {
         PunishmentScope scope = this.scopes()[this.scopeIndex];
         return RuleHelperConfig.applyPunishmentScope(command, null,
            scope == PunishmentScope.RULE_DEFAULT ? PunishmentScope.CURRENT : scope);
      }
      command = RuleHelperConfig.formatCause(command, this.rule.code(), this.rule.title());
      if (this.rule.code().startsWith("1.") || this.rule.code().startsWith("2.") || this.rule.code().startsWith("3.")) {
         return RuleHelperConfig.applyPunishmentScope(command, this.rule.code(), PunishmentScope.RULE_DEFAULT);
      }
      return RuleHelperConfig.applyPunishmentScope(command, this.rule.code(), this.scopes()[this.scopeIndex]);
   }

   private boolean isLocalRuleOrRelease() {
      String code = this.rule.code();
      return code.equals("release") || code.startsWith("4.") || code.startsWith("5.");
   }

   private PunishmentScope[] scopes() {
      return new PunishmentScope[]{PunishmentScope.RULE_DEFAULT, PunishmentScope.SURVIVAL,
         PunishmentScope.GRIEF, PunishmentScope.MINIGAMES};
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
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
      super.render(graphics, mouseX, mouseY, delta);
   }

   private void back() { if (this.minecraft != null) this.minecraft.setScreen(new RuleMenuScreen(this.playerName, this.previousScroll, this)); }
   @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(null); }
}
