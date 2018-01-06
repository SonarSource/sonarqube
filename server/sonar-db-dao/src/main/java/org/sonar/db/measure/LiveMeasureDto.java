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
package org.sonar.db.measure;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class LiveMeasureDto {

  private static final int MAX_TEXT_VALUE_LENGTH = 4000;

  private String componentUuid;
  private String projectUuid;
  private int metricId;
  @Nullable
  private Double value;
  @Nullable
  private String textValue;
  @Nullable
  private byte[] data;
  @Nullable
  private Double variation;

  public String getComponentUuid() {
    return componentUuid;
  }

  public LiveMeasureDto setComponentUuid(String s) {
    this.componentUuid = s;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public LiveMeasureDto setProjectUuid(String s) {
    this.projectUuid = s;
    return this;
  }

  public int getMetricId() {
    return metricId;
  }

  public LiveMeasureDto setMetricId(int i) {
    this.metricId = i;
    return this;
  }

  @CheckForNull
  public Double getValue() {
    return value;
  }

  public LiveMeasureDto setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  @CheckForNull
  public String getTextValue() {
    return textValue;
  }

  @CheckForNull
  public byte[] getData() {
    return data;
  }

  @CheckForNull
  public String getDataAsString() {
    if (data != null) {
      return new String(data, StandardCharsets.UTF_8);
    }
    return textValue;
  }

  public LiveMeasureDto setData(@Nullable String data) {
    if (data == null) {
      this.textValue = null;
      this.data = null;
    } else if (data.length() > MAX_TEXT_VALUE_LENGTH) {
      this.textValue = null;
      this.data = data.getBytes(StandardCharsets.UTF_8);
    } else {
      this.textValue = data;
      this.data = null;
    }
    return this;
  }

  public LiveMeasureDto setData(@Nullable byte[] data) {
    this.textValue = null;
    this.data = data;
    return this;
  }

  @CheckForNull
  public Double getVariation() {
    return variation;
  }

  public LiveMeasureDto setVariation(@Nullable Double variation) {
    this.variation = variation;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("LiveMeasureDto{");
    sb.append("componentUuid='").append(componentUuid).append('\'');
    sb.append(", projectUuid='").append(projectUuid).append('\'');
    sb.append(", metricId=").append(metricId);
    sb.append(", value=").append(value);
    sb.append(", variation=").append(variation);
    sb.append(", textValue='").append(textValue).append('\'');
    sb.append(", data=").append(Arrays.toString(data));
    sb.append('}');
    return sb.toString();
  }
}
