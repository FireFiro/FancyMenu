package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class CycleButton<T> extends ExtendedButton {

    protected final ILocalizedValueCycle<T> cycle;
    protected final CycleButtonClickFeedback<T> clickFeedback;

    public CycleButton(int x, int y, int width, int height, @NotNull ILocalizedValueCycle<T> cycle, @NotNull CycleButtonClickFeedback<T> clickFeedback) {
        super(x, y, width, height, Components.empty(), var1 -> {});
        this.cycle = cycle;
        this.clickFeedback = clickFeedback;
        this.setPressAction(var1 -> {
            this.click();
        });
        this.setLabel(this.cycle.getCycleComponent());
    }

    public void click() {
        this.cycle.next();
        this.clickFeedback.onClick(cycle.current(), this);
    }

    @NotNull
    public T getSelectedValue() {
        return this.cycle.current();
    }

    public CycleButton<T> setSelectedValue(@NotNull T value) {
        this.cycle.setCurrentValue(Objects.requireNonNull(value));
        return this;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.setLabel(this.cycle.getCycleComponent());
        super.render(pose, mouseX, mouseY, partial);
    }

    @FunctionalInterface
    public interface CycleButtonClickFeedback<T> {
        void onClick(T value, CycleButton<T> button);
    }

}
