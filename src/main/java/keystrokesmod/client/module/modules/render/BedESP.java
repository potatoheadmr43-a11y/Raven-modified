package keystrokesmod.client.module.modules.render;

import java.awt.Color;
import org.lwjgl.opengl.GL11;
import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import keystrokesmod.client.utils.RenderUtils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class BedESP extends Module {
    
    public static RGBSetting color;
    public static TickSetting rainbow;
    private int rgbColor;

    public BedESP() {
        super("BedESP", ModuleCategory.render);
        this.registerSetting(color = new RGBSetting("Color", 255, 0, 255));
        this.registerSetting(rainbow = new TickSetting("Rainbow", false));
    }

    public void guiUpdate() {
        this.rgbColor = new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB();
    }

    @Subscribe
    public void onForgeEvent(ForgeEvent fe) {
        if (fe.getEvent() instanceof RenderWorldLastEvent) {
            if (Utils.Player.isPlayerInGame()) {
                renderBeds();
            }
        }
    }

    private void renderBeds() {
        int color = rainbow.isToggled() ? Utils.Client.rainbowDraw(1, 0) : this.rgbColor;
        
        // Search for beds in reasonable range
        int searchRadius = 100;
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    // Check distance
                    double distance = mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distance > 100 * 100) continue; // 100 block limit
                    
                    // Check if block is a bed
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (state.getBlock() instanceof BlockBed) {
                        // Render the bed ESP
                        renderBedESP(pos, color);
                    }
                }
            }
        }
    }

    private void renderBedESP(BlockPos pos, int color) {
        // Setup rendering
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        
        // Calculate position relative to player
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;
        
        // Draw bed outline (bed is 0.5625 blocks high)
        drawBoundingBox(x, y, z, x + 1, y + 0.5625, z + 1, 2.0f, color);
        
        // Restore rendering state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    private void drawBoundingBox(double x, double y, double z, double x2, double y2, double z2, float lineWidth, int color) {
        float[] rgb = new float[]{
            (color >> 16 & 255) / 255.0f,
            (color >> 8 & 255) / 255.0f,
            (color & 255) / 255.0f,
            (color >> 24 & 255) / 255.0f
        };
        
        GL11.glColor4f(rgb[0], rgb[1], rgb[2], rgb[3]);
        GL11.glLineWidth(lineWidth);
        
        // Draw box edges
        GL11.glBegin(GL11.GL_LINES);
        
        // Bottom edges
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x2, y, z);
        
        GL11.glVertex3d(x2, y, z);
        GL11.glVertex3d(x2, y, z2);
        
        GL11.glVertex3d(x2, y, z2);
        GL11.glVertex3d(x, y, z2);
        
        GL11.glVertex3d(x, y, z2);
        GL11.glVertex3d(x, y, z);
        
        // Top edges
        GL11.glVertex3d(x, y2, z);
        GL11.glVertex3d(x2, y2, z);
        
        GL11.glVertex3d(x2, y2, z);
        GL11.glVertex3d(x2, y2, z2);
        
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x, y2, z2);
        
        GL11.glVertex3d(x, y2, z2);
        GL11.glVertex3d(x, y2, z);
        
        // Vertical edges
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y2, z);
        
        GL11.glVertex3d(x2, y, z);
        GL11.glVertex3d(x2, y2, z);
        
        GL11.glVertex3d(x2, y, z2);
        GL11.glVertex3d(x2, y2, z2);
        
        GL11.glVertex3d(x, y, z2);
        GL11.glVertex3d(x, y2, z2);
        
        GL11.glEnd();
    }
}
