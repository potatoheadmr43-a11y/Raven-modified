package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.PacketEvent;
import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Fly extends Module {
    public static ComboSetting mode;
    public static SliderSetting speed;

    // BlocksMC Variables
    private boolean isTeleported = false;

    // Hypixel Variables
    private int tickCounter = 0;

    // Glide Variables
    private boolean opf = false;

    public Fly() {
        super("Fly", ModuleCategory.movement);
        this.registerSetting(mode = new ComboSetting("Mode", Mode.Vanilla));
        this.registerSetting(speed = new SliderSetting("Speed", 2.0D, 1.0D, 5.0D, 0.1D));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        isTeleported = false;
        tickCounter = 0;
        opf = false;

        if (mode.getMode() == Mode.BlocksMC) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY - 0.05,
                    mc.thePlayer.posZ,
                    false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    false
            ));
            isTeleported = true;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        mc.thePlayer.capabilities.isFlying = false;
        mc.thePlayer.capabilities.setFlySpeed(0.05F);
        Utils.Client.getTimer().timerSpeed = 1.0f;
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (mc.thePlayer == null) return;

        switch ((Mode) mode.getMode()) {
            case Vanilla:
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.getInput()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case Glide:
                if (mc.thePlayer.movementInput.moveForward > 0.0F) {
                    if (!this.opf) {
                        this.opf = true;
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        }
                    } else {
                        if (mc.thePlayer.onGround || mc.thePlayer.isCollidedHorizontally) {
                            this.disable();
                            return;
                        }

                        double s = 1.94D * speed.getInput();
                        double r = Math.toRadians(mc.thePlayer.rotationYaw + 90.0F);
                        mc.thePlayer.motionX = s * Math.cos(r);
                        mc.thePlayer.motionZ = s * Math.sin(r);
                    }
                }
                break;
            case BlocksMC:
                if (isTeleported) {
                    mc.thePlayer.motionY = 0.0;
                    if (!mc.thePlayer.onGround) {
                        if (mc.thePlayer.ticksExisted % 7 == 0) {
                            Utils.Client.getTimer().timerSpeed = 0.415f;
                        } else {
                            Utils.Client.getTimer().timerSpeed = 0.35f;
                        }
                    } else {
                        Utils.Client.getTimer().timerSpeed = 1.0f;
                    }
                    
                    if (mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                        double s = speed.getInput();
                        double r = Math.toRadians(mc.thePlayer.rotationYaw + 90.0F);
                        mc.thePlayer.motionX = s * Math.cos(r);
                        mc.thePlayer.motionZ = s * Math.sin(r);
                    } else {
                        mc.thePlayer.motionX = 0;
                        mc.thePlayer.motionZ = 0;
                    }
                }
                break;
            case Hypixel:
                Utils.Client.getTimer().timerSpeed = 1.0f;
                tickCounter++;
                if (tickCounter >= 2) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1.0E-5, mc.thePlayer.posZ);
                    tickCounter = 0;
                }
                break;
        }
    }

    @Subscribe
    public void onPacket(PacketEvent e) {
        if (mode.getMode() == Mode.Hypixel) {
            if (e.getPacket() instanceof C03PacketPlayer) {
                Utils.setPacketOnGround((C03PacketPlayer) e.getPacket(), false);
            }
        }
    }

    public enum Mode {
        Vanilla, Glide, BlocksMC, Hypixel
    }
}
