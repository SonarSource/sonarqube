/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.measures;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.qualitymodel.Characteristic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * A class to handle measures.
 * <p/>
 *
 * @since 1.10
 */
public class Measure {
  protected static final int MAX_TEXT_SIZE = 96;

  /**
   * Default precision when saving a float type metric
   */
  public final static int DEFAULT_PRECISION = 1;

  private Long id; // for internal use
  protected Metric metric;
  protected Double value;
  protected String data;
  protected String description;
  protected Metric.Level alertStatus;
  protected String alertText;
  protected Integer tendency;
  protected Date date;
  protected Double diff1, diff2, diff3;
  protected String url;
  protected Characteristic characteristic;
  protected PersistenceMode persistenceMode = PersistenceMode.FULL;

  /**
   * Creates a measure with a metric
   *
   * @param metric the metric
   */
  public Measure(Metric metric) {
    this.metric = metric;
  }

  /**
   * Creates a measure with a metric and a value
   *
   * @param metric the metric
   * @param value  its value
   */
  public Measure(Metric metric, Double value) {
    this.metric = metric;
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
   * Gets the persistence mode of the measure. Default persistence mode is FULL,
   * except when instantiating the measure with a String parameter.
   */
  public PersistenceMode getPersistenceMode() {
    return persistenceMode;
  }

  /**
   * <p>Sets the persistence mode of a measure.</p>
   * <p><b>WARNING : </b>Being able to reuse measures saved in memory is only possible within the same tree.
   * In a multi-module project for example, a measure save in memory at the module level will not be accessible by
   * the root project. In that case, database should be used.
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

  /**
   * Set the underlying metric
   *
   * @param metric the metric
   * @return the measure object instance
   */
  public Measure setMetric(Metric metric) {
    this.metric = metric;
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
  public Measure setValue(Double v) {
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
  public Measure setValue(Double v, int precision) {
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
    if (s != null && s.length() >= MAX_TEXT_SIZE && !metric.isDataType()) {
      throw new IllegalArgumentException("Data is too long for non-data metric : size=" + s.length() + ", max=" + MAX_TEXT_SIZE);
    }
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
   * Sets the tendency for the measure
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
   * @return the first differential value of the measure
   */
  public Double getDiffValue1() {
    return diff1;
  }

  /**
   * Sets the first differential value of the measure
   *
   * @param diff1 the diff
   * @return the measure object instance
   */
  public Measure setDiffValue1(Double diff1) {
    this.diff1 = diff1;
    return this;
  }

  /**
   * @return the second differential value of the measure
   */
  public Double getDiffValue2() {
    return diff2;
  }

  /**
   * Sets the second differential value of the measure
   *
   * @param diff2 the diff
   * @return the measure object instance
   */
  public Measure setDiffValue2(Double diff2) {
    this.diff2 = diff2;
    return this;
  }

  /**
   * @return the third differential value of the measure
   */
  public Double getDiffValue3() {
    return diff3;
  }

  /**
   * Sets the third differential value of the measure
   *
   * @param diff3 the diff
   * @return the measure object instance
   */
  public Measure setDiffValue3(Double diff3) {
    this.diff3 = diff3;
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

  public final Characteristic getCharacteristic() {
    return characteristic;
  }

  public final Measure setCharacteristic(Characteristic characteristic) {
    this.characteristic = characteristic;
    return this;
  }


//  @Override
//  public boolean equals(Object obj) {
//    if (!(obj.getClass().equals(Measure.class))) {
//      return false;
//    }
//    if (this == obj) {
//      return true;
//    }
//    Measure rhs = (Measure) obj;
//    return new EqualsBuilder()
//        .append(metric, rhs.getMetric())
//        .append(characteristic, rhs.getCharacteristic())
//        .isEquals();
//  }
//
//  @Override
//  public int hashCode() {
//    return (metric != null ? metric.hashCode() : 0);
//  }
//


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o.getClass().equals(Measure.class))) {
      return false;
    }

    Measure measure = (Measure) o;
    if (metric != null ? !metric.equals(measure.metric) : measure.metric != null) {
      return false;
    }
    if (characteristic != null ? !characteristic.equals(measure.characteristic) : measure.characteristic != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = metric != null ? metric.hashCode() : 0;
    result = 31 * result + (characteristic != null ? characteristic.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).
        append("id", id).
        append("metric", metric).
        append("value", value).
        append("data", data).
        append("description", description).
        append("alertStatus", alertStatus).
        append("alertText", alertText).
        append("tendency", tendency).
        append("characteristic", characteristic).
        append("diff1", diff1).
        append("diff2", diff2).
        append("diff3", diff3).
        toString();
  }

  public boolean hasOptionalData() {
    return getAlertStatus() != null ||
        getAlertText() != null ||
        getDescription() != null ||
        getDiffValue1() != null ||
        getDiffValue2() != null ||
        getDiffValue3() != null ||
        getData() != null ||
        getTendency() != null ||
        getUrl() != null;
  }
}
