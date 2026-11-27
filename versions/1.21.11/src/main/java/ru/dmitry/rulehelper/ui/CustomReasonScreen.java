package ru.dmitry.rulehelper.ui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.ReasonTemplates;
import ru.dmitry.rulehelper.RuleCatalog;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Draft editor: changing tabs/rules preserves edits; only Save changes the active templates. */
public final class CustomReasonScreen extends Screen {
   private enum Target { GENERAL, RULE, RELEASE }
   private final Screen parent;
   private final Map<String, String> drafts = new HashMap<>();
   private Target target = Target.GENERAL;
   private String selectedRule = "2.3";
   private int firstRule;
   private int panelX, panelY, panelWidth, panelHeight, editorX, editorWidth, bodyY;
   private int listX, listY, listWidth, visibleRows;
   private int nextColor;
   private EditBox templateInput;
   private StringWidget preview, status;
   private BlueButton saveButton;

   public CustomReasonScreen(Screen parent) {
      super(Component.literal("Свои шаблоны причин"));
      this.parent = parent;
      ReasonTemplates saved = RuleHelperConfig.reasonTemplates();
      this.drafts.put("general", saved.general());
      this.drafts.put("release", saved.release());
      saved.overrides().forEach((code, value) -> this.drafts.put("rule:" + code, value));
   }

