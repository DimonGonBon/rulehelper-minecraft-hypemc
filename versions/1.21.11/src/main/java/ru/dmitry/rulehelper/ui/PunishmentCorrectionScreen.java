package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.PunishmentScope;
import ru.dmitry.rulehelper.DonatorScopePolicy;
import ru.dmitry.rulehelper.PunishmentCorrectionCommands;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

/** Shows the captured announcement and fields before any correction command is sent. */
public final class PunishmentCorrectionScreen extends Screen {
   private final PunishmentAnnouncement announcement;
   private boolean correctionPrepared;
   private String selectedDuration;
   private String sentReleaseCommand;
   private int panelX, panelY, panelWidth, panelHeight;
   private int detailsBottom;

   public PunishmentCorrectionScreen(PunishmentAnnouncement announcement) {
      super(Component.literal("Проверка наказания"));
      this.announcement = announcement;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(820, this.width - 24);
      this.panelHeight = Math.min(500, this.height - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int contentX = this.panelX + 18;
      int contentWidth = this.panelWidth - 36;

      int styleWidth = Math.min(190, contentWidth / 2);
      this.addText(contentX, this.panelY + 14, Component.literal("ПРОВЕРКА НАКАЗАНИЯ")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), contentWidth - styleWidth - 12, 16);
      this.addRenderableWidget(new BlueButton(this.panelX + this.panelWidth - styleWidth - 18,
         this.panelY + 9, styleWidth, 22,
         Component.literal("Причина · " + RuleHelperConfig.causeStyle().label()),
         b -> this.minecraft.setScreen(new CauseStyleScreen(this))));
      this.addCenteredText(this.panelY + 35, Component.literal("Сверь объявление и данные перед снятием или выдачей наказания")
         .withStyle(ChatFormatting.GRAY), contentWidth, 14);

      int y = this.panelY + 62;
      this.addText(contentX + 12, y, Component.literal("ИСХОДНОЕ ОБЪЯВЛЕНИЕ")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), contentWidth - 24, 13);
      y += 18;
      y = this.addWrappedText(y, this.announcement.originalLine(), contentX + 12,
         contentWidth - 24, ChatFormatting.AQUA, 15);
      y += 5;

