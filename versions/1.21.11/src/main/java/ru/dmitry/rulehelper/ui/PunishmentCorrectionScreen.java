package ru.dmitry.rulehelper.ui;

// Temporary CI trigger for test artifact generation.

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7842;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

/** Shows the captured announcement and fields before any correction command is sent. */
public final class PunishmentCorrectionScreen extends class_437 {
   private final PunishmentAnnouncement announcement;
   private boolean released;
   private String selectedDuration;

   public PunishmentCorrectionScreen(PunishmentAnnouncement announcement) {
      super(class_2561.method_43470("Проверка наказания"));
      this.announcement = announcement;
   }

   protected void method_25426() {
      int maxWidth = Math.min(this.field_22789 - 28, 560);
      int y = 14;
      this.addCenteredText(y, class_2561.method_43470("ПРОВЕРЬ ДАННЫЕ ПЕРЕД КОМАНДОЙ").method_27695(new class_124[]{class_124.field_1054, class_124.field_1067}), maxWidth);
      y += 22;
      this.addCenteredText(y, class_2561.method_43470("Строка наказания:").method_27692(class_124.field_1080), maxWidth);
      y += 14;
      y = this.addWrappedText(y, this.announcement.originalLine(), maxWidth, class_124.field_1075);
      y += 3;
      class_2561 issuer = class_2561.method_43470("Выдал наказание: ").method_27695(new class_124[]{class_124.field_1067, class_124.field_1054})
         .method_10852(class_2561.method_43470(this.announcement.issuer()).method_27692(class_124.field_1075));
      this.addCenteredText(y, issuer, maxWidth);
      y += 15;
      this.addCenteredText(y, class_2561.method_43470("Получил наказание: " + this.announcement.target()).method_27692(class_124.field_1065), maxWidth);
      y += 15;
      y = this.addField(y, "Причина:", this.announcement.cleanReason(), maxWidth);
      y += 2;
      y = this.addField(y, "Срок:", this.announcement.duration(), maxWidth);
      y += 2;
      y = this.addField(y, "Режим:", this.announcement.mode(), maxWidth);
      y += 16;

      if (!RuleHelperConfig.isConfigured()) {
         this.addCenteredText(y, class_2561.method_43470("Сначала укажи ник и донат в настройке Rule Helper."), maxWidth);
      } else if (!this.released) {
         this.addCenteredText(y, class_2561.method_43470("Выбери наказание 3.4. Исходный бан/мут снимется без -g."), maxWidth);
         y += 20;
         if (this.announcement.issuer().equalsIgnoreCase(this.announcement.target())) {
            this.addButton("Снять и подготовить 3.4 — 6 часов", this.field_22789 / 2 - 155, y, 310, () -> this.releaseAndPrepare("6h"));
         } else {
            this.addButton("Снять и подготовить 3.4 — 3 дня", this.field_22789 / 2 - 155, y, 310, () -> this.releaseAndPrepare("3d"));
            y += 25;
            this.addButton("Снять и подготовить 3.4 — 1 день", this.field_22789 / 2 - 155, y, 310, () -> this.releaseAndPrepare("1d"));
         }
      } else {
         this.addCenteredText(y, class_2561.method_43470("Отправлено: " + this.releaseCommand()).method_27692(class_124.field_1075), maxWidth);
         y += 16;
         this.addCenteredText(y, class_2561.method_43470("Проверь ответ сервера. После успешного снятия подтверди ниже."), maxWidth);
         y += 20;
         this.addButton("Подтверждаю снятие — выдать 3.4", this.field_22789 / 2 - 155, y, 310, this::issueRule34);
      }

      this.addCenteredText(this.field_22790 - 58, class_2561.method_43470("ВНИМАНИЕ: ВНИМАТЕЛЬНО ПРОВЕРЬ ВСЕ ДАННЫЕ!").method_27692(class_124.field_1061), maxWidth);
      this.addCenteredText(this.field_22790 - 46, class_2561.method_43470("НИКИ, ПРИЧИНУ, СРОК И РЕЖИМ ПЕРЕД ДЕЙСТВИЕМ.").method_27692(class_124.field_1061), maxWidth);

      this.addButton("Отмена", this.field_22789 / 2 - 48, this.field_22790 - 32, 96, this::close);
   }

   private int addField(int y, String label, String value, int maxWidth) {
      String labelPrefix = label + " ";
      List<String> lines = this.wrap(labelPrefix + value, maxWidth);
      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);
         class_2561 text;
         if (i == 0) {
            String content = line.length() > labelPrefix.length() ? line.substring(labelPrefix.length()) : "";
            text = class_2561.method_43470(labelPrefix).method_27692(class_124.field_1067)
               .method_10852(class_2561.method_43470(content).method_27692(class_124.field_1075));
         } else {
            text = class_2561.method_43470(line).method_27692(class_124.field_1075);
         }
         this.addCenteredText(y, text, maxWidth);
         y += 12;
      }
      return y;
   }

   private int addWrappedText(int y, String value, int maxWidth, class_124 color) {
      for (String line : this.wrap(value, maxWidth)) {
         class_2561 text = class_2561.method_43470(line).method_27692(color);
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
         if (this.field_22793.method_27525(class_2561.method_43470(candidate)) <= maxWidth) {
            line.setLength(0);
            line.append(candidate);
            continue;
         }
         if (!line.isEmpty()) {
            lines.add(line.toString());
            line.setLength(0);
         }
         if (this.field_22793.method_27525(class_2561.method_43470(word)) <= maxWidth) {
            line.append(word);
         } else {
            StringBuilder part = new StringBuilder();
            for (int offset = 0; offset < word.length();) {
               int codePoint = word.codePointAt(offset);
               String character = new String(Character.toChars(codePoint));
               if (!part.isEmpty() && this.field_22793.method_27525(class_2561.method_43470(part + character)) > maxWidth) {
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
      if (this.field_22787 == null || this.field_22787.method_1562() == null || this.released) return;
      this.selectedDuration = duration;
      String command = this.releaseCommand();
      this.field_22787.method_1562().method_45730(command.substring(1));
      this.released = true;
      this.method_41843();
   }

   private String releaseCommand() {
      String verb = this.announcement.kind() == PunishmentAnnouncement.Kind.MUTE
         ? "/unmute "
         : (this.announcement.ipTarget() ? "/unbanip " : "/unban ");
      return verb + this.announcement.target() + " nekk";
   }

   private void issueRule34() {
      if (!this.released || this.field_22787 == null || this.field_22787.method_1562() == null || !RuleHelperConfig.isConfigured()) return;
      String raw = "/ban " + this.announcement.issuer() + " " + this.selectedDuration + " 3.4";
      String command = RuleHelperConfig.applyDonationScope(raw, "3.4");
      this.field_22787.method_1562().method_45730(command.substring(1));
      this.close();
   }

   private void addButton(String label, int x, int y, int width, Runnable action) {
      this.method_37063(class_4185.method_46430(class_2561.method_43470(label), button -> action.run())
         .method_46434(x, y, width, 20).method_46431());
   }

   private void addCenteredText(int y, class_2561 text, int maxWidth) {
      int width = Math.min(this.field_22793.method_27525(text), maxWidth);
      this.method_37063(new class_7842(this.field_22789 / 2 - width / 2, y, width, 12, text, this.field_22793));
   }

   private void close() {
      if (this.field_22787 != null) this.field_22787.method_1507(null);
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -653652456);
      super.method_25394(context, mouseX, mouseY, delta);
   }
}
