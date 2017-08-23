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
package org.sonarqube.ws.client.projectbranches;

import org.sonarqube.ws.WsBranches.ListWsResponse;
import org.sonarqube.ws.WsBranches.ShowWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_LIST;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_DELETE;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_RENAME;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.CONTROLLER;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_PROJECT;

public class ProjectBranchesService extends BaseService {

  public ProjectBranchesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER);
  }

  public ListWsResponse list(String project) {
    GetRequest get = new GetRequest(path(ACTION_LIST))
      .setParam(PARAM_PROJECT, project);
    return call(get, ListWsResponse.parser());
  }

  public ShowWsResponse show(String project, String branch) {
    GetRequest get = new GetRequest(path(ACTION_SHOW))
      .setParam(PARAM_PROJECT, project)
      .setParam(PARAM_BRANCH, branch);
    return call(get, ShowWsResponse.parser());
  }

  public void delete(String project, String branch) {
    PostRequest post = new PostRequest(path(ACTION_DELETE))
      .setParam(PARAM_PROJECT, project)
      .setParam(PARAM_BRANCH, branch);
    call(post);
  }
  
  public void rename(String project, String branch) {
    PostRequest post = new PostRequest(path(ACTION_RENAME))
      .setParam(PARAM_PROJECT, project)
      .setParam(PARAM_BRANCH, branch);
    call(post);
  }

}
