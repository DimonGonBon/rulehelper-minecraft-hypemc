package ru.dmitry.rulehelper.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.chat.ChatPrefixTooltip;

@Mixin(value = Gui.class)
public abstract class ChatPrefixTooltipHudMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At("TAIL"))
   private void ruleHelper$showPrefixPlayerTooltip(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.screen == null) {
         ChatPrefixTooltip.render(graphics, client,
            (int)(client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth()),
            (int)(client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight()));
      }
   }
}
