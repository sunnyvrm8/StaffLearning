// Scenario: checkout fee rules for a payments service (flat, percent, regional)
// Demonstrates: Open/Closed — register new FeeRule without editing Calculator
// Trade-off: small registry vs giant switch; YAGNI if only one rule exists

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

interface FeeRule {
    String id();
    BigDecimal apply(BigDecimal subtotal, Map<String, String> ctx);
}

final class FlatFeeRule implements FeeRule {
    private final String id;
    private final BigDecimal amount;
    FlatFeeRule(String id, BigDecimal amount) { this.id = id; this.amount = amount; }
    public String id() { return id; }
    public BigDecimal apply(BigDecimal subtotal, Map<String, String> ctx) { return amount; }
}

final class FeeCalculator {
    private final Map<String, FeeRule> rules;
    FeeCalculator(List<FeeRule> rules) {
        this.rules = rules.stream().collect(
            java.util.stream.Collectors.toMap(FeeRule::id, Function.identity()));
    }
    BigDecimal total(String ruleId, BigDecimal subtotal, Map<String, String> ctx) {
        FeeRule rule = rules.get(ruleId);
        if (rule == null) throw new IllegalArgumentException("unknown rule: " + ruleId);
        return rule.apply(subtotal, ctx);
    }
}
