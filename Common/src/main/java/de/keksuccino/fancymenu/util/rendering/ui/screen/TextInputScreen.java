package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedEditBox;
import de.keksuccino.konkrete.input.CharacterFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TextInputScreen extends Screen {

    @NotNull
    protected Consumer<String> callback;

    protected ExtendedEditBox input;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;

    public TextInputScreen(@NotNull Component title, @Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {

        super(title);
        this.callback = callback;

        this.input = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.empty());
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(filter);

    }

    @Override
    protected void init() {

        this.addWidget(this.input);

        this.cancelButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.input.getValue());
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

    }

    @Override
    public void tick() {
        this.input.tick();
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color_darker.getColorInt());

        RenderSystem.enableBlend();
        MutableComponent t = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        int titleWidth = Minecraft.getInstance().font.width(t);
        this.font.draw(pose, t, (int)(this.width / 2) - (int)(titleWidth / 2), (int)(this.height / 2) - 30, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.input.x = (this.width / 2) - (this.input.getWidth() / 2);
        this.input.y = (this.height / 2) - (this.input.getHeight() / 2);
        this.input.render(pose, mouseX, mouseY, partial);

        this.cancelButton.x = (this.width / 2) - 5 - this.cancelButton.getWidth();
        this.cancelButton.y = this.height - 40;
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        this.doneButton.x = (this.width / 2) + 5;
        this.doneButton.y = this.height - 40;
        this.doneButton.render(pose, mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        // ENTER
        if (button == InputConstants.KEY_ENTER) {
            this.callback.accept(this.input.getValue());
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    public void setText(@NotNull String text) {
        this.input.setValue(text);
    }

}
