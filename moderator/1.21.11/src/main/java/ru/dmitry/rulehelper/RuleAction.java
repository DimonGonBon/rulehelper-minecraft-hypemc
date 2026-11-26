package ru.dmitry.rulehelper;

public record RuleAction(String label, String commandTemplate) {
   public String commandFor(String player) {
      return this.commandTemplate.replace("{player}", player);
   }
}
