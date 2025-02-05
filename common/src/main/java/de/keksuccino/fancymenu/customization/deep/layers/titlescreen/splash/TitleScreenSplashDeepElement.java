package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.splash;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class TitleScreenSplashDeepElement extends AbstractDeepElement {

    private static final DrawableColor DEFAULT_COLOR = DrawableColor.of(new Color(255, 255, 0));

    public static String cachedSplashText;

    public TitleScreenSplashDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        this.baseWidth = 100;
        this.baseHeight = 30;

        RenderSystem.enableBlend();
        this.renderSplash(graphics, Minecraft.getInstance().font);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    protected void renderSplash(GuiGraphics graphics, Font font) {

        if (cachedSplashText == null) {
            cachedSplashText = Minecraft.getInstance().getSplashManager().getSplash();
        }
        if (cachedSplashText == null) {
            cachedSplashText = "§c< ERROR! UNABLE TO GET SPLASH TEXT! >";
        }

        graphics.pose().pushPose();
        graphics.pose().translate(this.getAbsoluteX() + 50, this.getAbsoluteY() + 15, 0.0F);
        graphics.pose().mulPose(Vector3f.ZP.rotationDegrees(-20));
        float f = 1.8F - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        f = f * 100.0F / (float) (font.width(cachedSplashText) + 32);
        graphics.pose().scale(f, f, f);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
        graphics.drawCenteredString(font, Components.literal(cachedSplashText), 0, -8, DEFAULT_COLOR.getColorIntWithAlpha(this.opacity));

        graphics.pose().popPose();

    }

    @Override
    public int getAbsoluteX() {
        return ((getScreenWidth() / 2) + 90) - 50;
    }

    @Override
    public int getAbsoluteY() {
        return 70 - 15;
    }

    @Override
    public int getAbsoluteWidth() {
        return 100;
    }

    @Override
    public int getAbsoluteHeight() {
        return 30;
    }

}