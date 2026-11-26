package ru.dmitry.rulehelper.chat;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.dmitry.rulehelper.HistoryRuleCatalog;

/** Counts history chat entries per known rule and records the highest visible #N marker separately. */
public final class ViolationHistoryCounter {
   private static final Pattern RULE = Pattern.compile("(?<![0-9])([1-5]\\.[1-9][0-9]*)(?:#([0-9]+))?");
   private static final Pattern ISSUED_PUNISHMENT = Pattern.compile(
      "(?iuU)\\b(?:был[ао]?\\s+(?:забанен[ао]?|замучен[ао]?|заглушен[ао]?|предупрежден[ао]?|предупреждён[ао]?|кикнут[ао]?)|получил[а]?\\s+(?:предупреждение|бан|мут))\\b"
   );
   private static final Pattern BAN_ACTION = Pattern.compile("(?iuU)\\bбыл[ао]?\\s+забанен[ао]?\\b");
   private static final Pattern WARN_ACTION = Pattern.compile(
      "(?iuU)\\b(?:был[ао]?\\s+предупрежден[ао]?|был[ао]?\\s+предупреждён[ао]?|получил[а]?\\s+предупреждение|выдано\\s+предупреждение)\\b"
   );
   private static final Pattern MUTE_ACTION = Pattern.compile("(?iu)замутил|был[ао]?\\s+(?:замучен[ао]?|заглушен[ао]?)");
   private static final Pattern EVIDENCE_REQUEST = Pattern.compile("(?iuU)предоставьте\\s+доказательства");
   private static final Pattern DATE_LINE = Pattern.compile("(?iuU)^\\s*-?\\s*\\d+\\s+(?:день|дня|дней|час|часа|часов|минут|минута|минуты)\\b.*назад");
   private static final Pattern DURATION = Pattern.compile("(?iu)(?:забанен[ао]?|замучен[ао]?|заглушен[ао]?|предупрежден[ао]?|предупреждён[ао]?|кикнут[ао]?)\\s+на\\s+(.+?)(?=\\s+по\\s+причине:|:|$)");
   private final Map<String, RuleCount> counts = new LinkedHashMap<>();
   private final Map<String, List<PunishmentEntry>> entries = new LinkedHashMap<>();
   private Map<String, Integer> pendingMarkers;
   private ActionType pendingActionType;
   private String currentDateLine = "";
   private String pendingDateLine = "";
   private String pendingMessage = "";
   private String pendingDuration = "";
   private boolean finished;

   public ViolationHistoryCounter() {
      HistoryRuleCatalog.ALL.forEach(rule -> {
         this.counts.put(rule.code(), new RuleCount());
         this.entries.put(rule.code(), new ArrayList<>());
      });
   }

   /** One server chat message is treated as one history record per distinct rule code in it. */
   public void accept(String message) {
      if (message == null || message.isBlank()) return;
      if (DATE_LINE.matcher(message).find()) {
         this.commitPending(false);
         this.currentDateLine = message;
         return;
      }

      // The evidence-request ban is also present in /hist. Its reason can be a
      // separate following chat component, so defer counting until that part arrives.
      if (EVIDENCE_REQUEST.matcher(message).find()) {
         if (ISSUED_PUNISHMENT.matcher(message).find()) {
            this.commitPending(false);
            Map<String, Integer> markers = parseRuleMarkers(message);
            if (markers.containsKey("3.4")) this.commitService34(markers.get("3.4"), message, parseDuration(message));
         } else if (this.pendingMarkers != null && this.pendingMarkers.containsKey("3.4")) {
            this.commitPending(true);
         } else {
            this.commitPending(false);
         }
         return;
      }

      if (this.pendingMarkers != null) {
         if (DATE_LINE.matcher(message).find()) {
            this.commitPending(false);
         } else if (ISSUED_PUNISHMENT.matcher(message).find()) {
            this.commitPending(false);
         } else {
            // The first non-action continuation completes the preceding entry.
            this.commitPending(false);
            return;
         }
      }

      // /hist also lists removals such as "был разбанен ...: 3.4". They are not
      // newly issued punishments and must not increase the rule count.
      if (!ISSUED_PUNISHMENT.matcher(message).find()) return;
      ActionType actionType = BAN_ACTION.matcher(message).find() ? ActionType.BAN
         : WARN_ACTION.matcher(message).find() ? ActionType.WARN
         : MUTE_ACTION.matcher(message).find() ? ActionType.MUTE
         : ActionType.OTHER;
      Map<String, Integer> markersInMessage = parseRuleMarkers(message);
      if (!markersInMessage.isEmpty()) {
         this.pendingMarkers = markersInMessage;
         this.pendingActionType = actionType;
         this.pendingDateLine = this.currentDateLine;
         this.pendingMessage = message;
         this.pendingDuration = parseDuration(message);
      }
   }

