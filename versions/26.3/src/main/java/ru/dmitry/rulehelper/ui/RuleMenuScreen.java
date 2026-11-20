package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleCatalog;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Three-column moderation menu: category, rule list, and selected rule actions. */
public final class RuleMenuScreen extends Screen {
   private static final int[] GROUPS = {0, 1, 2, 3, 4, 5};
   private static final String[] GROUP_NAMES = {"Все", "1 · Общие", "2 · Общение", "3 · Игровые", "4 · Выживание", "5 · Мини-игры"};
   private static final int[] GROUP_COUNTS = {26, 7, 6, 6, 4, 3};
   private final String playerName;
   private final Screen returnTo;
   private int selectedGroup;
   private int firstRow;
   private boolean keepSelectionVisible = true;
   private String selectedRuleCode = "3.4";
   private String searchText = "";
   private EditBox searchBox;
   private int panelTop;
   private int panelBottom;
   private int leftX;
   private int leftWidth;
   private int listX;
   private int listWidth;
   private int detailX;
   private int detailWidth;
   private int listTop;
   private int rowPitch;
   private int visibleRuleRows;
   private List<RuleEntry> visibleRules = List.of();

   public RuleMenuScreen(String playerName) {
      this(playerName, 0, 0, null);
   }

   public RuleMenuScreen(String playerName, int firstRow, Screen returnTo) {
      this(playerName, firstRow, 0, returnTo);
   }

   private RuleMenuScreen(String playerName, int firstRow, int selectedGroup, Screen returnTo) {
      super(Component.literal("Наказание игрока"));
      this.playerName = playerName;
      this.firstRow = firstRow;
      this.selectedGroup = selectedGroup;
      this.returnTo = returnTo;
   }

   @Override
   protected void init() {
      this.computeLayout();
      this.addCentered(10, Component.literal("RULE HELPER · НАКАЗАНИЕ ИГРОКА")
         .withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), Math.min(this.width - 60, 420));

