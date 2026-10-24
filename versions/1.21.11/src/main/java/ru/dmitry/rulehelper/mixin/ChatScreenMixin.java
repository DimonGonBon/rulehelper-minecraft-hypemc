package ru.dmitry.rulehelper.mixin;

import net.minecraft.class_11909;
import net.minecraft.class_310;
import net.minecraft.class_408;
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

@Mixin(class_408.class)
public abstract class ChatScreenMixin {
   @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true)
   private void ruleHelper$openMenuOnNameClick(class_11909 click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (click.method_74245() == 0) {
         class_310 client = class_310.method_1551();
         PunishmentAnnouncement announcement = ChatNameHitTest.findPunishment(client, click.comp_4798(), click.comp_4799());
         if (announcement != null) {
            client.method_1507(new PunishmentCorrectionScreen(announcement));
            cir.setReturnValue(true);
            return;
         }
         String name = ChatNameHitTest.findName(client, click.comp_4798(), click.comp_4799());
         if (name != null) {
            if (RuleHelperConfig.isConfigured()) {
               client.method_1507(new RuleMenuScreen(name));
            } else {
               client.method_1507(new DonationSetupScreen(name));
            }
            cir.setReturnValue(true);
         }
      }
   }
}
