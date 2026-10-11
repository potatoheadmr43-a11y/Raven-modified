package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;

import keystrokesmod.client.event.impl.MoveInputEvent;
import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.util.MathHelper;

public class EnhancedSpeed extends Module {
    
    public static DescriptionSetting desc;
    public static ComboSetting mode;
    public static TickSetting onGround, inAir, inWater, jump, autoJump;
    public static SliderSetting speed, jumpHeight, glide, timerBoost;
    public static SliderSetting strafeFactor, boostChance, smoothSpeed;
    
    private int jumpTimer = 0;
    private boolean wasJumping = false;
    private double lastSpeed = 0;
    private int stage = 0;
    
    public enum SpeedMode {
        BOUNCE,        // Bhop-style speed
        LOWHOP,        // Low jump speed
        YPORT,         // Y-axis teleportation
        GROUND,        // Ground-based speed
        SMOOTH,        // Smooth acceleration
        LEGIT,         // Legit-looking speed
        CUSTOM         // Custom speed settings
    }

    public EnhancedSpeed() {
        super("EnhancedSpeed", ModuleCategory.movement);
        this.registerSetting(desc = new DescriptionSetting("Advanced movement speed"));
        this.registerSetting(mode = new ComboSetting("Mode", SpeedMode.BOUNCE));
        
        // Conditions
        this.registerSetting(onGround = new TickSetting("On Ground", true));
        this.registerSetting(inAir = new TickSetting("In Air", true));
        this.registerSetting(inWater = new TickSetting("In Water", false));
        this.registerSetting(jump = new TickSetting("Auto Jump", true));
        this.registerSetting(autoJump = new TickSetting("Smart Jump", false));
        
        // Speed settings
        this.registerSetting(speed = new SliderSetting("Speed", 1.8, 1.0, 5.0, 0.1));
        this.registerSetting(jumpHeight = new SliderSetting("Jump Height", 0.42, 0.1, 1.0, 0.01));
        this.registerSetting(glide = new SliderSetting("Glide", 0.0, -0.5, 0.5, 0.01));
        this.registerSetting(timerBoost = new SliderSetting("Timer Boost", 1.0, 1.0, 2.0, 0.05));
        
        // Advanced settings
        this.registerSetting(strafeFactor = new SliderSetting("Strafe Factor", 1.0, 0.5, 2.0, 0.1));
        this.registerSetting(boostChance = new SliderSetting("Boost Chance", 100, 0, 100, 5));
        this.registerSetting(smoothSpeed = new SliderSetting("Smooth Speed", 0.1, 0.01, 1.0, 0.01));
    }

    @Override
    public void onEnable() {
        jumpTimer = 0;
        wasJumping = false;
        lastSpeed = 0;
        stage = 0;
    }

    @Subscribe
    public void onUpdateEvent(UpdateEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;
        
        SpeedMode currentMode = (SpeedMode) mode.getMode();
        
        switch (currentMode) {
            case BOUNCE:
                handleBounceSpeed(event);
                break;
            case LOWHOP:
                handleLowhopSpeed(event);
                break;
            case YPORT:
                handleYportSpeed(event);
                break;
            case GROUND:
                handleGroundSpeed(event);
                break;
            case SMOOTH:
                handleSmoothSpeed(event);
                break;
            case LEGIT:
                handleLegitSpeed(event);
                break;
            case CUSTOM:
                handleCustomSpeed(event);
                break;
        }
    }

    @Subscribe
    public void onMoveInputEvent(MoveInputEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;
        
        SpeedMode currentMode = (SpeedMode) mode.getMode();
        
        // Modify strafe for certain modes
        if (currentMode == SpeedMode.BOUNCE || currentMode == SpeedMode.LOWHOP) {
            float strafe = (float) strafeFactor.getInput();
            event.setStrafe(event.getStrafe() * strafe);
            event.setForward(event.getForward() * strafe);
        }
    }

    private void handleBounceSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        if (mc.thePlayer.onGround && jump.isToggled()) {
            mc.thePlayer.motionY = jumpHeight.getInput();
            jumpTimer = 0;
        }
        
        if (jump.isToggled()) {
            jumpTimer++;
            if (jumpTimer >= 2) {
                mc.thePlayer.motionY = glide.getInput();
            }
        }
        
        // Apply speed boost
        double currentSpeed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + 
                                     mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        
        if (currentSpeed > 0) {
            double targetSpeed = speed.getInput();
            double acceleration = (targetSpeed - currentSpeed) * smoothSpeed.getInput();
            
            mc.thePlayer.motionX += acceleration * mc.thePlayer.motionX / currentSpeed;
            mc.thePlayer.motionZ += acceleration * mc.thePlayer.motionZ / currentSpeed;
        }
        
