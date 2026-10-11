package ru.dmitry.rulehelper.mixin;

import java.util.List;
import net.minecraft.class_338;
import net.minecraft.class_303.class_7590;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_338.class)
public interface ChatHudAccessor {
   @Accessor("field_2064")
   List<class_7590> ruleHelper$getVisibleMessages();

   @Accessor("field_2066")
   int ruleHelper$getScrolledLines();
}
