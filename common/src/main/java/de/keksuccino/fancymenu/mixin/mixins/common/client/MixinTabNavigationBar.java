package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.widget.RenderTabNavigationBarHeaderBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabNavigationBar.class)
public class MixinTabNavigationBar {

    @Shadow @Final private static int HEIGHT;
    @Shadow private int width;

    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/TabManager;setCurrentTab(Lnet/minecraft/client/gui/components/tabs/Tab;Z)V", shift = At.Shift.AFTER))
    private void afterSetCurrentTabInSetFocused_FancyMenu(GuiEventListener guiEventListener, CallbackInfo info) {
        this.reInitScreenAfterTabChanged_FancyMenu();
    }

    @Inject(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/TabManager;setCurrentTab(Lnet/minecraft/client/gui/components/tabs/Tab;Z)V", shift = At.Shift.AFTER))
    private void afterSetCurrentTabInSelectTab_FancyMenu(int i, boolean b, CallbackInfo info) {
        this.reInitScreenAfterTabChanged_FancyMenu();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private boolean wrapHeaderSeparatorRenderingInRender_FancyMenu(GuiGraphics instance, ResourceLocation p_283272_, int p_283605_, int p_281879_, float p_282809_, float p_282942_, int p_281922_, int p_282385_, int p_282596_, int p_281699_) {
        if (this.isBarPartOfCurrentScreen_FancyMenu()) {
            if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
                ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
                if (layer != null) {
                    return layer.layoutBase.renderScrollListHeaderShadow;
                }
            }
        }
        return true;
    }

    //TODO experimental
//    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
//    private void wrapHeaderBackgroundRenderingInRender_FancyMenu(GuiGraphics instance, int fromX, int fromY, int toX, int toY, int color, Operation<Void> original) {
//
//        if (this.isBarPartOfCurrentScreen_FancyMenu()) {
//
//            RenderTabNavigationBarHeaderBackgroundEvent.Pre pre = new RenderTabNavigationBarHeaderBackgroundEvent.Pre(this.getBar_FancyMenu(), instance, toX, toY);
//            EventHandler.INSTANCE.postEvent(pre);
//
//            if (!pre.isCanceled()) {
//                original.call(instance, fromX, fromY, toX, toY, color);
//            }
//
//            EventHandler.INSTANCE.postEvent(new RenderTabNavigationBarHeaderBackgroundEvent.Post(this.getBar_FancyMenu(), instance, toX, toY));
//
//        } else {
//            original.call(instance, fromX, fromY, toX, toY, color);
//        }
//
//    }

    //TODO experimental
    @Inject(method = "render", at = @At("HEAD"))
    private void head_render_FancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new RenderTabNavigationBarHeaderBackgroundEvent.Pre(this.getBar_FancyMenu(), graphics, this.width, HEIGHT));
    }

    //TODO experimental
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V", shift = At.Shift.AFTER))
    private void after_disableBlend_in_render_FancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new RenderTabNavigationBarHeaderBackgroundEvent.Post(this.getBar_FancyMenu(), graphics, this.width, HEIGHT));
    }

    //Without this FancyMenu wouldn't be able to customize Tab screens (even with this it's still not fully working, but at least it doesn't completely break everything anymore)
    @Unique
    private void reInitScreenAfterTabChanged_FancyMenu() {
        if (Minecraft.getInstance().screen != null) {
            if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen) && this.isBarPartOfCurrentScreen_FancyMenu()) {
                ScreenCustomization.reInitCurrentScreen();
            }
        }
    }

    @Unique
    private boolean isBarPartOfCurrentScreen_FancyMenu() {
        if (Minecraft.getInstance().screen == null) return false;
        return ((IMixinScreen)Minecraft.getInstance().screen).getChildrenFancyMenu().contains(this.getBar_FancyMenu());
    }

    @Unique
    private TabNavigationBar getBar_FancyMenu() {
        return (TabNavigationBar)((Object)this);
    }

}
