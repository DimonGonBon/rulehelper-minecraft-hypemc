package ru.dmitry.rulehelper.ui;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import ru.dmitry.rulehelper.RuleAction;
import ru.dmitry.rulehelper.RuleCatalog;
import ru.dmitry.rulehelper.RuleEntry;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.RuleHelperExtra;

public final class RuleMenuScreen extends Screen {
   private static final int ROW_HEIGHT = 24;
   private static final int LIST_TOP = 92;
   private static final int[] GROUPS = new int[]{0, 1, 2, 3, 4};
   private static final String[] GROUP_NAMES = new String[]{"Все", "Аккаунт", "Чат", "Команда", "Игра"};
   private static final int[] GROUP_COUNTS = new int[]{15, 3, 6, 3, 3};
   private final String playerName;
   private int firstRow;
   private int selectedGroup;
   private final Screen returnTo;

   public RuleMenuScreen(String playerName) {
      this(playerName, 0, 0, null);
   }

   public RuleMenuScreen(String playerName, int firstRow, Screen returnTo) {
      this(playerName, firstRow, 0, returnTo);
   }

   private RuleMenuScreen(String playerName, int firstRow, int selectedGroup, Screen returnTo) {
      super(Component.literal("Наказание игрока"));
      this.playerName = playerName;
      this.firstRow = firstRow;
      this.selectedGroup = selectedGroup;
      this.returnTo = returnTo;
   }

