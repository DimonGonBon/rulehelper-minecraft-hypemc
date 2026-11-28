package ru.dmitry.rulehelper.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dmitry.rulehelper.RuleHelperConfig;
import ru.dmitry.rulehelper.chat.ChatMenuPrefix;
import ru.dmitry.rulehelper.chat.DailyPunishmentHistory;
import ru.dmitry.rulehelper.chat.ModerationAlertDetector;
import ru.dmitry.rulehelper.chat.PunishmentCopyPrefixes;
import ru.dmitry.rulehelper.chat.PunishmentAnnouncement;

@Mixin(value = ChatComponent.class)
public abstract class ChatHistoryCaptureMixin {
   private static boolean ruleHelper$addingAlert;

   @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
      at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private Component ruleHelper$addPrefixes(Component content) {
      if (ru.dmitry.rulehelper.chat.PrivateMessageMenu.parse(content.getString(), RuleHelperConfig.nickname()) != null) {
         return ru.dmitry.rulehelper.chat.PrivateMessageMenu.add(content, RuleHelperConfig.nickname());
      }
      return PunishmentCopyPrefixes.add(ChatMenuPrefix.add(ru.dmitry.rulehelper.chat.CoreProtectNameCopy.addNativeCopy(content)));
   }

   @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
      at = @At("HEAD"), cancellable = true)
   private void ruleHelper$detectChatViolations(Component content, MessageSignature signature,
                                                 GuiMessageTag tag, CallbackInfo ci) {
      String line = content.getString();
      if (DailyPunishmentHistory.consumeHistoryLine(line, RuleHelperConfig.nickname())) {
         ci.cancel();
         return;
      }
      if (ruleHelper$addingAlert) return;
      var preferences = RuleHelperConfig.notifications();
      String alert = ModerationAlertDetector.notification(line, RuleHelperConfig.nickname(), preferences);
      if (alert == null) return;
      ruleHelper$addingAlert = true;
      try {
         Minecraft client = Minecraft.getInstance();
         client.gui.getChat().addMessage(Component.empty()
            .append(Component.literal(RuleHelperConfig.alertPrefix()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(alert).withStyle(ChatFormatting.WHITE)));
         if (preferences.sound()) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.2F));
      } finally {
         ruleHelper$addingAlert = false;
      }
   }
}
