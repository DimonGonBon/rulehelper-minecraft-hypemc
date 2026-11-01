package ru.dmitry.rulehelper;

import java.util.List;

public record RuleEntry(String code, String title, String summary, String punishment, List<RuleAction> actions) {
}
