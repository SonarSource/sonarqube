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
package org.sonar.api.database.model;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is the Hibernate model to store a measure in the DB
 */
@Entity
@Table(name = "project_measures")
public class MeasureModel implements Cloneable {

  public static final int TEXT_VALUE_LENGTH = 96;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id")
  @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
  private Metric metric;

  @Column(name = "snapshot_id", updatable = true, nullable = true)
  private Integer snapshotId;

  @Column(name = "project_id", updatable = true, nullable = true)
  private Integer projectId;

  @Column(name = "description", updatable = true, nullable = true, length = 4000)
  private String description;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "measure_date", updatable = true, nullable = true)
  private Date measureDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rule_id")
  @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
  private Rule rule;

  @Column(name = "rules_category_id")
  private Integer rulesCategoryId;

  @Column(name = "rule_priority", updatable = false, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority rulePriority;

  @Column(name = "alert_status", updatable = true, nullable = true, length = 5)
  private String alertStatus;

  @Column(name = "alert_text", updatable = true, nullable = true, length = 4000)
  private String alertText;

  @Column(name = "diff_value_1", updatable = true, nullable = true)
  private Double diffValue1;

  @Column(name = "diff_value_2", updatable = true, nullable = true)
  private Double diffValue2;

  @Column(name = "diff_value_3", updatable = true, nullable = true)
  private Double diffValue3;

  @Column(name = "url", updatable = true, nullable = true, length = 2000)
  private String url;

  @OneToMany(mappedBy = "measure", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
  private List<MeasureData> measureData = new ArrayList<MeasureData>();

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "characteristic_id")
  private Characteristic characteristic;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
  
  /**
   * Creates a measure based on a metric and a double value
   */
  public MeasureModel(Metric metric, Double val) {
    if (val.isNaN() || val.isInfinite()) {
      throw new IllegalArgumentException("Measure value is NaN. Metric=" + metric);
    }
    this.metric = metric;
    this.value = val;
  }

  /**
   * Creates a measure based on a metric and an alert level
   */
  public MeasureModel(Metric metric, Metric.Level level) {
    this.metric = metric;
    if (level != null) {
      this.textValue = level.toString();
    }
  }

  /**
   * Creates a measure based on a metric and a string value
   */
  public MeasureModel(Metric metric, String val) {
    this.metric = metric;
    setData(val);
  }

  /**
   * Creates an empty measure
   */
  public MeasureModel() {
  }

  /**
   * @return the measure double value
   */
  public Double getValue() {
    return value;
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
   * Sets the measure value
   *
   * @throws IllegalArgumentException in case value is not a valid double
   */
  public MeasureModel setValue(Double value) throws IllegalArgumentException {
    if (value != null && (value.isNaN() || value.isInfinite())) {
      throw new IllegalArgumentException();
    }
    this.value = value;
    return this;
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
   * @return whether the measure is about rule
   */
  public boolean isRuleMeasure() {
    return rule != null || rulePriority != null || rulesCategoryId != null;
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
   * @return the measure metric
   */
  public Metric getMetric() {
    return metric;
  }

  /**
   * Sets the measure metric
   */
  public void setMetric(Metric metric) {
    this.metric = metric;
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

  /**
   * @return the rule
   */
  public Rule getRule() {
    return rule;
  }

  /**
   * Sets the rule for the measure
   *
   * @return the current object
   */
  public MeasureModel setRule(Rule rule) {
    this.rule = rule;
    return this;
  }

  /**
   * @return the rule category id
   */
  public Integer getRulesCategoryId() {
    return rulesCategoryId;
  }

  /**
   * Sets the rule category id
   *
   * @return the current object
   */
  public MeasureModel setRulesCategoryId(Integer id) {
    this.rulesCategoryId = id;
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
    return measureDate;
  }

  /**
   * Sets the date for the measure
   *
   * @return the current object
   */
  public MeasureModel setMeasureDate(Date measureDate) {
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
  public String getData() {
    if (this.textValue != null) {
      return this.textValue;
    }
    if (metric.isDataType() && !measureData.isEmpty()) {
      return measureData.get(0).getText();
    }
    return null;
  }

  /**
   * Sets the measure data
   */
  public final void setData(String data) {
    if (data == null) {
      this.textValue = null;
      measureData.clear();

    } else {
      if (data.length() > TEXT_VALUE_LENGTH) {
        measureData.clear();
        measureData.add(new MeasureData(this, data));

      } else {
        this.textValue = data;
      }
    }
  }

  /**
   * Use getData() instead
   */
  public MeasureData getMeasureData() {
    if (!measureData.isEmpty()) {
      return measureData.get(0);
    }
    return null;
  }

  /**
   * Use setData() instead
   */
  //@Deprecated
  public void setMeasureData(MeasureData data) {
    measureData.clear();
    if (data != null) {
      this.measureData.add(data);
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
    return new ToStringBuilder(this).
        append("value", value).
        append("metric", metric).
        toString();
  }

  /**
   * @return the rule id of the measure
   */
  public Integer getRuleId() {
    if (getRule() != null) {
      return getRule().getId();
    }
    return null;
  }

  /**
   * @return diffValue1
   */
  public Double getDiffValue1() {
    return diffValue1;
  }

  /**
   * Sets the diffValue1
   */
  public void setDiffValue1(Double diffValue1) {
    this.diffValue1 = diffValue1;
  }

  /**
   * @return diffValue2
   */
  public Double getDiffValue2() {
    return diffValue2;
  }

  /**
   * Sets the diffValue2
   */
  public void setDiffValue2(Double diffValue2) {
    this.diffValue2 = diffValue2;
  }

  /**
   * @return diffValue3
   */
  public Double getDiffValue3() {
    return diffValue3;
  }

  /**
   * Sets the diffValue3
   */
  public void setDiffValue3(Double diffValue3) {
    this.diffValue3 = diffValue3;
  }

  /**
   * Saves the current object to database
   *
   * @return the current object
   */
  public MeasureModel save(DatabaseSession session) {
    this.metric = session.reattach(Metric.class, metric.getId());
    MeasureData data = getMeasureData();
    setMeasureData(null);
    session.save(this);

    if (data != null) {
      data.setMeasure(session.getEntity(MeasureModel.class, getId()));
      data.setSnapshotId(snapshotId);
      session.save(data);
      setMeasureData(data);
    }
    return this;
  }

  public Characteristic getCharacteristic() {
    return characteristic;
  }

  public MeasureModel setCharacteristic(Characteristic c) {
    this.characteristic = c;
    return this;
  }

  @Override
  public Object clone() {
    MeasureModel clone = new MeasureModel();
    clone.setMetric(getMetric());
    clone.setDescription(getDescription());
    clone.setTextValue(getTextValue());
    clone.setAlertStatus(getAlertStatus());
    clone.setAlertText(getAlertText());
    clone.setTendency(getTendency());
    clone.setDiffValue1(getDiffValue1());
    clone.setDiffValue2(getDiffValue2());
    clone.setDiffValue3(getDiffValue3());
    clone.setValue(getValue());
    clone.setRulesCategoryId(getRulesCategoryId());
    clone.setRulePriority(getRulePriority());
    clone.setRule(getRule());
    clone.setSnapshotId(getSnapshotId());
    clone.setMeasureDate(getMeasureDate());
    clone.setUrl(getUrl());
    clone.setCharacteristic(getCharacteristic());
    return clone;
  }

/**
   * True if other fields than 'value' are set.
   */
  public boolean hasOptionalData() {
    return getAlertStatus()!=null ||
        getAlertText()!=null ||
        getDescription()!=null ||
        getDiffValue1()!=null ||
        getDiffValue2()!=null ||
        getDiffValue3()!=null ||
        getMeasureData()!=null ||
        getTendency()!=null ||
        getUrl()!=null;
  }


  /**
   * Builds a MeasureModel from a Measure
   */
  public static MeasureModel build(Measure measure) {
    return build(measure, new MeasureModel());
  }

  /**
   * Merges a Measure into a MeasureModel
   */
  public static MeasureModel build(Measure measure, MeasureModel merge) {
    merge.setMetric(measure.getMetric());
    merge.setDescription(measure.getDescription());
    merge.setData(measure.getData());
    merge.setAlertStatus(measure.getAlertStatus());
    merge.setAlertText(measure.getAlertText());
    merge.setTendency(measure.getTendency());
    merge.setDiffValue1(measure.getDiffValue1());
    merge.setDiffValue2(measure.getDiffValue2());
    merge.setDiffValue3(measure.getDiffValue3());
    merge.setUrl(measure.getUrl());
    merge.setCharacteristic(measure.getCharacteristic());
    if (measure.getValue() != null) {
      merge.setValue(measure.getValue().doubleValue());
    } else {
      merge.setValue(null);
    }
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      merge.setRulesCategoryId(ruleMeasure.getRuleCategory());
      merge.setRulePriority(ruleMeasure.getRulePriority());
      merge.setRule(ruleMeasure.getRule());
    }
    return merge;
  }

  /**
   * @return a measure from the current object
   */
  public Measure toMeasure() {
    Measure measure;
    if (isRuleMeasure()) {
      measure = new RuleMeasure(getMetric(), getRule(), getRulePriority(), getRulesCategoryId());
    } else {
      measure = new Measure(getMetric());
    }
    measure.setId(getId());
    measure.setDescription(getDescription());
    measure.setValue(getValue());
    measure.setData(getData());
    measure.setAlertStatus(getAlertStatus());
    measure.setAlertText(getAlertText());
    measure.setTendency(getTendency());
    measure.setDiffValue1(getDiffValue1());
    measure.setDiffValue2(getDiffValue2());
    measure.setDiffValue3(getDiffValue3());
    measure.setUrl(getUrl());
    measure.setCharacteristic(getCharacteristic());
    return measure;
  }

  /**
   * Transforms a list of MeasureModel into a list of Measure
   *
   * @return an empty list if models is null
   */
  public static List<Measure> toMeasures(List<MeasureModel> models) {
    List<Measure> result = new ArrayList<Measure>();
    for (MeasureModel model : models) {
      if (model != null) {
        result.add(model.toMeasure());
      }
    }
    return result;
  }
}
