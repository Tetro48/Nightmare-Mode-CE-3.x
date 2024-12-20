package com.itlesports.nightmaremode.mixin;

import net.minecraft.src.GuiIngameMenu;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.I18n;
import net.minecraft.src.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiIngameMenu.class)
public class GuiIngameMenuMixin {
    @Redirect(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/Minecraft;displayGuiScreen(Lnet/minecraft/src/GuiScreen;)V",ordinal = 5))
    private void crashGame(Minecraft instance, GuiScreen var3) throws Exception {
        throw new Exception("Player is trying to cheat.");
    }


    @Redirect(method = "initGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/I18n;getString(Ljava/lang/String;)Ljava/lang/String;",ordinal = 4))
    private String displayCheatText(String string){
        return I18n.getString("gui.pauseMenu.turnCheatsOn");
    }
    
//        @Inject(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/Minecraft;displayGuiScreen(Lnet/minecraft/src/GuiScreen;)V",ordinal = 5))
//    private void crashGame(GuiButton par1GuiButton, CallbackInfo ci) throws Exception {
//        if(!MinecraftServer.getIsServer()){
//            throw new Exception("Player is trying to cheat.");
//        }
//    }
//    @Redirect(method = "initGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/I18n;getString(Ljava/lang/String;)Ljava/lang/String;",ordinal = 4))
//    private String displayCheatText(String string){
//        if (!MinecraftServer.getIsServer()) {
//            return "Turn Cheats On";
//        }
//        return I18n.getString(string);
//    }

}