        // Timer boost
        if (timerBoost.getInput() > 1.0) {
            Utils.Client.getTimer().timerSpeed = (float) timerBoost.getInput();
        }
    }

    private void handleLowhopSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        if (mc.thePlayer.onGround && jump.isToggled()) {
            mc.thePlayer.motionY = jumpHeight.getInput() * 0.5; // Lower jump
            jumpTimer = 0;
        }
        
        if (jump.isToggled()) {
            jumpTimer++;
            if (jumpTimer >= 3) {
                mc.thePlayer.motionY = -0.02; // Slight downward motion
            }
        }
        
        // Apply smooth speed
        applySmoothSpeed(speed.getInput() * 0.8); // Slightly slower than bounce
    }

    private void handleYportSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        if (mc.thePlayer.onGround) {
            stage = 0;
        }
        
        switch (stage) {
            case 0:
                if (mc.thePlayer.onGround && jump.isToggled()) {
                    mc.thePlayer.motionY = jumpHeight.getInput();
                    stage = 1;
                }
                break;
            case 1:
                if (!mc.thePlayer.onGround) {
                    // Teleport forward
                    double motionX = mc.thePlayer.motionX;
                    double motionZ = mc.thePlayer.motionZ;
                    double targetSpeed = speed.getInput();
                    
                    mc.thePlayer.motionX = motionX + (motionX / Math.abs(motionX + 0.0001)) * (targetSpeed - Math.abs(motionX));
                    mc.thePlayer.motionZ = motionZ + (motionZ / Math.abs(motionZ + 0.0001)) * (targetSpeed - Math.abs(motionZ));
                    
                    stage = 2;
                }
                break;
            case 2:
                if (mc.thePlayer.onGround) {
                    stage = 0;
                }
                break;
        }
    }

    private void handleGroundSpeed(UpdateEvent event) {
        if (!shouldApplySpeed() || !mc.thePlayer.onGround) return;
        
        // Ground-based speed (no jumping)
        applySmoothSpeed(speed.getInput() * 0.6);
        
        // Add small glide effect
        if (glide.getInput() < 0) {
            mc.thePlayer.motionY = glide.getInput() * 0.1;
        }
    }

    private void handleSmoothSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        // Very smooth acceleration
        double currentSpeed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + 
                                     mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        double targetSpeed = speed.getInput();
        
        if (currentSpeed < targetSpeed) {
            double acceleration = smoothSpeed.getInput() * 0.5;
            double newSpeed = Math.min(currentSpeed + acceleration, targetSpeed);
            
            if (currentSpeed > 0) {
                double ratio = newSpeed / currentSpeed;
                mc.thePlayer.motionX *= ratio;
                mc.thePlayer.motionZ *= ratio;
            }
        }
        
        // Smart jump
        if (autoJump.isToggled() && mc.thePlayer.onGround && 
            (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() ||
             mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown())) {
            mc.thePlayer.motionY = jumpHeight.getInput() * 0.3;
        }
    }

    private void handleLegitSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        // Legit-looking speed with subtle boosts
        double boostMultiplier = 1.0;
        
        // Random chance for boost
        if (Math.random() * 100 < boostChance.getInput()) {
            boostMultiplier = 1.2;
        }
        
        double targetSpeed = speed.getInput() * 0.4 * boostMultiplier; // Much lower for legit
        applySmoothSpeed(targetSpeed);
        
        // Minimal jump assistance
        if (jump.isToggled() && mc.thePlayer.onGround && 
            mc.gameSettings.keyBindForward.isKeyDown()) {
            mc.thePlayer.motionY = jumpHeight.getInput() * 0.2;
        }
    }

    private void handleCustomSpeed(UpdateEvent event) {
        if (!shouldApplySpeed()) return;
        
        // Custom speed with all settings
        if (mc.thePlayer.onGround && jump.isToggled()) {
            mc.thePlayer.motionY = jumpHeight.getInput();
        }
        
        applySmoothSpeed(speed.getInput());
        
        // Apply glide
        if (glide.getInput() != 0 && !mc.thePlayer.onGround) {
            mc.thePlayer.motionY = glide.getInput();
        }
        
        // Timer boost
        Utils.Client.getTimer().timerSpeed = (float) timerBoost.getInput();
    }

    private void applySmoothSpeed(double targetSpeed) {
        double currentSpeed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + 
                                     mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        
        if (currentSpeed < targetSpeed) {
            double acceleration = smoothSpeed.getInput();
            double newSpeed = Math.min(currentSpeed + acceleration, targetSpeed);
            
            if (currentSpeed > 0) {
                double ratio = newSpeed / currentSpeed;
                mc.thePlayer.motionX *= ratio;
                mc.thePlayer.motionZ *= ratio;
            }
        }
    }

    private boolean shouldApplySpeed() {
        boolean moving = mc.gameSettings.keyBindForward.isKeyDown() || 
                        mc.gameSettings.keyBindBack.isKeyDown() ||
                        mc.gameSettings.keyBindLeft.isKeyDown() || 
                        mc.gameSettings.keyBindRight.isKeyDown();
        
        if (!moving) return false;
        
        if (mc.thePlayer.onGround && !onGround.isToggled()) return false;
        if (!mc.thePlayer.onGround && !inAir.isToggled()) return false;
        if (mc.thePlayer.isInWater() && !inWater.isToggled()) return false;
        
        return true;
    }

    @Override
    public void onDisable() {
        // Reset timer
        Utils.Client.getTimer().timerSpeed = 1.0f;
        
        // Reset motion
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }
    }

    public double getCurrentSpeed() {
        if (mc.thePlayer == null) return 0;
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + 
                        mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public SpeedMode getCurrentMode() {
        return (SpeedMode) mode.getMode();
    }
}
