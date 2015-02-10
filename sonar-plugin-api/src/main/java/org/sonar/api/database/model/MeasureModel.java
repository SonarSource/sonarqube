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
package org.sonar.api.database.model;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;
import org.sonar.api.rules.RulePriority;

import javax.persistence.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * This class is the Hibernate model to store a measure in the DB
 */
@Entity
@Table(name = "project_measures")
public class MeasureModel implements Cloneable {

  public static final int TEXT_VALUE_LENGTH = 4000;

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "value", updatable = true, nullable = true, precision = 30, scale = 20)
  private Double value = 0.0;

  @Column(name = "text_value", updatable = true, nullable = true, length = TEXT_VALUE_LENGTH)
  private String textValue;

  @Column(name = "tendency", updatable = true, nullable = true)
  private Integer tendency;

  @Column(name = "metric_id", updatable = false, nullable = false)
  private Integer metricId;

  @Column(name = "snapshot_id", updatable = true, nullable = true)
  private Integer snapshotId;

  @Column(name = "project_id", updatable = true, nullable = true)
  private Integer projectId;

  @Column(name = "description", updatable = true, nullable = true, length = 4000)
  private String description;

  @Column(name = "measure_date", updatable = true, nullable = true)
  private Long measureDate;

  @Column(name = "rule_id", updatable = true, nullable = true)
  private Integer ruleId;

  @Column(name = "rule_priority", updatable = false, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority rulePriority;

  @Column(name = "alert_status", updatable = true, nullable = true, length = 5)
  private String alertStatus;

  @Column(name = "alert_text", updatable = true, nullable = true, length = 4000)
  private String alertText;

  @Column(name = "variation_value_1", updatable = true, nullable = true)
  private Double variationValue1;

  @Column(name = "variation_value_2", updatable = true, nullable = true)
  private Double variationValue2;

  @Column(name = "variation_value_3", updatable = true, nullable = true)
  private Double variationValue3;

  @Column(name = "variation_value_4", updatable = true, nullable = true)
  private Double variationValue4;

  @Column(name = "variation_value_5", updatable = true, nullable = true)
  private Double variationValue5;

  @Column(name = "url", updatable = true, nullable = true, length = 2000)
  private String url;

  @Column(name = "characteristic_id", nullable = true)
  private Integer characteristicId;

  @Column(name = "person_id", updatable = true, nullable = true)
  private Integer personId;

  @Column(name = "measure_data", updatable = true, nullable = true, length = 167772150)
  private byte[] data;

  /**
   * Creates a measure based on a metric and a double value
   */
  public MeasureModel(int metricId, Double val) {
    if (val.isNaN() || val.isInfinite()) {
      throw new IllegalArgumentException("Measure value is NaN. Metric=" + metricId);
    }
    this.metricId = metricId;
    this.value = val;
  }

  /**
   * Creates a measure based on a metric and an alert level
   */
  public MeasureModel(int metricId, Metric.Level level) {
    this.metricId = metricId;
    if (level != null) {
      this.textValue = level.toString();
    }
  }

  /**
   * Creates a measure based on a metric and a string value
   */
  public MeasureModel(int metricId, String val) {
    this.metricId = metricId;
    setData(val);
  }

  /**
   * Creates an empty measure
   */
  public MeasureModel() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the measure double value
   */
  public Double getValue() {
    return value;
  }

  /**
   * Sets the measure value
   *
   * @throws IllegalArgumentException in case value is not a valid double
   */
  public MeasureModel setValue(Double value) {
    if (value != null && (value.isNaN() || value.isInfinite())) {
      throw new IllegalArgumentException();
    }
    this.value = value;
    return this;
  }

  /**
   * @return the measure description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the measure description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the measure alert level
   */
  public Metric.Level getLevelValue() {
    if (textValue != null) {
      return Metric.Level.valueOf(textValue);
    }
    return null;
  }

  /**
   * Use getData() instead
   */
  public String getTextValue() {
    return textValue;
  }

  /**
   * Use setData() instead
   */
  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  /**
   * @return the measure tendency
   */
  public Integer getTendency() {
    return tendency;
  }

  /**
   * Sets the measure tendency
   *
   * @return the current object
   */
  public MeasureModel setTendency(Integer tendency) {
    this.tendency = tendency;
    return this;
  }

  /**
   * @return whether the measure is about rule
   */
  public boolean isRuleMeasure() {
    return ruleId != null || rulePriority != null;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public void setMetricId(Integer metricId) {
    this.metricId = metricId;
  }

  /**
   * @return the snapshot id the measure is attached to
   */
  public Integer getSnapshotId() {
    return snapshotId;
  }

  /**
   * Sets the snapshot id
   *
   * @return the current object
   */
  public MeasureModel setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  /**
   * Sets the rule for the measure
   *
   * @return the current object
   */
  public MeasureModel setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  /**
   * @return the rule priority
   */
  public RulePriority getRulePriority() {
    return rulePriority;
  }

  /**
   * Sets the rule priority
   */
  public void setRulePriority(RulePriority rulePriority) {
    this.rulePriority = rulePriority;
  }

  /**
   * @return the project id
   */
  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Sets the project id
   */
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  /**
   * @return the date of the measure
   */
  public Date getMeasureDate() {
    return longToDate(measureDate);
  }

  /**
   * Sets the date for the measure
   *
   * @return the current object
   */
  public MeasureModel setMeasureDate(Date measureDate) {
    this.measureDate = dateToLong(measureDate);
    return this;
  }

  /**
   * @return the date of the measure
   */
  public Long getMeasureDateMs() {
    return measureDate;
  }

  /**
   * Sets the date for the measure
   *
   * @return the current object
   */
  public MeasureModel setMeasureDateMs(Long measureDate) {
    this.measureDate = measureDate;
    return this;
  }

  /**
   * @return the alert status if there is one, null otherwise
   */
  public Metric.Level getAlertStatus() {
    if (alertStatus == null) {
      return null;
    }
    return Metric.Level.valueOf(alertStatus);
  }

  /**
   * Sets the measure alert status
   *
   * @return the current object
   */
  public MeasureModel setAlertStatus(Metric.Level level) {
    if (level != null) {
      this.alertStatus = level.toString();
    } else {
      this.alertStatus = null;
    }
    return this;
  }

  /**
   * @return the measure data
   */
  public String getData(Metric metric) {
    if (this.textValue != null) {
      return this.textValue;
    }
    if (metric.isDataType() && data != null) {
      try {
        return new String(data, Charsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        // how is it possible to not support UTF-8 ?
        Throwables.propagate(e);
      }
    }
    return null;
  }

  /**
   * Sets the measure data
   */
  public final void setData(String data) {
    if (data == null) {
      this.textValue = null;
      this.data = null;

    } else {
      if (data.length() > TEXT_VALUE_LENGTH) {
        this.textValue = null;
        this.data = data.getBytes(Charsets.UTF_8);
      } else {
        this.textValue = data;
        this.data = null;
      }
    }
  }

  /**
   * @return the text of the alert
   */
  public String getAlertText() {
    return alertText;
  }

  /**
   * Sets the text for the alert
   */
  public void setAlertText(String alertText) {
    this.alertText = alertText;
  }

  /**
   * @return the measure URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the measure URL
   */
  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  public Double getVariationValue1() {
    return variationValue1;
  }

  public void setVariationValue1(Double d) {
    this.variationValue1 = d;
  }

  public Double getVariationValue2() {
    return variationValue2;
  }

  public void setVariationValue2(Double d) {
    this.variationValue2 = d;
  }

  public Double getVariationValue3() {
    return variationValue3;
  }

  public void setVariationValue3(Double d) {
    this.variationValue3 = d;
  }

  public Double getVariationValue4() {
    return variationValue4;
  }

  public void setVariationValue4(Double d) {
    this.variationValue4 = d;
  }

  public Double getVariationValue5() {
    return variationValue5;
  }

  public void setVariationValue5(Double d) {
    this.variationValue5 = d;
  }

  /**
   * Saves the current object to database
   *
   * @return the current object
   */
  public MeasureModel save(DatabaseSession session) {
    session.save(this);
    return this;
  }

  public Integer getCharacteristicId() {
    return characteristicId;
  }

  public MeasureModel setCharacteristicId(Integer characteristicId) {
    this.characteristicId = characteristicId;
    return this;
  }

  public Integer getPersonId() {
    return personId;
  }

  public MeasureModel setPersonId(Integer i) {
    this.personId = i;
    return this;
  }

  @Override
  public Object clone() {
    MeasureModel clone = new MeasureModel();
    clone.setMetricId(getMetricId());
    clone.setDescription(getDescription());
    clone.setTextValue(getTextValue());
    clone.setAlertStatus(getAlertStatus());
    clone.setAlertText(getAlertText());
    clone.setTendency(getTendency());
    clone.setVariationValue1(getVariationValue1());
    clone.setVariationValue2(getVariationValue2());
    clone.setVariationValue3(getVariationValue3());
    clone.setVariationValue4(getVariationValue4());
    clone.setVariationValue5(getVariationValue5());
    clone.setValue(getValue());
    clone.setRulePriority(getRulePriority());
    clone.setRuleId(getRuleId());
    clone.setSnapshotId(getSnapshotId());
    clone.setMeasureDate(getMeasureDate());
    clone.setUrl(getUrl());
    clone.setCharacteristicId(getCharacteristicId());
    clone.setPersonId(getPersonId());
    return clone;
  }

}
