//package junit.samples.money;
package com.cenqua.samples.money;

import java.util.*;

/**
 * A MoneyBag defers exchange rate conversions. For example adding
 * 12 Swiss Francs to 14 US Dollars is represented as a bag
 * containing the two Monies 12 CHF and 14 USD. Adding another
 * 10 Swiss francs gives a bag with 22 CHF and 14 USD. Due to
 * the deferred exchange rate conversion we can later value a
 * MoneyBag with different exchange rates.
 * <p/>
 * A MoneyBag is represented as a list of Monies and provides
 * different constructors to create a MoneyBag.
 */
class MoneyBag implements IMoney {
    private Vector fMonies = new Vector(5);

    static IMoney create(IMoney m1, IMoney m2) {
        MoneyBag result = new MoneyBag();
        m1.appendTo(result);
        m2.appendTo(result);
        return result.simplify();
    }

    public IMoney add(IMoney m) {
        return m.addMoneyBag(this);
    }

    public IMoney addMoney(Money m) {
        return MoneyBag.create(m, this);
    }

    public IMoney addMoneyBag(MoneyBag s) {
        return MoneyBag.create(s, this);
    }

    void appendBag(MoneyBag aBag) {
        for (Enumeration e = aBag.fMonies.elements(); e.hasMoreElements();)
            appendMoney((Money) e.nextElement());
    }

    void appendMoney(Money aMoney) {
        if (aMoney.isZero()) return;
        IMoney old = findMoney(aMoney.currency());
        if (old == null) {
            fMonies.addElement(aMoney);
            return;
        }
        fMonies.removeElement(old);
        IMoney sum = old.add(aMoney);
        if (sum.isZero())
            return;
        fMonies.addElement(sum);
    }

    public boolean equals(Object anObject) {
        if (isZero())
            if (anObject instanceof IMoney)
                return ((IMoney) anObject).isZero();

        if (anObject instanceof MoneyBag) {
            MoneyBag aMoneyBag = (MoneyBag) anObject;
            if (aMoneyBag.fMonies.size() != fMonies.size())
                return false;

            for (Enumeration e = fMonies.elements(); e.hasMoreElements();) {
                Money m = (Money) e.nextElement();
                if (!aMoneyBag.contains(m))
                    return false;
            }
            return true;
        }
        return false;
    }

    private Money findMoney(String currency) {
        for (Enumeration e = fMonies.elements(); e.hasMoreElements();) {
            Money m = (Money) e.nextElement();
            if (m.currency().equals(currency))
                return m;
        }
        return null;
    }

    private boolean contains(Money m) {
        Money found = findMoney(m.currency());
        if (found == null) return false;
        return found.amount() == m.amount();
    }

    public int hashCode() {
        int hash = 0;
        for (Enumeration e = fMonies.elements(); e.hasMoreElements();) {
            Object m = e.nextElement();
            hash ^= m.hashCode();
        }
        return hash;
    }

    public boolean isZero() {
        return fMonies.size() == 0;
    }

    public IMoney multiply(int factor) {
        MoneyBag result = new MoneyBag();
        if (factor != 0) {
            for (Enumeration e = fMonies.elements(); e.hasMoreElements();) {
                Money m = (Money) e.nextElement();
                result.appendMoney((Money) m.multiply(factor));
            }
        }
        return result;
    }

    public IMoney negate() {
        MoneyBag result = new MoneyBag();
        for (Enumeration e = fMonies.elements(); e.hasMoreElements();) {
            Money m = (Money) e.nextElement();
            result.appendMoney((Money) m.negate());
        }
        return result;
    }

    private IMoney simplify() {
        if (fMonies.size() == 1)
            return (IMoney) fMonies.elements().nextElement();
        return this;
    }

    public IMoney subtract(IMoney m) {
        return add(m.negate());
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        for (Enumeration e = fMonies.elements(); e.hasMoreElements();)
            buffer.append(e.nextElement());
        buffer.append("}");
        return buffer.toString();
    }

    public void appendTo(MoneyBag m) {
        m.appendBag(this);
    }
}