package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.advanced;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.objecthunter.exp4j.Expression;
import de.keksuccino.konkrete.objecthunter.exp4j.ExpressionBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class CalculatorPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public CalculatorPlaceholder() {
        super("calc");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String decimalString = dps.values.get("decimal");
        boolean decimal = true;
        if ((decimalString != null) && decimalString.equalsIgnoreCase("false")) {
            decimal = false;
        }
        String ex = dps.values.get("expression");
        if (ex != null) {
            try {
                Expression expression = new ExpressionBuilder(ex).build();
                if (expression.validate().isValid()) {
                    if (decimal) {
                        return "" + expression.evaluate();
                    }
                    return "" + Math.round(expression.evaluate());
                }
            } catch (Exception exception) {
                LOGGER.error("[FANCYMENU] Failed to parse expression in Calculator placeholder: Placeholder: " + dps + " | Original String: " + dps.originalString, exception);
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("expression");
        l.add("decimal");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.placeholder.calc");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.calc.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("decimal", "true");
        m.put("expression", "2 + 1 - 10");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
