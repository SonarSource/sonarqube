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
package org.sonarqube.ws.client.metrics;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/metrics/create">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class CreateRequest {

  private String description;
  private String domain;
  private String key;
  private String name;
  private String type;

  /**
   * Example value: "Size of the team"
   */
  public CreateRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Example value: "Tests"
   */
  public CreateRequest setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "team_size"
   */
  public CreateRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "Team Size"
   */
  public CreateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "INT"
   * Possible values:
   * <ul>
   *   <li>"INT"</li>
   *   <li>"FLOAT"</li>
   *   <li>"PERCENT"</li>
   *   <li>"BOOL"</li>
   *   <li>"STRING"</li>
   *   <li>"MILLISEC"</li>
   *   <li>"DATA"</li>
   *   <li>"LEVEL"</li>
   *   <li>"DISTRIB"</li>
   *   <li>"RATING"</li>
   *   <li>"WORK_DUR"</li>
   * </ul>
   */
  public CreateRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return type;
  }
}
