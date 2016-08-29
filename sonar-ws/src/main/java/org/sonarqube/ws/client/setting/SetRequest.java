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

package org.sonarqube.ws.client.setting;

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;

public class SetRequest {
  private final String key;
  private final String value;
  private final List<String> values;
  private final List<Map<String, String>> fieldValues;
  private final String componentId;
  private final String componentKey;

  public SetRequest(Builder builder) {
    this.key = builder.key;
    this.value = builder.value;
    this.values = builder.values;
    this.fieldValues = builder.fieldValues;
    this.componentId = builder.componentId;
    this.componentKey = builder.componentKey;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  public String getValue() {
    return value;
  }

  public List<String> getValues() {
    return values;
  }

  public List<Map<String, String>> getFieldValues() {
    return fieldValues;
  }

  @CheckForNull
  public String getComponentId() {
    return componentId;
  }

  @CheckForNull
  public String getComponentKey() {
    return componentKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String key;
    private String value;
    private List<String> values = emptyList();
    private List<Map<String, String>> fieldValues = emptyList();
    private String componentId;
    private String componentKey;

    private Builder() {
      // enforce factory method use
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setValue(@Nullable String value) {
      this.value = value;
      return this;
    }

    public Builder setValues(List<String> values) {
      this.values = values;
      return this;
    }

    public Builder setFieldValues(List<Map<String, String>> fieldValues) {
      this.fieldValues = fieldValues;
      return this;
    }

    public Builder setComponentId(@Nullable String componentId) {
      this.componentId = componentId;
      return this;
    }

    public Builder setComponentKey(@Nullable String componentKey) {
      this.componentKey = componentKey;
      return this;
    }

    public SetRequest build() {
      checkArgument(key != null && !key.isEmpty(), "Setting key is mandatory and must not be empty");
      checkArgument(values != null, "Setting values must not be null");
      checkArgument(fieldValues != null, "Setting fields values must not be null");
      return new SetRequest(this);
    }
  }
}
