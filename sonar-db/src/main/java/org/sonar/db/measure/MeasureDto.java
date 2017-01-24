/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.measure;

import com.google.common.base.MoreObjects;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MeasureDto {
  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";
  private static final int MAX_TEXT_VALUE_LENGTH = 4000;

  private Double value;
  private String textValue;
  private byte[] dataValue;
  private Double variation1;
  private Double variation2;
  private Double variation3;
  private Double variation4;
  private Double variation5;
  private String alertStatus;
  private String alertText;
  private String description;
  private String componentUuid;
  private String analysisUuid;
  private int metricId;
  private Long developerId;

  @CheckForNull
  public Double getValue() {
    return value;
  }

  public MeasureDto setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public MeasureDto setComponentUuid(String s) {
    this.componentUuid = s;
    return this;
  }

  @CheckForNull
  public String getData() {
    if (dataValue != null) {
      return new String(dataValue, StandardCharsets.UTF_8);
    }
    return textValue;
  }

  public MeasureDto setData(@Nullable String data) {
    if (data == null) {
      this.textValue = null;
      this.dataValue = null;
    } else if (data.length() > MAX_TEXT_VALUE_LENGTH) {
      this.textValue = null;
      this.dataValue = data.getBytes(StandardCharsets.UTF_8);
    } else {
      this.textValue = data;
      this.dataValue = null;
    }

    return this;
  }

  /**
   * @param index starts at 1
   */
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

  public int getMetricId() {
    return metricId;
  }

  public MeasureDto setMetricId(int metricId) {
    this.metricId = metricId;
    return this;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public MeasureDto setAnalysisUuid(String s) {
    this.analysisUuid = s;
    return this;
  }

  @CheckForNull
  public Long getDeveloperId() {
    return developerId;
  }

  public MeasureDto setDeveloperId(@Nullable Long developerId) {
    this.developerId = developerId;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("value", value)
      .add("textValue", textValue)
      .add("dataValue", dataValue)
      .add("variation1", variation1)
      .add("variation2", variation2)
      .add("variation3", variation3)
      .add("variation4", variation4)
      .add("variation5", variation5)
      .add("alertStatus", alertStatus)
      .add("alertText", alertText)
      .add("description", description)
      .add("componentUuid", componentUuid)
      .add("analysisUuid", analysisUuid)
      .add("metricId", metricId)
      .add("developerId", developerId)
      .toString();
  }
}
