package ru.dmitry.rulehelper.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class PrivateMessageMenuTest {
   public static void main(String[] args) {
      check("[я → D_IKSI] .", "sh1lesert", "sh1lesert", "D_IKSI");
      check("[sh1lesert → я] .", "Other123", "sh1lesert", "Other123");
      check("[CW] [SHAMEHUT0CTЬ → Zpixel_228Ivan_] qq", "Moderator", "SHAMEHUT0CTЬ", "Zpixel_228Ivan_");
      check("[СШ] [Alpha123 -> Beta456] привет", "Moderator", "Alpha123", "Beta456");
      check("[SS] [Alpha123 ⇨ Beta456] текст → с другой стрелкой", "Moderator", "Alpha123", "Beta456");
      check("[Я ➜ Player123] сообщение", "OwnName", "OwnName", "Player123");
      check("§6[§cSender123 §6→ §cя§6] §fпривет", "OwnName", "Sender123", "OwnName");
      var body = PrivateMessageMenu.parse("[Alpha123 → Beta456] текст [Other123 → Victim123]", "Moderator");
      require(body.body().equals("текст [Other123 → Victim123]"), "message-body arrows never replace the participants");
      var unknown = PrivateMessageMenu.parse("[я → Player123] привет", "");
      require(unknown.sender() == null && unknown.recipient().equals("Player123"), "never turn the word я into a player name when the profile is missing");
      for (String unrelated : new String[]{"Ⓖ Player123 → [Other123 → Victim123] привет", "[Игрок] Player123 → привет",
         "❨！❩ Игрок Mod123 разбанил Player123 по причине: 3.4", "[CW] Игрок Player123 зашёл", "[x → Victim123] текст"}) {
         require(PrivateMessageMenu.parse(unrelated, "Moderator") == null, "not a private-message header: " + unrelated);
      }
      System.out.println("PASS: outgoing/incoming/social-spy PM menus, self-name resolution, two distinct menu targets, hover labels and original formatting");
   }
   private static void check(String text, String own, String sender, String recipient) {
      var parsed = PrivateMessageMenu.parse(text, own);
      require(parsed != null && parsed.sender().equals(sender) && parsed.recipient().equals(recipient), "parse participants: " + text);
      Component original = Component.literal(text).withStyle(ChatFormatting.GOLD);
      Component decorated = PrivateMessageMenu.add(original, own);
      require(decorated.getString().equals(PrivateMessageMenu.LABELS + text), "two symbols prepend the original PM");
      require(decorated.getSiblings().getLast() == original, "preserve all private-message styles and text");
      require(!decorated.getStyle().isBold() && decorated.getStyle().getColor() == null, "symbols cannot color or bold the whole PM");
      require(decorated.getSiblings().get(0).getStyle().getClickEvent() == null
         && decorated.getSiblings().get(2).getStyle().getClickEvent() == null, "PM menu symbols do not copy text to the clipboard");
      require(decorated.getSiblings().get(0).getStyle().getHoverEvent() != null
         && decorated.getSiblings().get(2).getStyle().getHoverEvent() != null, "both participants have hover hints");
      var participants = PrivateMessageMenu.parse(decorated.getString(), own);
      require(sender.equals(PrivateMessageMenu.targetAt(participants, decorated.getString(), 1, 9, 9)), "first symbol opens sender menu");
      require(recipient.equals(PrivateMessageMenu.targetAt(participants, decorated.getString(), 10, 9, 9)), "second symbol opens recipient menu");
      require(PrivateMessageMenu.targetAt(participants, decorated.getString(), 18, 9, 9) == null, "message body is not either menu marker");
      require(PrivateMessageMenu.targetAt(participants, "продолжение длинного сообщения", 1, 9, 9) == null, "wrapped continuation has no phantom menu markers");
      require(PrivateMessageMenu.add(decorated, own) == decorated, "do not insert the markers twice");
   }
   private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
