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
package org.sonarqube.ws.client.measure;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class SearchRequest {
  public static final int MAX_NB_PROJECTS = 100;

  private final List<String> metricKeys;
  private final List<String> projectKeys;

  public SearchRequest(Builder builder) {
    metricKeys = builder.metricKeys;
    projectKeys = builder.projectKeys;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public List<String> getProjectKeys() {
    return projectKeys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> metricKeys;
    private List<String> projectKeys;

    private Builder() {
      // enforce method constructor
    }

    public Builder setMetricKeys(List<String> metricKeys) {
      this.metricKeys = metricKeys;
      return this;
    }

    public Builder setProjectKeys(List<String> projectKeys) {
      this.projectKeys = projectKeys;
      return this;
    }

    public SearchRequest build() {
      checkArgument(metricKeys != null && !metricKeys.isEmpty(), "Metric keys must be provided");
      checkArgument(projectKeys != null && !projectKeys.isEmpty(), "Project keys must be provided");
      int nbComponents = projectKeys.size();
      checkArgument(nbComponents <= MAX_NB_PROJECTS,
        "%s projects provided, more than maximum authorized (%s)", nbComponents, MAX_NB_PROJECTS);
      return new SearchRequest(this);
    }
  }
}
