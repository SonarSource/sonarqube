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
package org.sonarqube.ws.client.projectanalyses;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/create_event">Further information about this action online (including a response example)</a>
 * @since 6.3
 */
@Generated("sonar-ws-generator")
public class CreateEventRequest {

  private String analysis;
  private String category;
  private String name;

  /**
   * This is a mandatory parameter.
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public CreateEventRequest setAnalysis(String analysis) {
    this.analysis = analysis;
    return this;
  }

  public String getAnalysis() {
    return analysis;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"VERSION"</li>
   *   <li>"OTHER"</li>
   * </ul>
   */
  public CreateEventRequest setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getCategory() {
    return category;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "5.6"
   */
  public CreateEventRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }
}
