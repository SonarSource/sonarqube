/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.measures;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/search">Further information about this action online (including a response example)</a>
 * @since 6.2
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private List<String> metricKeys;
  private List<String> projectKeys;

  /**
   * This is a mandatory parameter.
   * Example value: "ncloc,complexity,violations"
   */
  public SearchRequest setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project,another_project"
   */
  public SearchRequest setProjectKeys(List<String> projectKeys) {
    this.projectKeys = projectKeys;
    return this;
  }

  public List<String> getProjectKeys() {
    return projectKeys;
  }
}
