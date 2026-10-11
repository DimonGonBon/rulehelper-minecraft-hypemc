package ru.dmitry.rulehelper.ui;

import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7842;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.RuleHelperConfig.DonationTier;

/** First-use setup: save the user's own server nickname and declared tier. */
public final class DonationSetupScreen extends class_437 {
   private final String selectedPlayer;
   private class_342 nicknameInput;

   public DonationSetupScreen(String selectedPlayer) {
      super(class_2561.method_43470("Настройка Rule Helper"));
      this.selectedPlayer = selectedPlayer;
   }

   protected void method_25426() {
      this.addCenteredText(22, class_2561.method_43470("НАСТРОЙКА RULE HELPER").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}), Math.min(this.field_22789 - 24, 420));
      this.addCenteredText(43, class_2561.method_43470("Введите ваш ник на сервере — это нужно один раз"), Math.min(this.field_22789 - 24, 500));
      this.nicknameInput = new class_342(this.field_22793, this.field_22789 / 2 - 110, 64, 220, 20, class_2561.method_43470("Ник на сервере"));
      this.nicknameInput.method_1877(16);
      if (RuleHelperConfig.isConfigured()) {
         this.nicknameInput.method_1852(RuleHelperConfig.nickname());
      }
      this.method_37063(this.nicknameInput);
      this.addCenteredText(91, class_2561.method_43470("Ник: 3–16 символов, латинские буквы, цифры и _"), Math.min(this.field_22789 - 24, 540));
      this.addCenteredText(117, class_2561.method_43470("Выбирайте только свой уровень доната." ).method_27692(class_124.field_1054), Math.min(this.field_22789 - 24, 520));
      this.addCenteredText(132, class_2561.method_43470("Выбор чужого уровня нарушает правило 3.4 и может привести к наказанию.").method_27692(class_124.field_1054), Math.min(this.field_22789 - 24, 620));
      this.addCenteredText(165, class_2561.method_43470("Какой у вас донат?"), Math.min(this.field_22789 - 24, 320));
      int buttonWidth = Math.min(230, (this.field_22789 - 48) / 2);
      int leftX = this.field_22789 / 2 - buttonWidth - 6;
      int rightX = this.field_22789 / 2 + 6;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Правитель+"), b -> this.save(DonationTier.LEADER)).method_46434(leftX, 191, buttonWidth, 22).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Staff-"), b -> this.save(DonationTier.STAFF)).method_46434(rightX, 191, buttonWidth, 22).method_46431());
      this.addCenteredText(229, class_2561.method_43470("Выбор сохраняется на этом компьютере."), Math.min(this.field_22789 - 24, 420));
   }

   private void save(DonationTier selectedTier) {
      String nickname = this.nicknameInput.method_1882().trim();
      if (!nickname.matches("[A-Za-z0-9_]{3,16}")) {
         return;
      }
      RuleHelperConfig.save(nickname, selectedTier);
      if (this.field_22787 != null) {
         this.field_22787.method_1507(new RuleMenuScreen(this.selectedPlayer));
      }
   }

   private void addCenteredText(int y, class_2561 text, int maxWidth) {
      int textWidth = Math.min(this.field_22793.method_27525(text), maxWidth);
      class_7842 widget = new class_7842(this.field_22789 / 2 - textWidth / 2, y, textWidth, 12, text, this.field_22793);
      widget.method_73396(maxWidth);
      this.method_37063(widget);
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -653652456);
      super.method_25394(context, mouseX, mouseY, delta);
   }
}
