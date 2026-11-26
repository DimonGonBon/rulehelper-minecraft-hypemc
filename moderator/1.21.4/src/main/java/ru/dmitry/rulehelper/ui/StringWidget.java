package ru.dmitry.rulehelper.ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
/** Compatibility adapter preserving the bounded text layout on 1.21.4. */
public final class StringWidget extends net.minecraft.client.gui.components.StringWidget {
 public StringWidget(int x,int y,int width,int height,Component text,Font font) { super(x,y,width,height,text,font); }
 public StringWidget setMaxWidth(int width) { setWidth(Math.min(getWidth(), Math.max(1,width))); return this; }
}
