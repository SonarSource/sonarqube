/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import java.util.Date;

public class MeasureDto {
  private final Long id;
  private final Double value;
  private final String textValue;
  private final Integer tendency;
  private final Integer metricId;
  private final Integer snapshotId;
  private final Integer projectId;
  private final String description;
  private final Date measureDate;
  private final Integer ruleId;
  private final Integer rulePriority;
  private final String alertStatus;
  private final String alertText;
  private final Double variationValue1;
  private final Double variationValue2;
  private final Double variationValue3;
  private final Double variationValue4;
  private final Double variationValue5;
  private final String url;
  private final Integer characteristicId;
  private final Integer personId;
  private final MeasureData measureData;

  public MeasureDto(MeasureModel model) {
    id = model.getId();
    value = model.getValue();
    textValue = model.getTextValue();
    tendency = model.getTendency();
    metricId = model.getMetricId();
    snapshotId = model.getSnapshotId();
    projectId = model.getProjectId();
    description = model.getDescription();
    measureDate = model.getMeasureDate();
    ruleId = model.getRuleId();
    rulePriority = (null == model.getRulePriority()) ? null : model.getRulePriority().ordinal();
    alertStatus = (null == model.getAlertStatus()) ? null : model.getAlertStatus().name();
    alertText = model.getAlertText();
    variationValue1 = model.getVariationValue1();
    variationValue2 = model.getVariationValue2();
    variationValue3 = model.getVariationValue3();
    variationValue4 = model.getVariationValue4();
    variationValue5 = model.getVariationValue5();
    url = model.getUrl();
    characteristicId = (null == model.getCharacteristic()) ? null : model.getCharacteristic().getId();
    personId = model.getPersonId();
    measureData = model.getMeasureData();
  }

  public Long getId() {
    return id;
  }

  public Double getValue() {
    return value;
  }

  public String getTextValue() {
    return textValue;
  }

  public Integer getTendency() {
    return tendency;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public String getDescription() {
    return description;
  }

  public Date getMeasureDate() {
    return measureDate;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public Integer getRulePriority() {
    return rulePriority;
  }

  public String getAlertStatus() {
    return alertStatus;
  }

  public String getAlertText() {
    return alertText;
  }

  public Double getVariationValue1() {
    return variationValue1;
  }

  public Double getVariationValue2() {
    return variationValue2;
  }

  public Double getVariationValue3() {
    return variationValue3;
  }

  public Double getVariationValue4() {
    return variationValue4;
  }

  public Double getVariationValue5() {
    return variationValue5;
  }

  public String getUrl() {
    return url;
  }

  public Integer getCharacteristicId() {
    return characteristicId;
  }

  public Integer getPersonId() {
    return personId;
  }

  public MeasureData getMeasureData() {
    return measureData;
  }
}
