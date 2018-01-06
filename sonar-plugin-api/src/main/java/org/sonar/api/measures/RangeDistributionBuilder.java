/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.measures;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang.NumberUtils;
import org.sonar.api.utils.KeyValueFormat;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Utility to build a distribution based on defined ranges
 * <br>
 * <p>An example of usage : you wish to record the percentage of lines of code that belong to method
 * with pre-defined ranges of complexity.
 *
 * @since 1.10
 * @deprecated since 5.2 use {@link org.sonar.api.ce.measure.RangeDistributionBuilder instead}
 */
@Deprecated
public class RangeDistributionBuilder implements MeasureBuilder {

  private final Metric<String> metric;
  private final SortedMultiset countBag = TreeMultiset.create();
  private boolean isEmpty = true;
  private Number[] bottomLimits;
  private RangeTransformer rangeValueTransformer;
  private boolean isValid = true;

  /**
   * RangeDistributionBuilder for a metric and a defined range
   * Each entry is initialized at zero
   *
   * @param metric       the metric to record the measure against
   * @param bottomLimits the bottom limits of ranges to be used
   */
  public RangeDistributionBuilder(Metric<String> metric, Number[] bottomLimits) {
    requireNonNull(metric, "Metric must not be null");
    checkArgument(metric.isDataType(), "Metric %s must have data type", metric.key());
    this.metric = metric;
    init(bottomLimits);
  }

  public RangeDistributionBuilder(Metric<String> metric) {
    this.metric = metric;
  }

  private void init(Number[] bottomLimits) {
    this.bottomLimits = new Number[bottomLimits.length];
    System.arraycopy(bottomLimits, 0, this.bottomLimits, 0, this.bottomLimits.length);
    Arrays.sort(this.bottomLimits);
    changeDoublesToInts();
    doClear();
    this.rangeValueTransformer = new RangeTransformer();
  }

  private void changeDoublesToInts() {
    boolean onlyInts = true;
    for (Number bottomLimit : bottomLimits) {
      if (NumberUtils.compare(bottomLimit.intValue(), bottomLimit.doubleValue()) != 0) {
        onlyInts = false;
      }
    }
    if (onlyInts) {
      for (int i = 0; i < bottomLimits.length; i++) {
        bottomLimits[i] = bottomLimits[i].intValue();
      }
    }
  }

  /**
   * Gives the bottom limits of ranges used
   *
   * @return the bottom limits of defined range for the distribution
   */
  public Number[] getBottomLimits() {
    return bottomLimits;
  }

  /**
   * Increments an entry by 1
   *
   * @param value the value to use to pick the entry to increment
   * @return the current object
   */
  public RangeDistributionBuilder add(Number value) {
    return add(value, 1);
  }

  /**
   * Increments an entry
   *
   * @param value the value to use to pick the entry to increment
   * @param count the number by which to increment
   * @return the current object
   */
  public RangeDistributionBuilder add(@Nullable Number value, int count) {
    if (value != null && greaterOrEqualsThan(value, bottomLimits[0])) {
      this.countBag.add(rangeValueTransformer.apply(value), count);
      isEmpty = false;
    }
    return this;
  }

  private RangeDistributionBuilder addLimitCount(Number limit, int count) {
    for (Number bottomLimit : bottomLimits) {
      if (NumberUtils.compare(bottomLimit.doubleValue(), limit.doubleValue()) == 0) {
        this.countBag.add(rangeValueTransformer.apply(limit), count);
        isEmpty = false;
        return this;
      }
    }
    isValid = false;
    return this;
  }

  /**
   * Adds an existing Distribution to the current one.
   * It will create the entries if they don't exist.
   * Can be used to add the values of children resources for example
   * <br>
   * Since 2.2, the distribution returned will be invalidated in case the
   * measure given does not use the same bottom limits
   *
   * @param measure the measure to add to the current one
   * @return the current object
   */
  public RangeDistributionBuilder add(@Nullable Measure<String> measure) {
    if (measure != null && measure.getData() != null) {
      Map<Double, Double> map = KeyValueFormat.parse(measure.getData(), KeyValueFormat.newDoubleConverter(), KeyValueFormat.newDoubleConverter());
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
    }
    return this;
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

  /**
   * Resets all entries to zero
   *
   * @return the current object
   */
  public RangeDistributionBuilder clear() {
    doClear();
    return this;
  }

  private void doClear() {
    countBag.clear();
    if (bottomLimits != null) {
      Collections.addAll(countBag, bottomLimits);
    }
    isEmpty = true;
  }

  /**
   * @return whether the current object is empty or not
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  /**
   * Shortcut for <code>build(true)</code>
   *
   * @return the built measure
   */
  @Override
  public Measure<String> build() {
    return build(true);
  }

  /**
   * Used to build a measure from the current object
   *
   * @param allowEmptyData should be built if current object is empty
   * @return the built measure
   */
  public Measure<String> build(boolean allowEmptyData) {
    if (isValid && (!isEmpty || allowEmptyData)) {
      return new Measure<>(metric, MultisetDistributionFormat.format(countBag));
    }
    return null;
  }

  private class RangeTransformer implements Function<Number, Number> {
    @Override
    public Number apply(Number n) {
      for (int i = bottomLimits.length - 1; i >= 0; i--) {
        if (greaterOrEqualsThan(n, bottomLimits[i])) {
          return bottomLimits[i];
        }
      }
      return null;
    }
  }

  private static boolean greaterOrEqualsThan(Number n1, Number n2) {
    return NumberUtils.compare(n1.doubleValue(), n2.doubleValue()) >= 0;
  }
}
