package keystrokesmod.client.clickgui.raven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.modules.client.GuiModule;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ButtonSetting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.RenderUtils;
import keystrokesmod.client.utils.ParticleSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class ModernClickGui extends GuiScreen {
    private int width = 600;
    private int height = 450;
    private int x, y;
    private ModuleCategory selectedCategory;
    private final List<CategoryTab> tabs = new ArrayList<>();
    private final List<ModuleButton> moduleButtons = new ArrayList<>();
    private double scrollAmount = 0;
    private int totalModuleHeight = 0;
    private float openAnimation = 0f;
    private long openStartTime = 0;
    private boolean searchMode = false;
    private String searchText = "";
    private ModuleButton bindingModule;
    private Setting draggingSlider;
    private int draggingSliderStart, draggingSliderEnd;
    private boolean draggingMaxHandle;
    private ParticleSystem particleSystem;

    public ModernClickGui() {
        tabs.add(new CategoryTab(null));
        for (ModuleCategory category : ModuleCategory.values()) {
            if (category != ModuleCategory.category && category != ModuleCategory.sumo) {
                tabs.add(new CategoryTab(category));
            }
        }
        this.openStartTime = System.currentTimeMillis();
        this.particleSystem = new ParticleSystem(100);
    }

    public void prepareToOpen() {
        openStartTime = System.currentTimeMillis();
        scrollAmount = 0;
        searchMode = false;
        searchText = "";
        bindingModule = null;
        draggingSlider = null;
        terminal().hide();
    }

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        width = Math.min(760, sr.getScaledWidth() - 32);
        height = Math.min(540, sr.getScaledHeight() - 32);
        this.x = (sr.getScaledWidth() - width) / 2;
        this.y = (sr.getScaledHeight() - height) / 2;
        refreshModules();
    }

    private void refreshModules() {
        moduleButtons.clear();
        List<Module> modules = selectedCategory == null
                ? Raven.moduleManager.getModules()
                : Raven.moduleManager.getModulesInCategory(selectedCategory);
        for (Module m : modules) {
            moduleButtons.add(new ModuleButton(m));
        }
        updateTotalHeight();
    }

    private void updateTotalHeight() {
        totalModuleHeight = 0;
        for (ModuleButton btn : moduleButtons) {
            totalModuleHeight += btn.getHeight() + 5;
        }
    }

    private void updateDraggedSlider(int mouseX) {
        if (draggingSlider == null || draggingSliderEnd <= draggingSliderStart)
            return;
        double percent = (double) (mouseX - draggingSliderStart)
                / (draggingSliderEnd - draggingSliderStart);
        percent = Math.max(0.0, Math.min(1.0, percent));
        if (draggingSlider instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) draggingSlider;
            slider.setValue(slider.getMin() + percent * (slider.getMax() - slider.getMin()));
        } else if (draggingSlider instanceof DoubleSliderSetting) {
            DoubleSliderSetting slider = (DoubleSliderSetting) draggingSlider;
            double value = slider.getMin() + percent * (slider.getMax() - slider.getMin());
            if (draggingMaxHandle) slider.setValueMax(value);
            else slider.setValueMin(value);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long currentTime = System.currentTimeMillis();
        
        // Calculate opening animation
        float deltaTime = (currentTime - openStartTime) / 1000f;
        openAnimation = Math.min(1f, deltaTime * 4f);
        
        // Background with blur
        drawBackground(mouseX, mouseY, openAnimation);
        
        float alpha = openAnimation;

        if (draggingSlider != null && Mouse.isButtonDown(0)) {
            updateDraggedSlider(mouseX);
        }

        drawMainWindow(x, y, width, height, alpha);
        drawTopTabs(x, y, width, mouseX, mouseY, alpha);
        drawSearchBar(x, y, width, mouseX, mouseY, alpha);
        drawModuleGrid(x, y, width, height, mouseX, mouseY, alpha);
        drawWatermark(x, y, width, height, alpha);
        Terminal terminal = terminal();
        terminal.update(mouseX, mouseY);
        terminal.draw();
    }
    
    private void drawBackground(int mouseX, int mouseY, float alpha) {
        ScaledResolution sr = new ScaledResolution(mc);
        drawRect(0, 0, super.width, super.height, withAlpha(0x000000, alpha * 0.58f));
        particleSystem.render(mouseX, mouseY, sr.getScaledWidth(), sr.getScaledHeight(), alpha);
    }

    private void drawMainWindow(int x, int y, int width, int height, float alpha) {
        RenderUtils.drawRoundedRect(x + 4, y + 4, x + width + 4, y + height + 4, 14,
                withAlpha(0x000000, alpha * 0.65f));
        RenderUtils.drawRoundedRect(x, y, x + width, y + height, 14, withAlpha(0x0D0D0F, alpha));
        RenderUtils.drawRoundedOutline(x, y, x + width, y + height, 14, 1,
                withAlpha(0x27272A, alpha));
    }
    
    private void drawTopTabs(int x, int y, int width, int mouseX, int mouseY, float alpha) {
        int logoX = x + 20;
        int logoY = y + 17;
        int dot = 4;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                RenderUtils.drawRoundedRect(logoX + col * 6, logoY + row * 6,
                        logoX + col * 6 + dot, logoY + row * 6 + dot, 2,
                        withAlpha(0xFFFFFF, alpha));
            }
        }

        int availableWidth = width - 56;
        int tabWidth = Math.max(42, availableWidth / tabs.size());
        int tabHeight = 24;
        int tabY = y + 8;
        int startX = x + 44;
        for (int i = 0; i < tabs.size(); i++) {
            CategoryTab tab = tabs.get(i);
            int tabX = startX + i * tabWidth;
            boolean selected = tab.category == selectedCategory;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth
                    && mouseY >= tabY && mouseY <= tabY + tabHeight;
            if (selected || hovered) {
                RenderUtils.drawRoundedRect(tabX + 2, tabY, tabX + tabWidth - 2, tabY + tabHeight, 8,
                        withAlpha(selected ? 0x1F1F23 : 0x18181B, alpha));
            }
            String label = tab.category == null ? "All" : getTabName(tab.category);
            drawCenteredText(label, tabX + tabWidth / 2, tabY + tabHeight / 2 - 4,
                    withAlpha(selected ? 0xFFFFFF : 0x80808A, alpha));
        }
    }
    
    private void drawSearchBar(int x, int y, int width, int mouseX, int mouseY, float alpha) {
        int searchBarY = y + 43;
        int searchBarWidth = Math.min(230, width - 40);
        int searchBarX = x + 20;
        RenderUtils.drawRoundedRect(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + 26,
                8, withAlpha(0x18181B, alpha));
        String displayText = searchText.isEmpty() ? "Search modules..." : searchText;
        drawText(displayText, searchBarX + 10, searchBarY + 8,
                withAlpha(searchText.isEmpty() ? 0x80808A : 0xFFFFFF, alpha));
        if (searchMode && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = searchBarX + 10 + mc.fontRendererObj.getStringWidth(displayText);
            RenderUtils.drawRoundedRect(caretX, searchBarY + 6, caretX + 1, searchBarY + 20,
                    1, withAlpha(0xFFFFFF, alpha));
        }

        Terminal terminal = terminal();
        int terminalButtonX = x + width - 106;
        int terminalButtonY = searchBarY;
        int terminalButtonColor = terminal.hidden() ? 0x18181B : accentColor();
        RenderUtils.drawRoundedRect(terminalButtonX, terminalButtonY, terminalButtonX + 86,
                terminalButtonY + 26, 8, withAlpha(terminalButtonColor, alpha));
        mc.fontRendererObj.drawStringWithShadow("Terminal",
                terminalButtonX + 43 - mc.fontRendererObj.getStringWidth("Terminal") / 2,
                terminalButtonY + 8, 0xFFFFFFFF);
    }

    private Terminal terminal() {
        return Raven.clickGui.terminal;
    }
    
    private void drawModuleGrid(int x, int y, int width, int height, int mouseX, int mouseY, float alpha) {
        int gridStartY = y + 80;
        int gridStartX = x + 20;
        int gridWidth = width - 40;
        int gridHeight = height - 100;

        List<ModuleButton> filteredModules = getFilteredModules();
        int spacing = 12;
        int modulesPerRow = Math.max(1, (gridWidth + spacing) / 180);
        int moduleWidth = (gridWidth - spacing * (modulesPerRow - 1)) / modulesPerRow;
        int contentHeight = getGridContentHeight(filteredModules, modulesPerRow, spacing);
        totalModuleHeight = contentHeight;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        GL11.glScissor(gridStartX * factor, mc.displayHeight - (gridStartY + gridHeight) * factor,
                gridWidth * factor, gridHeight * factor);

        int maxScroll = Math.max(0, contentHeight - gridHeight);
        if (scrollAmount > maxScroll) scrollAmount = maxScroll;
        if (scrollAmount < 0) scrollAmount = 0;

        int currentY = gridStartY - (int) scrollAmount;
        for (int rowStart = 0; rowStart < filteredModules.size(); rowStart += modulesPerRow) {
            int rowCount = Math.min(modulesPerRow, filteredModules.size() - rowStart);
            int rowHeight = 0;
            for (int i = 0; i < rowCount; i++) {
                rowHeight = Math.max(rowHeight, filteredModules.get(rowStart + i).getHeight());
            }
            for (int col = 0; col < rowCount; col++) {
                ModuleButton btn = filteredModules.get(rowStart + col);
                int moduleX = gridStartX + col * (moduleWidth + spacing);
                if (currentY + rowHeight >= gridStartY && currentY <= gridStartY + gridHeight) {
                    btn.drawGrid(moduleX, currentY, moduleWidth, btn.getHeight(), mouseX, mouseY, alpha);
                }
            }
            currentY += rowHeight + spacing;
        }
        if (filteredModules.isEmpty()) {
            String message = searchText.isEmpty() ? "No modules in this category" : "No modules found";
            drawCenteredText(message, gridStartX + gridWidth / 2,
                    gridStartY + 12, withAlpha(0x80808A, alpha));
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (contentHeight > gridHeight) {
            drawScrollbar(gridStartX + gridWidth, gridStartY, gridHeight, totalModuleHeight, alpha);
        }
    }

    private List<ModuleButton> getFilteredModules() {
        List<ModuleButton> filtered = new ArrayList<>();
        String query = searchText.toLowerCase();
        for (ModuleButton button : moduleButtons) {
            if (query.isEmpty() || button.module.getName().toLowerCase().contains(query)) {
                filtered.add(button);
            }
        }
        return filtered;
    }

    private int getGridContentHeight(List<ModuleButton> modules, int columns, int spacing) {
        int total = 0;
        for (int rowStart = 0; rowStart < modules.size(); rowStart += columns) {
            int rowHeight = 0;
            int rowCount = Math.min(columns, modules.size() - rowStart);
            for (int i = 0; i < rowCount; i++) {
                rowHeight = Math.max(rowHeight, modules.get(rowStart + i).getHeight());
            }
            total += rowHeight + spacing;
        }
        return Math.max(0, total - spacing);
    }
    
    private void drawScrollbar(int x, int y, int height, int totalHeight, float alpha) {
        int scrollbarHeight = Math.max(24, height * height / totalHeight);
        int maxScroll = Math.max(1, totalHeight - height);
        int scrollbarY = y + (height - scrollbarHeight) * (int) scrollAmount / maxScroll;
        RenderUtils.drawRoundedRect(x + 2, y, x + 5, y + height, 2, withAlpha(0x18181B, alpha));
        RenderUtils.drawRoundedRect(x + 2, scrollbarY, x + 5, scrollbarY + scrollbarHeight,
                2, withAlpha(0x80808A, alpha));
    }
    
    private void drawWatermark(int x, int y, int width, int height, float alpha) {
        int watermarkColor = withAlpha(0x80808A, alpha * 0.5f);
        drawCenteredText("RAVEN B++", x + width / 2, y + height - 20, watermarkColor);
    }

    private void drawText(String text, int x, int y, int color) {
        mc.fontRendererObj.drawString(text, x, y, color, false);
    }

    private void drawCenteredText(String text, int centerX, int y, int color) {
        drawText(text, centerX - mc.fontRendererObj.getStringWidth(text) / 2, y, color);
    }

    private int accentColor() {
        return GuiModule.getClickGuiColor();
    }

    private static int lighten(int rgb, float factor) {
        int red = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        int green = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        int blue = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static String getTabName(ModuleCategory category) {
        switch (category) {
            case movement: return "Move";
            case minigames: return "Mini";
            default: return category.getName();
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int searchBarY = y + 43;
        int searchBarWidth = Math.min(230, width - 40);
        int searchBarX = x + 20;
        int terminalButtonX = x + width - 106;
        if (mouseX >= terminalButtonX && mouseX <= terminalButtonX + 86
                && mouseY >= searchBarY && mouseY <= searchBarY + 26) {
            Terminal terminal = terminal();
            if (terminal.hidden()) {
                terminal.setLocation(x + width - terminal.getWidth() - 16, y + 76);
                terminal.openAndFocus();
            } else {
                terminal.hide();
            }
            searchMode = false;
            return;
        }

        Terminal terminal = terminal();
        terminal.mouseDown(mouseX, mouseY, mouseButton);
        if (terminal.overPosition(mouseX, mouseY))
            return;

        if (mouseX >= searchBarX && mouseX <= searchBarX + searchBarWidth
                && mouseY >= searchBarY && mouseY <= searchBarY + 26) {
            searchMode = true;
            return;
        }
        searchMode = false;
        int tabWidth = Math.max(42, (width - 56) / tabs.size());
        int tabY = y + 8;
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x + 44 + i * tabWidth;
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY && mouseY <= tabY + 24) {
                selectedCategory = tabs.get(i).category;
                scrollAmount = 0;
                bindingModule = null;
                refreshModules();
                return;
            }
        }

        int gridStartY = y + 80;
        int gridStartX = x + 20;
        int gridWidth = width - 40;
        int gridHeight = height - 100;
        if (mouseX < gridStartX || mouseX > gridStartX + gridWidth
                || mouseY < gridStartY || mouseY > gridStartY + gridHeight)
            return;

        List<ModuleButton> filtered = getFilteredModules();
        int spacing = 12;
        int columns = Math.max(1, (gridWidth + spacing) / 180);
        int cardWidth = (gridWidth - spacing * (columns - 1)) / columns;
        int cardY = gridStartY - (int) scrollAmount;
        for (int rowStart = 0; rowStart < filtered.size(); rowStart += columns) {
            int rowCount = Math.min(columns, filtered.size() - rowStart);
            int rowHeight = 0;
            for (int i = 0; i < rowCount; i++) {
                rowHeight = Math.max(rowHeight, filtered.get(rowStart + i).getHeight());
            }
            for (int col = 0; col < rowCount; col++) {
                ModuleButton button = filtered.get(rowStart + col);
                int cardX = gridStartX + col * (cardWidth + spacing);
                if (button.isMouseOverGrid(mouseX, mouseY, cardX, cardY, cardWidth, button.getHeight())) {
                    button.onClick(mouseX, mouseY, mouseButton, cardX, cardY, cardWidth);
                    return;
                }
            }
            cardY += rowHeight + spacing;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        terminal().mouseReleased(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            draggingSlider = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw != 0) {
            scrollAmount += (dw > 0 ? 50 : -50);
            int gridWidth = width - 40;
            int columns = Math.max(1, (gridWidth + 12) / 180);
            int contentHeight = getGridContentHeight(getFilteredModules(), columns, 12);
            int maxScroll = Math.max(0, contentHeight - (height - 100));
            if (scrollAmount < 0) scrollAmount = 0;
            if (scrollAmount > maxScroll) scrollAmount = maxScroll;
        }
    }

    private class CategoryTab {
        ModuleCategory category;

        CategoryTab(ModuleCategory category) {
            this.category = category;
        }
    }

    private class ModuleButton {
        private static final int CARD_HEIGHT = 118;
        Module module;
        boolean expanded = false;
        private long lastAnimationTime = 0;
        private float hoverAnimation = 0f;

        ModuleButton(Module module) {
            this.module = module;
            this.lastAnimationTime = System.currentTimeMillis();
        }

        int getHeight() {
            if (!expanded)
                return CARD_HEIGHT;
            return CARD_HEIGHT + module.getSettings().size() * 26 + (module.isBindable() ? 26 : 0);
        }

        void drawGrid(int bx, int by, int width, int height, int mouseX, int mouseY, float alpha) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastAnimationTime) / 1000f;
            lastAnimationTime = currentTime;

            boolean hover = mouseX >= bx && mouseX <= bx + width
                    && mouseY >= by && mouseY <= by + height;
            float targetHover = hover ? 1f : 0f;
            hoverAnimation += (targetHover - hoverAnimation) * deltaTime * 8f;
            hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

            int cardColor = module.isEnabled() ? accentColor() : 0x18181B;
            RenderUtils.drawRoundedRect(bx, by, bx + width, by + height, 12,
                    withAlpha(cardColor, alpha));
            if (hoverAnimation > 0.01f && !module.isEnabled()) {
                RenderUtils.drawRoundedOutline(bx, by, bx + width, by + height, 12, 1,
                        withAlpha(0x27272A, hoverAnimation * alpha));
            }

            int iconColor = module.isEnabled() ? 0xFFFFFF : 0x80808A;
            RenderUtils.drawRoundedRect(bx + 18, by + 18, bx + 38, by + 38, 5,
                    withAlpha(module.isEnabled() ? lighten(accentColor(), 1.1f) : 0x27272A, alpha));
            drawCenteredText(module.getName().substring(0, 1).toUpperCase(),
                    bx + 28, by + 24, withAlpha(iconColor, alpha));

            int toggleX = bx + width - 61;
            RenderUtils.drawRoundedRect(toggleX, by + 20, toggleX + 28, by + 34, 7,
                    withAlpha(module.isEnabled() ? 0xD9F3F2 : 0x27272A, alpha));
            int knobX = module.isEnabled() ? toggleX + 16 : toggleX + 2;
            RenderUtils.drawRoundedRect(knobX, by + 22, knobX + 10, by + 32, 5,
                    withAlpha(module.isEnabled() ? accentColor() : 0x55555D, alpha));
            for (int i = 0; i < 3; i++) {
                RenderUtils.drawRoundedRect(bx + width - 22, by + 20 + i * 5,
                        bx + width - 19, by + 23 + i * 5, 2,
                        withAlpha(module.isEnabled() ? 0xD9F3F2 : 0x80808A, alpha));
            }

            drawText(module.getName(), bx + 18, by + 68,
                    withAlpha(module.isEnabled() ? 0xFFFFFF : 0xE4E4E7, alpha));
            String subtitle = module.moduleCategory().getName()
                    + (module.getSettings().isEmpty() ? "" : "  |  " + module.getSettings().size() + " settings");
            drawText(subtitle, bx + 18, by + 88,
                    withAlpha(module.isEnabled() ? 0xB7D8D7 : 0x80808A, alpha));

            if (expanded) {
                int sy = by + CARD_HEIGHT;
                for (Setting s : module.getSettings()) {
                    drawSetting(s, bx, sy, width, alpha);
                    sy += 26;
                }
                if (module.isBindable()) {
                    String bindLabel = bindingModule == this ? "Press a key..." : module.getBindAsString();
                    drawText("Keybind", bx + 12, sy + 7,
                            withAlpha(0xB4B4BC, alpha));
                    drawText(bindLabel,
                            bx + width - 12 - mc.fontRendererObj.getStringWidth(bindLabel), sy + 7,
                            withAlpha(0xFFFFFF, alpha));
                }
            }
        }
        
        private void drawSetting(Setting setting, int bx, int sy, int cardWidth, float alpha) {
            int labelColor = withAlpha(0xB4B4BC, alpha);
            int valueColor = withAlpha(0xFFFFFF, alpha);
            drawText(setting.getName(), bx + 12, sy + 6, labelColor);

            if (setting instanceof TickSetting) {
                TickSetting tick = (TickSetting) setting;
                int toggleX = bx + cardWidth - 42;
                RenderUtils.drawRoundedRect(toggleX, sy + 5, toggleX + 28, sy + 19, 7,
                        withAlpha(tick.isToggled() ? accentColor() : 0x27272A, alpha));
                int knobX = tick.isToggled() ? toggleX + 16 : toggleX + 2;
                RenderUtils.drawRoundedRect(knobX, sy + 7, knobX + 10, sy + 17, 5,
                        withAlpha(0xFFFFFF, alpha));
            } else if (setting instanceof SliderSetting) {
                SliderSetting slider = (SliderSetting) setting;
                String value = String.format("%.1f", slider.getInput());
                drawText(value,
                        bx + cardWidth - 12 - mc.fontRendererObj.getStringWidth(value), sy + 6, valueColor);
                drawSliderBar(bx + 12, sy + 18, bx + cardWidth - 12, slider.getInput(),
                        slider.getMin(), slider.getMax(), alpha);
            } else if (setting instanceof DoubleSliderSetting) {
                DoubleSliderSetting slider = (DoubleSliderSetting) setting;
                String value = String.format("%.1f - %.1f", slider.getInputMin(), slider.getInputMax());
                drawText(value,
                        bx + cardWidth - 12 - mc.fontRendererObj.getStringWidth(value), sy + 6, valueColor);
                if (slider.getMax() > slider.getMin()) {
                    int start = bx + 12;
                    int end = bx + cardWidth - 12;
                    int trackY = sy + 18;
                    double range = slider.getMax() - slider.getMin();
                    int minX = start + (int) ((end - start)
                            * ((slider.getInputMin() - slider.getMin()) / range));
                    int maxX = start + (int) ((end - start)
                            * ((slider.getInputMax() - slider.getMin()) / range));
                    RenderUtils.drawRoundedRect(start, trackY, end, trackY + 4, 2,
                            withAlpha(0x27272A, alpha));
                    RenderUtils.drawRoundedRect(minX, trackY, maxX, trackY + 4, 2,
                            withAlpha(lighten(accentColor(), 1.45f), alpha));
                    RenderUtils.drawRoundedRect(minX - 3, trackY - 2, minX + 3, trackY + 6, 3,
                            withAlpha(0xFFFFFF, alpha));
                    RenderUtils.drawRoundedRect(maxX - 3, trackY - 2, maxX + 3, trackY + 6, 3,
                            withAlpha(0xFFFFFF, alpha));
                }
            } else if (setting instanceof ComboSetting) {
                ComboSetting<?> combo = (ComboSetting<?>) setting;
                String value = combo.getMode().toString();
                drawText(value,
                        bx + cardWidth - 12 - mc.fontRendererObj.getStringWidth(value), sy + 6, valueColor);
            } else if (setting instanceof ButtonSetting) {
                drawText("Click", bx + cardWidth - 42, sy + 6, valueColor);
            }
        }

        private void drawSliderBar(int start, int y, int end, double value,
                double min, double max, float alpha) {
            if (max <= min)
                return;
            double percent = Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
            int fillEnd = start + (int) ((end - start) * percent);
            RenderUtils.drawRoundedRect(start, y, end, y + 4, 2, withAlpha(0x27272A, alpha));
            if (fillEnd > start) {
                RenderUtils.drawRoundedRect(start, y, fillEnd, y + 4, 2,
                        withAlpha(lighten(accentColor(), 1.45f), alpha));
            }
            RenderUtils.drawRoundedRect(fillEnd - 3, y - 2, fillEnd + 3, y + 6, 3,
                    withAlpha(0xFFFFFF, alpha));
        }

        boolean isMouseOverGrid(int mx, int my, int bx, int by, int width, int height) {
            return mx >= bx && mx <= bx + width && my >= by && my <= by + height;
        }
        
        void onClick(int mx, int my, int button, int bx, int by, int cardWidth) {
            if (expanded && my >= by + CARD_HEIGHT) {
                int settingY = by + CARD_HEIGHT;
                for (Setting setting : module.getSettings()) {
                    if (my >= settingY && my < settingY + 26) {
                        handleSettingClick(setting, mx, button, bx, cardWidth);
                        return;
                    }
                    settingY += 26;
                }
                if (module.isBindable() && my >= settingY && my < settingY + 26) {
                    bindingModule = this;
                    return;
                }
            } else if (my < by + CARD_HEIGHT) {
                boolean optionsClick = mx >= bx + cardWidth - 30;
                if (optionsClick && (module.isBindable() || !module.getSettings().isEmpty())) {
                    expanded = !expanded;
                } else if (button == 0) {
                    module.toggle();
                } else if (button == 1 && (module.isBindable() || !module.getSettings().isEmpty())) {
                    expanded = !expanded;
                }
            }
        }

        private void handleSettingClick(Setting setting, int mx, int button, int bx, int cardWidth) {
            if (setting instanceof TickSetting) {
                ((TickSetting) setting).toggle();
                module.guiButtonToggled(setting);
            } else if (setting instanceof ComboSetting) {
                ComboSetting<?> combo = (ComboSetting<?>) setting;
                if (button == 0) combo.nextMode();
                else if (button == 1) combo.prevMode();
                module.guiButtonToggled(setting);
            } else if (setting instanceof SliderSetting) {
                SliderSetting slider = (SliderSetting) setting;
                draggingSlider = button == 0 ? slider : null;
                draggingSliderStart = bx + 12;
                draggingSliderEnd = bx + cardWidth - 12;
                updateDraggedSlider(mx);
            } else if (setting instanceof DoubleSliderSetting) {
                DoubleSliderSetting slider = (DoubleSliderSetting) setting;
                double value = sliderValue(mx, bx, cardWidth, slider.getMin(), slider.getMax());
                double minDistance = Math.abs(value - slider.getInputMin());
                double maxDistance = Math.abs(value - slider.getInputMax());
                draggingMaxHandle = minDistance > maxDistance;
                draggingSlider = button == 0 ? slider : null;
                draggingSliderStart = bx + 12;
                draggingSliderEnd = bx + cardWidth - 12;
                updateDraggedSlider(mx);
            } else if (setting instanceof ButtonSetting) {
                module.guiButtonToggled(setting);
            }
        }

        private double sliderValue(int mouseX, int bx, int cardWidth, double min, double max) {
            int start = bx + 12;
            int end = bx + cardWidth - 12;
            double percent = (double) (mouseX - start) / (end - start);
            percent = Math.max(0.0, Math.min(1.0, percent));
            return min + percent * (max - min);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        Terminal terminal = terminal();
        terminal.keyTyped(typedChar, keyCode);
        if (terminal.isFocused())
            return;

        if (bindingModule != null) {
            bindingModule.module.setbind(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            bindingModule = null;
            return;
        }

        if (searchMode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                searchMode = false;
                return;
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                searchMode = false;
                return;
            } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && keyCode == Keyboard.KEY_A) {
                searchText = "";
                scrollAmount = 0;
                return;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    scrollAmount = 0;
                }
            } else if (!Character.isISOControl(typedChar) && typedChar != 167) {
                searchText += typedChar;
                scrollAmount = 0;
            }
            return;
        }
        try {
            super.keyTyped(typedChar, keyCode);
        } catch (Exception e) {
            // Ignore any exceptions from super method
        }
    }
}
