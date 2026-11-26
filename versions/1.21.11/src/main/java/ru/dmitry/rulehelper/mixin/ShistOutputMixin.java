package ru.dmitry.rulehelper.mixin;

import net.minecraft.class_2561;
import net.minecraft.class_338;
import net.minecraft.class_7469;
import net.minecraft.class_7591;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.chat.SelfShistSuppressor;

@Mixin(class_338.class)
public abstract class ShistOutputMixin {
   @Inject(method = "method_1812(Lnet/minecraft/class_2561;)V", at = @At("HEAD"), cancellable = true)
   private void ruleHelper$hideSelfHistoryText(class_2561 message, CallbackInfo ci) {
      if (SelfShistSuppressor.suppress(message.getString())) ci.cancel();
   }

   @Inject(method = "method_44811(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V",
      at = @At("HEAD"), cancellable = true)
   private void ruleHelper$hideSelfHistoryText(class_2561 message, class_7469 signature,
                                               class_7591 indicator, CallbackInfo ci) {
      if (SelfShistSuppressor.suppress(message.getString())) ci.cancel();
   }
}
