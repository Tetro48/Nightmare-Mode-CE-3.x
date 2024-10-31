package com.itlesports.nightmaremode.mixin;

import btw.item.BTWItems;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.Item;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(GuiMainMenu.class)
public class GuiMainMenuMixin {
    @Shadow private String splashText;
    @Shadow @Final private static Random rand;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void manageSplashText(CallbackInfo ci){
//        this.splashText = "Nightmare Mode!";
        this.splashText = getQuotes().get(rand.nextInt(getQuotes().size()));
    }


    @Unique
    private static @NotNull List<String> getQuotes() {
        List<String> quotesList = new ArrayList<>();
        quotesList.add("Nightmare Mode!");
        quotesList.add("Also try MEA!");
        quotesList.add("Oops! All Hostile!");
        quotesList.add("Also try Hostile!");
        quotesList.add("Am I dreaming?");
        quotesList.add("Wakey wakey!");
        quotesList.add("Orange-flavored creepers?!");
        quotesList.add("Better Than MEA?");
        quotesList.add("It's just a bad dream!");
        return quotesList;
    }
}