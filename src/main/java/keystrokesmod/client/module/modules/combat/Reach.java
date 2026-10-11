package keystrokesmod.client.module.modules.combat;

import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.combat.aura.KillAura;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;

import java.util.concurrent.ThreadLocalRandom;

public class Reach extends Module {
    public static DoubleSliderSetting reach;
    public static SliderSetting chance;
    public static TickSetting weapon_only;
    public static TickSetting moving_only;
    public static TickSetting sprint_only;
    public static KillAura la;
    private static int lastChanceCheckTick = Integer.MIN_VALUE;
    private static double lastChanceValue = -1.0;
    private static boolean chancePassed;

    public Reach() {
        super("Reach", ModuleCategory.combat);
        this.registerSetting(reach = new DoubleSliderSetting("Reach (Blocks)", 3.1, 3.3, 3, 6, 0.05));
        this.registerSetting(chance = new SliderSetting("Chance", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(weapon_only = new TickSetting("Weapon only", false));
        this.registerSetting(moving_only = new TickSetting("Moving only", false));
        this.registerSetting(sprint_only = new TickSetting("Sprint only", false));
    }

    @Override
    public void postApplyConfig() {
       la = (KillAura) Raven.moduleManager.getModuleByClazz(KillAura.class);
    }

    public static double getReach() {
        double normal = mc.playerController.extendedReach()? 5 : 3;
        double customReach;

        if (la != null && la.isEnabled()) {
            customReach = KillAura.reach.getInput();
        } else {
            if (!Utils.Player.isPlayerInGame() || (weapon_only.isToggled() && !Utils.Player.isPlayerHoldingWeapon()))
                return normal;

            if (moving_only.isToggled() && ((double) mc.thePlayer.moveForward == 0.0D)
                    && ((double) mc.thePlayer.moveStrafing == 0.0D))
                return normal;

            if (sprint_only.isToggled() && !mc.thePlayer.isSprinting())
                return normal;

            customReach = Utils.Client.ranModuleVal(reach, Utils.Java.rand())
                    + (mc.playerController.extendedReach()? 2 : 0);
        }

        if (customReach <= 3.0)
            return customReach;

        if (!shouldUseExtendedReach())
            return 3.0;

        return customReach;
    }

    private static boolean shouldUseExtendedReach() {
        double chanceValue = chance.getInput();
        if (chanceValue <= 0.0)
            return false;
        if (chanceValue >= 100.0)
            return true;

        if (mc.thePlayer == null)
            return false;

        int currentTick = mc.thePlayer.ticksExisted;
        if (currentTick != lastChanceCheckTick || chanceValue != lastChanceValue) {
            lastChanceCheckTick = currentTick;
            lastChanceValue = chanceValue;
            chancePassed = ThreadLocalRandom.current().nextDouble(0.0, 100.0) <= chanceValue;
        }

        return chancePassed;
    }
}
