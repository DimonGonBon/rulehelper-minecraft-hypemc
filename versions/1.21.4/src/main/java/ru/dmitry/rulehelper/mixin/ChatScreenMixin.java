package ru.dmitry.rulehelper.mixin;

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

@Mixin(value = ChatScreen.class)
public abstract class ChatScreenMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
   private void ruleHelper$showPrefixPlayerTooltip(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                                   org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
      ru.dmitry.rulehelper.chat.ChatPrefixTooltip.render(graphics, Minecraft.getInstance(), mouseX, mouseY);
   }

   @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
   private void ruleHelper$openMenuOnNameClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
         Minecraft client = Minecraft.getInstance();
         String copiedName = ChatNameHitTest.findPunishmentCopyTarget(client, mouseX, mouseY);
         if (copiedName != null) {
            client.keyboardHandler.setClipboard(copiedName);
            client.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Скопирован ник: " + copiedName)
               .withStyle(net.minecraft.ChatFormatting.AQUA));
            cir.setReturnValue(true);
            return;
         }
         String menuName = ChatNameHitTest.findMenuNameCopyTarget(client, mouseX, mouseY);
         if (menuName != null) {
            client.keyboardHandler.setClipboard(menuName);
            client.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Скопирован ник: " + menuName)
               .withStyle(net.minecraft.ChatFormatting.AQUA));
            cir.setReturnValue(true);
            return;
         }
         String name = ChatNameHitTest.findPrefixTarget(client, mouseX, mouseY);
         if (name != null) {
            if (RuleHelperConfig.isConfigured()) {
               client.setScreen(new RuleMenuScreen(name));
            } else {
               client.setScreen(new DonationSetupScreen(name));
            }
            cir.setReturnValue(true);
            return;
         }
         PunishmentAnnouncement announcement = ChatNameHitTest.findPunishment(client, mouseX, mouseY);
         if (announcement != null) {
            client.setScreen(new PunishmentCorrectionScreen(announcement));
            cir.setReturnValue(true);
            return;
         }
      }
   }
}
