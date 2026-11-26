package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.DailyPunishmentHistory;

/** Shows this moderator's issued ban, mute and warning announcements for today. */
public final class DailyPunishmentHistoryScreen extends Screen {
   private final Screen returnTo;
   private int firstRow;
   private int panelX, panelY, panelWidth, panelHeight, listTop, visibleRows;
   private final List<Row> rows = new ArrayList<>();
   private boolean queryStarted;
   private long renderedVersion = -1;

   public DailyPunishmentHistoryScreen(Screen returnTo) {
      super(Component.literal("Наказания за день"));
      this.returnTo = returnTo;
   }

   @Override
   protected void init() {
      rows.clear();
      if (!this.queryStarted && this.minecraft != null && this.minecraft.getConnection() != null
         && !RuleHelperConfig.nickname().isBlank()) {
         this.queryStarted = true;
         DailyPunishmentHistory.beginQuery(RuleHelperConfig.nickname());
         this.minecraft.getConnection().sendCommand("shist " + RuleHelperConfig.nickname());
      }
      this.panelWidth = Math.min(790, this.width - 24);
      this.panelHeight = Math.min(470, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.listTop = this.panelY + 139;
      this.visibleRows = Math.max(1, (this.panelY + this.panelHeight - 47 - this.listTop) / 23);

      this.addText(this.panelX + 18, this.panelY + 13, "ИСТОРИЯ НАКАЗАНИЙ ЗА ДЕНЬ", ChatFormatting.AQUA, true, this.panelWidth - 36);
      this.addCentered(this.panelY + 34, Component.literal("Сегодня · " + DailyPunishmentHistory.dateLabel()
         + " · Наказания, выданные " + RuleHelperConfig.nickname()).withStyle(ChatFormatting.GRAY));
      this.addCountCard(0, "БАНЫ", DailyPunishmentHistory.count(DailyPunishmentHistory.Kind.BAN), ChatFormatting.RED);
      this.addCountCard(1, "МУТЫ", DailyPunishmentHistory.count(DailyPunishmentHistory.Kind.MUTE), ChatFormatting.AQUA);
      this.addCountCard(2, "ВАРНЫ", DailyPunishmentHistory.count(DailyPunishmentHistory.Kind.WARN), ChatFormatting.BLUE);
      this.addCountCard(3, "ВСЕГО", DailyPunishmentHistory.totalCount(), ChatFormatting.AQUA);

      this.addColumnLabel(this.panelX + 16, this.panelY + 119, "ВРЕМЯ", 58);
      this.addColumnLabel(this.panelX + 82, this.panelY + 119, "ТИП", 48);
      this.addColumnLabel(this.panelX + 139, this.panelY + 119, "ИГРОК", 118);
      this.addColumnLabel(this.panelX + 263, this.panelY + 119, "СРОК", 88);
      this.addColumnLabel(this.panelX + 356, this.panelY + 119, "ПРИЧИНА / РЕЖИМ", this.panelWidth - 372);

      List<DailyPunishmentHistory.Entry> entries = DailyPunishmentHistory.entries();
      int end = Math.min(entries.size(), this.firstRow + this.visibleRows);
      for (int index = this.firstRow; index < end; index++) {
         DailyPunishmentHistory.Entry entry = entries.get(entries.size() - 1 - index);
         int y = this.listTop + (index - this.firstRow) * 23;
         ChatFormatting color = switch (entry.kind()) {
            case BAN -> ChatFormatting.RED;
            case MUTE -> ChatFormatting.AQUA;
            case WARN -> ChatFormatting.BLUE;
         };
         String type = switch (entry.kind()) { case BAN -> "БАН"; case MUTE -> "МУТ"; case WARN -> "ВАРН"; };
         this.addRowCell(DailyPunishmentHistory.formatTime(entry), this.panelX + 16, y, 58, ChatFormatting.GRAY);
         this.addRowCell(type, this.panelX + 82, y, 48, color);
         this.addRowCell(entry.target(), this.panelX + 139, y, 118, ChatFormatting.WHITE);
         this.addRowCell(entry.duration(), this.panelX + 263, y, 88, ChatFormatting.GRAY);
         String description = entry.reason() + (entry.mode().equals("—") || entry.mode().equals("не указан") ? "" : " · " + entry.mode());
         this.addRowCell(description, this.panelX + 356, y, this.panelWidth - 372, ChatFormatting.WHITE);
         rows.add(new Row(y));
      }
      if (entries.isEmpty()) {
         String message = DailyPunishmentHistory.loading()
            ? "Запрашиваю /shist и проверяю время каждого наказания…"
            : "Сегодняшних наказаний в ответе /shist не найдено.";
         this.addCentered(this.listTop + 20, Component.literal(message).withStyle(ChatFormatting.GRAY));
      }
      this.addCentered(this.panelY + this.panelHeight - 43, Component.literal("Колёсико мыши — прокрутить список")
         .withStyle(ChatFormatting.DARK_GRAY));
      this.addRenderableWidget(new BlueButton(this.width / 2 - 162, this.panelY + this.panelHeight - 30,
         96, 21, Component.literal("Обновить"), b -> this.requestHistory()));
      this.addRenderableWidget(new BlueButton(this.width / 2 - 48, this.panelY + this.panelHeight - 30,
         96, 21, Component.literal("Назад"), b -> this.back()));
      this.addRenderableWidget(new BlueButton(this.width / 2 + 66, this.panelY + this.panelHeight - 30,
         96, 21, Component.literal("Закрыть"), b -> this.onClose()));
      this.renderedVersion = DailyPunishmentHistory.version();
   }

   private void requestHistory() {
      if (this.minecraft == null || this.minecraft.getConnection() == null || RuleHelperConfig.nickname().isBlank()) return;
      DailyPunishmentHistory.beginQuery(RuleHelperConfig.nickname());
      this.queryStarted = true;
      this.minecraft.getConnection().sendCommand("shist " + RuleHelperConfig.nickname());
      this.rebuildWidgets();
   }

   @Override
   public void tick() {
      super.tick();
      long latestVersion = DailyPunishmentHistory.version();
      if (latestVersion != this.renderedVersion) {
         this.firstRow = 0;
         this.rebuildWidgets();
      }
   }

   private void addCountCard(int index, String label, int count, ChatFormatting color) {
      int gap = 7;
      int cardWidth = (this.panelWidth - 42 - gap * 3) / 4;
      int x = this.panelX + 14 + index * (cardWidth + gap);
      int y = this.panelY + 57;
      StringWidget title = new StringWidget(x + 9, y + 7, cardWidth - 18, 12,
         Component.literal(label).withStyle(color, ChatFormatting.BOLD), this.font);
      title.setMaxWidth(cardWidth - 18);
      this.addRenderableWidget(title);
      StringWidget value = new StringWidget(x + cardWidth - 44, y + 7, 35, 12,
         Component.literal(Integer.toString(count)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), this.font);
      value.setMaxWidth(35);
      this.addRenderableWidget(value);
   }

   private void addText(int x, int y, String text, ChatFormatting color, boolean bold, int maxWidth) {
      Component component = Component.literal(text).withStyle(bold ? new ChatFormatting[]{color, ChatFormatting.BOLD} : new ChatFormatting[]{color});
      StringWidget widget = new StringWidget(x, y, Math.min(maxWidth, this.font.width(component)), 16, component, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addCentered(int y, Component component) {
      int maxWidth = this.panelWidth - 32;
      int width = Math.min(this.font.width(component), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 14, component, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addColumnLabel(int x, int y, String label, int width) {
      StringWidget widget = new StringWidget(x, y, Math.max(1, width), 12,
         Component.literal(label).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.font);
      widget.setMaxWidth(Math.max(1, width));
      this.addRenderableWidget(widget);
   }

   private void addRowCell(String text, int x, int y, int width, ChatFormatting color) {
      StringWidget widget = new StringWidget(x, y + 4, Math.max(1, width), 12,
         Component.literal(text).withStyle(color), this.font);
      widget.setMaxWidth(Math.max(1, width));
      this.addRenderableWidget(widget);
   }

   private void back() { if (this.minecraft != null) this.minecraft.setScreen(this.returnTo); }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int size = DailyPunishmentHistory.entries().size();
      int max = Math.max(0, size - this.visibleRows);
      int next = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0 ? 1 : -1)));
      if (next != this.firstRow) { this.firstRow = next; this.rebuildWidgets(); }
      return true;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 2, this.panelY - 2, this.panelX + this.panelWidth + 2, this.panelY + this.panelHeight + 2, 0xE808101A);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xD015202E);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 2, 0xFF39C7E8);
      graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF39C7E8);
      graphics.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF39C7E8);
      graphics.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF39C7E8);
      int gap = 7;
      int cardWidth = (this.panelWidth - 42 - gap * 3) / 4;
      for (int i = 0; i < 4; i++) {
         int x = this.panelX + 14 + i * (cardWidth + gap);
         int y = this.panelY + 55;
         graphics.fill(x, y, x + cardWidth, y + 34, 0xD21E3041);
         graphics.fill(x, y, x + cardWidth, y + 1, 0xFF3D90B0);
      }
      graphics.fill(this.panelX + 12, this.panelY + 111, this.panelX + this.panelWidth - 12, this.panelY + 132, 0xD91B2B3A);
      graphics.fill(this.panelX + 12, this.listTop - 2, this.panelX + this.panelWidth - 12,
         this.panelY + this.panelHeight - 43, 0x8D08111B);
      for (Row row : rows) {
         graphics.fill(this.panelX + 14, row.y, this.panelX + this.panelWidth - 14, row.y + 21, 0x80223140);
      }
      super.render(graphics, mouseX, mouseY, delta);
   }

   @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(this.returnTo); }
   private record Row(int y) {}
}
