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
package org.sonarqube.ws.client.permissions;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/update_template">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class UpdateTemplateRequest {

  private String description;
  private String id;
  private String name;
  private String projectKeyPattern;

  /**
   * Example value: "Permissions for all projects related to the financial service"
   */
  public UpdateTemplateRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getDescription() {
    return description;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "af8cb8cc-1e78-4c4e-8c00-ee8e814009a5"
   */
  public UpdateTemplateRequest setId(String id) {
    this.id = id;
    return this;
  }

  public String getId() {
    return id;
  }

  /**
   * Example value: "Financial Service Permissions"
   */
  public UpdateTemplateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * Example value: ".*\\.finance\\..*"
   */
  public UpdateTemplateRequest setProjectKeyPattern(String projectKeyPattern) {
    this.projectKeyPattern = projectKeyPattern;
    return this;
  }

  public String getProjectKeyPattern() {
    return projectKeyPattern;
  }
}
