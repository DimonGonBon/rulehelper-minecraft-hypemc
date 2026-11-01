package ru.dmitry.rulehelper.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.StringWidget;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

/** Shows the captured announcement and fields before any correction command is sent. */
public final class PunishmentCorrectionScreen extends Screen {
   private final PunishmentAnnouncement announcement;
   private boolean released;
   private String selectedDuration;

   public PunishmentCorrectionScreen(PunishmentAnnouncement announcement) {
      super(Component.literal("Проверка наказания"));
      this.announcement = announcement;
   }

   @Override
   protected void init() {
      int maxWidth = Math.min(this.width - 28, 560);
      int y = 14;
      this.addCenteredText(y, Component.literal("ПРОВЕРЬ ДАННЫЕ ПЕРЕД КОМАНДОЙ").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}), maxWidth);
      y += 22;
      this.addCenteredText(y, Component.literal("Строка наказания:").withStyle(ChatFormatting.GRAY), maxWidth);
      y += 14;
      y = this.addWrappedText(y, this.announcement.originalLine(), maxWidth, ChatFormatting.AQUA);
      y += 3;
      Component issuer = Component.literal("Выдал наказание: ").withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.YELLOW})
         .append(Component.literal(this.announcement.issuer()).withStyle(ChatFormatting.AQUA));
      this.addCenteredText(y, issuer, maxWidth);
      y += 15;
      this.addCenteredText(y, Component.literal("Получил наказание: " + this.announcement.target()).withStyle(ChatFormatting.GOLD), maxWidth);
      y += 15;
      y = this.addField(y, "Причина:", this.announcement.cleanReason(), maxWidth);
      y += 2;
      y = this.addField(y, "Срок:", this.announcement.duration(), maxWidth);
      y += 2;
      y = this.addField(y, "Режим:", this.announcement.mode(), maxWidth);
      y += 16;

      if (!RuleHelperConfig.isConfigured()) {
         this.addCenteredText(y, Component.literal("Сначала укажи ник и донат в настройке Rule Helper."), maxWidth);
      } else if (!this.released) {
         this.addCenteredText(y, Component.literal("Выбери наказание 3.4. Исходный бан/мут снимется без -g."), maxWidth);
         y += 20;
         if (this.announcement.issuer().equalsIgnoreCase(this.announcement.target())) {
            this.addButton("Снять и подготовить 3.4 — 6 часов", this.width / 2 - 155, y, 310, () -> this.releaseAndPrepare("6h"));
         } else {
            this.addButton("Снять и подготовить 3.4 — 3 дня", this.width / 2 - 155, y, 310, () -> this.releaseAndPrepare("3d"));
            y += 25;
            this.addButton("Снять и подготовить 3.4 — 1 день", this.width / 2 - 155, y, 310, () -> this.releaseAndPrepare("1d"));
         }
      } else {
         this.addCenteredText(y, Component.literal("Отправлено: " + this.releaseCommand()).withStyle(ChatFormatting.AQUA), maxWidth);
         y += 16;
         this.addCenteredText(y, Component.literal("Проверь ответ сервера. После успешного снятия подтверди ниже."), maxWidth);
         y += 20;
         this.addButton("Подтверждаю снятие — выдать 3.4", this.width / 2 - 155, y, 310, this::issueRule34);
      }

      this.addCenteredText(this.height - 58, Component.literal("ВНИМАНИЕ: ВНИМАТЕЛЬНО ПРОВЕРЬ ВСЕ ДАННЫЕ!").withStyle(ChatFormatting.RED), maxWidth);
      this.addCenteredText(this.height - 46, Component.literal("НИКИ, ПРИЧИНУ, СРОК И РЕЖИМ ПЕРЕД ДЕЙСТВИЕМ.").withStyle(ChatFormatting.RED), maxWidth);

      this.addButton("Отмена", this.width / 2 - 48, this.height - 32, 96, this::close);
   }

   private int addField(int y, String label, String value, int maxWidth) {
      String labelPrefix = label + " ";
      List<String> lines = this.wrap(labelPrefix + value, maxWidth);
      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);
         Component text;
         if (i == 0) {
            String content = line.length() > labelPrefix.length() ? line.substring(labelPrefix.length()) : "";
            text = Component.literal(labelPrefix).withStyle(ChatFormatting.BOLD)
               .append(Component.literal(content).withStyle(ChatFormatting.AQUA));
         } else {
            text = Component.literal(line).withStyle(ChatFormatting.AQUA);
         }
         this.addCenteredText(y, text, maxWidth);
         y += 12;
      }
      return y;
   }

   private int addWrappedText(int y, String value, int maxWidth, ChatFormatting color) {
      for (String line : this.wrap(value, maxWidth)) {
         Component text = Component.literal(line).withStyle(color);
         this.addCenteredText(y, text, maxWidth);
         y += 12;
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

   private void releaseAndPrepare(String duration) {
      if (this.minecraft == null || this.minecraft.getConnection() == null || this.released) return;
      this.selectedDuration = duration;
      String command = this.releaseCommand();
      this.minecraft.getConnection().sendCommand(command.substring(1));
      this.released = true;
      this.rebuildWidgets();
   }

   private String releaseCommand() {
      String verb = this.announcement.kind() == PunishmentAnnouncement.Kind.MUTE
         ? "/unmute "
         : (this.announcement.ipTarget() ? "/unbanip " : "/unban ");
      return verb + this.announcement.target() + " nekk";
   }

   private void issueRule34() {
      if (!this.released || this.minecraft == null || this.minecraft.getConnection() == null || !RuleHelperConfig.isConfigured()) return;
      String raw = "/ban " + this.announcement.issuer() + " " + this.selectedDuration + " 3.4";
      String command = RuleHelperConfig.applyDonationScope(raw, "3.4");
      this.minecraft.getConnection().sendCommand(command.substring(1));
      this.close();
   }

   private void addButton(String label, int x, int y, int width, Runnable action) {
      this.addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
         .bounds(x, y, width, 20).build());
   }

   private void addCenteredText(int y, Component text, int maxWidth) {
      int width = Math.min(this.font.width(text), maxWidth);
      this.addRenderableWidget(new StringWidget(this.width / 2 - width / 2, y, width, 12, text, this.font));
   }

   private void close() {
      if (this.minecraft != null) this.minecraft.gui.setScreen(null);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -653652456);
      super.extractRenderState(context, mouseX, mouseY, delta);
   }
}
