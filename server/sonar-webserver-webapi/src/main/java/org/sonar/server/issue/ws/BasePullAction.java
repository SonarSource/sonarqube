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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.ws.pull.ProtobufObjectGenerator;
import org.sonar.server.issue.ws.pull.PullActionIssuesRetriever;
import org.sonar.server.issue.ws.pull.PullActionResponseWriter;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsAction;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.sonar.api.web.UserRole.USER;

public abstract class BasePullAction implements WsAction {
  protected static final String PROJECT_KEY_PARAM = "projectKey";
  protected static final String BRANCH_NAME_PARAM = "branchName";
  protected static final String LANGUAGES_PARAM = "languages";
  protected static final String RULE_REPOSITORIES_PARAM = "ruleRepositories";
  protected static final String RESOLVED_ONLY_PARAM = "resolvedOnly";
  protected static final String CHANGED_SINCE_PARAM = "changedSince";
  protected final String actionName;
  protected final String issueType;
  protected final String repositoryExample;
  protected final String sinceVersion;
  protected final String resourceExample;

  private final ComponentFinder componentFinder;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PullActionResponseWriter pullActionResponseWriter;

  protected BasePullAction(System2 system2, ComponentFinder componentFinder, DbClient dbClient, UserSession userSession,
    ProtobufObjectGenerator protobufObjectGenerator, String actionName, String issueType,
    String repositoryExample, String sinceVersion, String resourceExample) {
    this.componentFinder = componentFinder;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.pullActionResponseWriter = new PullActionResponseWriter(system2, protobufObjectGenerator);
    this.actionName = actionName;
    this.issueType = issueType;
    this.repositoryExample = repositoryExample;
    this.sinceVersion = sinceVersion;
    this.resourceExample = resourceExample;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(actionName)
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource(resourceExample))
      .setDescription(format("This endpoint fetches and returns all (unless filtered by optional params) the %s for a given branch. " +
        "The %s returned are not paginated, so the response size can be big. Requires project 'Browse' permission.", issueType, issueType))
      .setSince(sinceVersion)
      .setChangelog(new Change("10.3", "The response field 'creationDate' contains the functional issue creation date " +
        "instead of the database operation date"));

    action.createParam(PROJECT_KEY_PARAM)
      .setRequired(true)
      .setDescription(format("Project key for which %s are fetched.", issueType))
      .setExampleValue("sonarqube");

    action.createParam(BRANCH_NAME_PARAM)
      .setRequired(true)
      .setDescription(format("Branch name for which %s are fetched.", issueType))
      .setExampleValue("develop");

    action.createParam(LANGUAGES_PARAM)
      .setDescription(format("Comma separated list of languages. If not present all %s regardless of their language are returned.", issueType))
      .setExampleValue("java,cobol");

    action.createParam(CHANGED_SINCE_PARAM)
      .setDescription(format("Timestamp. If present only %s modified after given timestamp are returned (both open and closed). " +
        "If not present all non-closed %s are returned.", issueType, issueType))
      .setExampleValue(1_654_032_306_000L);

