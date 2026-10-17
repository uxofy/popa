package com.example.popa;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PopaScreen extends Screen {
    private final Screen parent;
    private Button intervalButton;

    public PopaScreen(Screen parent) {
        super(Component.literal("Popa"));
        this.parent = parent;
    }

    private static String onOff(boolean v) { return v ? "ВКЛ" : "ВЫКЛ"; }

    private Component intervalText() {
        return Component.literal("Интервал: " + PopaConfig.interval + " тик.");
    }

    @Override
    protected void init() {
        int w = 240, h = 20, x = this.width / 2 - w / 2, y = this.height / 4, gap = 26;

        addRenderableWidget(Button.builder(Component.literal("Мод: " + onOff(PopaConfig.enabled)), b -> {
            PopaConfig.enabled = !PopaConfig.enabled;
            b.setMessage(Component.literal("Мод: " + onOff(PopaConfig.enabled)));
        }).bounds(x, y, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Ставить кристаллы: " + onOff(PopaConfig.place)), b -> {
            PopaConfig.place = !PopaConfig.place;
            b.setMessage(Component.literal("Ставить кристаллы: " + onOff(PopaConfig.place)));
        }).bounds(x, y + gap, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Взрывать кристаллы: " + onOff(PopaConfig.explode)), b -> {
            PopaConfig.explode = !PopaConfig.explode;
            b.setMessage(Component.literal("Взрывать кристаллы: " + onOff(PopaConfig.explode)));
        }).bounds(x, y + gap * 2, w, h).build());

        // [-] [Интервал: N] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            PopaConfig.interval = Math.max(1, PopaConfig.interval - 1);
            intervalButton.setMessage(intervalText());
        }).bounds(x, y + gap * 3, 30, h).build());

        intervalButton = Button.builder(intervalText(), b -> {
            PopaConfig.interval = 3; // сброс на 3
            b.setMessage(intervalText());
        }).bounds(x + 34, y + gap * 3, w - 68, h).build();
        addRenderableWidget(intervalButton);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            PopaConfig.interval = Math.min(20, PopaConfig.interval + 1);
            intervalButton.setMessage(intervalText());
        }).bounds(x + w - 30, y + gap * 3, 30, h).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x, y + gap * 4 + 8, w, h).build());
    }

    private void centered(GuiGraphics g, String text, int cx, int y, int color) {
        g.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        centered(g, "Popa", this.width / 2, 20, 0xFFFFFFFF);
        centered(g, "Клавиши: Настройки > Управление > Popa", this.width / 2, this.height - 30, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        PopaConfig.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
