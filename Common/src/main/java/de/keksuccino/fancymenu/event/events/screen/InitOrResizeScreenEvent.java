package de.keksuccino.fancymenu.event.events.screen;

import de.keksuccino.fancymenu.event.acara.EventBase;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Objects;


public class InitOrResizeScreenEvent extends EventBase {

    protected final Screen screen;

    protected InitOrResizeScreenEvent(Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public Screen getScreen() {
        return this.screen;
    }

    /** You should not get the widgets list in Pre, because it is fired BEFORE the list gets cleared. **/
    public static class Pre extends InitOrResizeScreenEvent {

        public Pre(Screen screen) {
            super(screen);
        }

    }

    public static class Post extends InitOrResizeScreenEvent {

        public Post(Screen screen) {
            super(screen);
        }

        public <T extends GuiEventListener & NarratableEntry> void addWidget(T widget) {
            ((IMixinScreen)this.getScreen()).getChildrenFancyMenu().add(widget);
        }

        public <T extends GuiEventListener & NarratableEntry & Renderable> void addRenderableWidget(T widget) {
            this.addWidget(widget);
            ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu().add(widget);
        }

        public List<GuiEventListener> getWidgets() {
            return ((IMixinScreen)this.getScreen()).getChildrenFancyMenu();
        }

        public List<Renderable> getRenderables() {
            return ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu();
        }

    }

}
