package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import ru.dmitry.rulehelper.PunishmentScope;
import ru.dmitry.rulehelper.PunishmentScope;

/** Compact, translucent rule picker for a selected player. */
public final class RuleMenuScreen extends Screen {
   private static final int[] GROUPS = {0, 1, 2, 3, 4, 5};
   private static final int[] GROUP_COUNTS = {26, 7, 6, 6, 4, 3};
   private static final String[] GROUP_NAMES = {"Все", "1 · Общие", "2 · Чат", "3 · Игровые", "4 · Выживание", "5 · Мини-игры"};
   private final String playerName;
   private final Screen returnTo;
   private int selectedGroup;
   private int firstRow;
   private boolean keepSelectionVisible = true;
   private String selectedRuleCode = "1.1";
   private String searchText = "";
   private int panelX, panelY, panelWidth, panelHeight;
   private int listX, listY, listWidth, detailX, detailWidth, rowPitch, visibleRuleRows;
   private EditBox searchBox;
   private List<RuleEntry> visibleRules = List.of();

   public RuleMenuScreen(String playerName) { this(playerName, 0, 0, null); }
   public RuleMenuScreen(String playerName, int firstRow, Screen returnTo) { this(playerName, firstRow, 0, returnTo); }
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
      this.addHeaderTitle(this.panelX + 16, this.panelY + 12,
         Component.literal("RULE HELPER · НАКАЗАНИЕ ИГРОКА").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelWidth - 330);
      this.addText(this.panelX + this.panelWidth - 190, this.panelY + 13, "Игрок: " + this.playerName,
         ChatFormatting.YELLOW, 12, 158);
      this.addRenderableWidget(new BlueButton(this.panelX + this.panelWidth - 320, this.panelY + 8, 122, 21,
         Component.literal("Причина · " + RuleHelperConfig.causeStyle().label()),
         b -> this.minecraft.setScreen(new CauseStyleScreen(this))));

