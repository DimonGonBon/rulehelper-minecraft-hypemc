package ru.dmitry.rulehelper.mixin;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.ChatNameHitTest;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;
import ru.dmitry.rulehelper.ui.DonationSetupScreen;
import ru.dmitry.rulehelper.ui.PunishmentCorrectionScreen;
import ru.dmitry.rulehelper.ui.RuleMenuScreen;

@Mixin(value = ChatScreen.class, remap = false)
public abstract class ChatScreenMixin {
   @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", remap = false, at = @At("HEAD"), cancellable = true)
   private void ruleHelper$openMenuOnNameClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (click.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
         Minecraft client = Minecraft.getInstance();
         PunishmentAnnouncement announcement = ChatNameHitTest.findPunishment(client, click.x(), click.y());
         if (announcement != null) {
            client.gui.setScreen(new PunishmentCorrectionScreen(announcement));
            cir.setReturnValue(true);
            return;
         }
         String name = ChatNameHitTest.findName(client, click.x(), click.y());
         if (name != null) {
            if (RuleHelperConfig.isConfigured()) {
               client.gui.setScreen(new RuleMenuScreen(name));
            } else {
               client.gui.setScreen(new DonationSetupScreen(name));
            }
            cir.setReturnValue(true);
         }
      }
   }
}