   @Override
   protected void init() {
      this.addCenteredText(13, Component.literal("НАКАЗАНИЕ ИГРОКА").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.BOLD}), 170);
      Component playerLabel = Component.literal("Игрок: ")
         .withStyle(ChatFormatting.WHITE)
         .append(Component.literal(this.playerName).withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}));
      this.addCenteredText(27, playerLabel, Math.min(this.width - 24, 520));
      this.addCenteredText(41, Component.literal("Выбери категорию и нужный пункт").withStyle(ChatFormatting.GRAY), 290);
      int tabWidth = Math.min(88, (this.width - 32) / GROUPS.length - 4);
      int tabGap = 4;
      int tabsWidth = GROUPS.length * tabWidth + (GROUPS.length - 1) * tabGap;
      int tabsX = (this.width - tabsWidth) / 2;

      for (int i = 0; i < GROUPS.length; i++) {
         int group = GROUPS[i];
         String label = GROUP_NAMES[i] + "  " + GROUP_COUNTS[i];
         Component tabText = Component.literal(label)
            .withStyle(
               new ChatFormatting[]{
                  group == this.selectedGroup ? ChatFormatting.AQUA : ChatFormatting.WHITE,
                  group == this.selectedGroup ? ChatFormatting.BOLD : ChatFormatting.RESET
               }
            );
         this.addRenderableWidget(Button.builder(tabText, b -> {
            this.selectedGroup = group;
            this.firstRow = 0;
            this.rebuildWidgets();
         }).bounds(tabsX + i * (tabWidth + tabGap), 60, tabWidth, 20).build());
      }

      List<RuleEntry> rules = this.filteredRules();
      int rows = this.visibleRows();
      int maxFirst = Math.max(0, rules.size() - rows);
      this.firstRow = Math.min(this.firstRow, maxFirst);
      int buttonWidth = Math.min(470, this.width - 36);
      int x = (this.width - buttonWidth) / 2;
      int end = Math.min(rules.size(), this.firstRow + rows);

      for (int i = this.firstRow; i < end; i++) {
         RuleEntry rule = rules.get(i);

         ChatFormatting codeColor = switch (rule.code().charAt(0)) {
            case '1' -> ChatFormatting.GOLD;
            case '2' -> ChatFormatting.AQUA;
            case '3' -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GREEN;
         };
         MutableComponent label = Component.literal(rule.code())
            .withStyle(new ChatFormatting[]{codeColor, ChatFormatting.BOLD})
            .append(Component.literal("   " + rule.title()).withStyle(ChatFormatting.WHITE));
         this.addRenderableWidget(
            Button.builder(label, button -> this.choose(rule))
               .bounds(x, 92 + (i - this.firstRow) * 24, buttonWidth, 20)
               .tooltip(Tooltip.create(this.tooltipFor(rule)))
               .build()
         );
      }

      int bottomY = this.height - 28;
      if (this.returnTo != null) {
         this.addRenderableWidget(
            Button.builder(Component.literal("← Назад"), b -> this.minecraft.gui.setScreen(this.returnTo))
               .bounds(this.width / 2 - 102, bottomY, 98, 20)
               .build()
         );
         this.addRenderableWidget(
            Button.builder(Component.literal("Закрыть"), b -> this.onClose())
               .bounds(this.width / 2 + 4, bottomY, 98, 20)
               .build()
         );
      } else {
         this.addRenderableWidget(
            Button.builder(Component.literal("Закрыть"), b -> this.onClose())
               .bounds(this.width / 2 - 49, bottomY, 98, 20)
               .build()
         );
      }

      this.addRenderableWidget(RuleHelperExtra.makeUnpunishButton(this.playerName, super.width, super.height));
      this.addRenderableWidget(RuleHelperExtra.makePexButton(this.playerName, super.width, super.height));
      this.addRenderableWidget(RuleHelperExtra.makePlayerCommandButton("/hist", this.playerName, super.width, super.height, 0));
      this.addRenderableWidget(RuleHelperExtra.makePlayerCommandButton("/shist", this.playerName, super.width, super.height, 1));
      this.addRenderableWidget(RuleHelperExtra.makePlayerCommandButton("/dupeip", this.playerName, super.width, super.height, 2));
      this.addLeftText(this.height - 54, Component.literal("Ваш ник: " + (RuleHelperConfig.nickname().isBlank() ? "не указан" : RuleHelperConfig.nickname())));
      this.addLeftText(this.height - 40, Component.literal("Выбранный донат: " + (RuleHelperConfig.isConfigured() ? RuleHelperConfig.tierLabel() : "не выбран")));
      this.addRenderableWidget(Button.builder(
         Component.literal("Изменить"),
         b -> this.minecraft.gui.setScreen(new DonationSetupScreen(this.playerName))
      ).bounds(12, this.height - 28, 82, 20).build());
   }

   private void addCenteredText(int y, Component text, int maxWidth) {
      int textWidth = Math.min(this.font.width(text), maxWidth);
      StringWidget widget = new StringWidget(this.width / 2 - textWidth / 2, y, textWidth, 12, text, this.font);
      widget.setMaxWidth(maxWidth);
      this.addRenderableWidget(widget);
   }

   private void addLeftText(int y, Component text) {
      int width = Math.min(this.width - 24, Math.max(160, this.font.width(text)));
      StringWidget widget = new StringWidget(12, y, width, 12, text, this.font);
      widget.setMaxWidth(width);
      this.addRenderableWidget(widget);
   }

   private List<RuleEntry> filteredRules() {
      if (this.selectedGroup == 0) {
         return RuleCatalog.ALL;
      }

      String prefix = this.selectedGroup + ".";
      return RuleCatalog.ALL.stream().filter(rule -> rule.code().startsWith(prefix)).toList();
   }

   private int visibleRows() {
      int controlsY = Math.max(61, this.height - 82);
      int lastRowY = controlsY - 10 - 20;
      return Math.max(1, (lastRowY - LIST_TOP) / ROW_HEIGHT + 1);
   }

   private void choose(RuleEntry rule) {
      if (rule.actions().size() == 1) {
         this.execute(rule, (RuleAction)rule.actions().getFirst());
      } else {
         this.minecraft.gui.setScreen(new ActionChoiceScreen(this.playerName, rule, this.firstRow));
      }
   }

   private void execute(RuleEntry rule, RuleAction action) {
      if (this.minecraft != null && this.minecraft.getConnection() != null) {
         String command = RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName), rule.code());
         this.minecraft.getConnection().sendCommand(command.startsWith("/") ? command.substring(1) : command);
         this.onClose();
      }
   }

   private Component tooltipFor(RuleEntry rule) {
      MutableComponent result = Component.literal(rule.summary())
         .withStyle(ChatFormatting.GRAY)
         .append(Component.literal("\nНаказание: ").withStyle(ChatFormatting.GOLD))
         .append(Component.literal(rule.punishment()).withStyle(ChatFormatting.YELLOW));

      for (RuleAction action : rule.actions()) {
         result = result.append(Component.literal("\n" + action.label() + ": ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(RuleHelperConfig.applyDonationScope(action.commandFor(this.playerName), rule.code())).withStyle(ChatFormatting.AQUA));
      }

      return result;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int max = Math.max(0, this.filteredRules().size() - this.visibleRows());
      this.firstRow = Math.max(0, Math.min(max, this.firstRow + (verticalAmount < 0.0 ? 1 : -1)));
      this.rebuildWidgets();
      return true;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -653652456);
      context.fill(this.width / 2 - 146, 55, this.width / 2 + 146, 56, -13246990);
      super.extractRenderState(context, mouseX, mouseY, delta);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(null);
      }
   }
}
