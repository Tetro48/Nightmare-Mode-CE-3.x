package com.itlesports.nightmaremode.mixin;

import btw.entity.item.FloatingItemEntity;
import btw.item.BTWItems;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "harvestBlock", at = @At("HEAD"))
    private void additionalDropsForToolHarvested(World par1World, EntityPlayer par2EntityPlayer, int par3, int par4, int par5, int par6, CallbackInfo ci){
        Block thisObj = (Block)(Object)this;
        if (par2EntityPlayer.getHeldItem() != null) {
            if(par2EntityPlayer.getHeldItem().itemID == Item.pickaxeIron.itemID){
                if (thisObj.blockID == Block.oreIron.blockID) {
                    par1World.spawnEntityInWorld(new FloatingItemEntity(par1World, par3, par4, par5, new ItemStack(BTWItems.ironOreChunk)));
                } else if(thisObj.blockID == Block.oreGold.blockID){
                    par1World.spawnEntityInWorld(new FloatingItemEntity(par1World, par3, par4, par5, new ItemStack(BTWItems.goldOreChunk)));
                }
            } else if(par2EntityPlayer.getHeldItem().itemID == Item.pickaxeDiamond.itemID){
                int bonus = par1World.rand.nextInt(4)==0 ? 2 : 3;
                if (thisObj.blockID == Block.oreIron.blockID) {
                    par1World.spawnEntityInWorld(new FloatingItemEntity(par1World, par3, par4, par5, new ItemStack(BTWItems.ironOreChunk, bonus)));
                } else if(thisObj.blockID == Block.oreGold.blockID){
                    par1World.spawnEntityInWorld(new FloatingItemEntity(par1World, par3, par4, par5, new ItemStack(BTWItems.goldOreChunk, bonus)));
                }
            }
        }
    }
}
