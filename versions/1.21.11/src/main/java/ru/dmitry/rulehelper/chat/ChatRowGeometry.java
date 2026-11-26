package ru.dmitry.rulehelper.chat;

/** Coordinate calculation kept identical to Minecraft 26.3 ChatComponent. */
final class ChatRowGeometry {
   private ChatRowGeometry() {}

   static int rowAt(double mouseY, int screenHeight, float scale, double spacing, int fontHeight, int count) {
      if (scale <= 0 || fontHeight <= 0 || count <= 0) return -1;
      int bottom = (int)Math.floor((float)(screenHeight - 40) / scale);
      int entryHeight = (int)(fontHeight * (spacing + 1.0));
      int offset = (int)Math.round(8.0 * (spacing + 1.0) - 4.0 * spacing);
      for (int row = 0; row < count; row++) {
         double y = (bottom - row * entryHeight - offset) * (double)scale;
         if (mouseY >= y && mouseY < y + fontHeight * (double)scale) return row;
      }
      return -1;
   }
}
