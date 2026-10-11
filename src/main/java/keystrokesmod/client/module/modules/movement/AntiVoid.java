package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;

public class AntiVoid extends Module {
    public static ComboSetting mode;
    public static SliderSetting triggerY;
    private BlockPos lastSafePos = null;
    private boolean wasOnGround = false;

    public AntiVoid() {
        super("AntiVoid", ModuleCategory.movement);
        this.registerSetting(mode = new ComboSetting("Mode", AntiVoidMode.Blink));
        this.registerSetting(triggerY = new SliderSetting("Trigger Y", 5, 0, 10, 1));
    }

    @Subscribe
    public void onUpdate(UpdateEvent e) {
        if (!Utils.Player.isPlayerInGame()) return;

        if (mc.thePlayer.onGround) {
            lastSafePos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            wasOnGround = true;
        }

        if (mc.thePlayer.posY < triggerY.getInput() && !mc.thePlayer.onGround) {
            if (mc.theWorld.isAirBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ))
                    && mc.thePlayer.motionY < 0) {
                switch ((AntiVoidMode) mode.getMode()) {
                    case Blink:
                        if (lastSafePos != null) {
                            mc.thePlayer.setPosition(lastSafePos.getX() + 0.5, lastSafePos.getY(), lastSafePos.getZ() + 0.5);
                            mc.thePlayer.motionY = 0;
                        }
                        break;
                    case Hover:
                        mc.thePlayer.motionY = 0;
                        break;
                    case Packet:
                        if (wasOnGround) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                            wasOnGround = false;
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        lastSafePos = null;
        wasOnGround = false;
        super.onDisable();
    }

    public enum AntiVoidMode {
        Blink("Blink"), Hover("Hover"), Packet("Packet");
        private final String display;
        AntiVoidMode(String display) { this.display = display; }
        @Override public String toString() { return display; }
    }
}
