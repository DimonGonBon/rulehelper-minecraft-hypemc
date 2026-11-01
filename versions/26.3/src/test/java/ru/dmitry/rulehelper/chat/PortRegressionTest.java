package ru.dmitry.rulehelper.chat;

public final class PortRegressionTest {
   public static void main(String[] args) {
      int clicks = 0;
      for (int height : new int[]{240, 360, 480, 720}) {
         for (float scale : new float[]{0.25f, 0.5f, 0.75f, 1.0f}) {
            for (double spacing : new double[]{0, 0.1, 0.5, 1}) {
               int bottom = (int)Math.floor((float)(height - 40) / scale);
               int entryHeight = (int)(9 * (spacing + 1));
               int textOffset = (int)Math.round(8 * (spacing + 1) - 4 * spacing);
               for (int row = 0; row < 20; row++) {
                  double y = (bottom - row * entryHeight - textOffset + 4.0) * scale;
                  require(ChatRowGeometry.rowAt(y, height, scale, spacing, 9, 20) == row,
                     "Wrong chat row: " + row + ", scale=" + scale + ", spacing=" + spacing);
                  clicks++;
               }
            }
         }
      }
      require(ChatRowGeometry.rowAt(0, 480, 0, 0, 9, 20) == -1, "Zero scale");
      require(ChatRowGeometry.rowAt(479, 480, 1, 0, 9, 20) == -1, "Outside chat");
      PunishmentAnnouncement ban = PunishmentAnnouncement.parse("❨！❩ Игрок LLLLL55555 забанил hohonit на 6 часов по причине: 3.4 (выживание)");
      require(ban != null && ban.issuer().equals("LLLLL55555") && ban.target().equals("hohonit"), "Ban players");
      require(ban.duration().equals("6 часов") && ban.cleanReason().equals("3.4") && ban.mode().equals("выживание"), "Ban fields");
      PunishmentAnnouncement mute = PunishmentAnnouncement.parse("❨！❩ Игрок Moderator замутил Player123 на 30 минут по причине: 2.1 (выживание)");
      require(mute != null && mute.kind() == PunishmentAnnouncement.Kind.MUTE, "Mute");
      require(PunishmentAnnouncement.parse("Ⓖ ArsenyNovikov ⇨ привет") == null, "Adjacent player message");
      require(PunishmentAnnouncement.parse("Player123 ⇨ ❨！❩ Игрок Moderator забанил Player456 на 6 часов по причине: 3.4") == null, "Player-spoofed prefix");
      System.out.println("PASS: " + clicks + " chat row clicks and punishment parser checks");
   }
   private static void require(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
