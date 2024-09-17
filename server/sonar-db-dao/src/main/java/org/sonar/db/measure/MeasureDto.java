/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import org.apache.commons.codec.digest.MurmurHash3;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MeasureDto {

  private static final Gson GSON = new Gson();

  private String componentUuid;
  private String branchUuid;
  // measures are kept sorted by metric key so that the value hash is consistent
  private Map<String, Object> metricValues = new TreeMap<>();
  private Long jsonValueHash;

  public MeasureDto() {
    // empty constructor
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public MeasureDto setComponentUuid(String s) {
    this.componentUuid = s;
    return this;
  }

  public String getBranchUuid() {
    return branchUuid;
  }

  public MeasureDto setBranchUuid(String s) {
    this.branchUuid = s;
    return this;
  }

  public Map<String, Object> getMetricValues() {
    return metricValues;
  }

  public MeasureDto addValue(String metricKey, Object value) {
    metricValues.put(metricKey, value);
    return this;
  }

  // used by MyBatis mapper
  public String getJsonValue() {
    return GSON.toJson(metricValues);
  }

  // used by MyBatis mapper
  public MeasureDto setJsonValue(String jsonValue) {
    metricValues = GSON.fromJson(jsonValue, new TypeToken<TreeMap<String, Object>>() {
    }.getType());
    return this;
  }

  public Long getJsonValueHash() {
    return jsonValueHash;
  }

  public long computeJsonValueHash() {
    jsonValueHash = MurmurHash3.hash128(getJsonValue().getBytes(UTF_8))[0];
    return jsonValueHash;
  }

  @CheckForNull
  public String getString(String metricKey) {
    Object value = metricValues.get(metricKey);
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }

  @CheckForNull
  public Double getDouble(String metricKey) {
    Object value = metricValues.get(metricKey);
    if (value == null) {
      return null;
    }
    return Double.parseDouble(value.toString());
  }

  @CheckForNull
  public Integer getInt(String metricKey) {
    Object value = metricValues.get(metricKey);
    if (value == null) {
      return null;
    }
    return (int) Double.parseDouble(value.toString());
  }

  @CheckForNull
  public Long getLong(String metricKey) {
    Object value = metricValues.get(metricKey);
    if (value == null) {
      return null;
    }
    return (long) Double.parseDouble(value.toString());
  }

  @Override
  public String toString() {
    return "MeasureDto{" +
      "componentUuid='" + componentUuid + '\'' +
      ", branchUuid='" + branchUuid + '\'' +
      ", metricValues=" + metricValues +
      ", jsonValueHash=" + jsonValueHash +
      '}';
  }
}
