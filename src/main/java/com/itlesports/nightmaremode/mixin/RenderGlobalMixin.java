package com.itlesports.nightmaremode.mixin;

import com.itlesports.nightmaremode.NightmareUtils;
import net.minecraft.src.RenderGlobal;
import net.minecraft.src.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin {
    @Unique private static final ResourceLocation BLOODMOON = new ResourceLocation("textures/bloodmoon.png");
    @Unique private static final ResourceLocation ECLIPSE = new ResourceLocation("textures/eclipse.png");

    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/prupe/mcpatcher/sky/SkyRenderer;setupCelestialObject(Lnet/minecraft/src/ResourceLocation;)Lnet/minecraft/src/ResourceLocation;",ordinal = 2))
    private ResourceLocation manageBloodMoonTexture(ResourceLocation defaultTexture){
        if(NightmareUtils.getIsBloodMoon()){
            return BLOODMOON;
        }
        return defaultTexture;
    }
    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/prupe/mcpatcher/sky/SkyRenderer;setupCelestialObject(Lnet/minecraft/src/ResourceLocation;)Lnet/minecraft/src/ResourceLocation;",ordinal = 0))
    private ResourceLocation manageEclipseTexture(ResourceLocation defaultTexture){
        if(NightmareUtils.getIsEclipse()){
            return ECLIPSE;
        }
        return defaultTexture;
    }
}
