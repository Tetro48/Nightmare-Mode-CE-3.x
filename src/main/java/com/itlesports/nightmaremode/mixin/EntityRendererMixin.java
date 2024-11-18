package com.itlesports.nightmaremode.mixin;

import net.minecraft.src.EntityRenderer;
import net.minecraft.src.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin implements EntityAccessor {
                            // MEA CODE. credit to Pot_tx
    @Shadow
    private Minecraft mc;

    @Shadow protected abstract void updateLightmap(float par1);

    @ModifyArgs(method = "updateCameraAndRender(F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/src/EntityClientPlayerMP;setAngles(FF)V",
                    ordinal = 0))
    private void slowSmoothCameraInWeb(Args args) {
        if (((EntityAccessor)this.mc.thePlayer).getIsInWeb()) {
            args.set(0, (float) args.get(0) * 0.25F);
            args.set(1, (float) args.get(1) * 0.25F);
        }
    }

    @ModifyArgs(
            method = "updateCameraAndRender(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityClientPlayerMP;setAngles(FF)V", ordinal = 1)
    )
    private void slowCameraInWeb(Args args) {
        if (((EntityAccessor)this.mc.thePlayer).getIsInWeb()) {
            args.set(0, (float) args.get(0) * 0.25F);
            args.set(1, (float) args.get(1) * 0.25F);

        }
    }

//    @Inject(method = "modUpdateLightmap",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/src/EntityRenderer;modUpdateLightmapOverworld(Lnet/minecraft/src/WorldClient;F)V",
//                    ordinal = 1,
//                    shift = At.Shift.AFTER),
//            locals = LocalCapture.CAPTURE_FAILHARD)
//    private void manageEndGloom(float fPartialTicks, CallbackInfo ci, WorldClient world){
//        if(world.provider.dimensionId == 1){
//            this.updateLightmap(fPartialTicks);
//        }
//    }
//    REMOVES END GLOOM
}
