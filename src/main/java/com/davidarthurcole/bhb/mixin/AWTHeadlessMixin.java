package com.davidarthurcole.bhb.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(Main.class)
public class AWTHeadlessMixin
{
    @Inject(at = @At("HEAD"), method = "main")
    private static void init(CallbackInfo info) {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            System.out.println("[BHB - Blazin's Hex Blender] Setting java.awt.headless to false");
            System.setProperty("java.awt.headless", "false");
        }
    }
}
