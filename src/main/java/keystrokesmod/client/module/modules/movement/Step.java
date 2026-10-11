package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Step extends Module {
    public static SliderSetting height;
    private boolean wasStep;

    public Step() {
        super("Step", ModuleCategory.movement);
        this.registerSetting(height = new SliderSetting("Height", 1.0D, 0.5D, 2.0D, 0.5D));
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (!Utils.Player.isPlayerInGame()) return;

        if (mc.thePlayer != null && mc.thePlayer.onGround && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder()) {
            mc.thePlayer.stepHeight = (float) height.getInput();
            wasStep = true;
        } else if (wasStep && mc.thePlayer != null && !mc.thePlayer.onGround) {
            mc.thePlayer.stepHeight = 0.5f;
            wasStep = false;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.stepHeight = 0.5f;
        }
        wasStep = false;
    }
}
