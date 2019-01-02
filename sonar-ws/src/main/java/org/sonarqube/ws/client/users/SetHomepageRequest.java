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
package org.sonarqube.ws.client.users;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/set_homepage">Further information about this action online (including a response example)</a>
 * @since 7.0
 */
@Generated("sonar-ws-generator")
public class SetHomepageRequest {

  private String parameter;
  private String type;

  /**
   * Example value: "my_project"
   */
  public SetHomepageRequest setParameter(String parameter) {
    this.parameter = parameter;
    return this;
  }

  public String getParameter() {
    return parameter;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"PROJECT"</li>
   *   <li>"ORGANIZATION"</li>
   *   <li>"MY_PROJECTS"</li>
   *   <li>"MY_ISSUES"</li>
   * </ul>
   */
  public SetHomepageRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return type;
  }
}
