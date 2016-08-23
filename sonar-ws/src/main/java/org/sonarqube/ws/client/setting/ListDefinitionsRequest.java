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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ListDefinitionsRequest {

  private final String componentId;
  private final String componentKey;

  private ListDefinitionsRequest(Builder builder) {
    this.componentId = builder.componentId;
    this.componentKey = builder.componentKey;
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
    private String componentId;
    private String componentKey;

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

    public ListDefinitionsRequest build() {
      return new ListDefinitionsRequest(this);
    }
  }

}
