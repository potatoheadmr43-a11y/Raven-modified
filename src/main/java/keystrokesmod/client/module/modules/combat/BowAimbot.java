package keystrokesmod.client.module.modules.combat;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.client.Targets;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class BowAimbot extends Module {
    public static SliderSetting fov, distance;
    public static TickSetting predict;

    public BowAimbot() {
        super("BowAimbot", ModuleCategory.combat);
        this.registerSetting(fov = new SliderSetting("FOV", 90.0D, 10.0D, 360.0D, 1.0D));
        this.registerSetting(distance = new SliderSetting("Distance", 40.0D, 5.0D, 100.0D, 1.0D));
        this.registerSetting(predict = new TickSetting("Predict", true));
    }

    @Subscribe
    public void onRender(ForgeEvent fe) {
        if (!(fe.getEvent() instanceof RenderWorldLastEvent)) return;
        if (!Utils.Player.isPlayerInGame()) return;

        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && mc.thePlayer.isUsingItem()) {
            EntityLivingBase target = Targets.getTarget() instanceof EntityLivingBase ? (EntityLivingBase) Targets.getTarget() : null;
            if (target == null) target = Utils.Player.getClosestPlayer(distance.getInput());

            if (target != null && Utils.Player.fov(target, (float) fov.getInput())) {
                float[] rots = getBowRotations(target);
                if (rots != null) {
                    mc.thePlayer.rotationYaw = rots[0];
                    mc.thePlayer.rotationPitch = rots[1];
                }
            }
        }
    }

    private float[] getBowRotations(Entity target) {
        double x = target.posX - mc.thePlayer.posX;
        double z = target.posZ - mc.thePlayer.posZ;
        double y = target.posY + target.getEyeHeight() - 0.1 - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();

        if (predict.isToggled()) {
            double bowPower = (mc.thePlayer.getItemInUseDuration()) / 20.0;
            bowPower = (bowPower * bowPower + bowPower * 2.0) / 3.0;
            if (bowPower > 1.0) bowPower = 1.0;
            
            double dist = MathHelper.sqrt_double(x * x + z * z);
            double velocity = bowPower * 3.0;
            double time = dist / velocity;
            
            x += (target.posX - target.prevPosX) * time;
            z += (target.posZ - target.prevPosZ) * time;
        }

        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        
        // Basic gravity compensation
        double v = 3.0; // max bow velocity
        double g = 0.05; // gravity
        double pitch = -Math.toDegrees(Math.atan((Math.pow(v, 2) - Math.sqrt(Math.pow(v, 4) - g * (g * Math.pow(dist, 2) + 2 * y * Math.pow(v, 2)))) / (g * dist)));

        if (Double.isNaN(pitch)) {
            pitch = (float) (-(Math.atan2(y, dist) * 180.0 / Math.PI));
        }

        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw), (float) pitch};
    }
}
