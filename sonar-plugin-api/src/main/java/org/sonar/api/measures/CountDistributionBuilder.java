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

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.SonarException;

/**
 * Utility to build a distribution based on discrete values
 *
 * <p>An example of usage : you wish to record the number of violations for each level of rules priority
 *
 * @since 1.10
 * @deprecated since 5.6. Scanner side is not responsible to aggregate measures since 5.2.
 */
@Deprecated
public class CountDistributionBuilder implements MeasureBuilder {

  private final Metric metric;
  private final Multiset countBag = TreeMultiset.create();

  /**
   * Creates an empty CountDistributionBuilder for a specified metric
   *
   * @param metric the metric
   */
  public CountDistributionBuilder(Metric metric) {
    if (metric == null || !metric.isDataType()) {
      throw new SonarException("Metric is null or has invalid type");
    }
    this.metric = metric;
  }

  /**
   * Increments an entry
   *
   * @param value the value that should be incremented
   * @param count the number by which to increment
   * @return the current object
   */
  public CountDistributionBuilder add(Object value, int count) {
    if (count == 0) {
      addZero(value);

    } else {
      if (this.countBag.add(value, count) == 0) {
        // hack
        this.countBag.add(value, 1);
      }
    }
    return this;
  }

  /**
   * Increments an entry by one
   *
   * @param value the value that should be incremented
   * @return the current object
   */
  public CountDistributionBuilder add(Object value) {
    return add(value, 1);
  }

  /**
   * Adds an entry without a zero count if it does not exist
   *
   * @param value the entry to be added
   * @return the current object
   */
  public CountDistributionBuilder addZero(Object value) {
    if (!countBag.contains(value)) {
      countBag.add(value, 1);
    }
    return this;
  }

  /**
   * Adds an existing Distribution to the current one.
   * It will create the entries if they don't exist.
   * Can be used to add the values of children resources for example
   *
   * @param measure the measure to add to the current one
   * @return the current object
   */
  public CountDistributionBuilder add(@Nullable Measure measure) {
    if (measure != null && measure.getData() != null) {
      Map<String, String> map = KeyValueFormat.parse(measure.getData());
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey();
        int value = StringUtils.isBlank(entry.getValue()) ? 0 : Integer.parseInt(entry.getValue());
        if (NumberUtils.isNumber(key)) {
          add(NumberUtils.toInt(key), value);
        } else {
          add(key, value);
        }
      }
    }
    return this;
  }

  /**
   * @return whether the current object is empty or not
   */
  public boolean isEmpty() {
    return countBag.isEmpty();
  }

  /**
   * Resets all entries to zero
   *
   * @return the current object
   */
  public CountDistributionBuilder clear() {
    countBag.clear();
    return this;
  }

  /**
   * Shortcut for <code>build(true)</code>
   *
   * @return the built measure
   */
  @Override
  public Measure build() {
    return build(true);
  }

  /**
   * Used to build a measure from the current object
   *
   * @param allowEmptyData should be built if current object is empty
   * @return the built measure
   */
  public Measure build(boolean allowEmptyData) {
    if (!isEmpty() || allowEmptyData) {
      return new Measure(metric, MultisetDistributionFormat.format(countBag));
    }
    return null;
  }
}
