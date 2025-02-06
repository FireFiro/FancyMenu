package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemKeyScreen extends StringBuilderScreen {

    @NotNull
    protected String itemKey;

    protected TextInputCell itemKeyCell;
    protected EditBoxSuggestions itemKeySuggestions;

    protected ItemKeyScreen(@NotNull String value, @NotNull Consumer<String> callback) {
        super(Components.translatable("fancymenu.elements.item.key"), callback);
        this.itemKey = value;
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        String key = this.getItemKeyString();
        this.addLabelCell(Components.translatable("fancymenu.elements.item.key.screen.key"));
        this.itemKeyCell = this.addTextInputCell(null, true, true).setText(key);

        this.addCellGroupEndSpacerCell();

        this.itemKeySuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.itemKeyCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, this.getItemKeys());
        UIBase.applyDefaultWidgetSkinTo(this.itemKeySuggestions);
        this.itemKeyCell.editBox.setResponder(s -> this.itemKeySuggestions.updateCommandInfo());

        this.addSpacerCell(20);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        this.itemKeySuggestions.render(graphics.pose(), mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int $$0, int $$1, int $$2) {
        if (this.itemKeySuggestions.keyPressed($$0, $$1, $$2)) return true;
        return super.keyPressed($$0, $$1, $$2);
    }

    @Override
    public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaY) {
        if (this.itemKeySuggestions.mouseScrolled(scrollDeltaY)) return true;
        return super.mouseScrolled($$0, $$1, scrollDeltaY);
    }

    @Override
    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        if (this.itemKeySuggestions.mouseClicked($$0, $$1, $$2)) return true;
        return super.mouseClicked($$0, $$1, $$2);
    }

    @Override
    public @NotNull String buildString() {
        return this.getItemKeyString();
    }

    @NotNull
    protected String getItemKeyString() {
        if (this.itemKeyCell != null) {
            return this.itemKeyCell.getText();
        }
        return this.itemKey;
    }

    @NotNull
    protected List<String> getItemKeys() {
        List<String> keys = new ArrayList<>();
        Registry.ITEM.keySet().forEach(location -> keys.add("" + location));
        return keys;
    }

}
