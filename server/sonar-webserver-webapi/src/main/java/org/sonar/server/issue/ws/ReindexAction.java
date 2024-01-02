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
package org.sonar.server.issue.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;

/**
 * Implementation of the {@code reindex} action for the Issues WebService.
 */
public class ReindexAction implements IssuesWsAction {

  private static final String ACTION = "reindex";
  private final DbClient dbClient;
  private final IssueIndexer issueIndexer;
  private final UserSession userSession;

  public ReindexAction(DbClient dbClient, IssueIndexer indexer, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueIndexer = indexer;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION)
      .setPost(true)
      .setDescription("Reindex issues for a project.<br> " +
        "Require 'Administer System' permission.")
      .setSince("9.8")
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    ProjectDto projectDto;
    try (DbSession dbSession = dbClient.openSession(false)) {
      projectDto = dbClient.projectDao().selectProjectByKey(dbSession, projectKey).orElseThrow(() -> new NotFoundException("project not found"));
    }

    issueIndexer.indexProject(projectDto.getUuid());
    response.noContent();
  }

}
