package ru.dmitry.rulehelper.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.ui.ProofRequestScreen;
import ru.dmitry.rulehelper.ui.DailyPunishmentHistoryScreen;
import ru.dmitry.rulehelper.ui.ModerationSettingsScreen;

/** Opens the configured RuleHelper screens and the fixed Home hotkey for key settings. */
@Mixin(value = KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
   @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
   private void ruleHelper$openProofRequest(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
      if (action != InputConstants.PRESS) return;
      Minecraft client = Minecraft.getInstance();
      if (client.screen != null || client.getConnection() == null) return;
      int key = keyCode;
      if (key == RuleHelperConfig.settingsKey()) {
         client.setScreen(new ModerationSettingsScreen(null));
      } else if (key == RuleHelperConfig.proofRequestKey()) {
         client.setScreen(new ProofRequestScreen());
      } else if (key == RuleHelperConfig.dailyHistoryKey()) {
         client.setScreen(new DailyPunishmentHistoryScreen(null));
      } else return;
      ci.cancel();
   }
}
