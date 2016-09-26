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

package org.sonarqube.ws.client.measure;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SearchRequest {
  private final List<String> metricKeys;
  private final List<String> componentIds;
  private final List<String> componentKeys;

  public SearchRequest(Builder builder) {
    metricKeys = builder.metricKeys;
    componentIds = builder.componentIds;
    componentKeys = builder.componentKeys;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public boolean hasComponentIds() {
    return componentIds != null;
  }

  public List<String> getComponentIds() {
    return requireNonNull(componentIds, "No component id in request");
  }

  public boolean hasComponentKeys() {
    return componentKeys != null;
  }

  public List<String> getComponentKeys() {
    return requireNonNull(componentKeys, "No component key in request");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> metricKeys;
    private List<String> componentIds;
    private List<String> componentKeys;

    private Builder() {
      // enforce method constructor
    }

    public Builder setMetricKeys(List<String> metricKeys) {
      this.metricKeys = metricKeys;
      return this;
    }

    public Builder setComponentIds(List<String> componentIds) {
      this.componentIds = componentIds;
      return this;
    }

    public Builder setComponentKeys(List<String> componentKeys) {
      this.componentKeys = componentKeys;
      return this;
    }

    public SearchRequest build() {
      checkArgument(metricKeys != null && !metricKeys.isEmpty(), "Metric keys must be provided");
      checkArgument(
        (componentIds != null && !componentIds.isEmpty())
          ^ (componentKeys != null && !componentKeys.isEmpty()),
        "Either component ids or component keys must be provided, not both.");
      return new SearchRequest(this);
    }
  }
}
