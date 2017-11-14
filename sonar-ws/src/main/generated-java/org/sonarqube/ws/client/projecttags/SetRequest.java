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
package org.sonarqube.ws.client.projecttags;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */

import java.util.List;

/**
 * Set tags on a project.<br>Requires the following permission: 'Administer' rights on the specified project
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_tags/set">Further information about this action online (including a response example)</a>
 * @since 6.4
 */
public class SetRequest {

  private String project;
  private List<String> tags;

  /**
   * Project key
   *
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public SetRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }

  /**
   * Comma-separated list of tags
   *
   * This is a mandatory parameter.
   * Example value: "finance, offshore"
   */
  public SetRequest setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }
}
