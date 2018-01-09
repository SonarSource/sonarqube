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

import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.batch.sensor.SensorContext;

/**
 * A class to handle measures.
 *
 * @since 1.10
 * @deprecated since 5.6. To create a new measure on scanner side use {@link SensorContext#newMeasure()}
 */
@Deprecated
public class Measure<G extends Serializable> implements Serializable {
  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";

  protected static final int MAX_TEXT_SIZE = 96;

  /**
   * Default precision when saving a float type metric
   * @deprecated in 5.3. Decimal scale is provided by metric, not by measure.
   */
  @Deprecated
  public static final int DEFAULT_PRECISION = 1;

  protected String metricKey;
  protected Metric<G> metric;
  protected Double value;
  protected String data;
  protected String description;
  protected Metric.Level alertStatus;
  protected String alertText;
  protected Date date;
  protected Double variation1;
  protected Double variation2;
  protected Double variation3;
  protected Double variation4;
  protected Double variation5;
  protected String url;

  public Measure(String metricKey) {
    this.metricKey = metricKey;
  }

  /**
   * Creates a measure with a metric
   *
   * @param metric the metric
   */
  public Measure(Metric metric) {
    this.metric = metric;
    this.metricKey = metric.getKey();
  }

  /**
   * Creates a measure with a metric and a value
   *
   * @param metric the metric
   * @param value  its value
   */
  public Measure(Metric metric, Double value) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    setValue(value);
  }

  /**
   * Creates a measure with a metric, a value and a precision for the value
   *
   * @param metric    the metric
   * @param value     its value
   * @param precision the value precision
   */
  public Measure(Metric metric, Double value, int precision) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    setValue(value, precision);
  }

  /**
   * Creates a measure with a metric, a value and a data field
   *
   * @param metric the metric
   * @param value  the value
   * @param data   the data field
   */
  public Measure(Metric metric, Double value, String data) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    setValue(value);
    setData(data);
  }

  /**
   * * Creates a measure with a metric and a data field
   *
   * @param metric the metric
   * @param data   the data field
   */
  public Measure(Metric metric, String data) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    setData(data);
  }

  /**
   * Creates a measure with a metric and an alert level
   *
   * @param metric the metric
   * @param level  the alert level
   */
  public Measure(Metric metric, @Nullable Metric.Level level) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    if (level != null) {
      this.data = level.toString();
    }
  }

  /**
   * Creates an empty measure
   */
  public Measure() {
  }

  /**
   * @return return the measures underlying metric
   */
  public Metric<G> getMetric() {
    return metric;
  }

  public String getMetricKey() {
    return metricKey;
  }

  /**
   * Set the underlying metric
   *
   * @param metric the metric
   * @return the measure object instance
   */
  public Measure<G> setMetric(Metric<G> metric) {
    this.metric = metric;
    this.metricKey = metric.getKey();
    return this;
  }

  /**
   * @return transforms and returns the data fields as a level of alert
   */
  public Metric.Level getDataAsLevel() {
    if (data != null) {
      return Metric.Level.valueOf(data);
    }
    return null;
  }

  public boolean hasData() {
    return data != null;
  }

  /**
   * @return the date of the measure, i.e. the date the measure was taken. Used only in TimeMachine queries
   */
  public Date getDate() {
    return date;
  }

  /**
   * Sets the date of the measure - Used only in TimeMachine queries
   *
   * @param date the date
   * @return the measure object instance
   */
  public Measure<G> setDate(Date date) {
    this.date = date;
    return this;
  }

  /**
   * @return the value of the measure as a double
   */
  @CheckForNull
  public Double getValue() {
    return value;
  }

  /**
   * For internal use.
   */
  public G value() {
    if (value == null) {
      return null;
    }
    switch (getMetric().getType()) {
      case BOOL:
        return (G) Boolean.valueOf(Double.doubleToRawLongBits(value) != 0L);
      case INT:
      case MILLISEC:
      case RATING:
        return (G) Integer.valueOf(value.intValue());
      case FLOAT:
      case PERCENT:
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        return (G) data;
      case WORK_DUR:
        return (G) Long.valueOf(value.longValue());
      default:
        if (getMetric().isNumericType() || getMetric().isDataType()) {
          return (G) value;
        }
        throw new UnsupportedOperationException("Unsupported type :" + getMetric().getType());
    }
  }

  /**
   * @return the value of the measure as an int
   */
  public Integer getIntValue() {
    if (value == null) {
      return null;
    }
    return value.intValue();
  }

  /**
   * Sets the measure value with the default precision of 1
   *
   * @param v the measure value
   * @return the measure object instance
   */
  public Measure<G> setValue(@Nullable Double v) {
    return setValue(v, DEFAULT_PRECISION);
  }

  /**
   * For internal use
   */
  public Measure<G> setRawValue(@Nullable Double v) {
    this.value = v;
    return this;
  }

  /**
   * Sets the measure value as an int
   *
   * @param i the value
   * @return the measure object instance
   */
  public Measure<G> setIntValue(@Nullable Integer i) {
    if (i == null) {
      this.value = null;
    } else {
      this.value = Double.valueOf(i);
    }
    return this;
  }

  /**
   * Sets the measure value with a given precision
   *
   * @return {@code this}
   * @deprecated in 5.3. The decimal scale is given by the metric, not by the measure. Anyway this parameter was enforced to 1 before version 5.3.
   */
  @Deprecated
  public Measure<G> setValue(@Nullable Double v, int decimalScale) {
    if (v != null) {
      if (Double.isNaN(v)) {
        throw new IllegalArgumentException("Measure value can not be NaN");
      }
      this.value = v;
    } else {
      this.value = null;
    }
    return this;
  }

  /**
   * @return the data field of the measure
   */
  @CheckForNull
  public String getData() {
    return data;
  }

  /**
   * Sets the data field of the measure.
   *
   * @param s the data
   * @return the measure object instance
   */
  public Measure<G> setData(String s) {
    this.data = s;
    return this;
  }

  /**
   * Sets an alert level as the data field
   *
   * @param level the alert level
   * @return the measure object instance
   */
  public Measure<G> setData(@Nullable Metric.Level level) {
    if (level == null) {
      this.data = null;
    } else {
      this.data = level.toString();
    }
    return this;
  }

  /**
   * @since 2.7
   */
  public Measure<G> unsetData() {
    this.data = null;
    return this;
  }

  /**
   * @return the description of the measure
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the measure description
   *
   * @param description the description
   * @return the measure object instance
   */
  public Measure<G> setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * @return the alert status of the measure
   */
  public Metric.Level getAlertStatus() {
    return alertStatus;
  }

  /**
   * Set the alert status of the measure
   *
   * @param status the status
   * @return the measure object instance
   */
  public Measure<G> setAlertStatus(@Nullable Metric.Level status) {
    this.alertStatus = status;
    return this;
  }

  /**
   * @return the text associated to the alert on the measure
   */
  public String getAlertText() {
    return alertText;
  }

  /**
   * Sets the text associated to the alert on the measure
   *
   * @param alertText the text
   * @return the measure object instance
   */
  public Measure<G> setAlertText(@Nullable String alertText) {
    this.alertText = alertText;
    return this;
  }

  /**
   * Concept of measure trend is dropped.
   * @deprecated since 5.2. See https://jira.sonarsource.com/browse/SONAR-6392
   * @return {@code null} since version 5.2
   */
  @Deprecated
  @CheckForNull
  public Integer getTendency() {
    return null;
  }

  /**
   * Concept of measure trend is dropped. This method does nothing.
   * @deprecated since 5.2. See https://jira.sonarsource.com/browse/SONAR-6392
   * @return the measure object instance
   */
  @Deprecated
  public Measure<G> setTendency(@Nullable Integer tendency) {
    return this;
  }

  /**
   * @return the first variation value
   * @since 2.5
   */
  public Double getVariation1() {
    return variation1;
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation1(@Nullable Double d) {
    this.variation1 = d;
    return this;
  }

  /**
   * @return the second variation value
   * @since 2.5
   */
  public Double getVariation2() {
    return variation2;
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation2(@Nullable Double d) {
    this.variation2 = d;
    return this;
  }

  /**
   * @return the third variation value
   * @since 2.5
   */
  public Double getVariation3() {
    return variation3;
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation3(@Nullable Double d) {
    this.variation3 = d;
    return this;
  }

  /**
   * @return the third variation value
   * @since 2.5
   */
  public Double getVariation4() {
    return variation4;
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation4(@Nullable Double d) {
    this.variation4 = d;
    return this;
  }

  /**
   * @return the third variation value
   * @since 2.5
   */
  public Double getVariation5() {
    return variation5;
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation5(@Nullable Double d) {
    this.variation5 = d;
    return this;
  }

  /**
   * @since 2.5
   */
  public Double getVariation(int index) {
    switch (index) {
      case 1:
        return variation1;
      case 2:
        return variation2;
      case 3:
        return variation3;
      case 4:
        return variation4;
      case 5:
        return variation5;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
  }

  /**
   * Internal use only
   *
   * @since 2.5
   */
  public Measure<G> setVariation(int index, Double d) {
    switch (index) {
      case 1:
        variation1 = d;
        break;
      case 2:
        variation2 = d;
        break;
      case 3:
        variation3 = d;
        break;
      case 4:
        variation4 = d;
        break;
      case 5:
        variation5 = d;
        break;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
    return this;
  }

  /**
   * @return the url of the measure
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL of the measure
   *
   * @param url the url
   * @return the measure object instance
   */
  public Measure<G> setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * @since 2.14
   * @deprecated in 6.5 with end of support of Developer cockpit plugin. Always return {@code null}.
   */
  @CheckForNull
  @Deprecated
  public Integer getPersonId() {
    return null;
  }

  /**
   * @since 2.14
   * @deprecated in 6.5 with end of support of Developer cockpit plugin.
   */
  @Deprecated
  public Measure<G> setPersonId(@Nullable Integer i) {
    return this;
  }

  /**
   * @since 3.2
   */
  public boolean isBestValue() {
    Double bestValue = metric.getBestValue();
    return metric.isOptimizedBestValue() == Boolean.TRUE
      && bestValue != null
      && (value == null || NumberUtils.compare(bestValue, value) == 0)
      && allNull(alertStatus, description, url, data)
      && isZeroVariation(variation1, variation2, variation3, variation4, variation5);
  }

  private static boolean isZeroVariation(Double... variations) {
    for (Double variation : variations) {
      if (variation != null && NumberUtils.compare(variation, 0.0) != 0) {
        return false;
      }
    }
    return true;
  }

  private static boolean allNull(Object... values) {
    for (Object value : values) {
      if (null != value) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Measure measure = (Measure) o;
    return metricKey != null ? metricKey.equals(measure.metricKey) : (measure.metricKey == null);
  }

  @Override
  public int hashCode() {
    return metricKey != null ? metricKey.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

}