   @Override protected void init() {
      this.panelWidth = Math.min(780, this.width - 24);
      this.panelHeight = Math.min(430, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      this.bodyY = this.panelY + 72;
      this.label(this.panelX + 16, this.panelY + 12, "СВОИ ШАБЛОНЫ ПРИЧИН", this.panelWidth - 32, ChatFormatting.AQUA);
      String[] labels = {"Общий", "По правилам", "Снятие"};
      int tabWidth = (this.panelWidth - 48) / 3;
      for (int i = 0; i < labels.length; i++) {
         final Target choice = Target.values()[i];
         Component label = Component.literal((this.target == choice ? "✓ " : "") + labels[i])
            .withStyle(this.target == choice ? ChatFormatting.AQUA : ChatFormatting.WHITE);
         this.addRenderableWidget(new BlueButton(this.panelX + 16 + i * (tabWidth + 8), this.panelY + 36,
            tabWidth, 22, label, b -> { this.target = choice; this.rebuildWidgets(); }));
      }

      this.listWidth = this.target == Target.RULE ? Math.min(190, this.panelWidth / 3) : 0;
      this.editorX = this.panelX + 16 + (this.listWidth > 0 ? this.listWidth + 12 : 0);
      this.editorWidth = this.panelWidth - 32 - (this.listWidth > 0 ? this.listWidth + 12 : 0);
      if (this.target == Target.RULE) this.addRuleList();
      String heading = switch (this.target) {
         case GENERAL -> "Общий вид для правил без своих настроек";
         case RELEASE -> "Разбан / размут / снятие предупреждения";
         case RULE -> this.selectedRule + " · " + this.ruleTitle(this.selectedRule);
      };
      this.label(this.editorX, this.bodyY, heading, this.editorWidth, ChatFormatting.WHITE);
      String hint = this.target == Target.RULE
         ? (this.drafts.containsKey(this.key()) ? "Индивидуальный шаблон" : "Сейчас используется общий шаблон")
         : "Меняй текст, цвета, шрифт и символы";
      this.label(this.editorX, this.bodyY + 16, hint, this.editorWidth, ChatFormatting.GRAY);
      this.templateInput = new EditBox(this.font, this.editorX, this.bodyY + 32, this.editorWidth, 22,
         Component.literal("Шаблон причины"));
      this.templateInput.setMaxLength(ReasonTemplates.MAX_LENGTH);
      this.templateInput.setValue(this.value());
      this.templateInput.setResponder(value -> { this.drafts.put(this.key(), value); this.refreshPreview(); });
      this.addRenderableWidget(this.templateInput);

      int y = this.bodyY + 62;
      if (this.panelHeight < 360) {
         String[] names = {"Цвет", "Жирный", "Курсив", "Символ"};
         String[] values = {"", "&l", "&o", "✦"};
         int w = (this.editorWidth - 12) / 4;
         for (int i = 0; i < names.length; i++) {
            final int index = i;
            this.addRenderableWidget(new BlueButton(this.editorX + i * (w + 4), y - 6, w, 18,
               Component.literal(names[i]), b -> this.insert(index == 0
                  ? "&" + "0123456789abcdef".charAt(this.nextColor++ % 16) : values[index])));
         }
         this.templateInput.setTooltip(Tooltip.create(Component.literal("{rule} — пункт; {reason} — текст; {extra} — вариант")));
         y += 18;
      } else {
         int cols = this.editorWidth >= 420 ? 16 : 8;
         int w = Math.min(24, (this.editorWidth - (cols - 1) * 3) / cols);
         for (int i = 0; i < 16; i++) {
            final char code = "0123456789abcdef".charAt(i);
            this.addRenderableWidget(new BlueButton(this.editorX + (i % cols) * (w + 3), y + (i / cols) * 22,
               w, 19, Component.literal("■").withStyle(ChatFormatting.getByCode(code)), b -> this.insert("&" + code))
               .withTooltip(Tooltip.create(Component.literal("Вставить цвет &" + code))));
         }
         y += (16 / cols) * 22 + 3;
         String[] names = {"Жирный", "Курсив", "Подчерк.", "Сброс"};
         String[] codes = {"&l", "&o", "&n", "&r"};
         int formatWidth = (this.editorWidth - 12) / 4;
         for (int i = 0; i < names.length; i++) {
            final String code = codes[i];
            this.addRenderableWidget(new BlueButton(this.editorX + i * (formatWidth + 4), y, formatWidth, 22,
               Component.literal(names[i]), b -> this.insert(code)));
         }
         y += 27;
         String[] symbols = {"✦", "┃", "━━➤", "「 」", "【 】"};
         int symbolWidth = (this.editorWidth - 16) / 5;
         for (int i = 0; i < symbols.length; i++) {
            final String symbol = symbols[i];
            this.addRenderableWidget(new BlueButton(this.editorX + i * (symbolWidth + 4), y, symbolWidth, 22,
               Component.literal(symbol), b -> this.insert(symbol)));
         }
         y += 28;
         this.label(this.editorX, y, "{rule} — пункт · {reason} — текст причины", this.editorWidth, ChatFormatting.GRAY);
         this.label(this.editorX, y + 15, "{extra} — уточнение варианта (необязательно)", this.editorWidth, ChatFormatting.GRAY);
         y += 34;
      }
      this.preview = this.component(this.editorX, y, Component.empty(), this.editorWidth);
      this.status = this.component(this.editorX, y + 15, Component.empty(), this.editorWidth);
      int footerY = this.panelY + this.panelHeight - 28;
      int w = (this.panelWidth - 48) / 3;
      String resetLabel = this.target == Target.RULE ? "Как общий" : "По умолчанию";
      this.addRenderableWidget(new BlueButton(this.panelX + 16, footerY, w, 22, Component.literal(resetLabel), b -> {
         if (this.target == Target.RULE) this.drafts.remove(this.key());
         else this.drafts.put(this.key(), this.target == Target.RELEASE ? ReasonTemplates.DEFAULT_RELEASE : ReasonTemplates.DEFAULT_PUNISHMENT);
         this.rebuildWidgets();
      }));
      this.saveButton = this.addRenderableWidget(new BlueButton(this.panelX + 24 + w, footerY, w, 22,
         Component.literal("Сохранить"), b -> this.save()));
      this.addRenderableWidget(new BlueButton(this.panelX + 32 + w * 2, footerY, w, 22,
         Component.literal("Отмена"), b -> this.onClose()));
      this.refreshPreview();
   }

   private void addRuleList() {
      this.listX = this.panelX + 16;
      this.listY = this.bodyY + 24;
      this.visibleRows = Math.max(1, (this.panelHeight - 136) / 22);
      this.firstRule = Math.max(0, Math.min(this.firstRule, RuleCatalog.ALL.size() - this.visibleRows));
      this.label(this.listX, this.bodyY + 3, "Правила", this.listWidth - 52, ChatFormatting.AQUA);
      this.addRenderableWidget(new BlueButton(this.listX + this.listWidth - 44, this.bodyY, 20, 18,
         Component.literal("▲"), b -> this.scrollRules(-this.visibleRows)));
      this.addRenderableWidget(new BlueButton(this.listX + this.listWidth - 20, this.bodyY, 20, 18,
         Component.literal("▼"), b -> this.scrollRules(this.visibleRows)));
      for (int i = 0; i < this.visibleRows && i + this.firstRule < RuleCatalog.ALL.size(); i++) {
         var rule = RuleCatalog.ALL.get(i + this.firstRule);
         String label = rule.code() + (this.drafts.containsKey("rule:" + rule.code()) ? " ✦ " : " · ") + rule.title();
         while (this.font.width(label) > this.listWidth - 14 && label.length() > 8) label = label.substring(0, label.length() - 2) + "…";
         this.addRenderableWidget(new BlueButton(this.listX, this.listY + i * 22, this.listWidth, 20,
            Component.literal(label).withStyle(rule.code().equals(this.selectedRule) ? ChatFormatting.AQUA : ChatFormatting.WHITE),
            b -> { this.selectedRule = rule.code(); this.rebuildWidgets(); })
            .withTooltip(Tooltip.create(Component.literal(rule.code() + " · " + rule.title()))));
      }
   }
   private String key() { return this.target == Target.RULE ? "rule:" + this.selectedRule : this.target == Target.RELEASE ? "release" : "general"; }
   private String value() { return this.drafts.getOrDefault(this.key(), this.drafts.get("general")); }
   private String ruleTitle(String code) {
      return RuleCatalog.ALL.stream().filter(rule -> rule.code().equals(code)).map(rule -> rule.title()).findFirst().orElse(code);
   }
   private void insert(String text) {
      this.templateInput.insertText(text);
      this.setFocused(this.templateInput);
      this.templateInput.setFocused(true);
   }
   private String error() {
      for (var entry : this.drafts.entrySet()) {
         String error = ReasonTemplates.validationError(entry.getValue());
         if (error != null) return (entry.getKey().equals("general") ? "Общий" : entry.getKey().equals("release")
            ? "Снятие" : entry.getKey().substring(5)) + ": " + error;
      }
      return null;
   }
   private void refreshPreview() {
      if (this.preview == null || this.status == null) return;
      String error = ReasonTemplates.validationError(this.value());
      if (error == null) {
         String code = this.target == Target.RELEASE ? "3.4" : this.target == Target.RULE ? this.selectedRule : "2.3";
         String title = code.equals("3.4") ? "Некорректное наказание" : this.ruleTitle(code);
         this.preview.setMessage(ReasonComponents.fromCodes(ReasonTemplates.renderTemplate(this.value(), code, title, null)));
      } else this.preview.setMessage(Component.literal("Предпросмотр появится после исправления шаблона").withStyle(ChatFormatting.GRAY));
      String anyError = this.error();
      this.status.setMessage(Component.literal(anyError == null ? "Изменения применятся после сохранения" : anyError)
         .withStyle(anyError == null ? ChatFormatting.GRAY : ChatFormatting.RED));
      if (this.saveButton != null) this.saveButton.active = anyError == null;
   }
   private void save() {
      if (this.error() != null) { this.refreshPreview(); return; }
      ReasonTemplates templates = new ReasonTemplates();
      templates.setGeneral(this.drafts.get("general"));
      templates.setRelease(this.drafts.get("release"));
      this.drafts.forEach((key, value) -> { if (key.startsWith("rule:")) templates.setRule(key.substring(5), value); });
      RuleHelperConfig.saveReasonTemplates(templates);
      this.onClose();
   }
   private void scrollRules(int amount) {
      int next = Math.max(0, Math.min(this.firstRule + amount, Math.max(0, RuleCatalog.ALL.size() - this.visibleRows)));
      if (next != this.firstRule) { this.firstRule = next; this.rebuildWidgets(); }
   }
   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (this.target == Target.RULE && x >= this.listX && x <= this.listX + this.listWidth
         && y >= this.bodyY && y <= this.panelY + this.panelHeight - 34) {
         this.scrollRules(vertical > 0 ? -1 : vertical < 0 ? 1 : 0); return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }
   private StringWidget component(int x, int y, Component text, int width) {
      StringWidget widget = new StringWidget(x, y, width, 13, text, this.font);
      widget.setMaxWidth(width);
      return this.addRenderableWidget(widget);
   }
   private void label(int x, int y, String text, int width, ChatFormatting color) {
      this.component(x, y, Component.literal(text).withStyle(color), width);
   }
   @Override public void onClose() { this.minecraft.setScreen(this.parent); }
   @Override public void render(GuiGraphics graphics, int x, int y, float delta) {
      graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1, this.panelY + this.panelHeight + 1, 0xFF38C5D2);
      graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xF01A2432);
      super.render(graphics, x, y, delta);
   }
}
