package com.itlesports.nightmaremode.AITasks;

import btw.world.util.difficulty.Difficulties;
import com.itlesports.nightmaremode.NightmareUtils;
import net.minecraft.src.*;

public class EntityAILunge extends EntityAITarget {
    private EntityLivingBase targetEntity;
    private int cooldown;


    public EntityAILunge(EntityCreature par1EntityCreature, boolean par2) {
        super(par1EntityCreature, par2);
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        if (this.taskOwner.getAttackTarget() instanceof EntityPlayer player) {
            this.targetEntity = player;
            boolean isEclipse = NightmareUtils.getIsEclipse();
            int range = isEclipse ? 50 : 30;

            return this.taskOwner.getDistanceSqToEntity(this.targetEntity) <= range  // 5.4 blocks
                    && !this.taskOwner.getNavigator().noPath()
                    && this.taskOwner.onGround
//                    && (this.taskOwner.getHeldItem() == null || isEclipse)
                    && this.taskOwner.worldObj != null // paranoid so I'm checking if it's null. a spawner should never execute this code
                    && this.taskOwner.worldObj.getDifficulty() == Difficulties.HOSTILE;
        }
        return false;
    }

    @Override
    public boolean continueExecuting(){
        boolean isHoldingItem = this.taskOwner.getHeldItem() != null;
        boolean isEclipse = NightmareUtils.getIsEclipse();

        if(isHoldingItem && !isEclipse){return false;}
        // ensures normal behavior on non-eclipse

        if (cooldown <= 0) {

            double var1 = this.targetEntity.posX - this.taskOwner.posX;
            double var2 = this.targetEntity.posZ - this.taskOwner.posZ;
            Vec3 vector = Vec3.createVectorHelper(var1, 0, var2);
            vector.normalize();
            this.taskOwner.motionX = vector.xCoord * 0.2;
            this.taskOwner.motionY = 0.34;
            this.taskOwner.motionZ = vector.zCoord * 0.2;

            if(isEclipse){
                this.cooldown = isHoldingItem ? 20 + this.taskOwner.rand.nextInt(20) : 0;
            }
            else {
                this.cooldown = 20 + this.taskOwner.rand.nextInt(20);
                // 1 second with a variance of 1s
            }
        }

        this.cooldown = Math.max(cooldown - 1, 0);
        return this.cooldown == 0 && this.taskOwner.onGround;
    }
}
