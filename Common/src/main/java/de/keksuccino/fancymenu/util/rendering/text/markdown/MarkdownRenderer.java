package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.FocuslessContainerEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class MarkdownRenderer extends GuiComponent implements Renderable, FocuslessContainerEventHandler, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected String text = "";
    protected String renderText;
    protected float x;
    protected float y;
    protected float optimalWidth;
    protected float realWidth;
    protected float realHeight;
    @NotNull
    protected DrawableColor codeBlockSingleLineColor = DrawableColor.of(new Color(115, 115, 115));
    @NotNull
    protected DrawableColor codeBlockMultiLineColor = DrawableColor.of(new Color(86, 86, 86));
    @NotNull
    protected DrawableColor headlineUnderlineColor = DrawableColor.of(new Color(169, 169, 169));
    @NotNull
    protected DrawableColor separationLineColor = DrawableColor.of(new Color(169, 169, 169));
    @NotNull
    protected DrawableColor hyperlinkColor = DrawableColor.of(new Color(7, 113, 252));
    @NotNull
    protected DrawableColor quoteColor = DrawableColor.of(new Color(129, 129, 129));
    protected float quoteIndent = 8;
    protected boolean quoteItalic = false;
    @NotNull
    protected DrawableColor bulletListDotColor = DrawableColor.of(new Color(169, 169, 169));
    protected float bulletListIndent = 8;
    protected float bulletListSpacing = 3;
    @NotNull
    protected DrawableColor textBaseColor = DrawableColor.WHITE;
    @NotNull
    protected TextCase textCase = TextCase.NORMAL;
    protected float textBaseScale = 1.0f;
    protected boolean autoLineBreaks = true;
    protected boolean textShadow = true;
    protected float lineSpacing = 2;
    protected float border = 2;
    public boolean skipRefresh = false;
    @Nullable
    protected Float parentRenderScale = null;
    @NotNull
    protected Font font = Minecraft.getInstance().font;
    protected boolean dragging;
    protected final List<MarkdownTextLine> lines = new ArrayList<>();
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();
    protected final List<ConsumingSupplier<MarkdownTextLine, Boolean>> lineRenderValidators = new ArrayList<>();

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.onRender(pose, mouseX, mouseY, partial, true);
    }

    protected void onRender(PoseStack pose, int mouseX, int mouseY, float partial, boolean shouldRender) {

        this.tick();

        float lineOffsetY = this.border;
        for (MarkdownTextLine line : this.lines) {
            float lineAlignmentOffsetX = 0;
            if (line.isAlignmentAllowed(line.alignment)) {
                float realInnerWidth = this.getRealWidth() - this.border - this.border;
                if (line.alignment == MarkdownLineAlignment.CENTERED) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.getLineWidth());
                    if (lineAlignmentOffsetX > 0) lineAlignmentOffsetX = lineAlignmentOffsetX / 2f;
                }
                if (line.alignment == MarkdownLineAlignment.RIGHT) {
                    lineAlignmentOffsetX = Math.max(0, realInnerWidth - line.getLineWidth());
                }
            }
            line.offsetX = this.border + lineAlignmentOffsetX;
            line.offsetY = lineOffsetY;
            if (shouldRender && this.isLineRenderingAllowedByValidators(line)) {
                line.render(pose, mouseX, mouseY, partial);
            }
            lineOffsetY += line.getLineHeight() + this.lineSpacing;
        }

//        if (shouldRender) {
//            UIBase.renderBorder(pose, this.x + this.border, this.y + this.border, this.x + this.realWidth - this.border, this.y + this.realHeight - this.border, 1, this.hyperlinkColor.getColorInt(), true, true, true, true);
//        }

