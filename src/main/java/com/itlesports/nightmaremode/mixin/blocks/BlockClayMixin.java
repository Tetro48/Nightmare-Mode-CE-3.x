package com.itlesports.nightmaremode.mixin.blocks;

import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockClay.class)
public class BlockClayMixin {
    @Inject(method = "dropBlockAsItemWithChance", at = @At("TAIL"))
    private void chanceToBeInfested(World world, int i, int j, int k, int iMetaData, float fChance, int iFortuneModifier, CallbackInfo ci){
        if(world.rand.nextFloat()<0.08){
            EntitySilverfish silverfish = new EntitySilverfish(world);
            silverfish.setPositionAndUpdate(i+0.5,j,k+0.5);
            silverfish.addPotionEffect(new PotionEffect(Potion.waterBreathing.id, 100000, 0));
            silverfish.setAttackTarget(world.getClosestPlayerToEntity(silverfish,10));
            world.spawnEntityInWorld(silverfish);
        }
    }
}
