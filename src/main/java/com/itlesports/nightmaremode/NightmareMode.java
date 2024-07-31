package com.itlesports.nightmaremode;

import btw.AddonHandler;
import btw.BTWAddon;
import btw.client.network.packet.handler.CustomEntityPacketHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.Packet24MobSpawn;
import net.minecraft.src.SpawnListEntry;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public class NightmareMode extends BTWAddon implements ModInitializer {
    private static NightmareMode instance;

//    private NightmareMode() {
//    }

    @Override
    public void initialize() {
        AddonHandler.logMessage(this.getName() + " Version " + this.getVersionString() + " Initializing...");
//        AddBiomeSpawn();
    }

    public static NightmareMode getInstance() {
        if (instance == null)
            instance = new NightmareMode();
        return instance;
    }

    @Override
    public void onInitialize() {
        addFireCreeperToSpawnEntities();
    }

    private static void addFireCreeperToSpawnEntities() {
        CustomEntityPacketHandler.entryMap.put(13, (world, dataStream, packet) -> {
            EntityFireCreeper entityToSpawn = new EntityFireCreeper(world);
            Packet24MobSpawn par1Packet24MobSpawn = new Packet24MobSpawn();
            par1Packet24MobSpawn.readPacketData(dataStream);
            double var2 = (double) par1Packet24MobSpawn.xPosition / 32.0;
            double var4 = (double) par1Packet24MobSpawn.yPosition / 32.0;
            double var6 = (double) par1Packet24MobSpawn.zPosition / 32.0;
            float var8 = (float) (par1Packet24MobSpawn.yaw * 360) / 256.0f;
            float var9 = (float) (par1Packet24MobSpawn.pitch * 360) / 256.0f;
            entityToSpawn.serverPosX = par1Packet24MobSpawn.xPosition;
            entityToSpawn.serverPosY = par1Packet24MobSpawn.yPosition;
            entityToSpawn.serverPosZ = par1Packet24MobSpawn.zPosition;
            entityToSpawn.rotationYawHead = (float) (par1Packet24MobSpawn.headYaw * 360) / 256.0f;
            entityToSpawn.entityId = par1Packet24MobSpawn.entityId;
            entityToSpawn.setPositionAndRotation(var2, var4, var6, var8, var9);
            entityToSpawn.motionX = (float) par1Packet24MobSpawn.velocityX / 8000.0f;
            entityToSpawn.motionY = (float) par1Packet24MobSpawn.velocityY / 8000.0f;
            entityToSpawn.motionZ = (float) par1Packet24MobSpawn.velocityZ / 8000.0f;
            List var14 = par1Packet24MobSpawn.getMetadata();
            if (var14 != null) {
                entityToSpawn.getDataWatcher().updateWatchedObjectsFromList(var14);
            }
            int timeSinceIgnited = dataStream.readInt();
            entityToSpawn.setTimeSinceIgnited(timeSinceIgnited);
            return entityToSpawn;
        });
    }
//
//    private void AddBiomeSpawn() {
//        BiomeGenBase.desert.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.extremeHills.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.forest.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.desertHills.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.extremeHillsEdge.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.forestHills.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//        BiomeGenBase.hell.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 15, 2, 2));
//        BiomeGenBase.plains.getSpawnableList(EnumCreatureType.monster).add(new SpawnListEntry(EntityFireCreeper.class, 10, 2, 2));
//    }
}