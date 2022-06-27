/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.issue.ws.pull.PullActionIssuesRetriever;
import org.sonar.server.issue.ws.pull.PullActionResponseWriter;
import org.sonar.server.user.UserSession;

import static java.util.Optional.*;
import static org.sonar.api.web.UserRole.USER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_PULL;

public class PullAction implements IssuesWsAction {

  private static final String PROJECT_KEY_PARAM = "projectKey";
  private static final String BRANCH_NAME_PARAM = "branchName";
  private static final String LANGUAGES_PARAM = "languages";
  private static final String RULE_REPOSITORIES_PARAM = "ruleRepositories";
  private static final String RESOLVED_ONLY_PARAM = "resolvedOnly";
  private static final String CHANGED_SINCE_PARAM = "changedSince";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PullActionResponseWriter pullActionResponseWriter;

  public PullAction(DbClient dbClient, UserSession userSession, PullActionResponseWriter pullActionResponseWriter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.pullActionResponseWriter = pullActionResponseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(ACTION_PULL)
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("pull-example.proto"))
      .setDescription("This endpoint fetches and returns all (unless filtered by optional params) the issues for a given branch." +
        "The issues returned are not paginated, so the response size can be big.")
      .setSince("9.5");

    action.createParam(PROJECT_KEY_PARAM)
      .setRequired(true)
      .setDescription("Project key for which issues are fetched.")
      .setExampleValue("sonarqube");

    action.createParam(BRANCH_NAME_PARAM)
      .setRequired(true)
      .setDescription("Branch name for which issues are fetched.")
      .setExampleValue("develop");

    action.createParam(LANGUAGES_PARAM)
      .setDescription("Comma seperated list of languages. If not present all issues regardless of their language are returned.")
      .setExampleValue("java,cobol");

    action.createParam(RULE_REPOSITORIES_PARAM)
      .setDescription("Comma seperated list of rule repositories. If not present all issues regardless of" +
        " their rule repository are returned.")
      .setExampleValue("java");

    action.createParam(RESOLVED_ONLY_PARAM)
      .setDescription("If true only issues with resolved status are returned")
      .setExampleValue("true");

    action.createParam(CHANGED_SINCE_PARAM)
      .setDescription("Timestamp. If present only issues modified after given timestamp are returned (both open and closed). " +
        "If not present all non-closed issues are returned.")
      .setExampleValue(1_654_032_306_000L);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PROJECT_KEY_PARAM);
    String branchName = request.mandatoryParam(BRANCH_NAME_PARAM);
    List<String> languages = request.paramAsStrings(LANGUAGES_PARAM);
    List<String> ruleRepositories = request.paramAsStrings(RULE_REPOSITORIES_PARAM);
    boolean resolvedOnly = Boolean.parseBoolean(request.param(RESOLVED_ONLY_PARAM));
    String changedSince = request.param(CHANGED_SINCE_PARAM);
    Long changedSinceTimestamp = changedSince != null ? Long.parseLong(changedSince) : null;

    streamResponse(projectKey, branchName, languages, ruleRepositories, resolvedOnly, changedSinceTimestamp, response.stream().output());
  }

  private void streamResponse(String projectKey, String branchName, @Nullable List<String> languages,
    @Nullable List<String> ruleRepositories, boolean resolvedOnly, @Nullable Long changedSince, OutputStream outputStream)
    throws IOException {

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ProjectDto> projectDto = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      validateProjectPermissions(projectDto);
      pullActionResponseWriter.appendTimestampToResponse(outputStream);
      var pullActionQueryParams = new IssueQueryParams(projectDto.get().getUuid(), branchName,
        languages, ruleRepositories, resolvedOnly, changedSince);
      retrieveAndSendIssues(dbSession, projectDto.get().getUuid(), pullActionQueryParams, outputStream);
    }
  }

  private void validateProjectPermissions(Optional<ProjectDto> projectDto) {
    if (projectDto.isEmpty()) {
      throw new IllegalArgumentException("Invalid " + PROJECT_KEY_PARAM + " parameter");
    }
    userSession.checkProjectPermission(USER, projectDto.get());
  }

  private void retrieveAndSendIssues(DbSession dbSession, String componentUuid, IssueQueryParams queryParams, OutputStream outputStream)
    throws IOException {

    var issuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);

    Set<String> issueKeysSnapshot = new HashSet<>(getIssueKeysSnapshot(componentUuid, queryParams.getChangedSince()));
    Consumer<List<IssueDto>> listConsumer = issueDtos -> pullActionResponseWriter.appendIssuesToResponse(issueDtos, outputStream);
    issuesRetriever.processIssuesByBatch(dbSession, issueKeysSnapshot, listConsumer);

    if (queryParams.getChangedSince() != null) {
      // in the "incremental mode" we need to send SonarLint also recently closed issues keys
      List<String> closedIssues = issuesRetriever.retrieveClosedIssues(dbSession);
      pullActionResponseWriter.appendClosedIssuesUuidsToResponse(closedIssues, outputStream);
    }
  }

  private Set<String> getIssueKeysSnapshot(String componentUuid, @Nullable Long changedSince) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<Long> changedSinceDate = ofNullable(changedSince);

      if (changedSinceDate.isPresent()) {
        return dbClient.issueDao().selectIssueKeysByComponentUuidAndChangedSinceDate(dbSession, componentUuid, changedSinceDate.get());
      }

      return dbClient.issueDao().selectIssueKeysByComponentUuid(dbSession, componentUuid);
    }
  }
}
