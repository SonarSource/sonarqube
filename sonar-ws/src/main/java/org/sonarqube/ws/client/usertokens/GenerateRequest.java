/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.client.usertokens;

import javax.annotation.Generated;
import javax.annotation.Nullable;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_tokens/generate">Further information about this action online (including a response example)</a>
 * @since 5.3
 */
@Generated("sonar-ws-generator")
public class GenerateRequest {

  private String login;
  private String name;
  private String type;
  private String projectKey;
  private String expirationDate;

  /**
   * Example value: "g.hopper"
   */
  public GenerateRequest setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getLogin() {
    return login;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "Project scan on Travis"
   */
  public GenerateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public GenerateRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public GenerateRequest setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getExpirationDate() {
    return expirationDate;
  }

  public GenerateRequest setExpirationDate(String expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }
}
