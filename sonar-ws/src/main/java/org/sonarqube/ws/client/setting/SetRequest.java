/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.setting;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;

public class SetRequest {
  private final String key;
  private final String value;
  private final List<String> values;
  private final List<String> fieldValues;
  private final String component;
  private final String branch;

  private SetRequest(Builder builder) {
    this.key = builder.key;
    this.value = builder.value;
    this.values = builder.values;
    this.fieldValues = builder.fieldValues;
    this.component = builder.component;
    this.branch = builder.branch;
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

  public List<String> getFieldValues() {
    return fieldValues;
  }

  @CheckForNull
  public String getComponent() {
    return component;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String key;
    private String value;
    private List<String> values = emptyList();
    private List<String> fieldValues = emptyList();
    private String component;
    private String branch;

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

    public Builder setFieldValues(List<String> fieldValues) {
      this.fieldValues = fieldValues;
      return this;
    }

    public Builder setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    public Builder setBranch(@Nullable String branch) {
      this.branch = branch;
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
