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
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MeasureDto extends Dto<MeasureKey>{

  private Integer id;

  private String metricKey;

  private String componentKey;

  private Double value;

  private String textValue;

  private byte[] data;

  private MeasureDto(){
    // Nothing here
  }

  public Integer getId() {
    return id;
  }

  public MeasureDto setId(Integer id) {
    this.id = id;
    return this;
  }

  private MeasureDto setMetricKey(String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  private MeasureDto setComponentKey(String componentKey) {
    this.componentKey = componentKey;
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

  public MeasureDto setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  @CheckForNull
  public MeasureDto setData(@Nullable byte[] data) {
    this.data = data;
    return this;
  }

  @CheckForNull
  public String getData() {
    if (data != null) {
      return new String(data, Charsets.UTF_8);
    }
    return textValue;
  }

  @Override
  public MeasureKey getKey() {
    return MeasureKey.of(componentKey, metricKey);
  }

  public static MeasureDto createFor(MeasureKey key){
    return new MeasureDto()
      .setComponentKey(key.componentKey())
      .setMetricKey(key.metricKey());
  }
}
