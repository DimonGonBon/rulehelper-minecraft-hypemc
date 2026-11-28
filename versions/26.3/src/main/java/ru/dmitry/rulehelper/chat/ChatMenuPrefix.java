package ru.dmitry.rulehelper.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.ChatNames;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Identifies the sender of a chat row and adds a dedicated click target before the row. */
public final class ChatMenuPrefix {
   private static final Pattern ARROW_NAME = Pattern.compile("(?iu)(?:^|[»>\\s])([\\p{L}\\p{N}_]{3,16})\\s*(?:⇨|->|→|➜)");
   private static final Pattern ANGLE_NAME = Pattern.compile("(?iu)<([\\p{L}\\p{N}_]{3,16})>");

   private ChatMenuPrefix() {}

   public static Component add(Component message) {
      return add(message, RuleHelperConfig.chatMenuPrefix());
   }

   public static Component add(Component message, String prefix) {
      if (prefix.isBlank() || sender(message.getString()) == null) return message;
      if (CoreProtectLog.actor(message.getString()) != null && !CoreProtectLog.hasPlayerActor(message)) return message;
      // Keep the marker's formatting local. Styling the root literal also
      // cascades to appended siblings and recolors/bolds the entire chat row.
      return Component.empty()
         .append(Component.literal(prefix + " ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
         .append(message);
   }

   public static String sender(String message) {
      if (message == null) return null;
      String actor = CoreProtectLog.actor(message);
      if (actor != null) return actor;
      Matcher matcher = ARROW_NAME.matcher(message);
      while (matcher.find()) if (ChatNames.isValid(matcher.group(1))) return matcher.group(1);
      matcher = ANGLE_NAME.matcher(message);
      return matcher.find() && ChatNames.isValid(matcher.group(1)) ? matcher.group(1) : null;
   }

   /** The marker is only clickable on its own rendered row, including its bold width. */
   public static boolean isPrefixHit(String message, String renderedRow, String prefix, double x, int markerWidth) {
      return prefix != null && !prefix.isBlank() && message.startsWith(prefix + " ")
         && renderedRow.startsWith(prefix + " ") && x >= 0 && x < markerWidth;
   }
}
