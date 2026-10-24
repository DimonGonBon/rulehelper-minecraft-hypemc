package ru.dmitry.rulehelper.ui;

import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7842;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

/** Two-step correction flow: release the original punishment, verify it, then issue 3.4. */
public final class PunishmentCorrectionScreen extends class_437 {
   private final PunishmentAnnouncement announcement;
   private boolean released;
   private String selectedDuration;

   public PunishmentCorrectionScreen(PunishmentAnnouncement announcement) {
      super(class_2561.method_43470("Проверка наказания"));
      this.announcement = announcement;
   }

   protected void method_25426() {
      this.addCenteredText(15, class_2561.method_43470("ПРОВЕРЬ НИКИ ПЕРЕД КОМАНДОЙ").method_27695(new class_124[]{class_124.field_1054, class_124.field_1067}), 420);
      this.addCenteredText(39, class_2561.method_43470("Выдал: " + this.announcement.issuer()).method_27692(class_124.field_1075), 420);
      this.addCenteredText(55, class_2561.method_43470("Наказан: " + this.announcement.target()).method_27692(class_124.field_1065), 420);
      this.addCenteredText(71, class_2561.method_43470("В чате: " + this.announcement.kind() + " / " + this.announcement.duration()).method_27692(class_124.field_1080), 480);
      this.addCenteredText(87, class_2561.method_43470("Причина: " + this.announcement.reason()).method_27692(class_124.field_1080), Math.min(this.field_22789 - 24, 520));

      if (!RuleHelperConfig.isConfigured()) {
         this.addCenteredText(118, class_2561.method_43470("Сначала укажи ник и донат в настройке Rule Helper."), Math.min(this.field_22789 - 24, 500));
      } else if (!this.released) {
         this.addCenteredText(118, class_2561.method_43470("Сначала снимется исходный бан/мут без -g."), Math.min(this.field_22789 - 24, 500));
         if (this.announcement.issuer().equalsIgnoreCase(this.announcement.target())) {
            this.addButton("Снять и подготовить 3.4 — 6 часов", this.field_22789 / 2 - 150, 145, 300, () -> this.releaseAndPrepare("6h"));
         } else {
            this.addButton("Снять и подготовить 3.4 — 3 дня", this.field_22789 / 2 - 150, 145, 300, () -> this.releaseAndPrepare("3d"));
            this.addButton("Снять и подготовить 3.4 — 1 день", this.field_22789 / 2 - 150, 171, 300, () -> this.releaseAndPrepare("1d"));
         }
      } else {
         String releaseCommand = this.releaseCommand();
         this.addCenteredText(116, class_2561.method_43470("Команда снятия отправлена: " + releaseCommand).method_27692(class_124.field_1075), Math.min(this.field_22789 - 24, 540));
         this.addCenteredText(133, class_2561.method_43470("Проверь ответ сервера. Если снятие подтверждено — нажми ниже."), Math.min(this.field_22789 - 24, 560));
         this.addButton("Подтверждаю снятие — выдать 3.4", this.field_22789 / 2 - 150, 158, 300, this::issueRule34);
      }

      this.addButton("Отмена", this.field_22789 / 2 - 48, this.field_22790 - 34, 96, this::close);
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
      String verb = this.announcement.kind() == PunishmentAnnouncement.Kind.BAN ? "/unban " : "/unmute ";
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
