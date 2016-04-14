/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.project;

import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * Maps web service {@code api/projects}.
 * @since 5.5
 */
public class ProjectsService extends BaseService {

  public ProjectsService(WsConnector wsConnector) {
    super(wsConnector, "api/projects");
  }

  /**
   * Provisions a new project.
   *
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   */
  public void create(CreateRequest project) {
    PostRequest request = new PostRequest(path("create"))
      .setParam("key", project.getKey())
      .setParam("name", project.getName())
      .setParam("branch", project.getBranch());
    call(request);
  }

  /**
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   */
  public void delete(DeleteRequest request) {
    call(new PostRequest(path("delete"))
      .setParam("id", request.getId())
      .setParam("key", request.getKey()));
  }
}