      int groupY = this.panelY + 49;
      int gap = 4;
      int tabWidth = (this.panelWidth - 28 - gap * 5) / 6;
      for (int i = 0; i < GROUPS.length; i++) {
         final int group = GROUPS[i];
         String label = (i == 0 ? "Все" : GROUP_NAMES[i]) + " · " + GROUP_COUNTS[i];
         Component text = Component.literal(label).withStyle(this.selectedGroup == group
            ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.WHITE});
         this.addRenderableWidget(new BlueButton(this.panelX + 14 + i * (tabWidth + gap), groupY, tabWidth, 20, text, b -> {
            this.selectedGroup = group;
            this.firstRow = 0;
            this.selectFirstVisibleRule();
            this.keepSelectionVisible = true;
            this.rebuildWidgets();
         }));
      }

      this.buildRuleList();
      this.buildSelectedRulePanel();
      this.buildQuickActions();
      this.addRenderableWidget(new BlueButton(this.panelX + this.panelWidth - 28, this.panelY + 7, 20, 18,
         Component.literal("×"), b -> this.onClose()).withTooltip(Tooltip.create(Component.literal("Закрыть"))));
   }

   private void computeLayout() {
      this.panelWidth = Math.min(760, Math.max(480, this.width - 24));
      this.panelHeight = Math.min(440, Math.max(300, this.height - 24));
      this.panelWidth = Math.min(this.panelWidth, this.width - 12);
      this.panelHeight = Math.min(this.panelHeight, this.height - 12);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int innerWidth = this.panelWidth - 36;
      this.listX = this.panelX + 12;
      this.listWidth = Math.max(180, (int)(innerWidth * 0.43));
      this.detailX = this.listX + this.listWidth + 10;
      this.detailWidth = this.panelX + this.panelWidth - 12 - this.detailX;
      this.listY = this.panelY + 105;
      this.rowPitch = 25;
      this.visibleRuleRows = Math.max(1, (this.panelY + this.panelHeight - 46 - this.listY) / this.rowPitch);
   }

   private void buildRuleList() {
      this.addText(this.listX + 5, this.panelY + 75, "ПРАВИЛА", ChatFormatting.GRAY, 11, this.listWidth - 10);
      this.searchBox = new EditBox(this.font, this.listX + 2, this.panelY + 84,
         this.listWidth - 4, 18, Component.literal("Поиск правила"));
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue(this.searchText);
      this.searchBox.setResponder(value -> {
         this.searchText = value;
         this.firstRow = 0;
         this.selectFirstVisibleRule();
         this.keepSelectionVisible = true;
         this.rebuildWidgets();
         if (this.searchBox != null) {
            this.searchBox.setCursorPosition(this.searchText.length());
            this.setFocused(this.searchBox);
         }
      });
      this.addRenderableWidget(this.searchBox);

      List<RuleEntry> filtered = this.filteredRules();
      this.firstRow = Math.min(this.firstRow, Math.max(0, filtered.size() - this.visibleRuleRows));
      int selectedIndex = this.indexOfSelected(filtered);
      if (this.keepSelectionVisible && selectedIndex >= 0
         && (selectedIndex < this.firstRow || selectedIndex >= this.firstRow + this.visibleRuleRows)) {
         this.firstRow = Math.max(0, selectedIndex - this.visibleRuleRows / 2);
      }
      this.keepSelectionVisible = false;
      int end = Math.min(filtered.size(), this.firstRow + this.visibleRuleRows);
      this.visibleRules = filtered.subList(this.firstRow, end);
      for (int i = 0; i < this.visibleRules.size(); i++) {
         RuleEntry rule = this.visibleRules.get(i);
         int y = this.listY + i * this.rowPitch;
         int buttonX = this.listX + 2;
         int buttonWidth = this.listWidth - 4;
         int numberColumnWidth = Math.min(52, Math.max(38, buttonWidth / 7));
         int titleX = buttonX + numberColumnWidth;
         int titleAreaWidth = buttonWidth - numberColumnWidth - 10;
         String title = this.fitText(this.listTitle(rule), titleAreaWidth - 12);
         this.addRenderableWidget(new BlueButton(buttonX, y, buttonWidth, 22, Component.empty(), b -> {
            this.selectedRuleCode = rule.code();
            this.rebuildWidgets();
         }).withTooltip(Tooltip.create(this.tooltipFor(rule))));
         Component number = Component.literal(rule.code()).withStyle(this.codeColor(rule.code()), ChatFormatting.BOLD);
         int numberWidth = this.font.width(number);
         StringWidget numberWidget = new StringWidget(buttonX + (numberColumnWidth - numberWidth) / 2, y + 5,
            numberWidth, 12, number, this.font);
         numberWidget.setMaxWidth(numberWidth);
         this.addRenderableWidget(numberWidget);
         Component titleComponent = Component.literal(title).withStyle(rule.code().equals(this.selectedRuleCode)
            ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}
            : new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD});
         int fittedTitleWidth = this.font.width(titleComponent);
         StringWidget titleWidget = new StringWidget(titleX + Math.max(0, (titleAreaWidth - fittedTitleWidth) / 2), y + 5,
            fittedTitleWidth, 12, titleComponent, this.font);
         titleWidget.setMaxWidth(fittedTitleWidth);
         this.addRenderableWidget(titleWidget);
      }
      if (filtered.isEmpty()) this.addText(this.listX + 8, this.listY + 8, "Правила не найдены", ChatFormatting.GRAY, 12, this.listWidth - 16);
   }

   private String fitText(String value, int maxWidth) {
      if (this.font.width(Component.literal(value).withStyle(ChatFormatting.BOLD)) <= maxWidth) return value;
      String ellipsis = "…";
      int end = value.length();
      while (end > 0 && this.font.width(Component.literal(value.substring(0, end) + ellipsis)
         .withStyle(ChatFormatting.BOLD)) > maxWidth) end--;
      return end == 0 ? ellipsis : value.substring(0, end).stripTrailing() + ellipsis;
   }

   private String listTitle(RuleEntry rule) {
      return switch (rule.code()) {
         case "2.6" -> "Оскорбление проекта или оф. команды";
         default -> rule.title();
      };
   }

   private void buildSelectedRulePanel() {
      this.addText(this.detailX + 8, this.panelY + 75, "ВЫБРАННОЕ ПРАВИЛО", ChatFormatting.GRAY, 11, this.detailWidth - 16);
      RuleEntry selected = this.selectedRule();
      if (selected == null) {
         this.addWrapped("Выбери правило, чтобы увидеть описание и действия.", this.detailX + 8,
            this.panelY + 101, this.detailWidth - 16, ChatFormatting.GRAY, 12, 5);
         return;
      }
      this.addText(this.detailX + 8, this.panelY + 94, selected.code(), this.codeColor(selected.code()), 18, 38);
      int titleEnd = this.addWrapped(selected.title(), this.detailX + 50, this.panelY + 94,
         this.detailWidth - 58, ChatFormatting.WHITE, 13, 2);
      int descY = Math.max(this.panelY + 121, titleEnd + 5);
      int descEnd = this.addWrapped(selected.summary(), this.detailX + 8, descY,
         this.detailWidth - 16, ChatFormatting.GRAY, 12, 5);
      int punishmentY = descEnd + 3;
      this.addText(this.detailX + 8, punishmentY, "НАКАЗАНИЕ", ChatFormatting.GOLD, 11, this.detailWidth - 16);
      int actionY = this.addWrapped(selected.punishment(), this.detailX + 8, punishmentY + 12,
         this.detailWidth - 16, ChatFormatting.YELLOW, 11, 2) + 4;
      this.addText(this.detailX + 8, actionY, "ДЕЙСТВИЯ", ChatFormatting.AQUA, 11, this.detailWidth - 16);
      actionY += 14;
      int actionLimit = this.panelY + this.panelHeight - 42;
      int count = selected.actions().size();
      int columns = Math.min(4, count);
      int gap = 4;
      int buttonWidth = (this.detailWidth - 8 - gap * (columns - 1)) / columns;
      for (int i = 0; i < count; i++) {
         RuleAction action = selected.actions().get(i);
         int buttonY = actionY + (i / columns) * 24;
         if (buttonY + 20 > actionLimit) break;
         int buttonX = this.detailX + 4 + (i % columns) * (buttonWidth + gap);
         String command = this.commandFor(selected, action);
         this.addRenderableWidget(new BlueButton(buttonX, buttonY, buttonWidth, 20,
            Component.literal(this.compactActionLabel(selected.code(), i, action.label())),
            b -> this.selectAction(selected, action))
            .withTooltip(Tooltip.create(Component.literal(command).withStyle(ChatFormatting.AQUA))));
      }
   }

   private void buildQuickActions() {
      int gap = 6;
      int width = (this.panelWidth - 36 - gap * 2) / 3;
      int x = this.panelX + 12;
      int y = this.panelY + this.panelHeight - 31;
      this.actionButton("Узнать аккаунты игрока", "/dupeip " + this.playerName, x, y, width,
         () -> this.runPlayerCommand("/dupeip"));
      this.actionButton("Снятие наказания", "/unban или /unmute", x + width + gap, y, width,
         this::openReleaseMenu);
      this.actionButton("Узнать донат игрока", "/pex user " + this.playerName, x + (width + gap) * 2, y, width,
         () -> {
            if (this.minecraft != null && this.minecraft.getConnection() != null && RuleHelperConfig.isConfigured()) {
               this.minecraft.getConnection().sendCommand("op " + RuleHelperConfig.nickname());
               this.minecraft.getConnection().sendCommand("pex user " + this.playerName);
               this.minecraft.setScreen(null);
            }
         });
   }

   private void actionButton(String label, String command, int x, int y, int width, Runnable action) {
      this.addRenderableWidget(new BlueButton(x, y, width, 22, Component.literal(label), b -> action.run())
         .withTooltip(Tooltip.create(Component.literal(command).withStyle(ChatFormatting.AQUA))));
   }

   private String compactActionLabel(String code, int index, String original) {
      return switch (code) {
         case "1.3" -> index == 0 ? "IP-бан навсегда" : "Умышленно · IP";
         case "1.8" -> index == 0 ? "Обход IP · навсегда" : "Обход · 3 дня";
         case "2.1" -> index == 0 ? "IP-бан навсегда" : "Реклама сервера";
         case "2.3" -> switch (index) { case 0 -> "Мут 3 ч"; case 1 -> "Мут 1 д"; case 2 -> "IP-мут 7 д"; default -> "IP-мут навсегда"; };
         case "2.6" -> switch (index) { case 0 -> "Команда · 3 д"; case 1 -> "Проект · навсегда"; default -> "Повтор · снять"; };
         case "3.1" -> index == 0 ? "IP-бан навсегда" : "Повтор · снять";
         case "3.4" -> switch (index) { case 0 -> "Обычное · 1 д"; case 1 -> "Самонаказание · 6 ч"; default -> "Без док-в · 3 д"; };
         case "3.6" -> index == 0 ? "Небольшое · 12 ч" : "Большое · 3 д";
         case "4.2" -> index == 0 ? "Предупреждение" : "Повтор · 3 ч";
         case "4.3" -> index == 0 ? "Изменение · 7 д" : "Умышленная порча";
         case "4.4" -> switch (index) { case 0 -> "Обычное · 1 д"; case 1 -> "Тяжёлый случай · 3 д"; default -> "5+ раз · навсегда"; };
         default -> original;
      };
   }

   private void runPlayerCommand(String command) {
      if (this.minecraft == null || this.minecraft.getConnection() == null) return;
      String full = command + " " + this.playerName;
      this.minecraft.getConnection().sendCommand(full.startsWith("/") ? full.substring(1) : full);
      this.minecraft.setScreen(null);
   }

   private String commandFor(RuleEntry rule, RuleAction action) {
      String command = RuleHelperConfig.formatCause(action.commandFor(this.playerName), rule.code(), rule.title());
      return RuleHelperConfig.applyDonationScope(command, rule.code());
   }

   private void selectAction(RuleEntry rule, RuleAction action) {
      if (rule.code().startsWith("4.") || rule.code().startsWith("5.")) {
         this.minecraft.setScreen(new ActionChoiceScreen(this.playerName, rule, this.firstRow));
      } else {
         this.execute(rule, action);
      }
   }

   private void openReleaseMenu() {
      if (this.minecraft == null) return;
      RuleEntry release = new RuleEntry("release", "Снятие наказания", "Выбери команду для игрока " + this.playerName,
         "Разбан или размут на выбранном режиме", List.of(
            new RuleAction("Разбан", "/unban {player} nekk"),
            new RuleAction("Размут", "/unmute {player} nekk")
         ));
      this.minecraft.setScreen(new ActionChoiceScreen(this.playerName, release, this.firstRow));
   }

   private void execute(RuleEntry rule, RuleAction action) {
      if (this.minecraft == null || this.minecraft.getConnection() == null) return;
      String command = this.commandFor(rule, action);
      this.minecraft.getConnection().sendCommand(command.startsWith("/") ? command.substring(1) : command);
      this.minecraft.setScreen(null);
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

   private void addHeaderTitle(int x, int y, Component text, int maxWidth) {
      StringWidget widget = new StringWidget(x, y, Math.min(this.font.width(text), maxWidth), 16, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addText(int x, int y, String value, ChatFormatting color, int lineHeight, int maxWidth) {
      StringWidget widget = new StringWidget(x, y, Math.min(Math.max(1, maxWidth), Math.max(1, this.font.width(value))),
         lineHeight + 2, Component.literal(value).withStyle(color), this.font);
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
            lines.add(line.toString()); line.setLength(0); line.append(word);
         } else { if (!line.isEmpty()) line.append(' '); line.append(word); }
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
         String command = this.commandFor(rule, action);
         result = result.append(Component.literal("\n" + action.label() + ": ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(command).withStyle(ChatFormatting.AQUA));
      }
      return result;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      boolean overList = mouseX >= this.listX && mouseX <= this.listX + this.listWidth
         && mouseY >= this.listY && mouseY < this.panelY + this.panelHeight - 42;
      if (!overList) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      int max = Math.max(0, this.filteredRules().size() - this.visibleRuleRows);
      int next = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0.0 ? 1 : -1)));
      if (next != this.firstRow) { this.firstRow = next; this.rebuildWidgets(); }
      return true;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 2, this.panelY - 2, this.panelX + this.panelWidth + 2, this.panelY + this.panelHeight + 2, 0xE808101A);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xCF14202E);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, 0xFF39C7E8);
      graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF39C7E8);
      graphics.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF39C7E8);
      graphics.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF39C7E8);
      graphics.fill(this.panelX + 10, this.panelY + 39, this.panelX + this.panelWidth - 10, this.panelY + 40, 0xFF354D63);
      graphics.fill(this.panelX + 10, this.panelY + 73, this.panelX + this.panelWidth - 10, this.panelY + 74, 0xFF354D63);
      graphics.fill(this.detailX - 6, this.panelY + 75, this.detailX - 5, this.panelY + this.panelHeight - 40, 0xFF354D63);
      for (int i = 0; i < this.visibleRules.size(); i++) {
         if (!this.visibleRules.get(i).code().equals(this.selectedRuleCode)) continue;
         int y = this.listY + i * this.rowPitch;
         graphics.fill(this.listX, y - 1, this.listX + this.listWidth, y + 22, 0xD6285C77);
         graphics.fill(this.listX, y - 1, this.listX + 2, y + 22, 0xFF55DDF0);
         graphics.fill(this.listX + this.listWidth - 2, y - 1, this.listX + this.listWidth, y + 22, 0xFF55DDF0);
      }
      graphics.fill(this.panelX + 10, this.panelY + this.panelHeight - 37,
         this.panelX + this.panelWidth - 10, this.panelY + this.panelHeight - 36, 0xFF354154);
      super.render(graphics, mouseX, mouseY, delta);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) this.minecraft.setScreen(this.returnTo);
   }
}
