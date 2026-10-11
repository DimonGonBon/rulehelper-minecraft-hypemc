package ru.dmitry.rulehelper.ui;

import java.util.List;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_7842;
import net.minecraft.class_7919;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleCatalog;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.RuleHelperExtra;

public final class RuleMenuScreen extends class_437 {
   private static final int ROW_HEIGHT = 24;
   private static final int LIST_TOP = 92;
   private static final int[] GROUPS = new int[]{0, 1, 2, 3, 4};
   private static final String[] GROUP_NAMES = new String[]{"Все", "Аккаунт", "Чат", "Команда", "Игра"};
   private static final int[] GROUP_COUNTS = new int[]{15, 3, 6, 3, 3};
   private final String playerName;
   private int firstRow;
   private int selectedGroup;
   private final class_437 returnTo;

   public RuleMenuScreen(String playerName) {
      this(playerName, 0, 0, null);
   }

   public RuleMenuScreen(String playerName, int firstRow, class_437 returnTo) {
      this(playerName, firstRow, 0, returnTo);
   }

   private RuleMenuScreen(String playerName, int firstRow, int selectedGroup, class_437 returnTo) {
      super(class_2561.method_43470("Наказание игрока"));
      this.playerName = playerName;
      this.firstRow = firstRow;
      this.selectedGroup = selectedGroup;
      this.returnTo = returnTo;
   }