      Component issuer = Component.literal("Выдал наказание: ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
         .append(Component.literal(this.announcement.issuer()).withStyle(ChatFormatting.AQUA));
      this.addCenteredText(y, issuer, contentWidth, 14);
      y += 16;
      this.addCenteredText(y, Component.literal("Получил наказание: ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
         .append(Component.literal(this.announcement.target()).withStyle(ChatFormatting.WHITE)), contentWidth, 14);
      y += 17;
      y = this.addField(y, "Причина", this.announcement.cleanReason(), contentWidth);
      y = this.addField(y, "Срок", this.announcement.duration(), contentWidth);
      y = this.addField(y, "Режим", this.announcement.mode(), contentWidth);
      this.detailsBottom = y + 7;

      int actionY = this.detailsBottom + 11;
      this.addCenteredText(actionY, Component.literal("ДЕЙСТВИЕ · " + this.announcement.kind().label())
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), contentWidth, 13);
      actionY += 19;
      if (!RuleHelperConfig.isConfigured()) {
         this.addCenteredText(actionY, Component.literal("Сначала укажи ник и группу доната в настройках RuleHelper.")
            .withStyle(ChatFormatting.RED), contentWidth, 14);
      } else if (!this.correctionPrepared) {
         if (this.mayCorrectWithoutRelease()) {
            String explanation = this.announcement.kind() == PunishmentAnnouncement.Kind.KICK
               ? "Кик уже завершён. Подготовь 3.4 выдавшему игроку."
               : "Режим варна не указан. Подготовь 3.4; само предупреждение сними отдельно.";
            this.addCenteredText(actionY, Component.literal(explanation)
               .withStyle(ChatFormatting.YELLOW), contentWidth, 14);
            actionY = this.addCorrectionPreview(actionY + 18, contentWidth);
            this.addDurationButtons(actionY + 3, contentWidth, false);
         } else {
            PunishmentScope detectedScope = this.detectedScope();
            if (detectedScope == null) {
               this.addCenteredText(actionY, Component.literal("Не удалось определить режим наказания из объявления.")
                  .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), contentWidth, 14);
               actionY += 17;
               this.addCenteredText(actionY, Component.literal("Автоматическое снятие отключено: режим должен быть известен.")
                  .withStyle(ChatFormatting.GRAY), contentWidth, 14);
            } else {
               String selector = switch (detectedScope) {
                  case GLOBAL -> "-g";
                  case SURVIVAL -> "server:выживание";
                  case GRIEF -> "server:гриферский";
                  case MINIGAMES -> "server:мини-игры";
                  default -> "текущий режим";
               };
               this.addCenteredText(actionY, Component.literal("Режим снятия определён автоматически: " + detectedScope.label()
                  + " · " + selector).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), contentWidth, 14);
               actionY = this.addCorrectionPreview(actionY + 16, contentWidth);
               String releaseLabel = this.announcement.kind() == PunishmentAnnouncement.Kind.WARN ? "Снятие варна: " : "Снятие: ";
               this.addCenteredText(actionY, Component.literal(releaseLabel).withStyle(ChatFormatting.GRAY)
                  .append(ReasonComponents.fromCodes(RuleHelperConfig.formatReleaseCause())), contentWidth, 14);
               this.addDurationButtons(actionY + 18, contentWidth, true);
            }
         }
      } else {
         boolean sentRelease = this.sentReleaseCommand != null;
         this.addCenteredText(actionY, Component.literal(sentRelease
            ? "Команда снятия отправлена:" : "Подготовлена команда 3.4 выдавшему наказание:")
            .withStyle(ChatFormatting.GRAY), contentWidth, 13);
         actionY += 15;
         this.addCenteredText(actionY, ReasonComponents.fromCodes(sentRelease
            ? this.sentReleaseCommand : this.correctionCommand()), contentWidth, 14);
         actionY += 18;
         this.addCenteredText(actionY, Component.literal(sentRelease
            ? "Проверь ответ сервера. Подтверди снятие перед выдачей 3.4."
            : "Проверь ник, причину и срок. Команда ещё не отправлена.")
            .withStyle(ChatFormatting.YELLOW), contentWidth, 14);
         actionY += 19;
         int buttonWidth = Math.min(410, contentWidth - 16);
         this.addButton(sentRelease ? "Подтверждаю снятие · выдать 3.4" : "Подтверждаю · выдать 3.4",
            this.width / 2 - buttonWidth / 2, actionY, buttonWidth, this::issueRule34);
      }

      int footerY = this.panelY + this.panelHeight - 63;
      this.addCenteredText(footerY, Component.literal("Проверь ник, причину, срок и режим перед действием.")
         .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), contentWidth, 14);
      this.addCenteredText(footerY + 15, Component.literal(this.mayCorrectWithoutRelease()
         ? "3.4 выдаётся игроку из поля «Выдал наказание»."
         : "Снятие и выдача наказания выполняются отдельными шагами.")
         .withStyle(ChatFormatting.GRAY), contentWidth, 13);
      this.addRenderableWidget(new BlueButton(this.width / 2 - 54, this.panelY + this.panelHeight - 31,
         108, 22, Component.literal("Отмена"), b -> this.close()));
   }

   private int addCorrectionPreview(int y, int contentWidth) {
      this.addCenteredText(y, Component.literal("Выдача: ").withStyle(ChatFormatting.GRAY)
         .append(ReasonComponents.fromCodes(RuleHelperConfig.previewReason(
            "3.4", "Некорректное наказание", null, false)))
         .append(Component.literal(DonatorScopePolicy.usesGlobal("3.4", RuleHelperConfig.tier())
            ? " · глобально (-g)" : " · текущий режим").withStyle(ChatFormatting.YELLOW)), contentWidth, 14);
      return y + 16;
   }

   private void addDurationButtons(int y, int contentWidth, boolean releaseFirst) {
      int buttonWidth = Math.min(410, contentWidth - 16);
      int x = this.width / 2 - buttonWidth / 2;
      String action = releaseFirst
         ? (this.announcement.kind() == PunishmentAnnouncement.Kind.WARN ? "Снять варн и подготовить 3.4" : "Снять и подготовить 3.4")
         : "Подготовить 3.4";
      if (this.announcement.issuer().equalsIgnoreCase(this.announcement.target())) {
         this.addButton(action + " · 6 часов", x, y, buttonWidth, () -> this.prepareCorrection("6h", releaseFirst));
      } else {
         this.addButton(action + " · 3 дня", x, y, buttonWidth, () -> this.prepareCorrection("3d", releaseFirst));
         this.addButton(action + " · 1 день", x, y + 27, buttonWidth, () -> this.prepareCorrection("1d", releaseFirst));
      }
   }

   private int addField(int y, String label, String value, int maxWidth) {
      if (value == null || value.isBlank()) value = "не указана";
      Component text = Component.literal(label + ": ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
         .append(Component.literal(value).withStyle(ChatFormatting.AQUA));
      return this.addWrappedComponent(y, text, this.panelX + 18, maxWidth, 14);
   }

   private int addWrappedText(int y, String value, int x, int maxWidth, ChatFormatting color, int lineHeight) {
      for (String line : this.wrap(value, maxWidth)) {
         this.addText(x, y, Component.literal(line).withStyle(color), maxWidth, lineHeight);
         y += lineHeight;
      }
      return y;
   }

   private int addWrappedComponent(int y, Component text, int x, int maxWidth, int lineHeight) {
      String plain = text.getString();
      List<String> lines = this.wrap(plain, maxWidth);
      for (int i = 0; i < lines.size(); i++) {
         Component line = Component.literal(lines.get(i)).withStyle(ChatFormatting.AQUA);
         if (i == 0) {
            int colon = lines.get(i).indexOf(':');
            if (colon >= 0) line = Component.literal(lines.get(i).substring(0, colon + 1) + " ")
               .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
               .append(Component.literal(lines.get(i).substring(colon + 1).stripLeading()).withStyle(ChatFormatting.AQUA));
         }
         this.addText(x, y, line, maxWidth, lineHeight);
         y += lineHeight;
      }
      return y;
   }

   private List<String> wrap(String value, int maxWidth) {
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String word : value.split("\\s+")) {
         String candidate = line.isEmpty() ? word : line + " " + word;
         if (this.font.width(Component.literal(candidate)) <= maxWidth) {
            line.setLength(0);
            line.append(candidate);
            continue;
         }
         if (!line.isEmpty()) {
            lines.add(line.toString());
            line.setLength(0);
         }
         if (this.font.width(Component.literal(word)) <= maxWidth) {
            line.append(word);
         } else {
            StringBuilder part = new StringBuilder();
            for (int offset = 0; offset < word.length();) {
               int codePoint = word.codePointAt(offset);
               String character = new String(Character.toChars(codePoint));
               if (!part.isEmpty() && this.font.width(Component.literal(part + character)) > maxWidth) {
                  lines.add(part.toString());
                  part.setLength(0);
               }
               part.append(character);
               offset += Character.charCount(codePoint);
            }
            line.append(part);
         }
      }
      if (!line.isEmpty()) lines.add(line.toString());
      return lines.isEmpty() ? List.of("") : lines;
   }

   private boolean mayCorrectWithoutRelease() {
      return PunishmentCorrectionCommands.mayCorrectWithoutRelease(this.announcement);
   }

   private void prepareCorrection(String duration, boolean releaseFirst) {
      if (this.minecraft == null || this.correctionPrepared || !RuleHelperConfig.isConfigured()) return;
      if (PunishmentCorrectionCommands.correction(this.announcement, duration, RuleHelperConfig.causeStyle().name(), RuleHelperConfig.reasonTemplates(), RuleHelperConfig.tier()) == null) return;
      if (releaseFirst) {
         if (this.minecraft.getConnection() == null) return;
         String command = PunishmentCorrectionCommands.release(this.announcement, RuleHelperConfig.causeStyle().name(), RuleHelperConfig.reasonTemplates());
         if (command == null) return;
         this.minecraft.getConnection().sendCommand(command.substring(1));
         this.sentReleaseCommand = command;
      } else if (!this.mayCorrectWithoutRelease()) {
         return;
      }
      this.selectedDuration = duration;
      this.correctionPrepared = true;
      this.rebuildWidgets();
   }

   private PunishmentScope detectedScope() {
      return PunishmentScope.fromAnnouncementMode(this.announcement.mode());
   }

   private String correctionCommand() {
      return PunishmentCorrectionCommands.correction(this.announcement, this.selectedDuration, RuleHelperConfig.causeStyle().name(), RuleHelperConfig.reasonTemplates(), RuleHelperConfig.tier());
   }

   private void issueRule34() {
      if (!this.correctionPrepared || this.minecraft == null || this.minecraft.getConnection() == null || !RuleHelperConfig.isConfigured()) return;
      String command = this.correctionCommand();
      if (command == null) return;
      this.minecraft.getConnection().sendCommand(command.substring(1));
      this.close();
   }

   private void addButton(String label, int x, int y, int width, Runnable action) {
      this.addRenderableWidget(new BlueButton(x, y, width, 23,
         Component.literal(label).withStyle(ChatFormatting.WHITE), b -> action.run()));
   }

   private void addCenteredText(int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - width / 2, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addText(int x, int y, Component text, int maxWidth, int height) {
      int width = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(x, y, width, height, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void close() { if (this.minecraft != null) this.minecraft.setScreen(null); }

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
      graphics.fill(this.panelX + 10, this.panelY + 56, this.panelX + this.panelWidth - 10,
         this.detailsBottom, 0x9C223447);
      graphics.fill(this.panelX + 10, this.panelY + 56, this.panelX + 12,
         this.detailsBottom, 0xFF38C5D2);
      super.render(graphics, mouseX, mouseY, delta);
   }
}
