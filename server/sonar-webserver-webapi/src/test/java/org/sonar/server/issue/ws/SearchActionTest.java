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
package org.sonar.server.issue.ws;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.Common.RuleType.BUG;
import static org.sonarqube.ws.Common.RuleType.SECURITY_HOTSPOT;
import static org.sonarqube.ws.Common.RuleType.VULNERABILITY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_HIDE_COMMENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;

public class SearchActionTest {

  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = none();

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new AvatarResolverImpl());
  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat,
    new MapSettings().asConfig(), System2.INSTANCE, dbClient));
  private StartupIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);

  @Before
  public void setUp() {
    issueWorkflow.start();
  }

  @Test
  public void response_contains_all_fields_except_additional_fields() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn(user);
    ComponentDto project = db.components().insertPublicProject(organization);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setType(CODE_SMELL)
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(
        Issue::getOrganization, Issue::getKey, Issue::getRule, Issue::getSeverity, Issue::getComponent, Issue::getResolution, Issue::getStatus, Issue::getMessage, Issue::getEffort,
        Issue::getAssignee, Issue::getAuthor, Issue::getLine, Issue::getHash, Issue::getTagsList, Issue::getCreationDate, Issue::getUpdateDate)
      .containsExactlyInAnyOrder(
        tuple(organization.getKey(), issue.getKey(), rule.getKey().toString(), Severity.MAJOR, file.getKey(), RESOLUTION_FIXED, STATUS_RESOLVED, "the message", "10min",
          simon.getLogin(), "John", 42, "a227e508d6646b55a086ee11d63b21e9", asList("bug", "owasp"), formatDateTime(issue.getIssueCreationDate()),
          formatDateTime(issue.getIssueUpdateDate())));
  }

  @Test
  public void issue_on_external_rule() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert(RuleTesting.EXTERNAL_XOO, r -> r.setIsExternal(true).setLanguage("xoo"));
    IssueDto issue = db.issues().insert(rule, project, file);
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getRule, Issue::getExternalRuleEngine)
      .containsExactlyInAnyOrder(tuple(issue.getKey(), rule.getKey().toString(), "xoo"));
  }

  @Test
  public void hide_author_if_not_member_of_organization() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("John"));
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::hasAuthor)
      .containsExactlyInAnyOrder(tuple(issue.getKey(), false));
  }

  @Test
  public void issue_with_cross_file_locations() {
    ComponentDto project = db.components().insertPublicProject();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    ComponentDto anotherFile = db.components().insertComponent(newFileDto(project));
    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder().addFlow(DbIssues.Flow.newBuilder().addAllLocation(Arrays.asList(
      DbIssues.Location.newBuilder()
        .setComponentId(file.uuid())
        .setMsg("FLOW MESSAGE")
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .setStartOffset(0)
          .setEndOffset(12)
          .build())
        .build(),
      DbIssues.Location.newBuilder()
        .setComponentId(anotherFile.uuid())
        .setMsg("ANOTHER FLOW MESSAGE")
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .setStartOffset(0)
          .setEndOffset(12)
          .build())
        .build(),
      DbIssues.Location.newBuilder()
        // .setComponentId(no component id set)
        .setMsg("FLOW MESSAGE WITHOUT FILE UUID")
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .setStartOffset(0)
          .setEndOffset(12)
          .build())
        .build())));
    RuleDefinitionDto rule = newRule().getDefinition();
    db.issues().insert(rule, project, file, i -> i.setLocations(locations.build()));
    indexIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesCount()).isEqualTo(1);
    assertThat(result.getIssues(0).getFlows(0).getLocationsList()).extracting(Issues.Location::getComponent, Issues.Location::getMsg)
      .containsExactlyInAnyOrder(
        tuple(file.getKey(), "FLOW MESSAGE"),
        tuple(anotherFile.getKey(), "ANOTHER FLOW MESSAGE"),
        tuple(file.getKey(), "FLOW MESSAGE WITHOUT FILE UUID"));
  }

  @Test
  public void issue_with_comments() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John"));
    UserDto fabrice = db.users().insertUser(u -> u.setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));
    ComponentDto project = db.components().insertPublicProject();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(john.getUuid())
        .setIssueChangeCreationDate(parseDateTime("2014-09-09T12:00:00+0000").getTime()));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(fabrice.getUuid())
        .setIssueChangeCreationDate(parseDateTime("2014-09-10T12:00:00+0000").getTime()));
    session.commit();
    indexIssues();
    userSession.logIn(john);

    ws.newRequest()
      .setParam("additionalFields", "comments,users")
      .execute()
      .assertJson(this.getClass(), "issue_with_comments.json");
  }

  @Test
  public void issue_with_comment_hidden() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto fabrice = db.users().insertUser(u -> u.setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));
    ComponentDto project = db.components().insertPublicProject();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule().getDefinition();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(john.getUuid())
        .setCreatedAt(parseDateTime("2014-09-09T12:00:00+0000").getTime()));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto().setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(fabrice.getUuid())
        .setCreatedAt(parseDateTime("2014-09-10T19:10:03+0000").getTime()));
    session.commit();
    indexIssues();
    userSession.logIn(john);

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_HIDE_COMMENTS, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, i -> i.getComments().getCommentsList())
      .containsExactlyInAnyOrder(tuple(issue.getKey(), Collections.emptyList()));
  }

  @Test
  public void load_additional_fields() {
    UserDto simon = db.users().insertUser(u -> u.setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    ComponentDto project = db.components().insertPublicProject();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule().getDefinition();
    db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(simon.getUuid()).setType(CODE_SMELL));
    indexIssues();
    userSession.logIn("john");

    ws.newRequest()
      .setParam("additionalFields", "_all").execute()
      .assertJson(this.getClass(), "load_additional_fields.json");
  }

  @Test
  public void load_additional_fields_with_issue_admin_permission() {
    UserDto simon = db.users().insertUser(u -> u.setLogin("simon").setName("Simon").setEmail("simon@email.com"));
    UserDto fabrice = db.users().insertUser(u -> u.setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY").setLanguage("java"));
    grantPermissionToAnyone(project, ISSUE_ADMIN);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY").setLanguage("js"));

    IssueDto issue = newDto(newRule(), file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAuthorLogin(fabrice.getLogin())
      .setAssigneeUuid(simon.getUuid())
      .setType(CODE_SMELL);
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();

    userSession.logIn("john")
      .addProjectPermission(ISSUE_ADMIN, project); // granted by Anyone
    ws.newRequest()
      .setParam("additionalFields", "_all").execute()
      .assertJson(this.getClass(), "load_additional_fields_with_issue_admin_permission.json");
  }

  @Test
  public void search_by_rule_key() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY").setLanguage("java"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY").setLanguage("java"));

    IssueDto issue = IssueTesting.newIssue(rule.getDefinition(), project, file);
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();

    userSession.logIn("john")
      .addProjectPermission(ISSUE_ADMIN, project); // granted by Anyone
    indexPermissions();

    TestResponse execute = ws.newRequest()
      .setParam(PARAM_RULES, rule.getKey().toString())
      .setParam("additionalFields", "_all")
      .execute();
    execute.assertJson(this.getClass(), "result_for_rule_search.json");
  }

  @Test
  public void issue_on_removed_file() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-2"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project, null).setUuid("REMOVED_FILE_ID")
      .setDbKey("REMOVED_FILE_KEY")
      .setEnabled(false));

    IssueDto issue = newDto(rule, removedFile, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setComponent(removedFile)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"));
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();

    ws.newRequest()
      .execute()
      .assertJson(this.getClass(), "issue_on_removed_file.json");
  }

  @Test
  public void apply_paging_with_one_component() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-2"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    for (int i = 0; i < SearchOptions.MAX_LIMIT + 1; i++) {
      IssueDto issue = newDto(rule, file, project).setAssigneeUuid(null);
      dbClient.issueDao().insert(session, issue);
    }
    session.commit();
    indexIssues();

    ws.newRequest().setParam(PARAM_COMPONENT_KEYS, file.getKey()).execute()
      .assertJson(this.getClass(), "apply_paging_with_one_component.json");
  }

  @Test
  public void components_contains_sub_projects() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("ProjectHavingModule"));
    indexPermissions();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project).setDbKey("ModuleHavingFile"));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, "BCDE").setDbKey("FileLinkedToModule"));
    IssueDto issue = newDto(newRule(), file, project);
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();

    ws.newRequest().setParam(PARAM_ADDITIONAL_FIELDS, "_all").execute()
      .assertJson(this.getClass(), "components_contains_sub_projects.json");
  }

  @Test
  public void filter_by_assigned_to_me() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    RuleDto rule = newRule();
    IssueDto issue1 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(null);
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    userSession.logIn(john);

    ws.newRequest()
      .setParam("resolved", "false")
      .setParam("assignees", "__me__")
      .setParam(FACETS, "assignees,assigned_to_me")
      .execute()
      .assertJson(this.getClass(), "filter_by_assigned_to_me.json");
  }

  @Test
  public void return_empty_when_login_is_unknown() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    RuleDto rule = newRule();
    IssueDto issue1 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newDto(rule, file, project)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(null);
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    userSession.logIn(john);

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam("assignees", "unknown")
      .setParam(FACETS, "assignees")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).isEmpty();
  }

  @Test
  public void filter_by_assigned_to_me_when_not_authenticate() {
    UserDto poy = db.users().insertUser(u -> u.setLogin("poy").setName("poypoy").setEmail("poypoy@email.com"));
    userSession.logIn(poy);
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    RuleDto rule = newRule();
    IssueDto issue1 = newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newDto(rule, file, project)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setAssigneeUuid(null);
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam("resolved", "false")
      .setParam("assignees", "__me__")
      .execute()
      .assertJson(this.getClass(), "empty_result.json");
  }

  @Test
  public void search_by_author() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("leia"));
    IssueDto issue2 = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("luke"));
    IssueDto issue3 = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("han, solo"));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setMultiParam("author", asList("leia", "han, solo"))
      .setParam(FACETS, "author")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue3.getKey());
    Common.Facet facet = response.getFacets().getFacetsList().get(0);
    assertThat(facet.getProperty()).isEqualTo("author");
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("leia", 1L),
        tuple("luke", 1L),
        tuple("han, solo", 1L));

    assertThat(ws.newRequest()
      .setMultiParam("author", singletonList("unknown"))
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .isEmpty();
  }

  @Test
  public void search_by_deprecated_authors_parameter() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("leia"));
    IssueDto issue2 = db.issues().insert(rule, project, file, i -> i.setAuthorLogin("luke"));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("authors", "leia")
      .setParam(FACETS, "authors")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getIssuesList()).extracting(Issue::getKey).containsExactlyInAnyOrder(issue1.getKey());
    Common.Facet facet = response.getFacets().getFacetsList().get(0);
    assertThat(facet.getProperty()).isEqualTo("authors");
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("leia", 1L),
        tuple("luke", 1L));

    // Deprecated parameter 'authors' will be ignored if new parameter 'author' is set
    assertThat(ws.newRequest()
      .setMultiParam("author", singletonList("luke"))
      // This parameter will be ignored
      .setParam("authors", "leia")
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey)
        .containsExactlyInAnyOrder(issue2.getKey());
  }

  @Test
  public void sort_by_updated_at() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-2"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    dbClient.issueDao().insert(session, newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac1")
      .setIssueUpdateDate(parseDateTime("2014-11-02T00:00:00+0100")));
    dbClient.issueDao().insert(session, newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setIssueUpdateDate(parseDateTime("2014-11-01T00:00:00+0100")));
    dbClient.issueDao().insert(session, newDto(rule, file, project)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac3")
      .setIssueUpdateDate(parseDateTime("2014-11-03T00:00:00+0100")));
    session.commit();
    indexIssues();

    TestResponse response = ws.newRequest()
      .setParam("s", IssueQuery.SORT_BY_UPDATE_DATE)
      .setParam("asc", "false")
      .execute();

    JsonElement parse = new JsonParser().parse(response.getInput());

    assertThat(parse.getAsJsonObject().get("issues").getAsJsonArray())
      .extracting(o -> o.getAsJsonObject().get("key").getAsString())
      .containsExactly("82fd47d4-b650-4037-80bc-7b112bd4eac3", "82fd47d4-b650-4037-80bc-7b112bd4eac1", "82fd47d4-b650-4037-80bc-7b112bd4eac2");
  }

  @Test
  public void security_hotspots_are_returned_by_default() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.BUG));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.VULNERABILITY));
    db.issues().insert(rule, project, file, i -> i.setType(CODE_SMELL));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getType)
      .containsExactlyInAnyOrder(BUG, VULNERABILITY, Common.RuleType.CODE_SMELL, SECURITY_HOTSPOT);
  }

  @Test
  public void security_hotspot_type_included_when_explicitly_selected() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = newRule().getDefinition();
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.BUG));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.VULNERABILITY));
    db.issues().insert(rule, project, file, i -> i.setType(CODE_SMELL));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));
    indexPermissions();
    indexIssues();

    assertThat(ws.newRequest()
      .setParam("types", RuleType.SECURITY_HOTSPOT.toString())
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getType)
        .containsExactlyInAnyOrder(SECURITY_HOTSPOT);

    assertThat(ws.newRequest()
      .setParam("types", String.format("%s,%s", RuleType.BUG, RuleType.SECURITY_HOTSPOT))
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getType)
        .containsExactlyInAnyOrder(BUG, SECURITY_HOTSPOT);
  }

  @Test
  public void security_hotspot_are_ignored_when_filtering_by_severities() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.BUG).setSeverity(Severity.MAJOR.name()));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.VULNERABILITY).setSeverity(Severity.MAJOR.name()));
    db.issues().insert(rule, project, file, i -> i.setType(CODE_SMELL).setSeverity(Severity.MAJOR.name()));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT).setSeverity(Severity.MAJOR.name()));
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("severities", Severity.MAJOR.name())
      .setParam(FACETS, "severities")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getType)
      .containsExactlyInAnyOrder(BUG, VULNERABILITY, Common.RuleType.CODE_SMELL);
    assertThat(result.getFacets().getFacets(0).getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(tuple("MAJOR", 3L), tuple("INFO", 0L), tuple("MINOR", 0L), tuple("CRITICAL", 0L), tuple("BLOCKER", 0L));
  }

  @Test
  public void do_not_return_severity_on_security_hotspots() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.BUG).setSeverity(Severity.MAJOR.name()));
    db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT).setSeverity(Severity.MAJOR.name()));
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("types", String.format("%s,%s", RuleType.BUG, RuleType.SECURITY_HOTSPOT))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getType, Issue::hasSeverity)
      .containsExactlyInAnyOrder(
        tuple(BUG, true),
        tuple(SECURITY_HOTSPOT, false));
  }

  @Test
  public void return_total_effort() {
    UserDto john = db.users().insertUser();
    userSession.logIn(john);
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue1 = db.issues().insert(rule, project, file, i -> i.setEffort(10L));
    IssueDto issue2 = db.issues().insert(rule, project, file, i -> i.setEffort(15L));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getEffortTotal()).isEqualTo(25L);
  }

  @Test
  public void paging() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = newDto(rule, file, project);
      dbClient.issueDao().insert(session, issue);
    }
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam(WebService.Param.PAGE, "2")
      .setParam(WebService.Param.PAGE_SIZE, "9")
      .execute()
      .assertJson(this.getClass(), "paging.json");
  }

  @Test
  public void paging_with_page_size_to_minus_one() {
    RuleDto rule = newRule();
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPublicProjectDto(organization, "PROJECT_ID").setDbKey("PROJECT_KEY"));
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setDbKey("FILE_KEY"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = newDto(rule, file, project);
      dbClient.issueDao().insert(session, issue);
    }
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam(WebService.Param.PAGE, "1")
      .setParam(WebService.Param.PAGE_SIZE, "-1")
      .execute()
      .assertJson(this.getClass(), "paging_with_page_size_to_minus_one.json");
  }

  @Test
  public void default_page_size_is_100() {
    ws.newRequest()
      .execute()
      .assertJson(this.getClass(), "default_page_size_is_100.json");
  }

  // SONAR-10217
  @Test
  public void empty_search_with_unknown_branch() {
    SearchWsResponse response = ws.newRequest()
      .setParam("onComponentOnly", "true")
      .setParam("componentKeys", "foo")
      .setParam("branch", "bar")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response)
      .extracting(SearchWsResponse::getIssuesList, r -> r.getPaging().getTotal())
      .containsExactlyInAnyOrder(Collections.emptyList(), 0);
  }

  @Test
  public void empty_search() {
    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response)
      .extracting(SearchWsResponse::getIssuesList, r -> r.getPaging().getTotal())
      .containsExactlyInAnyOrder(Collections.emptyList(), 0);
  }

  @Test
  public void fail_when_invalid_format() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Date 'wrong-date-input' cannot be parsed as either a date or date+time");

    ws.newRequest()
      .setParam(PARAM_CREATED_AFTER, "wrong-date-input")
      .execute();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("3.6");
    assertThat(def.responseExampleAsString()).isNotEmpty();

    assertThat(def.params()).extracting("key").containsExactlyInAnyOrder(
      "additionalFields", "asc", "assigned", "assignees", "authors", "author", "componentKeys", "componentUuids", "branch",
      "pullRequest", "organization",
      "createdAfter", "createdAt", "createdBefore", "createdInLast", "directories", "facetMode", "facets", "fileUuids", "issues", "languages", "moduleUuids", "onComponentOnly",
      "p", "projects", "ps", "resolutions", "resolved", "rules", "s", "severities", "sinceLeakPeriod",
      "statuses", "tags", "types", "owaspTop10", "sansTop25", "cwe", "sonarsourceSecurity");

    assertThat(def.param("organization"))
      .matches(WebService.Param::isInternal)
      .matches(p -> p.since().equals("6.4"));

    WebService.Param branch = def.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isTrue();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");

    WebService.Param projectUuids = def.param("projects");
    assertThat(projectUuids.description()).isEqualTo("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
      "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. If this parameter is set, projectUuids must not be set.");
  }

  private RuleDto newRule() {
    RuleDto rule = RuleTesting.newXooX1()
      .setName("Rule name")
      .setDescription("Rule desc")
      .setStatus(RuleStatus.READY);
    db.rules().insert(rule.getDefinition());
    return rule;
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
  }

  private void grantPermissionToAnyone(ComponentDto project, String permission) {
    dbClient.groupPermissionDao().insert(session,
      new GroupPermissionDto()
        .setOrganizationUuid(project.getOrganizationUuid())
        .setGroupId(null)
        .setResourceId(project.getId())
        .setRole(permission));
    session.commit();
    userSession.logIn().addProjectPermission(permission, project);
  }

}