   protected void method_25426() {
      this.addCenteredText(13, class_2561.method_43470("НАКАЗАНИЕ ИГРОКА").method_27695(new class_124[]{class_124.field_1080, class_124.field_1067}), 170);
      class_2561 playerLabel = class_2561.method_43470("Игрок: ")
         .method_27692(class_124.field_1068)
         .method_10852(class_2561.method_43470(this.playerName).method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}));
      this.addCenteredText(27, playerLabel, Math.min(this.field_22789 - 24, 520));
      this.addCenteredText(41, class_2561.method_43470("Выбери категорию и нужный пункт").method_27692(class_124.field_1080), 290);
      int tabWidth = Math.min(88, (this.field_22789 - 32) / GROUPS.length - 4);
      int tabGap = 4;
      int tabsWidth = GROUPS.length * tabWidth + (GROUPS.length - 1) * tabGap;
      int tabsX = (this.field_22789 - tabsWidth) / 2;

      for (int i = 0; i < GROUPS.length; i++) {
         int group = GROUPS[i];
         String label = GROUP_NAMES[i] + "  " + GROUP_COUNTS[i];
         class_2561 tabText = class_2561.method_43470(label)
            .method_27695(
               new class_124[]{
                  group == this.selectedGroup ? class_124.field_1075 : class_124.field_1068,
                  group == this.selectedGroup ? class_124.field_1067 : class_124.field_1070
               }
            );
         this.method_37063(class_4185.method_46430(tabText, b -> {
            this.selectedGroup = group;
            this.firstRow = 0;
            this.method_41843();
         }).method_46434(tabsX + i * (tabWidth + tabGap), 60, tabWidth, 20).method_46431());
      }

      List<RuleEntry> rules = this.filteredRules();
      int rows = this.visibleRows();
      int maxFirst = Math.max(0, rules.size() - rows);
      this.firstRow = Math.min(this.firstRow, maxFirst);
      int buttonWidth = Math.min(470, this.field_22789 - 36);
      int x = (this.field_22789 - buttonWidth) / 2;
      int end = Math.min(rules.size(), this.firstRow + rows);

      for (int i = this.firstRow; i < end; i++) {
         RuleEntry rule = rules.get(i);

         class_124 codeColor = switch (rule.code().charAt(0)) {
            case '1' -> class_124.field_1065;
            case '2' -> class_124.field_1075;
            case '3' -> class_124.field_1076;
            default -> class_124.field_1060;
         };
         class_5250 label = class_2561.method_43470(rule.code())
            .method_27695(new class_124[]{codeColor, class_124.field_1067})
            .method_10852(class_2561.method_43470("   " + rule.title()).method_27692(class_124.field_1068));
         this.method_37063(
            class_4185.method_46430(label, button -> this.choose(rule))
               .method_46434(x, 92 + (i - this.firstRow) * 24, buttonWidth, 20)
               .method_46436(class_7919.method_47407(this.tooltipFor(rule)))
               .method_46431()
         );
      }

      int bottomY = this.field_22790 - 28;
      if (this.returnTo != null) {
         this.method_37063(
            class_4185.method_46430(class_2561.method_43470("← Назад"), b -> this.field_22787.method_1507(this.returnTo))
               .method_46434(this.field_22789 / 2 - 102, bottomY, 98, 20)
               .method_46431()
         );
         this.method_37063(
            class_4185.method_46430(class_2561.method_43470("Закрыть"), b -> this.method_25419())
               .method_46434(this.field_22789 / 2 + 4, bottomY, 98, 20)
               .method_46431()
         );
      } else {
         this.method_37063(
            class_4185.method_46430(class_2561.method_43470("Закрыть"), b -> this.method_25419())
               .method_46434(this.field_22789 / 2 - 49, bottomY, 98, 20)
               .method_46431()
         );
      }

      this.method_37063(RuleHelperExtra.makeUnpunishButton(this.playerName, super.field_22789, super.field_22790));
      this.method_37063(RuleHelperExtra.makePexButton(this.playerName, super.field_22789, super.field_22790));
      this.addLeftText(this.field_22790 - 54, class_2561.method_43470("Ваш ник: " + (RuleHelperConfig.nickname().isBlank() ? "не указан" : RuleHelperConfig.nickname())));
      this.addLeftText(this.field_22790 - 40, class_2561.method_43470("Выбранный донат: " + (RuleHelperConfig.isConfigured() ? RuleHelperConfig.tierLabel() : "не выбран")));
      this.method_37063(class_4185.method_46430(
         class_2561.method_43470("Изменить"),
         b -> this.field_22787.method_1507(new DonationSetupScreen(this.playerName))
      ).method_46434(12, this.field_22790 - 28, 82, 20).method_46431());
   }

   private void addCenteredText(int y, class_2561 text, int maxWidth) {
      int textWidth = Math.min(this.field_22793.method_27525(text), maxWidth);
      class_7842 widget = new class_7842(this.field_22789 / 2 - textWidth / 2, y, textWidth, 12, text, this.field_22793);
      this.method_37063(widget);
   }

   private void addLeftText(int y, class_2561 text) {
      int width = Math.min(this.field_22789 - 24, Math.max(160, this.field_22793.method_27525(text)));
      class_7842 widget = new class_7842(12, y, width, 12, text, this.field_22793);
      this.method_37063(widget);
   }

   private List<RuleEntry> filteredRules() {
      if (this.selectedGroup == 0) {
         return RuleCatalog.ALL;
      }

      String prefix = this.selectedGroup + ".";
      return RuleCatalog.ALL.stream().filter(rule -> rule.code().startsWith(prefix)).toList();
   }

   private int visibleRows() {
      int controlsY = Math.max(61, this.field_22790 - 82);
      int lastRowY = controlsY - 10 - 20;
      return Math.max(1, (lastRowY - LIST_TOP) / ROW_HEIGHT + 1);
   }

   private void choose(RuleEntry rule) {
      if (rule.actions().size() == 1) {
         this.execute((RuleAction)rule.actions().getFirst());
      } else {
         this.field_22787.method_1507(new ActionChoiceScreen(this.playerName, rule, this.firstRow));
      }
   }

   private void execute(RuleAction action) {
      if (this.field_22787 != null && this.field_22787.method_1562() != null) {
         String command = RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName));
         this.field_22787.method_1562().method_45730(command.startsWith("/") ? command.substring(1) : command);
         this.method_25419();
      }
   }

   private class_2561 tooltipFor(RuleEntry rule) {
      class_5250 result = class_2561.method_43470(rule.summary())
         .method_27692(class_124.field_1080)
         .method_10852(class_2561.method_43470("\nНаказание: ").method_27692(class_124.field_1065))
         .method_10852(class_2561.method_43470(rule.punishment()).method_27692(class_124.field_1054));

      for (RuleAction action : rule.actions()) {
         result = result.method_10852(class_2561.method_43470("\n" + action.label() + ": ").method_27692(class_124.field_1080))
            .method_10852(class_2561.method_43470(RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName))).method_27692(class_124.field_1075));
      }

      return result;
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int max = Math.max(0, this.filteredRules().size() - this.visibleRows());
      this.firstRow = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0.0 ? 1 : -1)));
      this.method_41843();
      return true;
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -653652456);
      context.method_25294(this.field_22789 / 2 - 146, 55, this.field_22789 / 2 + 146, 56, -13246990);
      super.method_25394(context, mouseX, mouseY, delta);
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         this.field_22787.method_1507(null);
      }
   }
}
