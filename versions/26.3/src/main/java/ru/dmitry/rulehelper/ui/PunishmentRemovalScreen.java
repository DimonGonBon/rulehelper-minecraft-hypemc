package ru.dmitry.rulehelper.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.PunishmentRemovalCommands;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.PunishmentRemovalAnnouncement;

/** Explicit one-command confirmation for rule 3.5. Opening the screen sends nothing. */
public final class PunishmentRemovalScreen extends Screen {
   private final PunishmentRemovalAnnouncement announcement;
   private boolean sent;
   private int panelX, panelY, panelWidth, panelHeight;

   public PunishmentRemovalScreen(PunishmentRemovalAnnouncement announcement) {
      super(Component.literal("Проверка снятия наказания · 3.5"));
      this.announcement = announcement;
   }

   @Override protected void init() {
      this.panelWidth = Math.min(720, this.width - 24);
      this.panelHeight = Math.min(340, this.height - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      boolean compact = this.panelHeight < 290;
      int pitch = compact ? 14 : 18;
      int inner = this.panelWidth - 32;
      int styleWidth = Math.min(182, inner / 2);
      this.text(this.panelY + 13, Component.literal("ПРОВЕРКА СНЯТИЯ · 3.5")
         .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), inner - styleWidth - 8);
      this.addRenderableWidget(new BlueButton(this.panelX + this.panelWidth - styleWidth - 16,
         this.panelY + 9, styleWidth, 22, Component.literal("Причина · " + RuleHelperConfig.causeStyle().label()),
         b -> this.minecraft.gui.setScreen(new CauseStyleScreen(this))));
      int y = this.panelY + (compact ? 37 : 43);
      if (!compact) {
         this.text(y, Component.literal("ИСХОДНОЕ ОБЪЯВЛЕНИЕ").withStyle(ChatFormatting.GRAY), inner);
         y += 16;
      }
      this.text(y, Component.literal(this.announcement.originalLine()).withStyle(ChatFormatting.AQUA), inner);
      y += compact ? 17 : 21;
      this.field(y, "Снял наказание: ", this.announcement.issuer(), ChatFormatting.YELLOW); y += pitch;
      this.field(y, "Кому снято: ", this.announcement.target(), ChatFormatting.WHITE); y += pitch;
      this.field(y, "Действие: ", this.announcement.kind().label() + " · " + this.announcement.mode(), ChatFormatting.AQUA); y += pitch;
      this.field(y, "Причина снятия: ", this.announcement.cleanReason().isBlank()
         ? "не указана" : this.announcement.cleanReason(), ChatFormatting.AQUA); y += pitch + 6;
      this.field(y, "Будет забанен: ", this.announcement.issuer() + " · 1 день · " + (ru.dmitry.rulehelper.DonatorScopePolicy.usesGlobal("3.5", RuleHelperConfig.tier()) ? "глобально (-g)" : "текущий режим"), ChatFormatting.RED); y += pitch;
      this.text(y, Component.literal("Причина бана: ").withStyle(ChatFormatting.WHITE)
         .append(ReasonComponents.fromCodes(RuleHelperConfig.previewReason(
            "3.5", "Некорректное снятие наказаний", null, false))), inner); y += 19;
      String command = this.command();
      if (!compact && command != null) this.text(y, ReasonComponents.fromCodes(command), inner);
      int footer = this.panelY + this.panelHeight - 63;
      this.text(footer, Component.literal("Проверь обоснованность снятия перед подтверждением.")
         .withStyle(ChatFormatting.YELLOW), inner);
      this.text(footer + 14, Component.literal(RuleHelperConfig.isConfigured()
         ? "Будет отправлена только показанная команда бана по 3.5."
         : "Сначала укажи ник и группу доната в настройках RuleHelper.").withStyle(ChatFormatting.GRAY), inner);
      int buttonWidth = (inner - 8) / 2;
      BlueButton confirm = this.addRenderableWidget(new BlueButton(this.panelX + 16,
         this.panelY + this.panelHeight - 29, buttonWidth, 22,
         Component.literal("Подтвердить бан · 1 день"), b -> this.confirm())
         .withTooltip(Tooltip.create(command == null ? Component.literal("Укажи ник и группу доната в настройках") : ReasonComponents.fromCodes(command))));
      confirm.active = !this.sent && command != null && RuleHelperConfig.isConfigured();
      this.addRenderableWidget(new BlueButton(this.panelX + 24 + buttonWidth,
         this.panelY + this.panelHeight - 29, buttonWidth, 22, Component.literal("Отмена"), b -> this.onClose()));
   }

   private String command() {
      return PunishmentRemovalCommands.correction(this.announcement,
         RuleHelperConfig.causeStyle().name(), RuleHelperConfig.reasonTemplates(), RuleHelperConfig.tier());
   }
   private void confirm() {
      if (this.sent || !RuleHelperConfig.isConfigured() || this.minecraft == null || this.minecraft.getConnection() == null) return;
      String command = this.command();
      if (command == null) return;
      this.sent = true;
      this.minecraft.getConnection().sendCommand(command.substring(1));
      this.onClose();
   }
   private void field(int y, String label, String value, ChatFormatting color) {
      this.text(y, Component.literal(label).withStyle(ChatFormatting.WHITE)
         .append(Component.literal(value).withStyle(color, ChatFormatting.BOLD)), this.panelWidth - 32);
   }
   private void text(int y, Component content, int maxWidth) {
      StringWidget text = new StringWidget(this.panelX + 16, y, maxWidth, 13, content, this.font);
      text.setMaxWidth(maxWidth);
      text.setTooltip(Tooltip.create(content));
      this.addRenderableWidget(text);
   }
   @Override public void onClose() { if (this.minecraft != null) this.minecraft.gui.setScreen(null); }
   @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1,
         this.panelY + this.panelHeight + 1, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth,
         this.panelY + this.panelHeight, 0xEF172330);
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }
}
