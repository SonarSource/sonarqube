/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sonar.api.utils.KeyValueFormat;

/**
 * Utility to build a distribution based on defined ranges
 * <p/>
 * <p>An example of usage : you wish to record the percentage of lines of code that belong to method
 * with pre-defined ranges of complexity.</p>
 *
 */
public class RangeDistributionBuilder {

  private Multiset<Number> distributionSet;
  private boolean isEmpty = true;
  private Number[] bottomLimits;
  private boolean isValid = true;

  /**
   * Adds an existing Distribution to the current one.
   * It will create the entries if they don't exist.
   * Can be used to add the values of children resources for example
   * <p/>
   * Since 2.2, the distribution returned will be invalidated in case the
   * measure given does not use the same bottom limits
   *
   * @param data the data to add to the current one
   * @return the current object
   */
  public RangeDistributionBuilder add(String data) {
    Map<Double, Double> map = KeyValueFormat.parse(data, KeyValueFormat.newDoubleConverter(), KeyValueFormat.newDoubleConverter());
    Number[] limits = map.keySet().toArray(new Number[map.size()]);
    if (bottomLimits == null) {
      init(limits);

    } else if (!areSameLimits(bottomLimits, map.keySet())) {
      isValid = false;
    }

    if (isValid) {
      for (Map.Entry<Double, Double> entry : map.entrySet()) {
        addLimitCount(entry.getKey(), entry.getValue().intValue());
      }
    }
    return this;
  }

  private void init(Number[] bottomLimits) {
    this.bottomLimits = new Number[bottomLimits.length];
    System.arraycopy(bottomLimits, 0, this.bottomLimits, 0, this.bottomLimits.length);
    Arrays.sort(this.bottomLimits);
    changeDoublesToInts();
    distributionSet = TreeMultiset.create(NumberComparator.INSTANCE);
  }

  private void changeDoublesToInts() {
    boolean onlyInts = true;
    for (Number bottomLimit : bottomLimits) {
      if (NumberComparator.INSTANCE.compare(bottomLimit.intValue(), bottomLimit.doubleValue()) != 0) {
        onlyInts = false;
      }
    }
    if (onlyInts) {
      for (int i = 0; i < bottomLimits.length; i++) {
        bottomLimits[i] = bottomLimits[i].intValue();
      }
    }
  }

  private static boolean areSameLimits(Number[] bottomLimits, Set<Double> limits) {
    if (limits.size() == bottomLimits.length) {
      for (Number l : bottomLimits) {
        if (!limits.contains(l.doubleValue())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private RangeDistributionBuilder addLimitCount(Number limit, int count) {
    for (Number bottomLimit : bottomLimits) {
      if (NumberComparator.INSTANCE.compare(bottomLimit.doubleValue(), limit.doubleValue()) == 0) {
        addValue(limit, count);
        isEmpty = false;
        return this;
      }
    }
    isValid = false;
    return this;
  }

  private void addValue(Number value, int count) {
    for (int i = bottomLimits.length - 1; i >= 0; i--) {
      if (greaterOrEqualsThan(value, bottomLimits[i])) {
        this.distributionSet.add(bottomLimits[i], count);
        return;
      }
    }
  }

  /**
   * @return whether the current object is empty or not
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  /**
   * Used to build a measure from the current object
   *
   * @return the built measure
   */
  public Optional<String> build() {
    if (isValid) {
      return Optional.of(KeyValueFormat.format(toMap()));
    }
    return Optional.absent();
  }

  private Map<Number, Integer> toMap() {
    if (bottomLimits == null || bottomLimits.length == 0) {
      return Collections.emptyMap();
    }
    Map<Number, Integer> map = new TreeMap<>();
    for (int i = 0; i < bottomLimits.length; i++) {
      Number value = bottomLimits[i];
      map.put(value, distributionSet.count(value));
    }
    return map;
  }

  private static boolean greaterOrEqualsThan(Number n1, Number n2) {
    return NumberComparator.INSTANCE.compare(n1, n2) >= 0;
  }

  private enum NumberComparator implements Comparator<Number> {
    INSTANCE;

    @Override
    public int compare(Number n1, Number n2) {
      return ((Double) n1.doubleValue()).compareTo(n2.doubleValue());
    }
  }

}
