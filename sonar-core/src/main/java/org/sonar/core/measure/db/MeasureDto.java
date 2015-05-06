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

package org.sonar.core.measure.db;

import com.google.common.base.Charsets;
import org.sonar.api.rule.Severity;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class MeasureDto {
  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";
  private static final int MAX_TEXT_VALUE_LENGTH = 4000;

  private Long id;
  private Double value;
  private String textValue;
  private byte[] dataValue;
  private Double variation1, variation2, variation3, variation4, variation5;
  private String alertStatus;
  private String alertText;
  private String description;
  private Integer severityIndex;

  private Long projectId;
  private Long snapshotId;
  private Integer metricId;
  private Integer ruleId;
  private Integer characteristicId;
  private Integer personId;

  // TODO to delete – not in db
  private String metricKey;
  private String componentKey;

  public Long getId() {
    return id;
  }

  public MeasureDto setId(Long id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public Double getValue() {
    return value;
  }

  public MeasureDto setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  @CheckForNull
  public String getData() {
    if (dataValue != null) {
      return new String(dataValue, Charsets.UTF_8);
    }
    return textValue;
  }

  public MeasureDto setData(@Nullable String data) {
    if (data == null) {
      this.textValue = null;
      this.dataValue = null;
    } else if (data.length() > MAX_TEXT_VALUE_LENGTH) {
      this.textValue = null;
      setByteData(data.getBytes(Charsets.UTF_8));
    } else {
      this.textValue = data;
      this.dataValue = null;
    }

    return this;
  }

  public MeasureDto setByteData(@Nullable byte[] data){
    this.dataValue = data;
    return this;
  }

  @CheckForNull
  public byte[] getByteData() {
    return dataValue;
  }

  @CheckForNull
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

  public MeasureDto setVariation(int index, @Nullable Double d) {
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

  @CheckForNull
  public String getAlertStatus() {
    return alertStatus;
  }

  public MeasureDto setAlertStatus(@Nullable String alertStatus) {
    this.alertStatus = alertStatus;
    return this;
  }

  @CheckForNull
  public String getAlertText() {
    return alertText;
  }

  public MeasureDto setAlertText(@Nullable String alertText) {
    this.alertText = alertText;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public MeasureDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public Long getComponentId() {
    return projectId;
  }

  public MeasureDto setComponentId(Long componentId) {
    this.projectId = componentId;
    return this;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public MeasureDto setMetricId(Integer metricId) {
    this.metricId = metricId;
    return this;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public MeasureDto setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  @CheckForNull
  public Integer getRuleId() {
    return ruleId;
  }

  public MeasureDto setRuleId(@Nullable Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public Integer getCharacteristicId() {
    return characteristicId;
  }

  public MeasureDto setCharacteristicId(@Nullable Integer characteristicId) {
    this.characteristicId = characteristicId;
    return this;
  }

  @CheckForNull
  public Integer getPersonId() {
    return personId;
  }

  public MeasureDto setPersonId(@Nullable Integer personId) {
    this.personId = personId;
    return this;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public MeasureDto setMetricKey(String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  public MeasureDto setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    if (severityIndex == null) {
      return null;
    }

    return Severity.ALL.get(severityIndex);
  }

  public MeasureDto setSeverity(@Nullable String severity) {
    if (severity == null) {
      return this;
    }

    checkArgument(Severity.ALL.contains(severity), "Severity must be included in the org.sonar.api.rule.Severity values");

    this.severityIndex = Severity.ALL.indexOf(severity);
    return this;
  }
}
