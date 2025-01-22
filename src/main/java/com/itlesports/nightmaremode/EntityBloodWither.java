package com.itlesports.nightmaremode;

import btw.block.BTWBlocks;
import btw.entity.LightningBoltEntity;
import btw.world.util.WorldUtils;
import com.itlesports.nightmaremode.block.NMBlocks;
import net.minecraft.src.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EntityBloodWither extends EntityWither {
    public EntityBloodWither(World world) {
        super(world);
        this.experienceValue = 4954;
    }
    private final float[] headPitch = new float[2]; // Represents the pitch (up/down angle) of the heads.
    private final float[] headYaw = new float[2]; // Represents the yaw (left/right angle) of the heads
    private final float[] previousHeadPitch = new float[2]; // Previous tick's pitch values for the heads
    private final float[] previousHeadYaw = new float[2]; // Previous tick's yaw values for the heads
    private final int[] headAttackCounts = new int[2]; // Counters for the number of attacks by the heads

    private int attackCycle; // used to be headAttackCooldowns. repurposed to track the cycle an attack is in. eg. the lightning attack has 3 cycles, prep, telegraph and fire
    private int shieldRegenCooldown; // Cooldown timer for regenerating the shield
    private int witherAttack; // controls which AI attack the wither should currently use
    private int passivityDuration = -1; // decides how long the wither should stay passive
    private double verticalOffset = 5.0; // the height difference between the player and the wither
    private boolean activity = false; // whether the wither should go to the sky and be passive or not
    private final List<Entity> trackedEntities = new ArrayList<>(5); // the entity we are tracking. usually a mob summoned by the wither
    private EntityPlayer playerTarget; // the player we are targetting
    private double targetX; // target's X position. used to calculate delta movement
    private double targetZ; // target's Z position. used to calculate delta movement
    private double deltaX; // target's deltaX movement
    private double deltaZ; // target's deltaZ movement
    private final double[] lightningX = {0,0,0,0,0,0,0,0,0,0}; // lightning X targets
    private final double[] lightningZ = {0,0,0,0,0,0,0,0,0,0}; // lightning Z targets
    private int[] origin; // coordinates of the origin, the center of the platform, where the wither spawned
    private long previousWorldTime; // stores the value of the world time before it was altered.
    private boolean isDoingCrystalStorm; // true if there's an ongoing crystal storm
    private ItemStack[] playerHotbar; // the player's hotbar. used for one attack
    private int currentAttackIndex; // the current attack the wither is set to do
    private int currentDurationBetweenAttacks; // the duration of the attack the wither will do
    private boolean isCurrentAttackSummoning; // true if the current attack is supposed to track entities
    private int currentAttackPassivityLength; // time that the current attack needs to spend being passive
    private static final IEntitySelector attackEntitySelector = new EntityWitherAttackFilter();
    private static final List<Integer> randomFullBlocks = new ArrayList<>(Arrays.asList(
            Block.stone.blockID,
            Block.grass.blockID,
            Block.dirt.blockID,
            Block.cobblestone.blockID,
            Block.planks.blockID,
            Block.bedrock.blockID,
            Block.sand.blockID,
            Block.gravel.blockID,
            Block.blockGold.blockID,
            Block.blockIron.blockID,
            Block.brick.blockID,
            Block.bookShelf.blockID,
            Block.cobblestoneMossy.blockID,
            Block.obsidian.blockID,
            Block.ice.blockID,
            Block.snow.blockID,
            BTWBlocks.gearBox.blockID,
            BTWBlocks.planter.blockID,
            Block.netherrack.blockID,
            Block.slowSand.blockID,
            Block.glowStone.blockID,
            Block.sponge.blockID,
            Block.blockLapis.blockID,
            Block.sandStone.blockID,
            BTWBlocks.charcoalBlock.blockID,
            Block.wood.blockID,
            BTWBlocks.hamper.blockID,
            Block.blockSnow.blockID,
            BTWBlocks.bloodWoodLog.blockID,
            Block.netherBrick.blockID,
            BTWBlocks.companionCube.blockID,
            BTWBlocks.hibachi.blockID,
            BTWBlocks.hopper.blockID,
            BTWBlocks.deepStrataStoneBrickStairs.blockID,
            BTWBlocks.workStump.blockID
    ));

    public boolean getActivity(){return this.activity;}

    private void setTargetY(double par1){
        this.verticalOffset = par1;
    }

    private void manageWitherPassivity(boolean isTrackingEntity){
        if (isTrackingEntity) {
            this.disengageWither();
            for(int i = 0; i < this.trackedEntities.size(); i++){
                if(this.trackedEntities.get(i).isDead){
                    this.trackedEntities.remove(i);
                    break;
                }
            }
            if(this.isDoingCrystalStorm) {
                if(this.trackedEntities.size() < 30 && this.passivityDuration > 50){
                    EntityFallingChicken chicken = new EntityFallingChicken(this.worldObj);
                    chicken.setPositionAndUpdate(this.origin[0] + (this.rand.nextBoolean() ? -1 : 1 ) * (this.rand.nextInt(28) + 2), 210 + (this.rand.nextInt(30) * 1.66), this.origin[2] + (this.rand.nextBoolean() ? -1 : 1 ) * (this.rand.nextInt(28)) + 2);
                    EntityEnderCrystal crystal = new EntityEnderCrystal(this.worldObj);
                    crystal.copyLocationAndAnglesFrom(chicken);
                    chicken.clearActivePotions();
                    chicken.addPotionEffect(new PotionEffect(Potion.invisibility.id, Integer.MAX_VALUE,0));
                    chicken.noClip = true;
                    this.worldObj.spawnEntityInWorld(crystal);
                    this.trackedEntities.add(chicken);
                    this.worldObj.spawnEntityInWorld(chicken);
                    crystal.mountEntity(chicken);
                }
                boolean shouldClear = this.passivityDuration <= 50;

                Iterator<Entity> iterator = this.trackedEntities.iterator();
                while (iterator.hasNext()) {
                    Entity trackedEntity = iterator.next();

                    if (shouldClear) {
                        if (trackedEntity.riddenByEntity != null) {
                            trackedEntity.riddenByEntity.attackEntityFrom(DamageSource.generic, 10f);
                        }
                        trackedEntity.setDead();
                        continue; // Skip further processing if clearing is needed
                    }
                    if (trackedEntity.posY <= 200 || trackedEntity.isDead) {
                        if (trackedEntity.riddenByEntity == null) {
                            trackedEntity.setDead();
                        } else {
                            trackedEntity.riddenByEntity.attackEntityFrom(DamageSource.generic, 10f);
                        }
                        iterator.remove(); // Remove entity safely
                    }
                }

                if (shouldClear) {
                    this.trackedEntities.clear();
                }
                if (this.passivityDuration > 0) {
                    this.setWitherPassiveFor(this.passivityDuration - 1);
                    this.disengageWither();
                } else {
                    this.engageWither();
                }
            }
            if(this.trackedEntities.isEmpty()){
                this.engageWither();
            }
        } else {
            if (this.passivityDuration > 0) {
                this.setWitherPassiveFor(this.passivityDuration - 1);
                this.disengageWither();
            } else {
                this.engageWither();
            }
        }
    }

    private void engageWither(){
        this.witherAttack += 1;
        this.passivityDuration = -1;
        this.activity = true;
        this.setInvisible(false);
        this.setTargetY(5d);
        if(this.previousWorldTime != 0){
            this.worldObj.setWorldTime(this.previousWorldTime);
            this.previousWorldTime = 0;
        }
    }
    private void disengageWither(){
        this.activity = false;
        this.setInvisible(true);
        this.setTargetY(10d);
    }
    private void setWitherPassiveFor(int par1){
        this.passivityDuration = par1;
    }

    @Override
    protected boolean isAIEnabled() {
        return this.activity;
    }

    private void updatePlayerMovementDeltas(EntityPlayer player){
        if(player.ticksExisted % 2 == 0){
            this.targetX = player.posX;
            this.targetZ = player.posZ;
        } else {
            this.deltaX = player.posX - this.targetX;
            this.deltaZ = player.posZ - this.targetZ;
        }
        // updates this.deltaX and this.deltaZ (which refer to the player's movement deltas) every 2nd tick. the values are reliable and can be used for predictive AI
    }



    public float func_82210_r(int par1) {
        return this.headPitch[par1];
    }
    public float func_82207_a(int par1) {
        return this.headYaw[par1];
    }


    private void executeAttack(int index, int delayBetweenAttacks){
        EntityPlayer player = this.playerTarget;
        if (player != null) {
            this.updatePlayerMovementDeltas(player);
            double x,y,z;
            boolean shouldCheck = this.ticksExisted % delayBetweenAttacks == 0 || delayBetweenAttacks == -1;
            if (shouldCheck) {
                Vec3 deltaMovement = Vec3.createVectorHelper(this.deltaX, 0, this.deltaZ);
                deltaMovement.normalize();

                switch (index){
                    case 0: // suffocation attack
                        for (int i = -1; i < 2; i++) {
                            for (int j = -1; j < 2; j++) {
                                x = player.posX + i + this.rand.nextInt(4);
                                y = player.posY + 20;
                                z = player.posZ + j + this.rand.nextInt(4);
                                // time to fall (in ticks) is sqrt(2 * 400 * y / 16), which is approximately 5.31.62277660168379 in this case

                                this.worldObj.setBlock((int)(x + deltaMovement.xCoord * 31.62277660168379), (int)y, (int)(z + deltaMovement.zCoord * 31.62277660168379), this.rand.nextBoolean() ? Block.sand.blockID : Block.gravel.blockID);
                                this.worldObj.setBlock((int)(x + deltaMovement.xCoord * 31.62277660168379), (int)y + 1, (int)(z + deltaMovement.zCoord * 31.62277660168379), this.rand.nextBoolean() ? Block.sand.blockID : Block.gravel.blockID);
                            }
                        }
                        break;
                    case 1: // tnt rain
                        x = player.posX + rand.nextInt(6);
                        y =  8 + rand.nextInt(10);
                        z = player.posZ + rand.nextInt(6);
                        int fuse = (int) Math.sqrt(800 * y / 16);

                        EntityTNTPrimed missile = new EntityTNTPrimed(this.worldObj,x + deltaMovement.xCoord * fuse,player.posY + y, z + deltaMovement.zCoord * fuse);
                        missile.fuse = fuse;
                        this.worldObj.spawnEntityInWorld(missile);
                        break;
                    case 2:
                        int lightningSpread = 5;
                        this.attackCycle += 1;
                        // lightning strikes
                        for (int i = 0; i < 10; i++) {
                            if(this.lightningX[i] == 0 || this.lightningZ[i] == 0){

                                this.lightningX[i] = this.origin[0] + (this.rand.nextBoolean() ? -1 : 1) * (3 + this.rand.nextInt(25));
                                this.lightningZ[i] = this.origin[2] + (this.rand.nextBoolean() ? -1 : 1) * (3 + this.rand.nextInt(25));
                            } else {
                                if (this.attackCycle % 4 == 3) {
                                    Entity lightningbolt = new LightningBoltEntity(this.worldObj, this.lightningX[i], 200, this.lightningZ[i]);
                                    this.worldObj.addWeatherEffect(lightningbolt);
                                } else if(this.attackCycle % 4 == 0) {
                                    Entity scatteredBolt0 = new LightningBoltEntity(this.worldObj, this.lightningX[i] + lightningSpread, 200, this.lightningZ[i] + lightningSpread);
                                    Entity scatteredBolt1 = new LightningBoltEntity(this.worldObj, this.lightningX[i] + lightningSpread, 200, this.lightningZ[i] - lightningSpread);
                                    Entity scatteredBolt2 = new LightningBoltEntity(this.worldObj, this.lightningX[i] - lightningSpread, 200, this.lightningZ[i] + lightningSpread);
                                    Entity scatteredBolt3 = new LightningBoltEntity(this.worldObj, this.lightningX[i] - lightningSpread, 200, this.lightningZ[i] - lightningSpread);
                                    this.worldObj.addWeatherEffect(scatteredBolt0);
                                    this.worldObj.addWeatherEffect(scatteredBolt1);
                                    this.worldObj.addWeatherEffect(scatteredBolt2);
                                    this.worldObj.addWeatherEffect(scatteredBolt3);
                                    this.lightningX[i] = this.lightningZ[i] = 0;
                                } else {
                                    for (int j = 2; j < 10; j++) {
                                        this.worldObj.newExplosion(this, this.lightningX[i], 205 + (j * 2), this.lightningZ[i], 1.75f, false, false);
                                    }
                                }
                            }
                        }
                        break;
                    case 3:
                        // standard horde
                        for(int i = 0; i < 12; i ++){
                            EntityLiving mobToSpawn;
                            int j = this.rand.nextInt(64);

                            mobToSpawn = switch (j) {
                                case  0,  1,  2,  3,  4,  5,  6,  7,  8,  9 -> new EntityZombie(this.worldObj);        // 10 occurrences
                                case 10, 11, 12, 13, 14, 15, 16, 17 -> new EntitySkeleton(this.worldObj);              // 8  occurrences
                                case 18, 19, 20, 21, 22, 23, 24 -> new EntityCreeper(this.worldObj);                   // 7  occurrences
                                case 25, 26, 27, 28, 29, 30 -> new EntitySpider(this.worldObj);                        // 6  occurrences
                                case 31, 32 -> new EntityEnderman(this.worldObj);                                      // 2  occurrences
                                case 33 -> new EntityWitch(this.worldObj);                                             // 1  occurrence
                                case 34, 35 -> new EntityBlaze(this.worldObj);                                         // 2  occurrences
                                case 36, 37, 38, 39 -> new EntitySlime(this.worldObj);                                 // 4  occurrences
                                case 40, 41, 42, 43 -> new EntityMagmaCube(this.worldObj);                             // 4  occurrences
                                case 44, 45, 46, 47, 48, 49 -> new EntityShadowZombie(this.worldObj);                  // 6  occurrences
                                case 50, 51, 52, 53, 54, 55, 56 -> new EntityFireCreeper(this.worldObj);               // 7  occurrences
                                case 57, 58, 59, 60, 61 -> new EntitySilverfish(this.worldObj);                        // 5  occurrences
                                case 62, 63 -> new EntityPigZombie(this.worldObj);                                     // 2  occurrences
                                default -> new EntityZombie(this.worldObj); // Fallback in case of unexpected input
                            };
                            mobToSpawn.setPositionAndUpdate(this.origin[0] + this.rand.nextInt(20), 200, this.origin[2] + this.rand.nextInt(20));
                            mobToSpawn.setAttackTarget(player);
                            this.worldObj.spawnEntityInWorld(mobToSpawn);
                        }

                        break;
                    case 4:
                        // falling crystals
                        this.isDoingCrystalStorm = this.passivityDuration > 0;
                        break;
//                    case 5:
//                        // launch player into the air and scramble their inventory
//                        this.attackCycle += 1;
//                        if(this.passivityDuration > 40) {
//                            if (player.onGround && this.attackCycle % 4 == 1) {
//                                this.worldObj.playSoundEffect(player.posX + (double) 0.5F, player.posY + (double) 0.5F, player.posZ + (double) 0.5F, "note.harp", 3.0F, 1);
//                            }
//                            if (player.onGround && this.attackCycle % 4 == 2) {
//                                player.setPositionAndUpdate(player.posX, player.posY + 30, player.posZ);
//                                player.motionY = 1;
//                                if (this.playerHotbar == null) {
//                                    this.playerHotbar = new ItemStack[9];
//                                    System.arraycopy(player.inventory.mainInventory, 0, this.playerHotbar, 0, 9);
//                                    System.arraycopy(this.getRandomItem(), 0, player.inventory.mainInventory, 0, 9);
//                                    player.inventory.mainInventory[this.rand.nextInt(9)] = new ItemStack(Item.bucketWater);
//                                }
//                                player.worldObj.playSoundEffect(player.posX, player.posY + 2, player.posZ, "random.explode", 4.0F, (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);
//                                this.playerHotbar = null;
//                            }
//                        }
//                        else if (this.attackCycle % 4 == 0){
//                            System.arraycopy(this.playerHotbar, 0, player.inventory.mainInventory, 0, 9);
//                            this.playerHotbar = null;
//                        }
//                        break;
                    case 5:
                        if(this.trackedEntities.isEmpty()){
                            EntityDragon dragon = new EntityDragon(this.worldObj);
                            dragon.setPositionAndUpdate(this.origin[0],this.origin[1] + 30, this.origin[2]);
                            this.trackedEntities.add(dragon);
                            this.worldObj.spawnEntityInWorld(dragon);
                        }
                        break;
                    case 6:
                        if(this.trackedEntities.isEmpty()){
                            EntityBlaze blaze0 = new EntityBlaze(this.worldObj);
                            blaze0.clearActivePotions();
                            blaze0.addPotionEffect(new PotionEffect(Potion.field_76443_y.id,1000000,0));
                            blaze0.setPositionAndUpdate(this.origin[0] + 5, 205, this.origin[2]);

                            EntityBlaze blaze1 = new EntityBlaze(this.worldObj);
                            blaze1.clearActivePotions();
                            blaze1.addPotionEffect(new PotionEffect(Potion.field_76443_y.id,1000000,0));
                            blaze1.addPotionEffect(new PotionEffect(Potion.waterBreathing.id,1000000,0));
                            blaze1.setPositionAndUpdate(this.origin[0], 205, this.origin[2] + 5);

                            EntityBlaze blaze2 = new EntityBlaze(this.worldObj);
                            blaze2.clearActivePotions();
                            blaze2.addPotionEffect(new PotionEffect(Potion.invisibility.id,1000000,0));
                            blaze2.setPositionAndUpdate(this.origin[0], 205, this.origin[2]);

                            EntityBlaze blaze3 = new EntityBlaze(this.worldObj);
                            blaze3.clearActivePotions();
                            blaze3.setPositionAndUpdate(this.origin[0] - 5, 210, this.origin[2]);

                            EntityBlaze blaze4 = new EntityBlaze(this.worldObj);
                            blaze4.clearActivePotions();
                            blaze4.addPotionEffect(new PotionEffect(Potion.field_76443_y.id,1000000,0));
                            blaze4.setPositionAndUpdate(this.origin[0], 205, this.origin[2] - 5);

                            this.trackedEntities.add(blaze0);
                            this.trackedEntities.add(blaze1);
                            this.trackedEntities.add(blaze2);
                            this.trackedEntities.add(blaze3);
                            this.trackedEntities.add(blaze4);
                            this.worldObj.spawnEntityInWorld(blaze0);
                            this.worldObj.spawnEntityInWorld(blaze1);
                            this.worldObj.spawnEntityInWorld(blaze2);
                            this.worldObj.spawnEntityInWorld(blaze3);
                            this.worldObj.spawnEntityInWorld(blaze4);
                        }
                }
            }
        }
    }


    @Override
    public void onLivingUpdate() {
        int particleIndex;
        int headIndex;
        double targetDistanceSquared;
        double deltaX;
        double deltaZ;
        EntityPlayer primaryTarget;

        this.motionY *= 0.6f;

        // Adjust motion towards the primary target
        if(this.playerTarget == null){
            this.playerTarget = this.worldObj.getClosestVulnerablePlayerToEntity(this,40);
        }

        // tudou
        if(!this.worldObj.isRemote && this.playerTarget != null && this.ticksExisted % 400 == 399 && this.passivityDuration == -1){
            this.currentAttackIndex = this.rand.nextInt(7);
            this.setAttackDetails(this.currentAttackIndex);
        }

        // all the attacks
        // summon attacks: executeAttack -> manageWitherPassivity
        // regular attacks: manageWitherPassivity -> executeAttack
        if (!this.worldObj.isRemote && this.playerTarget != null) {
            primaryTarget = this.playerTarget;
            if(this.witherAttack == 600) {
                // suffocation attack

                primaryTarget.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 300,0));
                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(300);
                }
                this.manageWitherPassivity(false);
                this.executeAttack(0, 8);
            }
            else if(this.witherAttack == 900){
                // tnt rain

                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(300);
                }
                this.manageWitherPassivity(false);
                this.executeAttack(1, 12);
            }
            else if(this.witherAttack == 1400){
                // lightning strikes
                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(600);
                }
                this.manageWitherPassivity(false);
                this.executeAttack(2, 20 + 1);
            }
            else if(this.witherAttack == 2300){
                // dragon summon
                this.executeAttack(5,-1);
                this.manageWitherPassivity(true);
            }
            else if(this.witherAttack == 2500){
                // blaze summon
                this.executeAttack(6,-1);
                this.manageWitherPassivity(true);
            }
            else if (this.witherAttack == 2700){
                // set blood moon and summon mobs
                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(600);
                }
                this.manageWitherPassivity(false);
                this.executeAttack(3, 300);
                if(!NightmareUtils.getIsBloodMoon()){
                    this.previousWorldTime = this.worldObj.getWorldTime();
                    this.worldObj.setWorldTime(getNextBloodMoonTime(this.worldObj.getWorldTime()));
                }
            }
            else if (this.witherAttack == 3000){
                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(600);
                }
                this.manageWitherPassivity(false);
                this.executeAttack(3, 400);
                if(!NightmareUtils.getIsEclipse()){
                    this.previousWorldTime = this.worldObj.getWorldTime();
                    this.worldObj.setWorldTime(getNextEclipse(this.worldObj.getWorldTime()));
                }
            }
            else if (this.witherAttack == 3200){
                if (this.passivityDuration == -1) {
                    this.setWitherPassiveFor(1000);
                }
                this.executeAttack(4, -1);
                this.manageWitherPassivity(true);
            }
            else{
                this.witherAttack = Math.min(this.witherAttack + 1, 4000);
                this.activity = true;
                this.attackCycle = 0;
            }


            if (this.getHealthTimer() <= 0) {
                if (this.posY < primaryTarget.posY || (!this.isArmored() && this.posY < primaryTarget.posY + this.verticalOffset)) {
                    if (this.motionY < 0.0) {
                        this.motionY = 0.0;
                    }
                    this.motionY += (0.5 - this.motionY) * 0.6f;
                }
                deltaX = primaryTarget.posX - this.posX;
                deltaZ = primaryTarget.posZ - this.posZ;
                targetDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                if (targetDistanceSquared > 9.0 && this.activity) {
                    double distance = MathHelper.sqrt_double(targetDistanceSquared);
                    this.motionX += (deltaX / distance * 0.5 - this.motionX) * 0.6f;
                    this.motionZ += (deltaZ / distance * 0.5 - this.motionZ) * 0.6f;
                }
                if(targetDistanceSquared > 1000 && !this.activity && this.trackedEntities.isEmpty()){
                    this.setPositionAndUpdate(primaryTarget.posX,210, primaryTarget.posZ);
                }
                if(!this.trackedEntities.isEmpty()){
                    deltaX = this.origin[0] - this.posX;
                    deltaZ = this.origin[2] - this.posZ;
                    targetDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                    if (targetDistanceSquared > 4.0) {
                        double distance = MathHelper.sqrt_double(targetDistanceSquared);
                        this.motionX += (deltaX / distance * 0.5 - this.motionX) * 0.2f;
                        this.motionZ += (deltaZ / distance * 0.5 - this.motionZ) * 0.2f;
                    }
                }
            }
        }

        // Adjust rotation based on movement
        if (this.motionX * this.motionX + this.motionZ * this.motionZ > 0.05f) {
            this.rotationYaw = (float) Math.atan2(this.motionZ, this.motionX) * 57.295776f - 90.0f;
        }

        super.onLivingUpdate();

        // Update head tracking for each head
        for (headIndex = 0; headIndex < 2; ++headIndex) {
            this.previousHeadYaw[headIndex] = this.headYaw[headIndex];
            this.previousHeadPitch[headIndex] = this.headPitch[headIndex];
        }

        for (headIndex = 0; headIndex < 2; ++headIndex) {
            int targetId = this.getWatchedTargetId(headIndex + 1);
            Entity headTarget = targetId > 0 ? this.worldObj.getEntityByID(targetId) : null;

            if (headTarget != null) {
                double headTargetX = this.getHeadX(headIndex + 1);
                double headTargetY = this.getHeadY(headIndex + 1);
                double headTargetZ = this.getHeadZ(headIndex + 1);

                double deltaTargetX = headTarget.posX - headTargetX;
                double deltaTargetY = headTarget.posY + headTarget.getEyeHeight() - headTargetY;
                double deltaTargetZ = headTarget.posZ - headTargetZ;

                double horizontalDistance = MathHelper.sqrt_double(deltaTargetX * deltaTargetX + deltaTargetZ * deltaTargetZ);
                float yaw = (float) (Math.atan2(deltaTargetZ, deltaTargetX) * 180.0 / Math.PI) - 90.0f;
                float pitch = (float) -(Math.atan2(deltaTargetY, horizontalDistance) * 180.0 / Math.PI);

                this.headPitch[headIndex] = this.clampAngle(this.headPitch[headIndex], pitch, 40.0f);
                this.headYaw[headIndex] = this.clampAngle(this.headYaw[headIndex], yaw, 10.0f);
            } else {
                this.headYaw[headIndex] = this.clampAngle(this.headYaw[headIndex], this.renderYawOffset, 10.0f);
            }
        }

        // Generate particles based on state
        boolean isShielded = this.isArmored();
        for (particleIndex = 0; particleIndex < 3; ++particleIndex) {
            double headX = this.getHeadX(particleIndex);
            double headY = this.getHeadY(particleIndex);
            double headZ = this.getHeadZ(particleIndex);

            this.worldObj.spawnParticle("smoke", headX + this.rand.nextGaussian() * 0.3, headY + this.rand.nextGaussian() * 0.3, headZ + this.rand.nextGaussian() * 0.3, 0.0, 0.0, 0.0);

            if (isShielded && this.rand.nextInt(4) == 0) {
                this.worldObj.spawnParticle("mobSpell", headX + this.rand.nextGaussian() * 0.3, headY + this.rand.nextGaussian() * 0.3, headZ + this.rand.nextGaussian() * 0.3, 0.7f, 0.7f, 0.5);
            }
        }

        if (this.getInvulnerabilityTime() > 0) {
            for (particleIndex = 0; particleIndex < 3; ++particleIndex) {
                this.worldObj.spawnParticle("mobSpell", this.posX + this.rand.nextGaussian(), this.posY + this.rand.nextFloat() * 3.3, this.posZ + this.rand.nextGaussian(), 0.7f, 0.7f, 0.9f);
            }
        }
    }

    public int getInvulnerabilityTime() {
        return this.dataWatcher.getWatchableObjectInt(20);
    }

    private float clampAngle(float par1, float par2, float par3) {
        float var4 = MathHelper.wrapAngleTo180_float(par2 - par1);
        if (var4 > par3) {
            var4 = par3;
        }
        if (var4 < -par3) {
            var4 = -par3;
        }
        return par1 + var4;
    }

    private double getHeadX(int headIndex) {
        if (headIndex <= 0) {
            return this.posX; // Return the central position for the main body
        }
        float angleOffset = (this.renderYawOffset + (180 * (headIndex - 1))) / 180.0f * (float) Math.PI;
        float offsetX = MathHelper.cos(angleOffset); // Calculate X offset
        return this.posX + offsetX * 1.3; // Add offset to the X position
    }

    private double getHeadY(int headIndex) {
        return headIndex <= 0 ? this.posY + 3.0 : this.posY + 2.2; // Return different heights based on index
    }

    private double getHeadZ(int headIndex) {
        if (headIndex <= 0) {
            return this.posZ; // Return the central position for the main body
        }
        float angleOffset = (this.renderYawOffset + (180 * (headIndex - 1))) / 180.0f * (float) Math.PI;
        float offsetZ = MathHelper.sin(angleOffset); // Calculate Z offset
        return this.posZ + offsetZ * 1.3; // Add offset to the Z position
    }
    public int getHealthTimer() {
        return this.dataWatcher.getWatchableObjectInt(20);
    }
    public void setHealthTimer(int par1) {
        this.dataWatcher.updateObject(20, par1);
    }
    private double getHeadXPositionOffset(int par1) {
        if (par1 <= 0) {
            return this.posX;
        }
        float var2 = (this.renderYawOffset + (float)(180 * (par1 - 1))) / 180.0f * (float)Math.PI;
        float var3 = MathHelper.cos(var2);
        return this.posX + (double)var3 * 1.3;
    }

    private double getHeadYPositionOffset(int headIndex) {
        return headIndex <= 0 ? this.posY + 3.0 : this.posY + 2.2;
    }

    private double getHeadZPositionOffset(int headIndex) {
        if (headIndex <= 0) {
            return this.posZ;
        }
        float angle = (this.renderYawOffset + (float)(180 * (headIndex - 1))) / 180.0f * (float)Math.PI;
        float sinAngle = MathHelper.sin(angle);
        return this.posZ + (double)sinAngle * 1.3;
    }

    private void spawnEntity(int headIndex, double targetX, double targetY, double targetZ, boolean isInvulnerable) {
        this.worldObj.playAuxSFXAtEntity(null, 1014, (int)this.posX, (int)this.posY, (int)this.posZ, 0);
        double offsetX = this.getHeadXPositionOffset(headIndex);
        double offsetY = this.getHeadYPositionOffset(headIndex);
        double offsetZ = this.getHeadZPositionOffset(headIndex);
        double deltaX = targetX - offsetX;
        double deltaY = targetY - offsetY;
        double deltaZ = targetZ - offsetZ;
        EntityWitherSkull witherSkull = new EntityWitherSkull(this.worldObj, this, deltaX, deltaY, deltaZ);
        if (isInvulnerable) {
            witherSkull.setInvulnerable(true);
        }
        witherSkull.posY = offsetY;
        witherSkull.posX = offsetX;
        witherSkull.posZ = offsetZ;
        this.worldObj.spawnEntityInWorld(witherSkull);
    }

    private void setTargetButInsteadItJustShoots(int par1, EntityLivingBase par2EntityLivingBase) {
        this.spawnEntity(par1, par2EntityLivingBase.posX, par2EntityLivingBase.posY + (double)par2EntityLivingBase.getEyeHeight() * 0.5, par2EntityLivingBase.posZ, par1 == 0 && this.rand.nextFloat() < 0.001f);
    }

    public void setTargetId(int par1, int par2) {
        this.dataWatcher.updateObject(17 + par1, par2);
    }

    private static void placeBlocksAtLinePartial(World world, int x, int y, int z, int line){
        int bonus = (line % 3) * 20;
        for(int i = bonus; i < 20 + bonus; i++) {
            int blockID = world.rand.nextBoolean() ? NMBlocks.specialObsidian.blockID : NMBlocks.cryingObsidian.blockID;
            world.setBlock(x + i, y, z - MathHelper.floor_double((double) line / 3), blockID);
        }
    }

    @Override
    protected void updateAITasks() {
        if (this.getHealthTimer() > 0) {
            int healthTimer = this.getHealthTimer() - 1;
            // builds the platform of the wither arena, in a lag efficient way

            // places 1 line every tick
//            if(isBetween(healthTimer,80,400)){
//                int line = healthTimer - 80;
//                // healthTimer holds a value between 0 and 300
//                placeBlocksAtLine(this.worldObj, (int) this.posX - 30, (int) this.posY - 1, (int) this.posZ + 30, line);
//            }

            // places one line every 2 ticks
            if(isBetween(healthTimer,40,400)){
                int line = healthTimer - 40;
                // healthTimer holds a value between 0 and 360
                if (line % 2 == 0) {
                    placeBlocksAtLinePartial(this.worldObj, (int) this.posX - 30, (int) this.posY - 1, (int) this.posZ + 30, MathHelper.floor_double((double) line / 2));
                }
            }

            if(healthTimer == 30){
                Entity lightningbolt = new LightningBoltEntity(this.worldObj, this.posX, this.posY + 1, this.posZ);
                this.worldObj.addWeatherEffect(lightningbolt);
            } else if(healthTimer == 15){
                LightningBoltEntity lightningbolt1 = new LightningBoltEntity(this.worldObj, this.posX + 3, this.posY + 1, this.posZ);
                LightningBoltEntity lightningbolt2 = new LightningBoltEntity(this.worldObj, this.posX - 3, this.posY + 1, this.posZ);
                LightningBoltEntity lightningbolt3 = new LightningBoltEntity(this.worldObj, this.posX, this.posY + 1, this.posZ + 3);
                LightningBoltEntity lightningbolt4 = new LightningBoltEntity(this.worldObj, this.posX, this.posY + 1, this.posZ - 3);

                this.worldObj.addWeatherEffect(lightningbolt1);
                this.worldObj.addWeatherEffect(lightningbolt2);
                this.worldObj.addWeatherEffect(lightningbolt3);
                this.worldObj.addWeatherEffect(lightningbolt4);
            }
            if (healthTimer <= 0) {
                this.worldObj.newExplosion(this, this.posX, this.posY + this.getEyeHeight(), this.posZ, 7.0f, true, this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"));
                this.worldObj.func_82739_e(1013, (int)this.posX, (int)this.posY, (int)this.posZ, 0);
                this.activity = true;
            }
            this.setHealthTimer(healthTimer);
            if (this.ticksExisted % 10 == 0) {
                this.heal(10.0f);
            }
        } else {
            int targetId;
            super.updateAITasks();
            // technically one of these fields should be replaced with field_82223_h which is headAttackCooldowns, but I'm keeping it like this
            for (int i = 1; i < 3; ++i) {
                if (this.ticksExisted < this.headPitch[i - 1]) continue;
                this.headPitch[i - 1] = this.ticksExisted + 10 + this.rand.nextInt(10);
                int previousAttackCount = this.headAttackCounts[i - 1];
                this.headAttackCounts[i - 1] = previousAttackCount + 1;
                if (previousAttackCount > 15) {
                    float rangeX = 10.0f;
                    float rangeY = 5.0f;
                    double posX = MathHelper.getRandomDoubleInRange(this.rand, this.posX - rangeX, this.posX + rangeX);
                    double posY = MathHelper.getRandomDoubleInRange(this.rand, this.posY - rangeY, this.posY + rangeY);
                    double posZ = MathHelper.getRandomDoubleInRange(this.rand, this.posZ - rangeX, this.posZ + rangeX);
                    this.spawnEntity(i + 1, posX, posY, posZ, true);
                    this.headAttackCounts[i - 1] = 0;
                }
                if ((targetId = this.getWatchedTargetId(i)) > 0) {
                    Entity targetEntity = this.worldObj.getEntityByID(targetId) == null ? this.worldObj.getClosestVulnerablePlayerToEntity(this,30) : this.worldObj.getEntityByID(targetId);
                    if (targetEntity != null && targetEntity.isEntityAlive() && this.getDistanceSqToEntity(targetEntity) <= 900.0 && this.canEntityBeSeen(targetEntity)) {
                        this.setTargetButInsteadItJustShoots(i + 1, (EntityLivingBase)targetEntity);
                        this.headPitch[i - 1] = this.ticksExisted + 40 + this.rand.nextInt(20);
                        this.headAttackCounts[i - 1] = 0;
                        continue;
                    }
                    this.setTargetId(i, 0);
                    continue;
                }
                List<EntityPlayer> nearbyEntities = this.worldObj.selectEntitiesWithinAABB(EntityPlayer.class, this.boundingBox.expand(20.0, 8.0, 20.0), attackEntitySelector);
                for (int j = 0; j < 10 && !nearbyEntities.isEmpty(); ++j) {
                    EntityPlayer entity = nearbyEntities.get(this.rand.nextInt(nearbyEntities.size()));
                    if (entity.isEntityAlive() && this.canEntityBeSeen(entity)) {
                        if (entity.capabilities.disableDamage) continue;
                        this.entityToAttack = entity;
                        this.setTargetId(i, entity.entityId);
                        continue;
                    }
                    nearbyEntities.remove(entity);
                }
            }

            if (this.entityToAttack != null) {
                this.setTargetId(0, this.entityToAttack.entityId);
            } else {
                this.setTargetId(0, 0);
            }

            if (this.shieldRegenCooldown > 0) {
                --this.shieldRegenCooldown;
                if (this.shieldRegenCooldown == 0 && this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
                    int floorPosY = MathHelper.floor_double(this.posY);
                    int floorPosX = MathHelper.floor_double(this.posX);
                    int floorPosZ = MathHelper.floor_double(this.posZ);
                    boolean blockDestroyed = false;
                    for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                        for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                            for (int yOffset = 0; yOffset <= 3; ++yOffset) {
                                int blockX = floorPosX + xOffset;
                                int blockY = floorPosY + yOffset;
                                int blockZ = floorPosZ + zOffset;
                                int blockId = this.worldObj.getBlockId(blockX, blockY, blockZ);
                                if (blockId <= 0 || blockId == Block.bedrock.blockID || blockId == Block.endPortal.blockID || blockId == Block.endPortalFrame.blockID || blockId == BTWBlocks.soulforgedSteelBlock.blockID) continue;
                                blockDestroyed = this.worldObj.destroyBlock(blockX, blockY, blockZ, true) || blockDestroyed;
                            }
                        }
                    }
                    if (blockDestroyed) {
                        this.worldObj.playAuxSFXAtEntity(null, 1012, (int) this.posX, (int) this.posY, (int) this.posZ, 0);
                    }
                }
            }
            if (this.ticksExisted % 20 == 0) {
                this.heal(1.0f);
            }
        }
    }

    private static boolean isBetween(int num, int min, int max){
        return num >= min && num <= max;
    }

    public static void summonWitherAtLocation(World world, int x, int z) {
        EntityBloodWither wither = new EntityBloodWither(world);
        wither.func_82206_m();
        world.playAuxSFX(2279, x, 200, z, 0);
        WorldUtils.gameProgressSetEndDimensionHasBeenAccessedServerOnly();
        wither.setLocationAndAngles(x + 0.25, 200, (double)z + 0.5, 0.0f, 0.0f);

        wither.origin = new int[]{x, 200, z};
        world.spawnEntityInWorld(wither);
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return this.activity && super.attackEntityFrom(par1DamageSource, par2);
    }

    @Override
    public void func_82206_m() {
        this.setHealthTimer(400);
        this.setHealth(this.getMaxHealth() / 16.0F);
    }

    private static long getNextBloodMoonTime(long currentTime) {
        int currentDay = (int) Math.ceil((double) currentTime / 24000);

        // Find the next day that satisfies the blood moon cycle (day % 16 == 9)
        int nextBloodMoonDay = currentDay + (15 - (currentDay % 16) + 9) % 16;

        // Convert back to ticks and set the time to 18000 (nighttime)
        return (nextBloodMoonDay * 24000L) + 18000;
    }
    private static long getNextEclipse(long currentTime) {
        return ((currentTime / 24000) + 1) * 24000 + 5000;
    }
    private ItemStack[] getRandomItem(){
        return new ItemStack[]{
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0),
                new ItemStack(randomFullBlocks.get(this.rand.nextInt(randomFullBlocks.size())), 1, 0)
        };
    }
    private static boolean areItemStackArraysEqual(ItemStack[] array1, ItemStack[] array2) {
        // Check if both arrays are null or have the same reference
        if (array1 == array2) {
            return true;
        }

        // Check if either array is null or lengths are different
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }

        // Compare each item stack in both arrays
        for (int i = 0; i < array1.length; i++) {
            if (!ItemStack.areItemStacksEqual(array1[i], array2[i])) {
                return false;
            }
        }
        return true;
    }
    private void setAttackDetails(int index) {
        switch (index) {
            case 0: // Suffocation attack
                this.currentDurationBetweenAttacks = 8;
                this.isCurrentAttackSummoning = false;
                this.currentAttackPassivityLength = 300;
                break;
            case 1: // TNT rain
                this.currentDurationBetweenAttacks = 12;
                this.isCurrentAttackSummoning = false;
                this.currentAttackPassivityLength = 300;
                break;
            case 2: // Lightning strikes
                this.currentDurationBetweenAttacks = 21;
                this.isCurrentAttackSummoning = false;
                this.currentAttackPassivityLength = 600;
                break;
            case 3: // Blood moon and mob summon
                this.currentDurationBetweenAttacks = 300;
                this.isCurrentAttackSummoning = false;
                this.currentAttackPassivityLength = 600;
                break;
            case 4: // Eclipse event
                this.currentDurationBetweenAttacks = 400;
                this.isCurrentAttackSummoning = false;
                this.currentAttackPassivityLength = 600;
                break;
            case 5: // Dragon summon
                this.currentDurationBetweenAttacks = -1;
                this.isCurrentAttackSummoning = true;
                this.currentAttackPassivityLength = 0; // No passivity duration set
                break;
            case 6: // Blaze summon
                this.currentDurationBetweenAttacks = -1;
                this.isCurrentAttackSummoning = true;
                this.currentAttackPassivityLength = 0; // No passivity duration set
                break;
            case 7: // Special case, passive for longer
                this.currentDurationBetweenAttacks = -1;
                this.isCurrentAttackSummoning = true;
                this.currentAttackPassivityLength = 1000;
                break;
            default:
                throw new IllegalArgumentException("Invalid attack index: " + index);
        }
    }
}
