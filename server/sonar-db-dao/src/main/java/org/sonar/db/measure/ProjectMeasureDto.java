/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.measure;

import com.google.common.base.MoreObjects;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.UuidFactoryImpl;

public class ProjectMeasureDto {
  private static final int MAX_TEXT_VALUE_LENGTH = 4000;

  private String uuid;
  private Double value;
  private String textValue;
  private byte[] dataValue;
  private String alertStatus;
  private String alertText;
  private String componentUuid;
  private String analysisUuid;
  private String metricUuid;

  public ProjectMeasureDto() {
    // empty constructor
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @CheckForNull
  public Double getValue() {
    return value;
  }

  public ProjectMeasureDto setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public ProjectMeasureDto setComponentUuid(String s) {
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

  public ProjectMeasureDto setData(@Nullable String data) {
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

  @CheckForNull
  public String getAlertStatus() {
    return alertStatus;
  }

  public ProjectMeasureDto setAlertStatus(@Nullable String alertStatus) {
    this.alertStatus = alertStatus;
    return this;
  }

  @CheckForNull
  public String getAlertText() {
    return alertText;
  }

  public ProjectMeasureDto setAlertText(@Nullable String alertText) {
    this.alertText = alertText;
    return this;
  }

  public String getMetricUuid() {
    return metricUuid;
  }

  public ProjectMeasureDto setMetricUuid(String metricUuid) {
    this.metricUuid = metricUuid;
    return this;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public ProjectMeasureDto setAnalysisUuid(String s) {
    this.analysisUuid = s;
    return this;
  }

  public ProjectMeasureDto copy() {
    ProjectMeasureDto copy = new ProjectMeasureDto()
      .setAlertStatus(alertStatus)
      .setAnalysisUuid(analysisUuid)
      .setAlertText(alertText)
      .setComponentUuid(componentUuid)
      .setMetricUuid(metricUuid)
      .setData(getData())
      .setValue(value);
    copy.setUuid(UuidFactoryImpl.INSTANCE.create());
    return copy;
  }
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("value", value)
      .add("textValue", textValue)
      .add("dataValue", dataValue)
      .add("alertStatus", alertStatus)
      .add("alertText", alertText)
      .add("componentUuid", componentUuid)
      .add("analysisUuid", analysisUuid)
      .add("metricUuid", metricUuid)
      .toString();
  }
}
