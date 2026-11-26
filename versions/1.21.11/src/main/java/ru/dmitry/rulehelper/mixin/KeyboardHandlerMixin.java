package ru.dmitry.rulehelper.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.ui.DailyPunishmentHistoryScreen;
import ru.dmitry.rulehelper.ui.DonatorSettingsScreen;

@Mixin(value = KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
   @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true)
   private void ruleHelper$openDonatorScreens(long window, int action, KeyEvent event, CallbackInfo ci) {
      if (action != InputConstants.PRESS) return;
      Minecraft client = Minecraft.getInstance();
      if (client.screen != null || client.getConnection() == null) return;
      if (event.key() == InputConstants.KEY_HOME) {
         client.setScreen(new DonatorSettingsScreen(null));
      } else if (event.key() == RuleHelperConfig.dailyHistoryKey()) {
         client.setScreen(new DailyPunishmentHistoryScreen(null));
      } else return;
      ci.cancel();
   }
}