      int styleWidth = Math.min(155, Math.max(112, this.width / 5));
      int styleX = this.width - 12 - styleWidth;
      int playerLabelX = 25;
      String playerCaption = "ВЫБРАННЫЙ ИГРОК";
      this.addRenderableWidget(new StringWidget(playerLabelX, 40, this.font.width(playerCaption), 12,
         Component.literal(playerCaption).withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), this.font));
      int nameX = playerLabelX + this.font.width(playerCaption) + 12;
      int nameWidth = Math.max(40, styleX - nameX - 10);
      StringWidget selectedPlayer = new StringWidget(nameX, 38, nameWidth, 16,
         Component.literal(this.playerName).withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}), this.font);
      selectedPlayer.setMaxWidth(nameWidth);
      this.addRenderableWidget(selectedPlayer);
      this.addRenderableWidget(Button.builder(Component.literal("Причина: " + RuleHelperConfig.causeStyle().label()),
         b -> this.minecraft.gui.setScreen(new CauseStyleScreen(this)))
         .bounds(styleX, 35, styleWidth, 20).build());

      this.buildCategoryPanel();
      this.buildRuleList();
      this.buildSelectedRulePanel();
      this.buildQuickActions();
   }

   private void computeLayout() {
      this.panelTop = 66;
      this.panelBottom = Math.max(this.panelTop + 120, this.height - 103);
      this.leftX = 12;
      this.leftWidth = Math.min(150, Math.max(106, (int)(this.width * 0.18)));
      this.detailWidth = Math.min(264, Math.max(174, (int)(this.width * 0.29)));
      int gap = 8;
      int usable = this.width - 24 - gap * 2;
      if (this.leftWidth + this.detailWidth > usable - 175) {
         this.leftWidth = Math.max(96, usable / 5);
         this.detailWidth = Math.max(164, usable / 3);
      }
      this.listX = this.leftX + this.leftWidth + gap;
      this.listWidth = Math.max(164, this.width - 12 - this.detailWidth - gap - this.listX);
      this.detailX = this.listX + this.listWidth + gap;
      this.detailWidth = Math.max(140, this.width - 12 - this.detailX);
      this.listTop = this.panelTop + 60;
      this.rowPitch = 27;
      this.visibleRuleRows = Math.max(1, (this.panelBottom - this.listTop - 8) / this.rowPitch);
   }

   private void buildCategoryPanel() {
      this.addText(this.leftX + 12, this.panelTop + 10, "РАЗДЕЛЫ", ChatFormatting.GRAY, 12, this.leftWidth - 24);
      for (int i = 0; i < GROUPS.length; i++) {
         int group = GROUPS[i];
         String name = shortGroupName(i) + " · " + GROUP_COUNTS[i];
         Component label = Component.literal(name).withStyle(group == this.selectedGroup
            ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.WHITE});
         int y = this.panelTop + 30 + i * 28;
         this.addRenderableWidget(Button.builder(label, b -> {
            this.selectedGroup = group;
            this.firstRow = 0;
            this.selectFirstVisibleRule();
            this.keepSelectionVisible = true;
            this.rebuildWidgets();
         }).bounds(this.leftX + 8, y, this.leftWidth - 16, 22).build());
      }
      this.addWrapped("Выбери раздел, затем правило.", this.leftX + 10,
         this.panelTop + 30 + GROUPS.length * 28 + 8, this.leftWidth - 20, ChatFormatting.GRAY, 11, 3);
   }

   private String shortGroupName(int index) {
      return switch (index) {
         case 1 -> "1 · Общие";
         case 2 -> "2 · Чат";
         case 3 -> "3 · Игровые";
         case 4 -> "4 · Выживание";
         case 5 -> "5 · Мини-игры";
         default -> "Все правила";
      };
   }

   private void buildRuleList() {
      this.addText(this.listX + 12, this.panelTop + 10, "ПУНКТЫ ПРАВИЛ", ChatFormatting.GRAY, 12, this.listWidth - 24);
      this.searchBox = new EditBox(this.font, this.listX + 10, this.panelTop + 28,
         this.listWidth - 20, 20, Component.literal("Поиск правила"));
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue(this.searchText);
      this.searchBox.setResponder(value -> {
         this.searchText = value;
         this.firstRow = 0;
         this.selectFirstVisibleRule();
         this.keepSelectionVisible = true;
         this.rebuildWidgets();
         this.searchBox.setCursorPosition(this.searchText.length());
         this.setFocused(this.searchBox);
      });
      this.addRenderableWidget(this.searchBox);

      List<RuleEntry> filtered = this.filteredRules();
      this.firstRow = Math.min(this.firstRow, Math.max(0, filtered.size() - this.visibleRuleRows));
      int selectedIndex = this.indexOfSelected(filtered);
      if (this.keepSelectionVisible && selectedIndex >= 0
         && (selectedIndex < this.firstRow || selectedIndex >= this.firstRow + this.visibleRuleRows)) {
         this.firstRow = Math.max(0, selectedIndex - Math.max(0, this.visibleRuleRows / 2));
      }
      this.keepSelectionVisible = false;
      int end = Math.min(filtered.size(), this.firstRow + this.visibleRuleRows);
      this.visibleRules = filtered.subList(this.firstRow, end);
      int rowY = this.listTop;
      for (int index = 0; index < this.visibleRules.size(); index++) {
         RuleEntry rule = this.visibleRules.get(index);
         int y = rowY + index * this.rowPitch;
         ChatFormatting codeColor = this.codeColor(rule.code());
         MutableComponent label = Component.literal(rule.code())
            .withStyle(new ChatFormatting[]{codeColor, ChatFormatting.BOLD})
            .append(Component.literal("   " + rule.title()).withStyle(rule.code().equals(this.selectedRuleCode)
               ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.WHITE}));
         this.addRenderableWidget(Button.builder(label, b -> {
            this.selectedRuleCode = rule.code();
            this.keepSelectionVisible = true;
            this.rebuildWidgets();
         }).bounds(this.listX + 8, y, this.listWidth - 16, 23)
            .tooltip(Tooltip.create(this.tooltipFor(rule))).build());
      }
      if (filtered.isEmpty()) {
         this.addText(this.listX + 14, this.listTop + 8, "Правила не найдены", ChatFormatting.GRAY, 12, this.listWidth - 28);
      }
   }

   private void buildSelectedRulePanel() {
      this.addText(this.detailX + 12, this.panelTop + 10, "ВЫБРАННОЕ ПРАВИЛО", ChatFormatting.GRAY, 12, this.detailWidth - 24);
      RuleEntry selected = this.selectedRule();
      if (selected == null) {
         this.addWrapped("Выбери правило в списке, чтобы увидеть пояснение и доступные варианты наказания.",
            this.detailX + 12, this.panelTop + 36, this.detailWidth - 24, ChatFormatting.GRAY, 12, 7);
         return;
      }

      this.addText(this.detailX + 12, this.panelTop + 32, selected.code(), this.codeColor(selected.code()), 20, 38);
      int titleX = this.detailX + 55;
      int titleWidth = this.detailWidth - 67;
      int titleY = this.panelTop + 31;
      int nextY = this.addWrapped(selected.title(), titleX, titleY, titleWidth, ChatFormatting.WHITE, 13, 2);
      int summaryY = Math.max(this.panelTop + 62, nextY + 6);
      int afterSummary = this.addWrapped(selected.summary(), this.detailX + 12, summaryY,
         this.detailWidth - 24, ChatFormatting.GRAY, 11, 5);
      int punishmentY = afterSummary + 4;
      this.addText(this.detailX + 12, punishmentY, "НАКАЗАНИЕ", ChatFormatting.GOLD, 11, this.detailWidth - 24);
      int afterPunishment = this.addWrapped(selected.punishment(), this.detailX + 12, punishmentY + 14,
         this.detailWidth - 24, ChatFormatting.YELLOW, 11, 3);
      int actionY = afterPunishment + 8;
      this.addText(this.detailX + 12, actionY, "ВАРИАНТЫ", ChatFormatting.GRAY, 11, this.detailWidth - 24);
      actionY += 17;
      for (RuleAction action : selected.actions()) {
         if (actionY + 20 > this.panelBottom - 8) break;
         String command = RuleHelperConfig.formatCause(action.commandFor(this.playerName), selected.code(), selected.title());
         command = RuleHelperConfig.applyDonationScope(command, selected.code());
         this.addRenderableWidget(Button.builder(Component.literal(action.label()).withStyle(ChatFormatting.WHITE), b -> this.execute(selected, action))
            .bounds(this.detailX + 8, actionY, this.detailWidth - 16, 20)
            .tooltip(Tooltip.create(Component.literal(command).withStyle(ChatFormatting.AQUA)))
            .build());
         actionY += 23;
      }
   }

   private void buildQuickActions() {
      int gap = 6;
      int width = Math.max(110, (this.width - 24 - gap * 2) / 3);
      int x0 = (this.width - (width * 3 + gap * 2)) / 2;
      int y1 = this.height - 77;
      int y2 = this.height - 51;
      this.quickButton("Копировать ник", x0, y1, width, () -> {
         Minecraft client = Minecraft.getInstance();
         if (client != null) {
            client.keyboardHandler.setClipboard(this.playerName);
            client.gui.hud.getChat().addClientSystemMessage(Component.literal("Скопирован ник: " + this.playerName));
            client.gui.setScreen(null);
         }
      });
      this.quickButton("/shist " + this.playerName, x0 + (width + gap) * 2, y1, width,
         () -> this.runPlayerCommand("/shist"));
      this.quickButton("/dupeip " + this.playerName, x0, y2, width,
         () -> this.runPlayerCommand("/dupeip"));
      this.quickButton("/hist " + this.playerName, x0 + width + gap, y2, width,
         () -> this.runPlayerCommand("/hist"));
      this.quickButton("Разбан / размут", x0 + (width + gap) * 2, y2, width,
         this::openReleaseMenu);

      int footerY = this.height - 22;
      this.addRenderableWidget(Button.builder(Component.literal("Изменить"), b ->
         this.minecraft.gui.setScreen(new DonationSetupScreen(this.playerName)))
         .bounds(12, footerY - 2, 78, 19).build());
      String profile = "Ваш ник: " + (RuleHelperConfig.nickname().isBlank() ? "не указан" : RuleHelperConfig.nickname())
         + "   ·   Выбранный донат: " + (RuleHelperConfig.isConfigured() ? RuleHelperConfig.tierLabel() : "не выбран");
      this.addText(98, footerY + 2, profile, ChatFormatting.GRAY, 11, Math.max(80, this.width - 208));
      if (this.returnTo != null) {
         this.quickButton("← Назад", this.width - 184, footerY - 2, 78, () -> this.minecraft.gui.setScreen(this.returnTo));
         this.quickButton("Закрыть", this.width - 96, footerY - 2, 84, this::onClose);
      } else {
         this.quickButton("Закрыть", this.width - 96, footerY - 2, 84, this::onClose);
      }
   }

   private void quickButton(String label, int x, int y, int width, Runnable action) {
      this.addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
         .bounds(x, y, width, 20).build());
   }

   private void runPlayerCommand(String command) {
      if (this.minecraft == null || this.minecraft.getConnection() == null) return;
      String full = RuleHelperConfig.applyDonationScope(command + " " + this.playerName);
      this.minecraft.getConnection().sendCommand(full.startsWith("/") ? full.substring(1) : full);
      this.minecraft.gui.setScreen(null);
   }

   private void openReleaseMenu() {
      if (this.minecraft == null) return;
      RuleEntry release = new RuleEntry("release", "Разбан и размут", "Выбери команду для игрока " + this.playerName,
         "Команда будет выполнена для выбранного ника", List.of(
            new RuleAction("/unban " + this.playerName + " nekk", "/unban {player} nekk"),
            new RuleAction("/unmute " + this.playerName + " nekk", "/unmute {player} nekk")
         ));
      this.minecraft.gui.setScreen(new ActionChoiceScreen(this.playerName, release, this.firstRow));
   }

   private void execute(RuleEntry rule, RuleAction action) {
      if (this.minecraft == null || this.minecraft.getConnection() == null) return;
      String command = RuleHelperConfig.formatCause(action.commandFor(this.playerName), rule.code(), rule.title());
      command = RuleHelperConfig.applyDonationScope(command, rule.code());
      this.minecraft.getConnection().sendCommand(command.startsWith("/") ? command.substring(1) : command);
      this.minecraft.gui.setScreen(null);
   }

   private RuleEntry selectedRule() {
      if (this.selectedRuleCode == null || this.selectedRuleCode.isBlank()) return null;
      return RuleCatalog.ALL.stream().filter(rule -> rule.code().equals(this.selectedRuleCode)).findFirst().orElse(null);
   }

   private int indexOfSelected(List<RuleEntry> rules) {
      for (int i = 0; i < rules.size(); i++) if (rules.get(i).code().equals(this.selectedRuleCode)) return i;
      return -1;
   }

   private void selectFirstVisibleRule() {
      List<RuleEntry> filtered = this.filteredRules();
      this.selectedRuleCode = filtered.isEmpty() ? "" : filtered.getFirst().code();
   }

   private List<RuleEntry> filteredRules() {
      String prefix = this.selectedGroup == 0 ? "" : this.selectedGroup + ".";
      String query = this.searchText.trim().toLowerCase(java.util.Locale.ROOT);
      return RuleCatalog.ALL.stream()
         .filter(rule -> prefix.isEmpty() || rule.code().startsWith(prefix))
         .filter(rule -> query.isEmpty() || (rule.code() + " " + rule.title() + " " + rule.summary()).toLowerCase(java.util.Locale.ROOT).contains(query))
         .toList();
   }

   private ChatFormatting codeColor(String code) {
      return switch (code.charAt(0)) {
         case '1' -> ChatFormatting.GOLD;
         case '2' -> ChatFormatting.AQUA;
         case '3' -> ChatFormatting.LIGHT_PURPLE;
         case '4' -> ChatFormatting.GREEN;
         default -> ChatFormatting.YELLOW;
      };
   }

   private void addCentered(int y, Component text, int maxWidth) {
      int width = Math.min(this.font.width(text), maxWidth);
      this.addRenderableWidget(new StringWidget(this.width / 2 - width / 2, y, width, 14, text, this.font).setMaxWidth(maxWidth));
   }

   private void addText(int x, int y, String value, ChatFormatting color, int lineHeight, int maxWidth) {
      int width = Math.min(Math.max(1, maxWidth), Math.max(1, this.font.width(value)));
      StringWidget widget = new StringWidget(x, y, width, lineHeight + 2,
         Component.literal(value).withStyle(color), this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private int addWrapped(String value, int x, int y, int maxWidth, ChatFormatting color, int lineHeight, int maxLines) {
      if (maxWidth <= 8 || value == null || value.isBlank()) return y;
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String word : value.split("\\s+")) {
         String test = line.isEmpty() ? word : line + " " + word;
         if (!line.isEmpty() && this.font.width(Component.literal(test)) > maxWidth) {
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
         } else {
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
         }
      }
      if (!line.isEmpty()) lines.add(line.toString());
      int count = Math.min(lines.size(), maxLines);
      for (int i = 0; i < count; i++) {
         String text = lines.get(i);
         if (i == maxLines - 1 && lines.size() > maxLines && text.length() > 2) text = text.substring(0, text.length() - 2) + "…";
         this.addText(x, y + i * lineHeight, text, color, lineHeight, maxWidth);
      }
      return y + count * lineHeight;
   }

   private Component tooltipFor(RuleEntry rule) {
      MutableComponent result = Component.literal(rule.summary()).withStyle(ChatFormatting.GRAY)
         .append(Component.literal("\nНаказание: ").withStyle(ChatFormatting.GOLD))
         .append(Component.literal(rule.punishment()).withStyle(ChatFormatting.YELLOW));
      for (RuleAction action : rule.actions()) {
         String command = RuleHelperConfig.formatCause(action.commandFor(this.playerName), rule.code(), rule.title());
         command = RuleHelperConfig.applyDonationScope(command, rule.code());
         result = result.append(Component.literal("\n" + action.label() + ": ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(command).withStyle(ChatFormatting.AQUA));
      }
      return result;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      boolean overList = mouseX >= this.listX && mouseX <= this.listX + this.listWidth
         && mouseY >= this.panelTop && mouseY <= this.panelBottom;
      if (!overList) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      int max = Math.max(0, this.filteredRules().size() - this.visibleRuleRows);
      int next = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0.0 ? 1 : -1)));
      if (next != this.firstRow) {
         this.firstRow = next;
         this.rebuildWidgets();
      }
      return true;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(0, 0, this.width, this.height, -653652456);
      this.panel(graphics, 12, 31, this.width - 24, 28, 0xE91A2B39, 0xFF36D7D1);
      this.panel(graphics, this.leftX, this.panelTop, this.leftWidth, this.panelBottom - this.panelTop, 0xE9161D29, 0xFF354154);
      this.panel(graphics, this.listX, this.panelTop, this.listWidth, this.panelBottom - this.panelTop, 0xE9161D29, 0xFF354154);
      this.panel(graphics, this.detailX, this.panelTop, this.detailWidth, this.panelBottom - this.panelTop, 0xE9161D29, 0xFF354154);
      for (int i = 0; i < this.visibleRules.size(); i++) {
         if (!this.visibleRules.get(i).code().equals(this.selectedRuleCode)) continue;
         int y = this.listTop + i * this.rowPitch;
         graphics.fill(this.listX + 5, y - 2, this.listX + this.listWidth - 5, y + 24, 0xFF284353);
         graphics.fill(this.listX + 5, y - 2, this.listX + 7, y + 24, 0xFF54E8E2);
         graphics.fill(this.listX + this.listWidth - 7, y - 2, this.listX + this.listWidth - 5, y + 24, 0xFF54E8E2);
      }
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }

   private void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
      if (width <= 0 || height <= 0) return;
      graphics.fill(x, y, x + width, y + height, fill);
      graphics.fill(x, y, x + width, y + 1, border);
      graphics.fill(x, y + height - 1, x + width, y + height, border);
      graphics.fill(x, y, x + 1, y + height, border);
      graphics.fill(x + width - 1, y, x + width, y + height, border);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) this.minecraft.gui.setScreen(null);
   }
}
