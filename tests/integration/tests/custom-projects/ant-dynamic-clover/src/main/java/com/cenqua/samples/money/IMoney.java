//package junit.samples.money;
package com.cenqua.samples.money;

/**
 * The common interface for simple Monies and MoneyBags
 */
public interface IMoney {
    /**
     * Adds a money to this money.
     */
    public abstract IMoney add(IMoney m);

    /**
     * Adds a simple Money to this money. This is a helper method for
     * implementing double dispatch
     */
    public abstract IMoney addMoney(Money m);

    /**
     * Adds a MoneyBag to this money. This is a helper method for
     * implementing double dispatch
     */
    public abstract IMoney addMoneyBag(MoneyBag s);

    /**
     * Tests whether this money is zero
     */
    public abstract boolean isZero();

    /**
     * Multiplies a money by the given factor.
     */
    public abstract IMoney multiply(int factor);

    /**
     * Negates this money.
     */
    public abstract IMoney negate();

    /**
     * Subtracts a money from this money.
     */
    public abstract IMoney subtract(IMoney m);

    /**
     * Append this to a MoneyBag m.
     */
    public abstract void appendTo(MoneyBag m);
}