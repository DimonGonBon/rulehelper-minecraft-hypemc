package ru.dmitry.rulehelper.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.chat.ChatPrefixTooltip;

@Mixin(value = Hud.class, remap = false)
public abstract class ChatPrefixTooltipHudMixin {
   @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At("TAIL"), remap = false)
   private void ruleHelper$showPrefixPlayerTooltip(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.gui.screen() == null) {
         ChatPrefixTooltip.render(graphics, client,
            (int)MouseHandler.getScaledXPos(client.getWindow(), client.mouseHandler.xpos()),
            (int)MouseHandler.getScaledYPos(client.getWindow(), client.mouseHandler.ypos()));
      }
   }
}
