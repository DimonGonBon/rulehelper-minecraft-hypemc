package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.HistoryRuleCatalog;
import ru.dmitry.rulehelper.PlayerNames;
import ru.dmitry.rulehelper.chat.ViolationHistorySession;

/** Separate moderator menu that summarizes the selected player's /hist output. */
public final class ViolationCountScreen extends Screen {
   private static final int LIST_TOP = 100;
   private static final int ROW_HEIGHT = 22;
   private final String playerName;
   private ViolationHistorySession session;
   private int firstRow;
   private StringWidget statusWidget;
   private StringWidget summaryWidget;
   private final List<RowWidgets> rowWidgets = new ArrayList<>();

   public ViolationCountScreen(String playerName, ViolationHistorySession session) {
      super(Component.literal("Подсчёт наказаний"));
      this.playerName = playerName;
      this.session = session;
   }

   public static void open(String playerName) {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.getConnection() == null || !PlayerNames.isValid(playerName)) return;
      ViolationHistorySession session = ViolationHistorySession.begin(playerName);
      client.setScreen(new ViolationCountScreen(playerName, session));
      client.getConnection().sendCommand("hist " + playerName + " 100000");
   }

   @Override
   protected void init() {
      this.rowWidgets.clear();
      this.addCentered(14, Component.literal("ПОДСЧЁТ НАКАЗАНИЙ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), 250);
      this.addCentered(33, Component.literal("Игрок: " + this.playerName).withStyle(ChatFormatting.WHITE), 250);
      this.statusWidget = this.addCentered(52, Component.literal(""), Math.min(800, this.width - 24));
      this.summaryWidget = this.addCentered(70, Component.literal(""), Math.min(900, this.width - 24));

      int rows = this.visibleRows();
      int end = Math.min(HistoryRuleCatalog.ALL.size(), this.firstRow + rows);
      int left = Math.max(12, this.width / 2 - Math.min(310, this.width / 2 - 20));
      int right = this.width - left;
      for (int i = this.firstRow; i < end; i++) {
         HistoryRuleCatalog.Rule rule = HistoryRuleCatalog.ALL.get(i);
         int y = LIST_TOP + (i - this.firstRow) * ROW_HEIGHT;
         ChatFormatting color = switch (rule.code().charAt(0)) {
            case '1' -> ChatFormatting.GOLD;
            case '2' -> ChatFormatting.AQUA;
            case '3' -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GREEN;
         };
         StringWidget code = new StringWidget(left, y, 42, 14,
            Component.literal(rule.code()).withStyle(color, ChatFormatting.BOLD), this.font);
         StringWidget title = new StringWidget(left + 46, y, Math.max(80, right - left - 360), 14,
            Component.literal(rule.title()).withStyle(ChatFormatting.WHITE), this.font);
         StringWidget details = new StringWidget(right - 310, y, 220, 14, Component.literal("Ожидание…").withStyle(ChatFormatting.GRAY), this.font);
         Button detailButton = Button.builder(Component.literal("Сроки"), b -> this.minecraft.setScreen(
            new ViolationRuleDetailsScreen(this.playerName, rule, this.session, this)))
            .bounds(right - 82, y - 3, 78, 20).build();
         detailButton.active = false;
         title.setMaxWidth(Math.max(80, right - left - 360));
         details.setMaxWidth(218);
         this.addRenderableWidget(code);
         this.addRenderableWidget(title);
         this.addRenderableWidget(details);
         this.addRenderableWidget(detailButton);
         this.rowWidgets.add(new RowWidgets(rule, details, detailButton));
      }

      this.addCentered(this.height - 77,
         Component.literal("#N — отметка рецидива из истории; она показана отдельно от числа записей.").withStyle(ChatFormatting.RED),
         Math.min(850, this.width - 24));
      this.addRenderableWidget(Button.builder(Component.literal("Обновить историю"), b -> this.refresh())
         .bounds(this.width / 2 - 105, this.height - 54, 112, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.back())
         .bounds(this.width / 2 + 13, this.height - 54, 86, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
         .bounds(this.width / 2 - 49, this.height - 29, 98, 20).build());
      this.updateText();
   }

   private StringWidget addCentered(int y, Component text, int maxWidth) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, 14, text, this.font);
      widget.setMaxWidth(maxWidth);
      return this.addRenderableWidget(widget);
   }

   private int visibleRows() {
      return Math.max(1, (this.height - LIST_TOP - 92) / ROW_HEIGHT);
   }

   private void updateText() {
      if (this.statusWidget == null || this.summaryWidget == null) return;
      if (this.session != null) this.session.update();
      ChatFormatting statusColor = this.session != null && this.session.state() == ViolationHistorySession.State.WAITING
         ? ChatFormatting.AQUA : ChatFormatting.GRAY;
      this.statusWidget.setMessage(Component.literal(this.statusText()).withStyle(statusColor));
      if (this.session != null && this.session.serverMessages() > 0) {
         this.summaryWidget.setMessage(Component.literal("Получено строк истории: " + this.session.serverMessages()
            + "   ·   Найдено записей по правилам: " + this.session.counter().totalRecords()).withStyle(ChatFormatting.WHITE));
      } else {
         this.summaryWidget.setMessage(Component.literal("Ожидаем строки /hist…").withStyle(ChatFormatting.GRAY));
      }
      for (RowWidgets row : this.rowWidgets) {
         int records = this.session == null ? 0 : this.session.counter().records(row.rule().code());
         int marker = this.session == null ? 0 : this.session.counter().highestMarker(row.rule().code());
         String details;
         if (this.session == null || this.session.state() == ViolationHistorySession.State.WAITING) {
            details = "Ожидание…";
         } else if (row.rule().code().equals("4.2")) {
            details = "Варны: " + this.session.counter().warnings("4.2") + "   Баны: "
               + this.session.counter().bans("4.2") + (marker > 0 ? "   #" + marker : "");
         } else if (row.rule().code().equals("3.4")) {
            details = "Обычные: " + this.session.counter().regular34Records()
               + "   Служебные: " + this.session.counter().service34Records();
         } else {
            details = "Записей: " + records + "   Отметка: " + (marker > 0 ? "#" + marker : "—");
         }
         row.details().setMessage(Component.literal(details).withStyle(records > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
         row.detailsButton().active = this.session != null && this.session.state() != ViolationHistorySession.State.WAITING && records > 0;
      }
   }

   private void refresh() {
      if (this.session != null) this.session.cancel();
      open(this.playerName);
   }

   private void back() {
      if (this.session != null) this.session.cancel();
      if (this.minecraft != null) this.minecraft.setScreen(new RuleMenuScreen(this.playerName));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int max = Math.max(0, HistoryRuleCatalog.ALL.size() - this.visibleRows());
      int next = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0.0 ? 1 : -1)));
      if (next != this.firstRow) {
         this.firstRow = next;
         this.rebuildWidgets();
      }
      return true;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      this.updateText();
      graphics.fill(0, 0, this.width, this.height, -653652456);
      super.render(graphics, mouseX, mouseY, delta);
   }

   private String statusText() {
      if (this.session == null) return "История не запущена";
      return switch (this.session.state()) {
         case WAITING -> "Запрашиваем /hist " + this.playerName + " 100000…";
         case COMPLETE -> this.session.counter().totalRecords() == 0
            ? "Ответ получен, но коды правил не распознаны"
            : "Подсчёт завершён";
         case TIMEOUT -> this.session.counter().totalRecords() == 0
            ? "История не распознана или сервер не ответил"
            : "Подсчёт завершён по полученным строкам (время ожидания истекло)";
         case CANCELLED -> "Подсчёт остановлен";
      };
   }

   @Override
   public void onClose() {
      if (this.session != null) this.session.cancel();
      if (this.minecraft != null) this.minecraft.setScreen(null);
   }

   private record RowWidgets(HistoryRuleCatalog.Rule rule, StringWidget details, Button detailsButton) {}
}
