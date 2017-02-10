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
package org.sonarqube.ws.client.measure;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ComponentWsRequest {
  private String componentId;
  private String componentKey;
  private List<String> metricKeys;
  private List<String> additionalFields;
  private String developerId;
  private String developerKey;

  @CheckForNull
  public String getComponentId() {
    return componentId;
  }

  public ComponentWsRequest setComponentId(@Nullable String componentId) {
    this.componentId = componentId;
    return this;
  }

  @CheckForNull
  public String getComponentKey() {
    return componentKey;
  }

  public ComponentWsRequest setComponentKey(@Nullable String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public ComponentWsRequest setMetricKeys(@Nullable List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  @CheckForNull
  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public ComponentWsRequest setAdditionalFields(@Nullable List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  @CheckForNull
  public String getDeveloperId() {
    return developerId;
  }

  public ComponentWsRequest setDeveloperId(@Nullable String developerId) {
    this.developerId = developerId;
    return this;
  }

  @CheckForNull
  public String getDeveloperKey() {
    return developerKey;
  }

  public ComponentWsRequest setDeveloperKey(@Nullable String developerKey) {
    this.developerKey = developerKey;
    return this;
  }
}
