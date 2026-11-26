package ru.dmitry.rulehelper.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import ru.dmitry.rulehelper.PlayerNames;
import ru.dmitry.rulehelper.RuleHelperConfig;

/** Identifies the sender of a chat row and adds a dedicated click target before the row. */
public final class ChatMenuPrefix {
   private static final Pattern ARROW_NAME = Pattern.compile("(?iu)(?:^|[»>\\s])([\\p{L}\\p{N}_]{3,16})\\s*(?:⇨|->|→|➜)");
   private static final Pattern ANGLE_NAME = Pattern.compile("(?iu)<([\\p{L}\\p{N}_]{3,16})>");

   private ChatMenuPrefix() {}

   public static Component add(Component message) {
      String prefix = RuleHelperConfig.chatMenuPrefix();
      if (prefix.isBlank() || sender(message.getString()) == null) return message;
      // Keep the marker's formatting local. Styling the root literal also
      // cascades to appended siblings and recolors/bolds the entire chat row.
      return Component.empty()
         .append(Component.literal(prefix + " ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
         .append(message);
   }

   public static String sender(String message) {
      if (message == null) return null;
      Matcher matcher = ARROW_NAME.matcher(message);
      while (matcher.find()) if (PlayerNames.isValid(matcher.group(1))) return matcher.group(1);
      matcher = ANGLE_NAME.matcher(message);
      return matcher.find() && PlayerNames.isValid(matcher.group(1)) ? matcher.group(1) : null;
   }
}
