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
package org.sonar.api.measures;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.collections.SortedBag;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.bag.TransformedSortedBag;
import org.apache.commons.collections.bag.TreeBag;
import org.apache.commons.lang.NumberUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.SonarException;

/**
 * Utility to build a distribution based on defined ranges
 * <p/>
 * <p>An example of usage : you wish to record the percentage of lines of code that belong to method
 * with pre-defined ranges of complexity.</p>
 *
 * @since 1.10
 * @deprecated since 5.2 use {@link org.sonar.api.ce.measure.RangeDistributionBuilder instead}
 */
@Deprecated
public class RangeDistributionBuilder implements MeasureBuilder {

  private Metric<String> metric;
  private SortedBag countBag;
  private boolean isEmpty = true;
  private Number[] bottomLimits;
  private boolean isValid = true;

  /**
   * RangeDistributionBuilder for a metric and a defined range
   * Each entry is initialized at zero
   *
   * @param metric       the metric to record the measure against
   * @param bottomLimits the bottom limits of ranges to be used
   */
  public RangeDistributionBuilder(Metric<String> metric, Number[] bottomLimits) {
    setMetric(metric);
    init(bottomLimits);
  }

  private void init(Number[] bottomLimits) {
    this.bottomLimits = new Number[bottomLimits.length];
    System.arraycopy(bottomLimits, 0, this.bottomLimits, 0, this.bottomLimits.length);
    Arrays.sort(this.bottomLimits);
    changeDoublesToInts();
    countBag = TransformedSortedBag.decorate(new TreeBag(), new RangeTransformer());
    doClear();
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

  public RangeDistributionBuilder(Metric<String> metric) {
    this.metric = metric;
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
  public RangeDistributionBuilder add(Number value, int count) {
    if (value != null && greaterOrEqualsThan(value, bottomLimits[0])) {
      this.countBag.add(value, count);
      isEmpty = false;
    }
    return this;
  }

  private RangeDistributionBuilder addLimitCount(Number limit, int count) {
    for (Number bottomLimit : bottomLimits) {
      if (NumberUtils.compare(bottomLimit.doubleValue(), limit.doubleValue()) == 0) {
        this.countBag.add(limit, count);
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
   * <p/>
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
    if (countBag != null) {
      countBag.clear();
    }
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
      return new Measure<>(metric, KeyValueFormat.format(countBag, -1));
    }
    return null;
  }

  private class RangeTransformer implements Transformer {
    @Override
    public Object transform(Object o) {
      Number n = (Number) o;
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

  private void setMetric(Metric metric) {
    if (metric == null || !metric.isDataType()) {
      throw new SonarException("Metric is null or has unvalid type");
    }
    this.metric = metric;
  }
}
