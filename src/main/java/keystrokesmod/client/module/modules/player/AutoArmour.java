package keystrokesmod.client.module.modules.player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.eventbus.Subscribe;

import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.CoolDown;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AutoArmour extends Module {

    private final DoubleSliderSetting firstDelay;
    private final DoubleSliderSetting delay;
    private final TickSetting dropWorse;
    private final TickSetting invOpen;
    private final CoolDown delayTimer = new CoolDown(0);

    public AutoArmour() {
        super("AutoArmour", ModuleCategory.player);
        this.registerSetting(firstDelay = new DoubleSliderSetting("Open delay", 250, 450, 0, 1000, 1));
        this.registerSetting(delay = new DoubleSliderSetting("Delay", 150, 250, 0, 1000, 1));
        this.registerSetting(dropWorse = new TickSetting("Drop Worse", true));
        this.registerSetting(invOpen = new TickSetting("Inv Open", true));
    }

    @Override
    public void onEnable() {
        delayTimer.setCooldown(0);
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        if (invOpen.isToggled() && !(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        if (!delayTimer.hasFinished()) {
            return;
        }

        int[] bestArmorSlots = new int[4];
        float[] bestArmorValues = new float[4];

        for (int i = 0; i < 4; i++) {
            bestArmorSlots[i] = -1;
            ItemStack stack = mc.thePlayer.inventory.armorItemInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                bestArmorValues[i] = getArmorValue((ItemArmor) stack.getItem(), stack);
            } else {
                bestArmorValues[i] = -1;
            }
        }

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                int armorType = 3 - armor.armorType;
                float armorValue = getArmorValue(armor, stack);

                if (armorValue > bestArmorValues[armorType]) {
                    bestArmorSlots[armorType] = i;
                    bestArmorValues[armorType] = armorValue;
                }
            }
        }

        boolean acted = false;
        
        for (int i = 0; i < 4; i++) {
            if (bestArmorSlots[i] != -1) {
                ItemStack equipped = mc.thePlayer.inventory.armorItemInSlot(i);
                if (equipped != null && dropWorse.isToggled()) {
                    // Drop equipped
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 8 - i, 1, 4, mc.thePlayer);
                    acted = true;
                    break;
                } else {
                    // Shift click new
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, bestArmorSlots[i], 0, 1, mc.thePlayer);
                    acted = true;
                    break;
                }
            }
        }

        if (!acted && dropWorse.isToggled()) {
            for (int i = 9; i < 45; i++) {
                ItemStack stack = mc.thePlayer.openContainer.getSlot(i).getStack();
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    ItemArmor armor = (ItemArmor) stack.getItem();
                    int armorType = 3 - armor.armorType;
                    float armorValue = getArmorValue(armor, stack);
                    
                    if (armorValue < bestArmorValues[armorType]) {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, i, 1, 4, mc.thePlayer);
                        acted = true;
                        break;
                    }
                }
            }
        }

        if (acted) {
            delayTimer.setCooldown((long) ThreadLocalRandom.current().nextDouble(delay.getInputMin(), delay.getInputMax() + 0.01));
            delayTimer.start();
        }
    }

    private float getArmorValue(ItemArmor armor, ItemStack stack) {
        float value = armor.damageReduceAmount;
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
        value += protection * 0.04f;
        return value;
    }
}
