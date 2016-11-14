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
package org.sonarqube.ws.client.webhook;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DeliveriesRequest {

  private final String componentKey;
  private final String ceTaskId;

  private DeliveriesRequest(Builder builder) {
    this.componentKey = builder.componentKey;
    this.ceTaskId = builder.ceTaskId;
  }

  @CheckForNull
  public String getComponentKey() {
    return componentKey;
  }

  @CheckForNull
  public String getCeTaskId() {
    return ceTaskId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String componentKey;
    private String ceTaskId;

    /**
     * @see #builder()
     */
    private Builder() {
    }

    public Builder setComponentKey(@Nullable String s) {
      this.componentKey = s;
      return this;
    }

    public Builder setCeTaskId(@Nullable String s) {
      this.ceTaskId = s;
      return this;
    }

    public DeliveriesRequest build() {
      return new DeliveriesRequest(this);
    }
  }
}