//        LOGGER.info("### TOTAL HEIGHT RENDER: " + lineOffsetY + " | REAL HEIGHT: " + this.getRealHeight());

    }

    public void tick() {

        String newRenderText = this.buildRenderText();
        if ((this.renderText == null) || !this.renderText.equals(newRenderText)) {
            this.renderText = newRenderText;
            this.refresh();
        }

        this.updateSize();

    }

    public void updateSize() {
        this.realWidth = 0;
        this.realHeight = this.border;
        for (MarkdownTextLine l : this.lines) {
            float lw = l.getLineWidth();
            if (lw > this.realWidth) {
                this.realWidth = lw;
            }
            this.realHeight = this.realHeight + l.getLineHeight() + this.lineSpacing;
        }
        this.realWidth += this.border + this.border;
        this.realHeight = this.realHeight + this.border;
    }

    public void refresh() {
        if (this.skipRefresh) return;
        if (this.renderText == null) {
            this.renderText = this.buildRenderText();
        }
        this.rebuildFragments();
        this.rebuildLines();
        this.onRender(null, 0, 0, 0, false);
    }

    public void rebuildFragments() {
        this.fragments.clear();
        if (this.renderText != null) {
            this.fragments.addAll(MarkdownParser.parse(this, this.renderText));
        }
    }

    protected void rebuildLines() {

        this.lines.clear();

        boolean queueNewLine = true;
        float totalWidth = 20;
        float totalHeight = this.border;
        float currentLineWidth = this.border;
        float currentLineHeight = 0;
        MarkdownTextFragment lastFragment = null;
        MarkdownTextLine line = new MarkdownTextLine(this);

        for (MarkdownTextFragment f : this.fragments) {

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            f.autoLineBreakAfter = false;

            //Handle Auto Line Break
            if (!isStartOfLine && this.isAutoLineBreakingEnabled()) {
                f.startOfRenderLine = false;
                if (lastFragment.endOfWord && ((lastFragment.codeBlockContext == null) || lastFragment.codeBlockContext.singleLine) && ((currentLineWidth + f.getRenderWidth() + this.border) > this.optimalWidth)) {
                    if (totalWidth < currentLineWidth) {
                        totalWidth = currentLineWidth;
                    }
                    currentLineWidth = this.border;
                    line.offsetX = this.border;
                    line.offsetY = totalHeight;
                    totalHeight += currentLineHeight + this.lineSpacing;
                    currentLineHeight = 0;
                    isStartOfLine = true;
                    line.prepareLine();
                    this.lines.add(line);
                    line = new MarkdownTextLine(this);
                }
            }

            f.startOfRenderLine = isStartOfLine;

            line.fragments.add(f);

            float fw = f.getRenderWidth();
            float fh = f.getRenderHeight();
            currentLineWidth += fw;
            if (currentLineHeight < fh) {
                currentLineHeight = fh;
            }

            //Handle Natural Line Break
            if (f.naturalLineBreakAfter) {
                if (totalWidth < currentLineWidth) {
                    totalWidth = currentLineWidth;
                }
                line.offsetX = this.border;
                line.offsetY = totalHeight;
                currentLineWidth = this.border;
                totalHeight += currentLineHeight + this.lineSpacing;
                currentLineHeight = 0;
                queueNewLine = true;
                line.prepareLine();
                this.lines.add(line);
                line = new MarkdownTextLine(this);
            }

            lastFragment = f;

        }

        if (this.lines.isEmpty()) {
            this.lines.add(line);
        }

    }

    @NotNull
    protected String buildRenderText() {
        return PlaceholderParser.replacePlaceholders(this.text).replace("%n%", "\n").replace("\r", "\n");
    }

    public MarkdownRenderer addLineRenderValidator(@NotNull ConsumingSupplier<MarkdownTextLine, Boolean> validator) {
        this.lineRenderValidators.add(validator);
        return this;
    }

    protected boolean isLineRenderingAllowedByValidators(@NotNull MarkdownTextLine line) {
        for (ConsumingSupplier<MarkdownTextLine, Boolean> validator : this.lineRenderValidators) {
            if (!validator.get(line)) return false;
        }
        return true;
    }

    @NotNull
    public String getText() {
        return this.text;
    }

    public MarkdownRenderer setText(@NotNull String text) {
        this.text = Objects.requireNonNull(text);
        return this;
    }

    public float getX() {
        return this.x;
    }

    public MarkdownRenderer setX(float x) {
        this.x = x;
        return this;
    }

    public float getY() {
        return this.y;
    }

    public MarkdownRenderer setY(float y) {
        this.y = y;
        return this;
    }

    public MarkdownRenderer setOptimalWidth(float width) {
        this.optimalWidth = width;
        this.refresh();
        return this;
    }

    public float getOptimalWidth() {
        return this.optimalWidth;
    }

    public float getRealWidth() {
        //If real width smaller than optimal, return optimal
        return Math.max(this.realWidth, this.optimalWidth);
    }

    public float getRealHeight() {
        return this.realHeight;
    }

    @Nullable
    public Float getParentRenderScale() {
        return this.parentRenderScale;
    }

    public MarkdownRenderer setParentRenderScale(@Nullable Float parentRenderScale) {
        this.parentRenderScale = parentRenderScale;
        this.refresh();
        return this;
    }

    public boolean isAutoLineBreakingEnabled() {
        return this.autoLineBreaks;
    }

    public MarkdownRenderer setAutoLineBreakingEnabled(boolean enabled) {
        this.autoLineBreaks = enabled;
        this.refresh();
        return this;
    }

    @NotNull
    public DrawableColor getSeparationLineColor() {
        return this.separationLineColor;
    }

    public MarkdownRenderer setSeparationLineColor(@NotNull DrawableColor separationLineColor) {
        this.separationLineColor = separationLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockSingleLineColor() {
        return this.codeBlockSingleLineColor;
    }

    public MarkdownRenderer setCodeBlockSingleLineColor(@NotNull DrawableColor codeBlockSingleLineColor) {
        this.codeBlockSingleLineColor = codeBlockSingleLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockMultiLineColor() {
        return this.codeBlockMultiLineColor;
    }

    public MarkdownRenderer setCodeBlockMultiLineColor(@NotNull DrawableColor codeBlockMultiLineColor) {
        this.codeBlockMultiLineColor = codeBlockMultiLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getHeadlineUnderlineColor() {
        return this.headlineUnderlineColor;
    }

    public MarkdownRenderer setHeadlineLineColor(@NotNull DrawableColor headlineUnderlineColor) {
        this.headlineUnderlineColor = headlineUnderlineColor;
        return this;
    }

    @NotNull
    public DrawableColor getHyperlinkColor() {
        return this.hyperlinkColor;
    }

    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.hyperlinkColor = Objects.requireNonNull(hyperlinkColor);
        return this;
    }

    @NotNull
    public DrawableColor getQuoteColor() {
        return this.quoteColor;
    }

    public MarkdownRenderer setQuoteColor(@NotNull DrawableColor quoteColor) {
        this.quoteColor = Objects.requireNonNull(quoteColor);
        return this;
    }

    @NotNull
    public TextCase getTextCase() {
        return this.textCase;
    }

    public MarkdownRenderer setTextCase(@NotNull TextCase textCase) {
        this.textCase = Objects.requireNonNull(textCase);
        return this;
    }

    public float getTextBaseScale() {
        return this.textBaseScale;
    }

    public MarkdownRenderer setTextBaseScale(float textBaseScale) {
        this.textBaseScale = textBaseScale;
        this.refresh();
        return this;
    }

    @NotNull
    public DrawableColor getTextBaseColor() {
        return this.textBaseColor;
    }

    public MarkdownRenderer setTextBaseColor(@NotNull DrawableColor textBaseColor) {
        this.textBaseColor = textBaseColor;
        return this;
    }

    @NotNull
    public DrawableColor getBulletListDotColor() {
        return this.bulletListDotColor;
    }

    public MarkdownRenderer setBulletListDotColor(@NotNull DrawableColor bulletListDotColor) {
        this.bulletListDotColor = bulletListDotColor;
        return this;
    }

    public float getBulletListIndent() {
        return this.bulletListIndent;
    }

    public MarkdownRenderer setBulletListIndent(float bulletListIndent) {
        this.bulletListIndent = bulletListIndent;
        this.refresh();
        return this;
    }

    public float getBulletListSpacing() {
        return this.bulletListSpacing;
    }

    public MarkdownRenderer setBulletListSpacing(float bulletListSpacing) {
        this.bulletListSpacing = bulletListSpacing;
        this.refresh();
        return this;
    }

    public boolean isQuoteItalic() {
        return this.quoteItalic;
    }

    public MarkdownRenderer setQuoteItalic(boolean quoteItalic) {
        this.quoteItalic = quoteItalic;
        return this;
    }

    public float getQuoteIndent() {
        return this.quoteIndent;
    }

    public MarkdownRenderer setQuoteIndent(float quoteIndent) {
        this.quoteIndent = quoteIndent;
        this.refresh();
        return this;
    }

    public boolean isTextShadow() {
        return this.textShadow;
    }

    public MarkdownRenderer setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public float getLineSpacing() {
        return this.lineSpacing;
    }

    public MarkdownRenderer setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
        this.refresh();
        return this;
    }

    public float getBorder() {
        return this.border;
    }

    public MarkdownRenderer setBorder(float border) {
        this.border = border;
        this.refresh();
        return this;
    }

    @Override
    @NotNull
    public List<MarkdownTextFragment> children() {
        return this.fragments;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    public enum TextCase {
        NORMAL,
        ALL_LOWER,
        ALL_UPPER
    }

    public enum MarkdownLineAlignment {
        LEFT,
        CENTERED,
        RIGHT
    }

}
