package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

public class Leafhat extends Module {
    public static SliderSetting heightOffset, size;
    private final Minecraft mc = Minecraft.getMinecraft();

    public Leafhat() {
        super("Leafhat", ModuleCategory.render);
        this.registerSetting(heightOffset = new SliderSetting("Height offset", 0.0D, -0.5D, 1.0D, 0.05D));
        this.registerSetting(size = new SliderSetting("Size", 0.5D, 0.1D, 1.0D, 0.05D));
    }

    @Subscribe
    public void onRender(ForgeEvent fe) {
        if (!(fe.getEvent() instanceof RenderWorldLastEvent)) return;
        if (!Utils.Player.isPlayerInGame() || mc.thePlayer == null || mc.thePlayer.isDead) return;
        if (mc.thePlayer.isPlayerSleeping()) return;

        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * Utils.Client.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosX;
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * Utils.Client.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosY;
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * Utils.Client.getTimer().renderPartialTicks - mc.getRenderManager().viewerPosZ;

        float s = (float) (double) size.getInput();

        GL11.glPushMatrix();
        GL11.glTranslated(x, y + mc.thePlayer.height + 0.2 + (double) heightOffset.getInput(), z);
        GL11.glRotated(-mc.thePlayer.rotationYawHead, 0.0, 1.0, 0.0);
        GL11.glScalef(s, s, s);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        // Leaf block on head
        GL11.glColor4f(0.2f, 0.8f, 0.2f, 0.9f);

        // Draw a simple leaf shape using quads
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        // leaf disc
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(0.0, 0.0, 0.0).color(0.2f, 0.8f, 0.2f, 0.9f).endVertex();
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double angle = i * 2.0 * Math.PI / segments;
            wr.pos(Math.cos(angle) * 0.3, Math.sin(angle) * 0.02, Math.sin(angle) * 0.3).color(0.1f, 0.7f, 0.1f, 0.8f).endVertex();
        }
        tessellator.draw();

        // stem
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-0.02, 0.0, -0.02).color(0.3f, 0.5f, 0.1f, 1.0f).endVertex();
        wr.pos(0.02, 0.0, -0.02).color(0.3f, 0.5f, 0.1f, 1.0f).endVertex();
        wr.pos(0.02, -0.3, -0.02).color(0.3f, 0.5f, 0.1f, 1.0f).endVertex();
        wr.pos(-0.02, -0.3, -0.02).color(0.3f, 0.5f, 0.1f, 1.0f).endVertex();
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }
}
