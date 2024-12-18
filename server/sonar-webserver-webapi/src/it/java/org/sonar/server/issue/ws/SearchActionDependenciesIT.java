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

import java.time.Clock;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.dependency.CveDto;
import org.sonar.db.dependency.IssuesDependencyDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.AsyncIssueIndexing;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newFileDto;

class SearchActionDependenciesIT {

  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);

  private final DbClient dbClient = db.getDbClient();
  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession), config);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), mock(AsyncIssueIndexing.class));
  private final IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter, mock(TaintChecker.class));
  private final SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private final Languages languages = new Languages();
  private final UserResponseFormatter userFormatter = new UserResponseFormatter(new AvatarResolverImpl());
  private final SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new TextRangeResponseFormatter(), userFormatter);
  private final PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);

  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = new IssueIndexSyncProgressChecker(db.getDbClient());

  private final WsActionTester ws = new WsActionTester(
    new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, searchResponseLoader, searchResponseFormat,
      System2.INSTANCE, dbClient));

  private RuleDto rule;
  private ProjectData projectData;
  private ComponentDto project;
  private ComponentDto projectFile;

  @BeforeEach
  void setup() {
    rule = db.rules().insertIssueRule();
    projectData = db.components().insertPublicProject();
    project = projectData.getMainBranchComponent();
    projectFile = db.components().insertComponent(newFileDto(project));
  }

  @Test
  void search_whenAttachedToCve_shouldReturnsCveId() {
    insertIssueWithCve("1");
    insertIssueWithCve("2");
    allowAnyoneOnProjects(projectData.getProjectDto());
    issueIndexer.indexAllIssues();

    SearchWsResponse searchWsResponse = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(searchWsResponse.getIssuesList())
      .extracting(Issue::getKey, Issue::getCveId).containsExactlyInAnyOrder(tuple("issue_key_1", "CVE-1"), tuple("issue_key_2", "CVE-2"));
  }

  private void insertIssueWithCve(String suffix) {
    IssueDto issueDto = db.issues().insertIssue(rule, project, projectFile, issue -> issue.setKee("issue_key_"+suffix));
    var cveDto = new CveDto("cve_uuid_"+suffix, "CVE-"+suffix, "Some CVE description "+suffix, 1.0, 2.0, 3.0, 4L, 5L, 6L, 7L);
    db.getDbClient().cveDao().insert(db.getSession(), cveDto);
    db.issues().insertIssuesDependency(new IssuesDependencyDto(issueDto.getKee(), cveDto.uuid()));
  }

  private void allowAnyoneOnProjects(ProjectDto... projects) {
    userSession.registerProjects(projects);
    Arrays.stream(projects).forEach(permissionIndexer::allowOnlyAnyone);
  }

}
