package keystrokesmod.client.command;

import com.mojang.authlib.GameProfile;
import keystrokesmod.client.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.UUID;

public class FakePlayerCommand extends CommandBase {
    private static final int ENTITY_ID = -8069;
    private static EntityOtherPlayerMP fakePlayer;

    @Override
    public String getCommandName() {
        return "fakeplayer";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fakeplayer [name|remove]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Utils.Player.isPlayerInGame()) {
            return;
        }

        if (args.length > 0 && "remove".equalsIgnoreCase(args[0])) {
            if (removeFakePlayer(mc)) {
                Utils.Player.sendMessageToSelf("&aRemoved fake player.");
            } else {
                Utils.Player.sendMessageToSelf("&cThere is no fake player to remove.");
            }
            return;
        }

        removeFakePlayer(mc);

        String fakeName = args.length > 0 ? args[0] : "FakePlayer";
        GameProfile profile = new GameProfile(UUID.randomUUID(), fakeName);
        EntityOtherPlayerMP spawnedFakePlayer = new EntityOtherPlayerMP(mc.theWorld, profile);

        spawnedFakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
        spawnedFakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
        spawnedFakePlayer.renderYawOffset = mc.thePlayer.renderYawOffset;
        spawnedFakePlayer.inventory.copyInventory(mc.thePlayer.inventory);
        spawnedFakePlayer.setCurrentItemOrArmor(0, mc.thePlayer.getHeldItem());
        spawnedFakePlayer.setSneaking(mc.thePlayer.isSneaking());
        spawnedFakePlayer.setSprinting(mc.thePlayer.isSprinting());
        spawnedFakePlayer.setInvisible(false);
        spawnedFakePlayer.posX -= Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)) * 2.0D;
        spawnedFakePlayer.posZ += Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)) * 2.0D;
        spawnedFakePlayer.setPosition(spawnedFakePlayer.posX, spawnedFakePlayer.posY, spawnedFakePlayer.posZ);

        mc.theWorld.addEntityToWorld(ENTITY_ID, spawnedFakePlayer);
        fakePlayer = spawnedFakePlayer;

        Utils.Player.sendMessageToSelf("&aSpawned fake player &e" + fakeName + "&a.");
    }

    private static boolean removeFakePlayer(Minecraft mc) {
        if (fakePlayer == null) {
            return false;
        }

        if (mc.theWorld != null) {
            mc.theWorld.removeEntityFromWorld(ENTITY_ID);
        }

        fakePlayer = null;
        return true;
    }
}
