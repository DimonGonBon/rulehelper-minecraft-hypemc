package ru.dmitry.rulehelper.chat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1041;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_338;
import net.minecraft.class_5481;
import net.minecraft.class_303.class_7590;
import ru.dmitry.rulehelper.mixin.ChatHudAccessor;

public final class ChatNameHitTest {
   private ChatNameHitTest() {
   }

   public static String findName(class_310 var0, double var1, double var3) {
      if (var0 != null && var0.field_1705 != null) {
         class_1041 var5 = var0.method_22683();
         if (var5 == null) {
            return null;
         }

         class_338 var6 = var0.field_1705.method_1743();
         if (var6 instanceof ChatHudAccessor var7) {
            List var8 = var7.ruleHelper$getVisibleMessages();
            if (var8 != null && !var8.isEmpty()) {
               double var9 = (Double)var0.field_1690.method_42554().method_41753();
               double var11 = (Double)var0.field_1690.method_42546().method_41753();
               if (var9 <= 0.0) {
                  return null;
               }

               int var13 = Math.max(0, var7.ruleHelper$getScrolledLines());
               int var14 = Math.min(var6.method_1813(), var8.size() - var13);
               if (var14 <= 0) {
                  return null;
               }

               double var15 = Math.floor((var5.method_4502() - 40.0) / var9);
               double var17 = Math.round(8.0 * (1.0 + var11) - 4.0 * var11);
               double var19 = 9.0 * (1.0 + var11);
               double var21 = var1 / var9 - 4.0;
               class_327 var23 = var0.field_1772;

               for (int var24 = 0; var24 < var14; var24++) {
                  double var25 = (var15 - var24 * var19 - var17) * var9;
                  if (var3 >= var25 && var3 < var25 + 9.0 * var9) {
                     int var27 = var24 + var13;
                     return findTokenAt(var23, ((class_7590)var8.get(var27)).comp_896(), var21);
                  }
               }

               return null;
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String findTokenAt(class_327 var0, class_5481 var1, double var2) {
      ArrayList var4 = new ArrayList();
      int[] var5 = new int[]{0};
      var1.accept((var3, var4x, var5x) -> {
         String var6x = new String(Character.toChars(var5x));
         int var7x = var0.method_27525(class_2561.method_43470(var6x).method_10862(var4x));
         var4.add(new ChatNameHitTest.Glyph(var6x, var5[0], var5[0] + Math.max(0, var7x), var3.method_10975() != null));
         var5[0] += Math.max(0, var7x);
         return true;
      });
      int var6 = -1;

      for (int var7 = 0; var7 < var4.size(); var7++) {
         ChatNameHitTest.Glyph var8 = (ChatNameHitTest.Glyph)var4.get(var7);
         if (var2 >= var8.start && var2 < var8.end && isNameChar(var8.text)) {
            // Respect clickable chat components (e.g. WorldGuard [Add] buttons).
            // RuleHelper should only intercept ordinary player-name clicks.
            if (var8.clickable) {
               return null;
            }
            var6 = var7;
            break;
         }
      }

      if (var6 < 0) {
         return null;
      }

      int var12 = var6;
      int var13 = var6 + 1;

      while (var12 > 0 && isNameChar(((ChatNameHitTest.Glyph)var4.get(var12 - 1)).text)) {
         var12--;
      }

      while (var13 < var4.size() && isNameChar(((ChatNameHitTest.Glyph)var4.get(var13)).text)) {
         var13++;
      }

      int var9 = var13 - var12;
      if (var9 >= 3 && var9 <= 16) {
         StringBuilder var10 = new StringBuilder();

         for (int var11 = var12; var11 < var13; var11++) {
            var10.append(((ChatNameHitTest.Glyph)var4.get(var11)).text);
         }

         String var14 = var10.toString();
         return var14.matches("[A-Za-z0-9_]{3,16}") ? var14 : null;
      } else {
         return null;
      }
   }

   private static boolean isNameChar(String var0) {
      if (var0.isEmpty()) {
         return false;
      }

      int var1 = var0.codePointAt(0);
      return var1 >= 65 && var1 <= 90 || var1 >= 97 && var1 <= 122 || var1 >= 48 && var1 <= 57 || var1 == 95;
   }

   private record Glyph(String text, int start, int end, boolean clickable) {
   }
}
