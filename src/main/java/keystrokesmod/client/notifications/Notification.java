package keystrokesmod.client.notifications;

import keystrokesmod.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class Notification {
    private NotificationType type;
    private String title;
    private String messsage;
    private long start;
    public long fadedIn;
    public long fadeOut;
    public long end;
    public float currentY;
    public float targetY;

    public Notification(NotificationType type, String title, String messsage, int length) {
        this.type = type;
        this.title = title;
        this.messsage = messsage;
        fadedIn = 200L * length;
        fadeOut = fadedIn + 1500L * length;
        end = fadeOut + 300L * length;
    }

    public void show() {
        start = System.currentTimeMillis();
    }

    public boolean isShown() {
        return getTime() <= end;
    }

    public long getTime() {
        return System.currentTimeMillis() - start;
    }

    public int getWidth() {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int titleW = fr.getStringWidth(title);
        int msgW = fr.getStringWidth(messsage);
        return Math.max(Math.max(titleW, msgW) + 24, 120);
    }

    public int getHeight() {
        return 30;
    }

    public void render(int x, float y, float alpha) {
        if (alpha <= 0.01f) return;

        int width = getWidth();
        int height = getHeight();

        int bgColor = ((int) (0x90 * alpha) << 24) | 0x101010;
        int borderColor = ((int) (0xFF * alpha) << 24) | 0xFF8C00;

        RenderUtils.drawBorderedRoundedRect(x, y, x + width, y + height, 4, 1, borderColor, bgColor);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int textColor = ((int) (0xFF * alpha) << 24) | 0xFFFFFF;
        int subColor = ((int) (0xCC * alpha) << 24) | 0xCCCCCC;

        fr.drawString(title, x + 8, (int) y + 5, textColor);
        fr.drawString(messsage, x + 8, (int) y + 17, subColor);
    }
}
