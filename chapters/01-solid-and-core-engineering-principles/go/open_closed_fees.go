// Scenario: checkout fee rules for a payments service (flat, percent, regional)
// Demonstrates: Open/Closed — register new FeeRule without editing Calculator
// Trade-off: map registry vs switch; Go uses small interfaces + registration

package main

import (
	"fmt"
	"math/big"
)

type FeeRule interface {
	ID() string
	Apply(subtotal *big.Rat, ctx map[string]string) *big.Rat
}

type flatFee struct{ id string; amount *big.Rat }

func (f flatFee) ID() string { return f.id }
func (f flatFee) Apply(_ *big.Rat, _ map[string]string) *big.Rat { return new(big.Rat).Set(f.amount) }

type FeeCalculator struct{ rules map[string]FeeRule }

func NewFeeCalculator(rules []FeeRule) *FeeCalculator {
	m := make(map[string]FeeRule, len(rules))
	for _, r := range rules {
		m[r.ID()] = r
	}
	return &FeeCalculator{rules: m}
}

func (c *FeeCalculator) Total(ruleID string, subtotal *big.Rat, ctx map[string]string) (*big.Rat, error) {
	r, ok := c.rules[ruleID]
	if !ok {
		return nil, fmt.Errorf("unknown rule: %s", ruleID)
	}
	return r.Apply(subtotal, ctx), nil
}
