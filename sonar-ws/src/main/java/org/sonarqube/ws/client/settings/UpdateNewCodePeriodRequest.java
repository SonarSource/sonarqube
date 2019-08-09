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
package org.sonarqube.ws.client.settings;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 *
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/settings/update_new_code_period">Further information about this action online (including a response example)</a>
 * @since 8.0
 */
@Generated("sonar-ws-generator")
public class UpdateNewCodePeriodRequest {

  private String branch;
  private String project;
  private String type;
  private String value;

  /**
   *
   */
  public UpdateNewCodePeriodRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   *
   */
  public UpdateNewCodePeriodRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }

  /**
   * This is a mandatory parameter.
   */
  public UpdateNewCodePeriodRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return type;
  }

  /**
   *
   */
  public UpdateNewCodePeriodRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public String getValue() {
    return value;
  }
}
