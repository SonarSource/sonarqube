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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEYS;

public class ValuesRequest {

  private final String componentId;
  private final String componentKey;
  private final List<String> keys;

  public ValuesRequest(Builder builder) {
    this.componentId = builder.componentId;
    this.componentKey = builder.componentKey;
    this.keys = builder.keys;
  }

  @CheckForNull
  public String getComponentId() {
    return componentId;
  }

  @CheckForNull
  public String getComponentKey() {
    return componentKey;
  }

  public List<String> getKeys() {
    return keys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String componentId;
    private String componentKey;
    private List<String> keys = new ArrayList<>();

    private Builder() {
      // enforce factory method use
    }

    public Builder setComponentId(@Nullable String componentId) {
      this.componentId = componentId;
      return this;
    }

    public Builder setComponentKey(@Nullable String componentKey) {
      this.componentKey = componentKey;
      return this;
    }

    public Builder setKeys(List<String> keys) {
      this.keys = requireNonNull(keys);
      return this;
    }

    public Builder setKeys(String... keys) {
      return setKeys(asList(keys));
    }

    public ValuesRequest build() {
      checkArgument(!keys.isEmpty(), "'%s' cannot be empty", PARAM_KEYS);
      return new ValuesRequest(this);
    }
  }

}
