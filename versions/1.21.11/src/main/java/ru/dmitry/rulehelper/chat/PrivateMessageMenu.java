package ru.dmitry.rulehelper.chat;

import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import ru.dmitry.rulehelper.ChatNames;

/** Two small menu targets for outgoing, incoming and staff-visible private messages. */
public final class PrivateMessageMenu {
   public static final String SENDER = "◀";
   public static final String RECIPIENT = "▶";
   public static final String LABELS = SENDER + " " + RECIPIENT + " ";
   private static final String ADDRESS = "(?:я|me|[\\p{L}\\p{N}_]{3,16})";
   private static final Pattern MESSAGE = Pattern.compile(
      "(?isu)^(?:\\[(?:[CС][WШ]|СС|SS|SocialSpy|ЛС|PM)\\]\\s*)?\\[(" + ADDRESS + ")\\s*"
         + "(?:→|➜|⇨|->)\\s*(" + ADDRESS + ")\\]\\s*(.*)$");
   public record Participants(String sender, String recipient, String body) {}
   private PrivateMessageMenu() {}

   public static Participants parse(String message, String ownNickname) {
      if (message == null) return null;
      String plain = message.replaceAll("(?i)[§&][0-9A-FK-OR]", "").replace('\u00a0', ' ').strip();
      if (plain.startsWith(LABELS)) plain = plain.substring(LABELS.length());
      var match = MESSAGE.matcher(plain);
      if (!match.matches()) return null;
      return new Participants(resolve(match.group(1), ownNickname), resolve(match.group(2), ownNickname), match.group(3));
   }
   private static String resolve(String address, String ownNickname) {
      if (address.equalsIgnoreCase("я") || address.equalsIgnoreCase("me")) {
         return ChatNames.isValid(ownNickname) ? ownNickname : null;
      }
      return ChatNames.isValid(address) ? address : null;
   }

   public static Component add(Component original, String ownNickname) {
      Participants participants = parse(original.getString(), ownNickname);
      if (participants == null || original.getString().startsWith(LABELS)) return original;
      return Component.empty().append(label(SENDER, "Отправитель", participants.sender(), ChatFormatting.BLUE))
         .append(Component.literal(" "))
         .append(label(RECIPIENT, "Получатель", participants.recipient(), ChatFormatting.AQUA))
         .append(Component.literal(" ")).append(original);
   }
   private static Component label(String symbol, String role, String nickname, ChatFormatting color) {
      String hint = nickname == null ? role + ": укажи свой ник в настройках"
         : role + ": " + nickname + " · открыть меню наказаний";
      // These symbols never carry a clipboard event. ChatScreen routes their clicks to RuleMenuScreen.
      return Component.literal(symbol).setStyle(Style.EMPTY.withColor(nickname == null ? ChatFormatting.GRAY : color)
         .withBold(false).withHoverEvent(new HoverEvent.ShowText(Component.literal(hint))));
   }

   public static String targetAt(Participants participants, String renderedRow, double x, int senderWidth, int recipientWidth) {
      if (participants == null || renderedRow == null || !renderedRow.startsWith(LABELS) || !Double.isFinite(x) || x < 0) return null;
      if (x < senderWidth) return participants.sender();
      if (x < senderWidth + recipientWidth) return participants.recipient();
      return null;
   }
}
