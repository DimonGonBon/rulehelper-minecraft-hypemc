package ru.dmitry.rulehelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Saved reason-only templates, independent from command targets, durations and modes. */
public final class ReasonTemplates {
   public static final int MAX_LENGTH = 256;
   public static final String DEFAULT_PUNISHMENT = "&f&l&o{rule} &b&l&o━━➤ 「 {reason} 」&r";
   public static final String DEFAULT_RELEASE = "&f&l&o{rule} &a&l&o━━➤ 「 {reason} 」&r";
   private static final String PREFIX = "reason_template.";
   private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_]+)\\}");
   private static final Pattern FLAGS = Pattern.compile("(?i)(?:^|\\s)(?:-g|server:\\S*)(?=\\s|$)");
   private String general = DEFAULT_PUNISHMENT;
   private String release = DEFAULT_RELEASE;
   private final Map<String, String> rules = new HashMap<>();

   public String general() { return this.general; }
   public String release() { return this.release; }
   public Map<String, String> overrides() { return Map.copyOf(this.rules); }
   public String forRule(String code) { return this.rules.getOrDefault(code, this.general); }
   public boolean hasOverride(String code) { return this.rules.containsKey(code); }
   public void setGeneral(String template) { check(template); this.general = template.strip(); }
   public void setRelease(String template) { check(template); this.release = template.strip(); }
   public void setRule(String code, String template) {
      if (code == null || !code.matches("[0-9]+\\.[0-9]+")) throw new IllegalArgumentException("Неверный пункт правила");
      if (template == null) { this.rules.remove(code); return; }
      check(template);
      this.rules.put(code, template.strip());
   }
   public ReasonTemplates copy() {
      ReasonTemplates copy = new ReasonTemplates();
      copy.general = this.general;
      copy.release = this.release;
      copy.rules.putAll(this.rules);
      return copy;
   }

   public static String validationError(String template) {
      if (template == null || template.isBlank()) return "Шаблон не должен быть пустым";
      if (template.length() > MAX_LENGTH) return "Максимум " + MAX_LENGTH + " символов";
      if (template.indexOf('\n') >= 0 || template.indexOf('\r') >= 0 || template.indexOf('\t') >= 0)
         return "Причина должна быть одной строкой";
      if (!template.contains("{rule}")) return "Добавь {rule}, чтобы сохранить номер выбранного пункта";
      Matcher matcher = PLACEHOLDER.matcher(template);
      while (matcher.find()) {
         String name = matcher.group(1);
         if (!(name.equals("rule") || name.equals("reason") || name.equals("extra")))
            return "Неизвестная подстановка: {" + name + "}";
      }
      if (FLAGS.matcher(template).find()) return "Режим и -g задаются действием, убери их из шаблона";
      return null;
   }
   private static void check(String template) {
      String error = validationError(template);
      if (error != null) throw new IllegalArgumentException(error);
   }

   public String render(String code, String title, String extra, boolean removing) {
      return renderTemplate(removing ? this.release : this.forRule(code), code, title, extra);
   }
   public static String renderTemplate(String template, String code, String title, String extra) {
      check(template);
      String detail = extra == null ? "" : extra.strip();
      String caption = title == null ? "" : title.strip();
      boolean explicitExtra = template.contains("{extra}");
      boolean hasReason = template.contains("{reason}");
      if (!detail.isEmpty() && !explicitExtra && hasReason) caption += " · " + detail;
      String text = template.replace("{rule}", code).replace("{reason}", caption).replace("{extra}", detail).strip();
      // Action qualifiers such as Родня/Смени ник remain present in number-only templates.
      if (!detail.isEmpty() && !explicitExtra && !hasReason) text += " · " + detail;
      if (Pattern.compile("(?i)[&§][0-9A-FK-OR]").matcher(text).find()
         && !text.toLowerCase(java.util.Locale.ROOT).endsWith("&r")
         && !text.toLowerCase(java.util.Locale.ROOT).endsWith("§r")) text += "&r";
      return text;
   }

   public void writeTo(Properties properties) {
      properties.keySet().removeIf(key -> key.toString().startsWith(PREFIX));
      properties.setProperty(PREFIX + "general", this.general);
      properties.setProperty(PREFIX + "release", this.release);
      this.rules.forEach((code, template) -> properties.setProperty(PREFIX + "rule." + code, template));
   }
   public static ReasonTemplates readFrom(Properties properties) {
      ReasonTemplates result = new ReasonTemplates();
      String general = properties.getProperty(PREFIX + "general");
      String release = properties.getProperty(PREFIX + "release");
      if (general != null && validationError(general) == null) result.setGeneral(general);
      if (release != null && validationError(release) == null) result.setRelease(release);
      String rulePrefix = PREFIX + "rule.";
      for (String key : properties.stringPropertyNames()) {
         if (!key.startsWith(rulePrefix)) continue;
         String code = key.substring(rulePrefix.length());
         String value = properties.getProperty(key);
         if (code.matches("[0-9]+\\.[0-9]+") && validationError(value) == null) result.setRule(code, value);
      }
      return result;
   }
}
