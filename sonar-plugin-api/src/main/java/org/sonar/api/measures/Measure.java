/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.annotations.Beta;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * A class to handle measures.
 *
 * @since 1.10
 */
public class Measure {
  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";

  protected static final int MAX_TEXT_SIZE = 96;

  /**
   * Default precision when saving a float type metric
   */
  public static final int DEFAULT_PRECISION = 1;

  // for internal use
  private Long id;
  protected String metricKey;
  protected Metric metric;
  protected Double value;
  protected String data;
  protected String description;
  protected Metric.Level alertStatus;
  protected String alertText;
  protected Integer tendency;
  protected Date date;
  protected Double variation1, variation2, variation3, variation4, variation5;
  protected String url;
  protected Characteristic characteristic;
  protected Requirement requirement;
  protected Integer personId;
  protected PersistenceMode persistenceMode = PersistenceMode.FULL;

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
  public Measure(Metric metric, Metric.Level level) {
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
   * Gets the persistence mode of the measure. Default persistence mode is FULL, except when instantiating the measure with a String
   * parameter.
   */
  public PersistenceMode getPersistenceMode() {
    return persistenceMode;
  }

  /**
   * <p>
   * Sets the persistence mode of a measure.
   * </p>
   * <p>
   * <b>WARNING : </b>Being able to reuse measures saved in memory is only possible within the same tree. In a multi-module project for
   * example, a measure save in memory at the module level will not be accessible by the root project. In that case, database should be
   * used.
   * </p>
   *
   * @param mode the mode
   * @return the measure object instance
   */
  public Measure setPersistenceMode(PersistenceMode mode) {
    this.persistenceMode = mode;
    return this;
  }

  /**
   * @return return the measures underlying metric
   */
  public Metric getMetric() {
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
  public Measure setMetric(Metric metric) {
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
  public Measure setDate(Date date) {
    this.date = date;
    return this;
  }

  /**
   * @return the value of the measure as a double
   */
  public Double getValue() {
    return value;
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
  public Measure setValue(@Nullable Double v) {
    return setValue(v, DEFAULT_PRECISION);
  }

  /**
   * Sets the measure value as an int
   *
   * @param i the value
   * @return the measure object instance
   */
  public Measure setIntValue(Integer i) {
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
   * @param v         the measure value
   * @param precision the measure value precision
   * @return the measure object instance
   */
  public Measure setValue(@Nullable Double v, int precision) {
    if (v != null) {
      if (Double.isNaN(v)) {
        throw new IllegalArgumentException("Measure value can not be NaN");
      }
      this.value = scaleValue(v, precision);
    } else {
      this.value = null;
    }
    return this;
  }

  private double scaleValue(double value, int scale) {
    BigDecimal bd = BigDecimal.valueOf(value);
    return bd.setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }

  /**
   * @return the data field of the measure
   */
  public String getData() {
    return data;
  }

  /**
   * Sets the data field of the measure.
   *
   * @param s the data
   * @return the measure object instance
   */
  public Measure setData(String s) {
    this.data = s;
    return this;
  }

  /**
   * Sets an alert level as the data field
   *
   * @param level the alert level
   * @return the measure object instance
   */
  public Measure setData(Metric.Level level) {
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
  public Measure unsetData() {
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
  public Measure setDescription(String description) {
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
  public Measure setAlertStatus(Metric.Level status) {
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
  public Measure setAlertText(String alertText) {
    this.alertText = alertText;
    return this;
  }

  /**
   * Gets the measure tendency
   *
   * @return the tendency
   */
  public Integer getTendency() {
    return tendency;
  }

  /**
   * Sets the tendency for the measure - Internal use only
   *
   * @param tendency the tendency
   * @return the measure object instance
   */
  public Measure setTendency(Integer tendency) {
    this.tendency = tendency;
    return this;
  }

  /**
   * @return the measure id - Internal use only
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the measure id - Internal use only
   *
   * @param id the id
   * @return the measure object instance
   */
  public Measure setId(Long id) {
    this.id = id;
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
  public Measure setVariation1(Double d) {
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
  public Measure setVariation2(Double d) {
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
  public Measure setVariation3(Double d) {
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
  public Measure setVariation4(Double d) {
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
  public Measure setVariation5(Double d) {
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
  public Measure setVariation(int index, Double d) {
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
  public Measure setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * @since 4.1
   */
  @CheckForNull
  public final Characteristic getCharacteristic() {
    return characteristic;
  }

  /**
   * @since 4.1
   */
  public final Measure setCharacteristic(@Nullable Characteristic characteristic) {
    this.characteristic = characteristic;
    return this;
  }

  /**
   * @since 4.1
   */
  @CheckForNull
  public final Requirement getRequirement() {
    return requirement;
  }

  /**
   * @since 4.1
   */
  public final Measure setRequirement(@Nullable Requirement requirement) {
    this.requirement = requirement;
    return this;
  }

  /**
   * @since 2.14
   */
  @Beta
  public Integer getPersonId() {
    return personId;
  }

  /**
   * @since 2.14
   */
  @Beta
  public Measure setPersonId(Integer i) {
    this.personId = i;
    return this;
  }

  /**
   * @since 3.2
   */
  public boolean isBestValue() {
    return metric.isOptimizedBestValue() == Boolean.TRUE
      && metric.getBestValue() != null
      && (value == null || NumberUtils.compare(metric.getBestValue(), value) == 0)
      && allNull(id, alertStatus, description, tendency, url, data)
      && isZeroVariation(variation1, variation2, variation3, variation4, variation5);
  }

  private static boolean isZeroVariation(Double... variations) {
    for (Double variation : variations) {
      if (!((variation == null) || NumberUtils.compare(variation.doubleValue(), 0.0) == 0)) {
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
    if (metricKey != null ? !metricKey.equals(measure.metricKey) : measure.metricKey != null) {
      return false;
    }
    if (characteristic != null ? !characteristic.equals(measure.characteristic) : measure.characteristic != null) {
      return false;
    }
    if (requirement != null ? !requirement.equals(measure.requirement) : measure.requirement != null) {
      return false;
    }
    if (personId != null ? !personId.equals(measure.personId) : measure.personId != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = metricKey != null ? metricKey.hashCode() : 0;
    result = 31 * result + (characteristic != null ? characteristic.hashCode() : 0);
    result = 31 * result + (requirement != null ? requirement.hashCode() : 0);
    result = 31 * result + (personId != null ? personId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
