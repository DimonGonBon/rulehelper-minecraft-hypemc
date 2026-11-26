package ru.dmitry.rulehelper.mixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChatComponent.class, remap = false)
public interface ChatHudAccessor {
   @Accessor("trimmedMessages")
   List<Line> ruleHelper$getVisibleMessages();

   @Accessor("allMessages")
   List<GuiMessage> ruleHelper$getAllMessages();

   @Accessor("chatScrollbarPos")
   int ruleHelper$getScrolledLines();
}
