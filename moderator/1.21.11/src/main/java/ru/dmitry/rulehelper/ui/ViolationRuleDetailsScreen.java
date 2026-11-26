package ru.dmitry.rulehelper.ui;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.HistoryRuleCatalog;
import ru.dmitry.rulehelper.ModeratorProofCommands;
import ru.dmitry.rulehelper.chat.ViolationHistoryCounter;
import ru.dmitry.rulehelper.chat.ViolationHistorySession;

/** Lists every issued history record and its actual duration for one rule. */
public final class ViolationRuleDetailsScreen extends Screen {
   private final String playerName;
   private final HistoryRuleCatalog.Rule rule;
   private final ViolationHistorySession session;
   private final Screen parent;
   private int firstEntry;
   private int visibleEntries;

   public ViolationRuleDetailsScreen(String playerName, HistoryRuleCatalog.Rule rule,
                                     ViolationHistorySession session, Screen parent) {
      super(Component.literal("Сроки наказаний"));
      this.playerName = playerName;
      this.rule = rule;
      this.session = session;
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.addCentered(12, Component.literal("СРОКИ НАКАЗАНИЙ · " + this.rule.code())
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
      this.addCentered(32, Component.literal(this.rule.title() + " · игрок: " + this.playerName));
      List<ViolationHistoryCounter.PunishmentEntry> entries = this.entries();
      int top = 62;
      int bottom = this.height - 62;
      int y = top;
      this.visibleEntries = 0;
      for (int i = this.firstEntry; i < entries.size(); i++) {
         ViolationHistoryCounter.PunishmentEntry entry = entries.get(i);
         int rowHeight = 42;
         if (y + rowHeight > bottom) break;
         String date = entry.dateLine().isBlank() ? "Дата в истории не указана" : entry.dateLine().replace('-', ' ').trim();
         String kind = entry.type() == ViolationHistoryCounter.ActionType.WARN ? "Предупреждение"
            : entry.type() == ViolationHistoryCounter.ActionType.BAN ? "Бан"
            : entry.type() == ViolationHistoryCounter.ActionType.MUTE ? "Мут" : "Наказание";
         String duration = entry.duration().isBlank() ? "Срок не распознан"
            : entry.duration().equals("Навсегда") ? "Навсегда" : ModeratorProofCommands.humanDuration(entry.duration());
         String service = entry.service34() ? " · служебный 3.4" : "";
         Component heading = Component.literal("#" + (i + 1) + " · " + kind + " · " + duration + service + " · " + date)
            .withStyle(ChatFormatting.YELLOW);
         this.addLine(y, heading, Math.min(1100, this.width - 32));
         Component raw = Component.literal(entry.message()).withStyle(ChatFormatting.WHITE);
         this.addLine(y + 17, raw, Math.min(1100, this.width - 32));
         y += rowHeight;
         this.visibleEntries++;
      }
      if (entries.isEmpty()) {
         this.addCentered(100, Component.literal("Записи по этому правилу не найдены.").withStyle(ChatFormatting.GRAY));
      }
      this.addCentered(this.height - 53,
         Component.literal("Наказаний по правилу: " + entries.size()
            + (this.session == null ? "" : " · отметка рецидива: #" + this.session.counter().highestMarker(this.rule.code())))
            .withStyle(ChatFormatting.GRAY));
      this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.returnToParent())
         .bounds(this.width / 2 - 48, this.height - 28, 96, 20).build());
   }

   private List<ViolationHistoryCounter.PunishmentEntry> entries() {
      return this.session == null ? List.of() : this.session.counter().punishmentEntries(this.rule.code());
   }

   private void addLine(int y, Component text, int maxWidth) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 14, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addCentered(int y, Component text) {
      this.addLine(y, text, Math.min(1100, this.width - 32));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int max = Math.max(0, this.entries().size() - Math.max(1, this.visibleEntries));
      int next = Math.max(0, Math.min(max, this.firstEntry + (verticalAmount < 0 ? 1 : -1)));
      if (next != this.firstEntry) {
         this.firstEntry = next;
         this.rebuildWidgets();
      }
      return true;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(0, 0, this.width, this.height, -653652456);
      super.render(graphics, mouseX, mouseY, delta);
   }

   private void returnToParent() {
      if (this.minecraft != null) this.minecraft.setScreen(this.parent);
   }

   @Override
   public void onClose() {
      this.returnToParent();
   }
}
