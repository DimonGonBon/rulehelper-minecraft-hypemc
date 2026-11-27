package ru.dmitry.rulehelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

public final class CustomReasonTest {
   public static void main(String[] args) throws Exception {
      ReasonTemplates templates = new ReasonTemplates();
      templates.setGeneral("&f{rule} &b✦ {reason}");
      templates.setRelease("&f{rule} &a【 {reason} 】");
      templates.setRule("2.3", "&f{rule} &d&l&o┃ {reason}");
      equal(CauseFormatter.formatCommand("/mute Player123 3h 2.3 -g", "2.3", "Оскорбительное поведение", "CUSTOM", templates),
         "/mute Player123 3h &f2.3 &d&l&o┃ Оскорбительное поведение&r -g", "per-rule override and intact command arguments");
      equal(CauseFormatter.reason("2.2", "Засорение чата", null, false, "CUSTOM", templates),
         "&f2.2 &b✦ Засорение чата&r", "unconfigured rules inherit general format");
      equal(CauseFormatter.releaseReason("CUSTOM", templates),
         "&f3.4 &a【 Некорректное наказание 】&r", "separate release format");
      templates.setRule("3.4", "{rule} ⌁ {reason}");
      var warn = PunishmentAnnouncement.parse("❨！❩ Игрок Moderator выдал предупреждение Player123 по причине: 4.2 (гриферский)");
      equal(PunishmentCorrectionCommands.release(warn, "CUSTOM", templates),
         "/unwarn Player123 &f3.4 &a【 Некорректное наказание 】&r server:гриферский", "custom removal reason preserves warning mode");
      equal(PunishmentCorrectionCommands.correction(warn, "1d", "CUSTOM", templates, RuleHelperConfig.DonationTier.LEADER),
         "/ban Moderator 1d 3.4 ⌁ Некорректное наказание -g", "per-rule 3.4 override also applies to correction screen");
      equal(CauseFormatter.reason("2.3", "Оскорбление", "Родня", false, "CUSTOM", templates),
         "&f2.3 &d&l&o┃ Оскорбление · Родня&r", "action qualifiers are preserved automatically");
      templates.setRule("2.3", "{rule} 「 {reason} 」 [{extra}]");
      equal(CauseFormatter.reason("2.3", "Оскорбление", "Родня", false, "CUSTOM", templates),
         "2.3 「 Оскорбление 」 [Родня]", "explicit extra placeholder does not duplicate qualifier");
      templates.setRule("2.3", "{rule}");
      equal(CauseFormatter.reason("2.3", "Оскорбление", "Родня", false, "CUSTOM", templates),
         "2.3 · Родня", "number-only template still keeps action qualifier");
      templates.setRule("2.3", null);
      require(!templates.hasOverride("2.3") && templates.forRule("2.3").equals(templates.general()), "clear override restores inheritance");
      ReasonTemplates draft = templates.copy();
      draft.setGeneral("{rule} другой {reason}");
      require(!draft.general().equals(templates.general()), "editor drafts do not mutate saved templates");
      Properties properties = new Properties();
      properties.setProperty("nickname", "Moderator");
      templates.writeTo(properties);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      properties.store(bytes, "test");
      Properties reloaded = new Properties();
      reloaded.load(new ByteArrayInputStream(bytes.toByteArray()));
      ReasonTemplates loaded = ReasonTemplates.readFrom(reloaded);
      equal(loaded.general(), templates.general(), "general template survives restart");
      equal(loaded.release(), templates.release(), "release template survives restart");
      equal(loaded.forRule("3.4"), templates.forRule("3.4"), "individual template survives restart");
      require(reloaded.getProperty("nickname").equals("Moderator"), "template persistence leaves other settings intact");
      templates.setRule("3.4", null);
      templates.writeTo(properties);
      require(!ReasonTemplates.readFrom(properties).hasOverride("3.4"), "removed overrides are not restored on reload");
      for (String bad : new String[]{"", "просто текст", "{rule}\n{reason}", "{rule} {player}", "{rule} server:выживание", "{rule} -g", "{rule}" + "x".repeat(256)}) {
         require(ReasonTemplates.validationError(bad) != null, "invalid template: " + bad);
      }
      properties.setProperty("reason_template.general", "bad");
      properties.setProperty("reason_template.rule.2.3", "{unknown}");
      ReasonTemplates safe = ReasonTemplates.readFrom(properties);
      equal(safe.general(), ReasonTemplates.DEFAULT_PUNISHMENT, "invalid settings fall back to a usable template");
      require(!safe.hasOverride("2.3"), "invalid individual template is skipped");
      equal(ReasonTemplates.renderTemplate("{rule} $ 「 {reason} 」", "2.3", "Текст", null),
         "2.3 $ 「 Текст 」", "literal symbols are not regex substitutions");
      equal(CauseFormatter.reason("2.3", "Оскорбление", null, false, "SHORT", loaded), "2.3", "builtin style selection still works");
      System.out.println("PASS: custom global/per-rule/release templates, command integration, qualifiers, drafts and settings roundtrip");
   }
   private static void equal(String actual, String expected, String label) {
      if (!expected.equals(actual)) throw new AssertionError(label + "\nexpected: " + expected + "\nactual: " + actual);
   }
   private static void require(boolean value, String label) {
      if (!value) throw new AssertionError(label);
   }
}
