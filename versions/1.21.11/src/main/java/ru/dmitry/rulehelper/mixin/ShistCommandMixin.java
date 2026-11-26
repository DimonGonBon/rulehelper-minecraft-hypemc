package ru.dmitry.rulehelper.mixin;

import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.SelfShistSuppressor;

@Mixin(class_634.class)
public abstract class ShistCommandMixin {
   @Inject(method = "method_45730(Ljava/lang/String;)V", at = @At("HEAD"))
   private void ruleHelper$watchShistCommand(String command, CallbackInfo ci) {
      SelfShistSuppressor.onOutgoingCommand(command, RuleHelperConfig.nickname());
   }
}