    additionalParams(action);
  }

  protected void additionalParams(WebService.NewAction action) {
    // define additional parameters if needed
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PROJECT_KEY_PARAM);
    String branchName = request.mandatoryParam(BRANCH_NAME_PARAM);
    List<String> languages = request.paramAsStrings(LANGUAGES_PARAM);
    String changedSince = request.param(CHANGED_SINCE_PARAM);
    Long changedSinceTimestamp = changedSince != null ? Long.parseLong(changedSince) : null;

    BasePullRequest wsRequest = new BasePullRequest(projectKey, branchName)
      .languages(languages)
      .changedSinceTimestamp(changedSinceTimestamp);

    processAdditionalParams(request, wsRequest);

    doHandle(wsRequest, response.stream().output());
  }

  protected void processAdditionalParams(Request request, BasePullRequest wsRequest) {
    // process additional parameters if needed
  }

  private void doHandle(BasePullRequest wsRequest, OutputStream outputStream) throws IOException {

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto projectDto = componentFinder.getProjectByKey(dbSession, wsRequest.projectKey);
      userSession.checkEntityPermission(USER, projectDto);
      BranchDto branchDto = componentFinder.getBranchOrPullRequest(dbSession, projectDto, wsRequest.branchName, null);
      IssueQueryParams issueQueryParams = initializeQueryParams(branchDto, wsRequest.languages, wsRequest.repositories,
        wsRequest.resolvedOnly, wsRequest.changedSinceTimestamp);
      retrieveAndSendIssues(dbSession, issueQueryParams, outputStream);
    }
  }

  protected abstract IssueQueryParams initializeQueryParams(BranchDto branchDto, @Nullable List<String> languages,
    @Nullable List<String> ruleRepositories, boolean resolvedOnly, @Nullable Long changedSince);

  private void retrieveAndSendIssues(DbSession dbSession, IssueQueryParams queryParams, OutputStream outputStream)
    throws IOException {
    pullActionResponseWriter.appendTimestampToResponse(outputStream);

    var issuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);

    processNonClosedIssuesInBatches(dbSession, queryParams, outputStream, issuesRetriever);

    if (queryParams.getChangedSince() != null) {
      // in the "incremental mode" we need to send SonarLint also recently closed issues keys
      List<String> closedIssues = issuesRetriever.retrieveClosedIssues(dbSession);
      pullActionResponseWriter.appendClosedIssuesUuidsToResponse(closedIssues, outputStream);
    }
  }

  private void processNonClosedIssuesInBatches(DbSession dbSession, IssueQueryParams queryParams, OutputStream outputStream,
    PullActionIssuesRetriever issuesRetriever) {
    int nextPage = 1;
    Map<String, RuleDto> ruleCache = new HashMap<>();
    do {
      Set<String> issueKeysSnapshot = getIssueKeysSnapshot(queryParams, nextPage);
      Consumer<List<IssueDto>> listConsumer = issueDtos -> {
        populateRuleCache(dbSession, ruleCache, issueDtos);
        pullActionResponseWriter.appendIssuesToResponse(issueDtos, ruleCache, outputStream);
      };
      Predicate<IssueDto> filter = issueDto -> filterNonClosedIssues(issueDto, queryParams);
      issuesRetriever.processIssuesByBatch(dbSession, issueKeysSnapshot, listConsumer, filter);

      if (issueKeysSnapshot.isEmpty()) {
        nextPage = -1;
      } else {
        nextPage++;
      }
    } while (nextPage > 0);
  }

  private void populateRuleCache(DbSession dbSession, Map<String, RuleDto> ruleCache, List<IssueDto> issueDtos) {
    Set<String> rulesToQueryFor = issueDtos.stream()
      .map(IssueDto::getRuleUuid)
      .filter(ruleUuid -> !ruleCache.containsKey(ruleUuid))
      .collect(Collectors.toSet());
    dbClient.ruleDao().selectByUuids(dbSession, rulesToQueryFor)
      .forEach(ruleDto -> ruleCache.putIfAbsent(ruleDto.getUuid(), ruleDto));
  }

  protected abstract boolean filterNonClosedIssues(IssueDto issueDto, IssueQueryParams queryParams);

  protected abstract Set<String> getIssueKeysSnapshot(IssueQueryParams queryParams, int page);

  protected static class BasePullRequest {
    private final String projectKey;
    private final String branchName;
    private List<String> languages = null;
    private List<String> repositories = null;
    private boolean resolvedOnly = false;
    private Long changedSinceTimestamp = null;

    public BasePullRequest(String projectKey, String branchName) {
      this.projectKey = projectKey;
      this.branchName = branchName;
    }

    public BasePullRequest languages(@Nullable List<String> languages) {
      this.languages = languages;
      return this;
    }

    public BasePullRequest repositories(@Nullable List<String> repositories) {
      this.repositories = repositories == null ? emptyList() : repositories;
      return this;
    }

    public BasePullRequest resolvedOnly(boolean resolvedOnly) {
      this.resolvedOnly = resolvedOnly;
      return this;
    }

    public BasePullRequest changedSinceTimestamp(@Nullable Long changedSinceTimestamp) {
      this.changedSinceTimestamp = changedSinceTimestamp;
      return this;
    }
  }

}
