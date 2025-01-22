package com.itlesports.nightmaremode.mixin;

import btw.entity.mob.KickingAnimal;
import com.itlesports.nightmaremode.NightmareUtils;
import net.minecraft.src.EntityHorse;
import net.minecraft.src.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityHorse.class)
public abstract class EntityHorseMixin extends KickingAnimal {
    public EntityHorseMixin(World par1World) {
        super(par1World);
    }

    @ModifyConstant(method = "applyEntityAttributes", constant = @Constant(doubleValue = 20.0d))
    private double increaseHP(double constant){
        return 24.0;
    }
    @ModifyArg(method = "applyEntityAttributes",at = @At(value = "INVOKE", target = "Lnet/minecraft/src/AttributeInstance;setAttribute(D)V",ordinal = 1))
    private double increaseSpeed(double var1){
        return NightmareUtils.getIsMobEclipsed(this) ? 0.4d : var1;
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void manageEclipseChance(World world, CallbackInfo ci){
        NightmareUtils.manageEclipseChance(this,4);
    }
    @Inject(method = "isSubjectToHunger", at = @At("HEAD"),cancellable = true)
    private void manageEclipseHunger(CallbackInfoReturnable<Boolean> cir){
        if(NightmareUtils.getIsMobEclipsed(this)){
            cir.setReturnValue(false);
        }
    }
}
