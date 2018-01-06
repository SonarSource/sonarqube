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

import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * An utility class to manipulate measures
 *
 * @since 1.10
 * @deprecated since 5.6. {@link Measure} is deprecated.
 */
@Deprecated
public final class MeasureUtils {

  /**
   * Class cannot be instantiated, it should only be access through static methods
   */
  private MeasureUtils() {
  }

  /**
   * Return true if all measures have numeric value
   *
   * @param measures the measures
   * @return true if all measures numeric values
   */
  public static boolean haveValues(Measure... measures) {
    if (measures.length == 0) {
      return false;
    }
    for (Measure measure : measures) {
      if (!hasValue(measure)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the value of a measure, or alternatively a default value
   *
   * @param measure      the measure
   * @param defaultValue the default value
   * @return <code>defaultValue</code> if measure is null or has no values.
   */

  public static Double getValue(Measure measure, @Nullable Double defaultValue) {
    if (MeasureUtils.hasValue(measure)) {
      return measure.getValue();
    }
    return defaultValue;
  }

  public static Long getValueAsLong(Measure measure, Long defaultValue) {
    if (MeasureUtils.hasValue(measure)) {
      return measure.getValue().longValue();
    }
    return defaultValue;
  }

  public static Double getVariation(@Nullable Measure measure, int periodIndex) {
    return getVariation(measure, periodIndex, null);
  }

  public static Double getVariation(@Nullable Measure measure, int periodIndex, @Nullable Double defaultValue) {
    Double result = null;
    if (measure != null) {
      result = measure.getVariation(periodIndex);
    }
    return result != null ? result : defaultValue;
  }

  public static Long getVariationAsLong(@Nullable Measure measure, int periodIndex) {
    return getVariationAsLong(measure, periodIndex, null);
  }

  public static Long getVariationAsLong(@Nullable Measure measure, int periodIndex, @Nullable Long defaultValue) {
    Double result = null;
    if (measure != null) {
      result = measure.getVariation(periodIndex);
    }
    return result == null ? defaultValue : Long.valueOf(result.longValue());
  }

  /**
   * Tests if a measure has a value
   *
   * @param measure the measure
   * @return whether the measure has a value
   */
  public static boolean hasValue(@Nullable Measure measure) {
    return measure != null && measure.getValue() != null;
  }

  /**
   * Tests if a measure has a data field
   *
   * @param measure the measure
   * @return whether the measure has a data field
   */
  public static boolean hasData(@Nullable Measure measure) {
    return measure != null && StringUtils.isNotBlank(measure.getData());
  }

  /**
   * Sums a series of measures
   *
   * @param zeroIfNone whether to return 0 or null in case measures is null
   * @param measures   the series of measures
   * @return the sum of the measure series
   */
  public static Double sum(boolean zeroIfNone, @Nullable Collection<Measure> measures) {
    if (measures != null) {
      return sum(zeroIfNone, measures.toArray(new Measure[measures.size()]));
    }
    return zeroIfNone(zeroIfNone);
  }

  /**
   * Sums a series of measures
   *
   * @param zeroIfNone whether to return 0 or null in case measures is null
   * @param measures   the series of measures
   * @return the sum of the measure series
   */
  public static Double sum(boolean zeroIfNone, Measure... measures) {
    Double sum = 0d;
    boolean hasValue = false;
    for (Measure measure : measures) {
      if (measure != null && measure.getValue() != null) {
        hasValue = true;
        sum += measure.getValue();
      }
    }

    if (hasValue) {
      return sum;
    }
    return zeroIfNone(zeroIfNone);
  }

  /**
   * Sums a series of measures for the given variation index
   *
   * @param zeroIfNone whether to return 0 or null in case measures is null
   * @param variationIndex the index of the variation to use
   * @param measures   the series of measures
   * @return the sum of the variations for the measure series
   */
  public static Double sumOnVariation(boolean zeroIfNone, int variationIndex, @Nullable Collection<Measure> measures) {
    if (measures == null) {
      return zeroIfNone(zeroIfNone);
    }
    Double sum = 0d;
    for (Measure measure : measures) {
      Double var = measure.getVariation(variationIndex);
      if (var != null) {
        sum += var;
      }
    }
    return sum;
  }

  private static Double zeroIfNone(boolean zeroIfNone) {
    return zeroIfNone ? 0d : null;
  }
}
