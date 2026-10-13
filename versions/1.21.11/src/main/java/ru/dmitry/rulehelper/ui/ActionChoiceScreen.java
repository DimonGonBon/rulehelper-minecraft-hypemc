package ru.dmitry.rulehelper.ui;

import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7842;
import net.minecraft.class_7919;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;

public final class ActionChoiceScreen extends class_437 {
   private final String playerName;
   private final RuleEntry rule;
   private final int previousScroll;

   public ActionChoiceScreen(String playerName, RuleEntry rule, int previousScroll) {
      super(class_2561.method_43470(rule.code() + " | " + rule.title()));
      this.playerName = playerName;
      this.rule = rule;
      this.previousScroll = previousScroll;
   }

   protected void method_25426() {
      class_2561 playerLabel = class_2561.method_43470("Игрок: ")
         .method_27692(class_124.field_1068)
         .method_10852(class_2561.method_43470(this.playerName).method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}));
      this.addCenteredText(18, playerLabel, Math.min(this.field_22789 - 24, 520));
      this.addCenteredText(
         34,
         class_2561.method_43470(this.rule.code() + "  |  " + this.rule.title()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}),
         Math.min(this.field_22789 - 24, 520)
      );
      int buttonWidth = Math.min(500, this.field_22789 - 36);
      int x = (this.field_22789 - buttonWidth) / 2;
      int top = Math.max(70, this.field_22790 / 2 - this.rule.actions().size() * 15);

      for (int i = 0; i < this.rule.actions().size(); i++) {
         RuleAction action = this.rule.actions().get(i);
         class_2561 tip = class_2561.method_43470(this.rule.summary())
            .method_27692(class_124.field_1080)
            .method_10852(class_2561.method_43470("\nНаказание: ").method_27692(class_124.field_1065))
            .method_10852(class_2561.method_43470(action.label()).method_27692(class_124.field_1054))
            .method_10852(class_2561.method_43470("\n" + RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName), this.rule.code())).method_27692(class_124.field_1075));
         this.method_37063(
            class_4185.method_46430(class_2561.method_43470(action.label()), b -> this.execute(action))
               .method_46434(x, top + i * 25, buttonWidth, 20)
               .method_46436(class_7919.method_47407(tip))
               .method_46431()
         );
      }

      this.method_37063(
         class_4185.method_46430(
               class_2561.method_43470("← Назад к правилам"), b -> this.field_22787.method_1507(new RuleMenuScreen(this.playerName, this.previousScroll, this))
            )
            .method_46434(this.field_22789 / 2 - 78, this.field_22790 - 34, 156, 20)
            .method_46431()
      );
   }

   private void addCenteredText(int y, class_2561 text, int maxWidth) {
      int textWidth = Math.min(this.field_22793.method_27525(text), maxWidth);
      this.method_37063(new class_7842(this.field_22789 / 2 - textWidth / 2, y, textWidth, 12, text, this.field_22793).method_73396(maxWidth));
   }

   private void execute(RuleAction action) {
      if (this.field_22787 != null && this.field_22787.method_1562() != null) {
         String command = RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName), this.rule.code());
         this.field_22787.method_1562().method_45730(command.startsWith("/") ? command.substring(1) : command);
         this.method_25419();
      }
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -385217000);
      context.method_25294(this.field_22789 / 2 - 120, 61, this.field_22789 / 2 + 120, 62, -13181454);
      super.method_25394(context, mouseX, mouseY, delta);
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         this.field_22787.method_1507(null);
      }
   }
}
