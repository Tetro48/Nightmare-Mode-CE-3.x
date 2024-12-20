package com.itlesports.nightmaremode.block;

import btw.block.BTWBlocks;
import btw.item.items.StoneItem;
import com.itlesports.nightmaremode.block.blocks.SteelOre;
import net.minecraft.src.Block;
import net.minecraft.src.ItemBlock;

public class NMBlocks {
    public static Block steelOre;

    public static void initNightmareBlocks(){
        steelOre = (new SteelOre(2305)).setHardness(13.0F).setResistance(200.0F).setStepSound(BTWBlocks.oreStepSound).setUnlocalizedName("nmSteelOre").setTextureName("steel_ore");
        StoneItem.itemsList[steelOre.blockID] = new ItemBlock(NMBlocks.steelOre.blockID - 256);
    }
}