   private Map<String, Integer> parseRuleMarkers(String message) {
      Map<String, Integer> markersInMessage = new LinkedHashMap<>();
      Matcher matcher = RULE.matcher(message);
      while (matcher.find()) {
         RuleCount count = this.counts.get(matcher.group(1));
         if (count == null) continue;
         int marker = matcher.group(2) == null ? 0 : parseMarker(matcher.group(2));
         markersInMessage.merge(matcher.group(1), marker, Math::max);
      }
      return markersInMessage;
   }

   /** Flushes the final history entry after the server stops sending response lines. */
   public void finish() {
      if (this.finished) return;
      this.finished = true;
      this.commitPending(false);
   }

   private void commitPending(boolean service34) {
      if (this.pendingMarkers == null) return;
      this.pendingMarkers.forEach((code, marker) -> {
         RuleCount count = this.counts.get(code);
         if (service34 && !code.equals("3.4")) return;
         count.records++;
         count.highestMarker = Math.max(count.highestMarker, marker);
         if (code.equals("3.4")) {
            if (service34) count.service34++;
            else count.regular34++;
         }
         if (code.equals("4.2")) {
            if (this.pendingActionType == ActionType.WARN) count.warnings++;
            if (this.pendingActionType == ActionType.BAN) count.bans++;
         }
         if (!service34 || code.equals("3.4")) {
            this.entries.get(code).add(new PunishmentEntry(this.pendingDateLine, this.pendingMessage,
               this.pendingDuration, this.pendingActionType, service34));
         }
      });
      this.pendingMarkers = null;
      this.pendingActionType = null;
      this.pendingDateLine = "";
      this.pendingMessage = "";
      this.pendingDuration = "";
   }

   private void commitService34(int marker, String message, String duration) {
      RuleCount count = this.counts.get("3.4");
      count.records++;
      count.service34++;
      count.highestMarker = Math.max(count.highestMarker, marker);
      this.entries.get("3.4").add(new PunishmentEntry(this.currentDateLine, message, duration, ActionType.BAN, true));
   }

   private static String parseDuration(String message) {
      Matcher matcher = DURATION.matcher(message);
      if (!matcher.find()) return "";
      String value = matcher.group(1).trim();
      if (value.matches("(?iu)навсегда|навечно|постоянно")) return "Навсегда";
      while (!value.isEmpty()) {
         String normalized = ru.dmitry.rulehelper.ModeratorProofCommands.normalizeDuration(value);
         if (normalized != null) return normalized;
         int lastSpace = value.lastIndexOf(' ');
         if (lastSpace < 0) break;
         value = value.substring(0, lastSpace).trim();
      }
      return "";
   }

   private static int parseMarker(String marker) {
      try {
         return Integer.parseInt(marker);
      } catch (NumberFormatException ignored) {
         return 0;
      }
   }

   public int records(String code) {
      RuleCount count = this.counts.get(code);
      return count == null ? 0 : count.records;
   }

   public int highestMarker(String code) {
      RuleCount count = this.counts.get(code);
      return count == null ? 0 : count.highestMarker;
   }

   public int warnings(String code) {
      RuleCount count = this.counts.get(code);
      return count == null ? 0 : count.warnings;
   }

   public int bans(String code) {
      RuleCount count = this.counts.get(code);
      return count == null ? 0 : count.bans;
   }

   public int regular34Records() { return this.counts.get("3.4").regular34; }
   public int service34Records() { return this.counts.get("3.4").service34; }
   public List<PunishmentEntry> punishmentEntries(String code) {
      return List.copyOf(this.entries.getOrDefault(code, List.of()));
   }

   public int totalRecords() {
      return this.counts.values().stream().mapToInt(count -> count.records).sum();
   }

   private static final class RuleCount {
      private int records;
      private int highestMarker;
      private int warnings;
      private int bans;
      private int regular34;
      private int service34;
   }

   public enum ActionType { WARN, BAN, MUTE, OTHER }
   public record PunishmentEntry(String dateLine, String message, String duration, ActionType type, boolean service34) {}
}
