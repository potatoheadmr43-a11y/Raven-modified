package keystrokesmod.client.utils;

import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();
    private final int count;
    private final Random random = new Random();

    public ParticleSystem(int count) {
        this.count = count;
    }

    public void render(int mouseX, int mouseY, int screenWidth, int screenHeight, float alpha) {
        if (particles.size() < count) {
            particles.add(new Particle(random.nextInt(Math.max(1, screenWidth)), random.nextInt(Math.max(1, screenHeight))));
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        for (Particle p : particles) {
            p.update(mouseX, mouseY, screenWidth, screenHeight);
            p.render(alpha);
        }

        // Draw connecting lines
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                float dist = p1.distanceTo(p2);
                if (dist < 100) {
                    float lineAlpha = (1.0f - (dist / 100.0f)) * alpha * 0.3f;
                    GL11.glColor4f(1f, 0.55f, 0f, lineAlpha); // Orange lines
                    GL11.glVertex2f(p1.x, p1.y);
                    GL11.glVertex2f(p2.x, p2.y);
                }
            }
        }
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float size;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextFloat() - 0.5f) * 0.5f;
            this.vy = (random.nextFloat() - 0.5f) * 0.5f;
            this.size = random.nextFloat() * 1.5f + 1f;
        }

        void update(int mouseX, int mouseY, int screenWidth, int screenHeight) {
            x += vx;
            y += vy;

            // Bounce off edges
            if (x < 0) x = screenWidth;
            if (x > screenWidth) x = 0;
            if (y < 0) y = screenHeight;
            if (y > screenHeight) y = 0;

            // Mouse interact
            float dx = x - mouseX;
            float dy = y - mouseY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 80 && dist > 0) {
                x += (dx / dist) * 1.5f;
                y += (dy / dist) * 1.5f;
            }
        }

        void render(float alpha) {
            GL11.glColor4f(1f, 0.55f, 0f, alpha * 0.5f); // Orange particles
            GL11.glBegin(GL11.GL_POLYGON);
            for (int i = 0; i <= 360; i += 45) {
                GL11.glVertex2d(x + Math.sin(i * Math.PI / 180.0) * size, y + Math.cos(i * Math.PI / 180.0) * size);
            }
            GL11.glEnd();
        }

        float distanceTo(Particle p) {
            return (float) Math.sqrt(Math.pow(this.x - p.x, 2) + Math.pow(this.y - p.y, 2));
        }
    }
}
