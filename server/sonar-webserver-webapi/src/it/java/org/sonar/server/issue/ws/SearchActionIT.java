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

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueFixedDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.db.component.ComponentQualifiers.UNIT_TEST_FILE;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.issue.CommentAction.COMMENT_KEY;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.Common.RuleType.BUG;
import static org.sonarqube.ws.Common.RuleType.SECURITY_HOTSPOT_VALUE;
import static org.sonarqube.ws.Common.RuleType.VULNERABILITY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CODE_VARIANTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_HIDE_COMMENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IN_NEW_CODE_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;

public class SearchActionIT {

  public static final DbIssues.MessageFormatting MESSAGE_FORMATTING = DbIssues.MessageFormatting.newBuilder()
    .setStart(0).setEnd(11).setType(CODE).build();
  private final UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();
  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession session = db.getSession();
  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession,
    new WebAuthorizationTypeSupport(userSession));
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private final IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private final SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient,
    new TransitionService(userSession, issueWorkflow));
  private final Languages languages = new Languages();
  private final UserResponseFormatter userFormatter = new UserResponseFormatter(new AvatarResolverImpl());
  private final SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages,
    new TextRangeResponseFormatter(), userFormatter);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = new IssueIndexSyncProgressChecker(dbClient);
  private final WsActionTester ws = new WsActionTester(
    new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, searchResponseLoader,
      searchResponseFormat, System2.INSTANCE, dbClient));
  private final PermissionIndexer permissionIndexer = new PermissionIndexer(dbClient, es.client(), issueIndexer);

  @Before
  public void setUp() {
    issueWorkflow.start();
  }

  @Test
  public void givenPrivateProject_responseContainsAllFieldsExceptAdditionalFields() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ProjectData projectData = db.components().insertPrivateProject();

    ProjectDto projectDto = projectData.getProjectDto();
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, projectDto);

    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(
        Issue::getKey, Issue::getRule, Issue::getSeverity, Issue::getComponent, Issue::getResolution, Issue::getStatus, Issue::getMessage
        , Issue::getMessageFormattingsList,
        Issue::getEffort, Issue::getAssignee, Issue::getAuthor, Issue::getLine, Issue::getHash, Issue::getTagsList,
        Issue::getCreationDate, Issue::getUpdateDate,
        Issue::getQuickFixAvailable, Issue::getCodeVariantsList)
      .containsExactlyInAnyOrder(
        tuple(issue.getKey(), rule.getKey().toString(), Severity.MAJOR, file.getKey(), RESOLUTION_FIXED, STATUS_RESOLVED, "the message",
          MessageFormattingUtils.dbMessageFormattingListToWs(List.of(MESSAGE_FORMATTING)), "10min",
          simon.getLogin(), "John", 42, "a227e508d6646b55a086ee11d63b21e9", asList("bug", "owasp"),
          formatDateTime(issue.getIssueCreationDate()),
          formatDateTime(issue.getIssueUpdateDate()), false, List.of("variant1", "variant2")));
  }

  @Test
  public void response_contains_correct_actions() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = newIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FIXED));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_ADDITIONAL_FIELDS, "actions")
      .setParam(PARAM_STATUSES, STATUS_OPEN)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(
      response
        .getIssuesList()
        .get(0)
        .getActions()
        .getActionsList())
      .isEqualTo(asList(ACTION_SET_TAGS, COMMENT_KEY, ACTION_ASSIGN));

    response = ws.newRequest()
      .setParam(PARAM_ADDITIONAL_FIELDS, "actions")
      .setParam(PARAM_STATUSES, STATUS_RESOLVED)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(
      response
        .getIssuesList()
        .get(0)
        .getActions()
        .getActionsList())
      .isEqualTo(asList(ACTION_SET_TAGS, COMMENT_KEY));
  }

  @Test
  public void issue_on_external_rule() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule(RuleTesting.EXTERNAL_XOO, r -> r.setIsExternal(true).setLanguage("xoo"));
    IssueDto issue = db.issues().insertIssue(rule, project, file);
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getRule, Issue::getExternalRuleEngine)
      .containsExactlyInAnyOrder(tuple(issue.getKey(), rule.getKey().toString(), "xoo"));
  }

  @Test
  public void issue_on_external_adhoc_rule_without_metadata() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule(RuleTesting.EXTERNAL_XOO, r -> r.setIsExternal(true)
      .setName("xoo:x1:name")
      .setAdHocName(null)
      .setLanguage("xoo")
      .setIsAdHoc(true));
    IssueDto issue = db.issues().insertIssue(rule, project, file);
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("additionalFields", "rules")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getRule, Issue::getExternalRuleEngine)
      .containsExactlyInAnyOrder(tuple(issue.getKey(), rule.getKey().toString(), "xoo"));

    assertThat((response.getRules().getRulesList()))
      .extracting(Common.Rule::getKey, Common.Rule::getName)
      .containsExactlyInAnyOrder(tuple(rule.getKey().toString(), rule.getName()));
  }

  @Test
  public void issue_on_external_adhoc_rule_with_metadata() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule(RuleTesting.EXTERNAL_XOO,
      r -> r
        .setIsExternal(true)
        .setLanguage("xoo")
        .setIsAdHoc(true)
        .setAdHocName("different_rule_name"));
    IssueDto issue = db.issues().insertIssue(rule, project, file);
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("additionalFields", "rules")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getRule, Issue::getExternalRuleEngine)
      .containsExactlyInAnyOrder(tuple(issue.getKey(), rule.getKey().toString(), "xoo"));

    assertThat(response.getRules().getRulesList())
      .extracting(Common.Rule::getKey, Common.Rule::getName)
      .containsExactlyInAnyOrder(tuple(rule.getKey().toString(), rule.getAdHocName()));
  }

  @Test
  public void issue_with_cross_file_locations() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
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
        .addMsgFormatting(DbIssues.MessageFormatting.newBuilder().setStart(0).setEnd(20).setType(CODE).build())
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
    RuleDto rule = newIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i.setLocations(locations.build()));
    indexIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesCount()).isOne();
    assertThat(result.getIssues(0).getFlows(0).getLocationsList()).extracting(Common.Location::getComponent, Common.Location::getMsg,
        Common.Location::getMsgFormattingsList)
      .containsExactlyInAnyOrder(
        tuple(file.getKey(), "FLOW MESSAGE", List.of()),
        tuple(anotherFile.getKey(), "ANOTHER FLOW MESSAGE", List.of(Common.MessageFormatting.newBuilder()
          .setStart(0).setEnd(20).setType(Common.MessageFormattingType.CODE).build())),
        tuple(file.getKey(), "FLOW MESSAGE WITHOUT FILE UUID", List.of()));
  }

  @Test
  public void issue_with_comments() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John"));
    UserDto fabrice = db.users().insertUser(u -> u.setLogin("fabrice").setName("Fabrice").setEmail("fabrice@email.com"));
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto()
        .setUuid(Uuids.createFast())
        .setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(john.getUuid())
        .setProjectUuid(project.branchUuid())
        .setIssueChangeCreationDate(parseDateTime("2014-09-09T12:00:00+0000").getTime()));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto()
        .setUuid(Uuids.createFast())
        .setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(fabrice.getUuid())
        .setProjectUuid(project.branchUuid())
        .setIssueChangeCreationDate(parseDateTime("2014-09-10T12:00:00+0000").getTime()));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto()
        .setUuid(Uuids.createFast())
        .setIssueKey(issue.getKey())
        .setKey("COMMENT-NO-USER")
        .setChangeData("Another comment without user")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setProjectUuid(project.branchUuid())
        .setIssueChangeCreationDate(parseDateTime("2022-09-10T12:00:00+0000").getTime()));
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
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto()
        .setUuid(Uuids.createFast())
        .setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCD")
        .setChangeData("*My comment*")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(john.getUuid())
        .setProjectUuid(project.branchUuid())
        .setCreatedAt(parseDateTime("2014-09-09T12:00:00+0000").getTime()));
    dbClient.issueChangeDao().insert(session,
      new IssueChangeDto()
        .setUuid(Uuids.createFast())
        .setIssueKey(issue.getKey())
        .setKey("COMMENT-ABCE")
        .setChangeData("Another comment")
        .setChangeType(IssueChangeDto.TYPE_COMMENT)
        .setUserUuid(fabrice.getUuid())
        .setProjectUuid(project.branchUuid())
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
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = newIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i.setAssigneeUuid(simon.getUuid()).setType(CODE_SMELL));
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

    ProjectData project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java"));
    grantPermissionToAnyone(project.getProjectDto(), ISSUE_ADMIN);
    indexPermissions();
    ComponentDto file =
      db.components().insertComponent(newFileDto(project.getMainBranchComponent(), null, "FILE_ID").setKey("FILE_KEY").setLanguage("js"));

    IssueDto issue = newIssue(newIssueRule(), project.getMainBranchComponent(), file)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAuthorLogin(fabrice.getLogin())
      .setAssigneeUuid(simon.getUuid());
    dbClient.issueDao().insert(session, issue);
    session.commit();
    indexIssues();

    userSession.logIn("john")
      .addProjectPermission(ISSUE_ADMIN, project.getMainBranchComponent()); // granted by Anyone
    ws.newRequest()
      .setParam("additionalFields", "_all").execute()
      .assertJson(this.getClass(), "load_additional_fields_with_issue_admin_permission.json");
  }

  @Test
  public void search_by_rule_key() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));

    db.issues().insertIssue(rule, project, file);
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
  public void search_adhoc_issue_by_rule_key_returns_correct_rule_name() {

    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule(RuleTesting.EXTERNAL_XOO, r -> r.setIsExternal(true)
      .setIsAdHoc(true)
      .setLanguage("xoo")
      .setName(RuleTesting.EXTERNAL_XOO.toString())
      .setAdHocName("adHocRuleName"));
    db.issues().insertIssue(rule, project, file);
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_RULES, rule.getKey().toString())
      .setParam("additionalFields", "_all")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getRules().getRulesList())
      .extracting(Common.Rule::getKey, Common.Rule::getName)
      .containsExactlyInAnyOrder(tuple(rule.getKey().toString(), rule.getAdHocName()));
  }

  @Test
  public void search_by_non_existing_rule_key() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));

    db.issues().insertIssue(rule, project, file);
    session.commit();
    indexIssues();

    userSession.logIn("john")
      .addProjectPermission(ISSUE_ADMIN, project); // granted by Anyone
    indexPermissions();

    TestResponse execute = ws.newRequest()
      .setParam(PARAM_RULES, "nonexisting:rulekey")
      .setParam("additionalFields", "_all")
      .execute();
    execute.assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  public void search_by_variants_with_facets() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    db.issues().insertIssue(rule, project, file, i -> i.setCodeVariants(List.of("variant1")));
    db.issues().insertIssue(rule, project, file, i -> i.setCodeVariants(List.of("variant2")));
    db.issues().insertIssue(rule, project, file, i -> i.setCodeVariants(List.of("variant1", "variant2")));
    db.issues().insertIssue(rule, project, file, i -> i.setCodeVariants(List.of("variant2", "variant3")));
    indexPermissionsAndIssues();

    ws.newRequest()
      .setParam(PARAM_CODE_VARIANTS, "variant2,variant3")
      .setParam(FACETS, PARAM_CODE_VARIANTS)
      .execute()
      .assertJson(this.getClass(), "search_by_variants_with_facets.json");
  }

  @Test
  public void search_whenFilteringByIssueStatuses_shouldReturnIssueStatusesFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN));
    IssueDto expectedIssue = db.issues().insertIssue(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FALSE_POSITIVE));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FIXED));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_CLOSED).setResolution(RESOLUTION_WONT_FIX));
    // security hotspot should be ignored
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_REVIEWED).setResolution(RESOLUTION_SAFE));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_ISSUE_STATUSES, IssueStatus.ACCEPTED.name())
      .setParam(FACETS, PARAM_ISSUE_STATUSES)
      .executeProtobuf(SearchWsResponse.class);

    List<Issue> issuesList = response.getIssuesList();
    assertThat(issuesList)
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(expectedIssue.getKey());

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_ISSUE_STATUSES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple(IssueStatus.OPEN.name(), 1L),
        tuple(IssueStatus.ACCEPTED.name(), 1L),
        tuple(IssueStatus.FIXED.name(), 2L),
        tuple(IssueStatus.FALSE_POSITIVE.name(), 1L));
  }

  @Test
  public void search_whenIssueStatusesFacetRequested_shouldReturnFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_OPEN));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_REOPENED));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_CONFIRMED));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FALSE_POSITIVE));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FIXED));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_CLOSED).setResolution(RESOLUTION_REMOVED));
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED));
    // security hotspot should be ignored
    db.issues().insertIssue(rule, project, file, i -> i.setStatus(STATUS_REVIEWED).setResolution(RESOLUTION_SAFE));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, PARAM_ISSUE_STATUSES)
      .executeProtobuf(SearchWsResponse.class);

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_ISSUE_STATUSES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple(IssueStatus.OPEN.name(), 2L),
        tuple(IssueStatus.ACCEPTED.name(), 1L),
        tuple(IssueStatus.CONFIRMED.name(), 1L),
        tuple(IssueStatus.FIXED.name(), 3L),
        tuple(IssueStatus.FALSE_POSITIVE.name(), 1L));

  }

  @Test
  public void search_whenImpactSoftwareQualitiesFacetRequested_shouldReturnFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.HIGH))
      .addImpact(new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH)));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH)));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.MEDIUM))
      .addImpact(new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW)));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, PARAM_IMPACT_SOFTWARE_QUALITIES)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey(), issue3.getKey());

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_IMPACT_SOFTWARE_QUALITIES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("MAINTAINABILITY", 3L),
        tuple("RELIABILITY", 3L),
        tuple("SECURITY", 2L));
  }

  @Test
  public void search_whenFilteredByImpactSeverities_shouldReturnImpactSoftwareQualitiesFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));

    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.SECURITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH))
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH)));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH)));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.SECURITY).setSeverity(org.sonar.api.issue.impact.Severity.MEDIUM))
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(org.sonar.api.issue.impact.Severity.INFO)));
    indexPermissionsAndIssues();
    Map<Common.SoftwareQuality, Common.ImpactSeverity> expectedImpacts = Map.of(Common.SoftwareQuality.SECURITY,
      Common.ImpactSeverity.MEDIUM,
      Common.SoftwareQuality.RELIABILITY, Common.ImpactSeverity.ImpactSeverity_INFO,
      Common.SoftwareQuality.MAINTAINABILITY, Common.ImpactSeverity.HIGH);

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_IMPACT_SEVERITIES, org.sonar.api.issue.impact.Severity.INFO.name())
      .setParam(FACETS, PARAM_IMPACT_SOFTWARE_QUALITIES)
      .executeProtobuf(SearchWsResponse.class);

    List<Issue> issuesList = response.getIssuesList();
    assertThat(issuesList)
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue3.getKey());

    Issue issue = issuesList.get(0);
    Map<Common.SoftwareQuality, Common.ImpactSeverity> impactsInResponse = issue.getImpactsList()
      .stream()
      .collect(Collectors.toMap(Common.Impact::getSoftwareQuality, Common.Impact::getSeverity));
    assertThat(impactsInResponse).isEqualTo(expectedImpacts);
    assertThat(issue.getCleanCodeAttribute()).isEqualTo(Common.CleanCodeAttribute.CLEAR);

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_IMPACT_SOFTWARE_QUALITIES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("MAINTAINABILITY", 0L),
        tuple("RELIABILITY", 1L),
        tuple("SECURITY", 0L));
  }

  @Test
  public void search_whenImpactSeveritiesFacetRequested_shouldReturnFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.HIGH),
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH))));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH))));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW),
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.MEDIUM),
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW))));
    IssueDto issue4 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.INFO),
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, PARAM_IMPACT_SEVERITIES)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey(), issue3.getKey(), issue4.getKey());

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_IMPACT_SEVERITIES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("BLOCKER", 1L),
        tuple("HIGH", 2L),
        tuple("MEDIUM", 1L),
        tuple("LOW", 1L),
        tuple("INFO", 1L));
  }

  @Test
  public void search_whenFilteredByImpactSoftwareQualities_shouldReturnFacet() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.HIGH),
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH))));
    db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH))));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER))));
    IssueDto issue4 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO))));
    IssueDto issue5 = db.issues().insertIssue(rule, project, file, i -> i.replaceAllImpacts(List.of(
      new ImpactDto(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW),
      new ImpactDto(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.MEDIUM),
      new ImpactDto(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW))));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_IMPACT_SOFTWARE_QUALITIES, SoftwareQuality.SECURITY.name())
      .setParam(FACETS, PARAM_IMPACT_SEVERITIES)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey(), issue3.getKey(), issue4.getKey(), issue5.getKey());

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_IMPACT_SEVERITIES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("HIGH", 1L),
        tuple("MEDIUM", 1L),
        tuple("LOW", 0L),
        tuple("INFO", 1L),
        tuple("BLOCKER", 2L));
  }

  @Test
  public void search_whenFilteredByCleanCodeAttributeCategory_shouldReturnFacet() {
    // INTENTIONAL
    RuleDto rule1 = newIssueRule("clear-rule", ruleDto -> ruleDto.setCleanCodeAttribute(CleanCodeAttribute.CLEAR));
    RuleDto rule2 = newIssueRule("complete-rule", ruleDto -> ruleDto.setCleanCodeAttribute(CleanCodeAttribute.COMPLETE));
    // ADAPTABLE
    RuleDto rule3 = newIssueRule("distinct-rule", ruleDto -> ruleDto.setCleanCodeAttribute(CleanCodeAttribute.DISTINCT));
    // RESPONSIBLE
    RuleDto rule4 = newIssueRule("lawful-rule", ruleDto -> ruleDto.setCleanCodeAttribute(CleanCodeAttribute.LAWFUL));
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID").setLanguage("java")).getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY").setLanguage("java"));
    IssueDto issue1 = db.issues().insertIssue(rule1, project, file);
    IssueDto issue2 = db.issues().insertIssue(rule2, project, file);
    IssueDto issue3 = db.issues().insertIssue(rule3, project, file);
    IssueDto issue4 = db.issues().insertIssue(rule4, project, file);
    IssueDto issue5 = db.issues().insertIssue(rule1, project, file);
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES, CleanCodeAttributeCategory.INTENTIONAL.name())
      .setParam(FACETS, PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey(), issue5.getKey());

    Optional<Common.Facet> first = response.getFacets().getFacetsList()
      .stream().filter(facet -> facet.getProperty().equals(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES))
      .findFirst();
    assertThat(first.get().getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(
        tuple("INTENTIONAL", 3L),
        tuple("ADAPTABLE", 1L),
        tuple("RESPONSIBLE", 1L),
        tuple("CONSISTENT", 0L));
  }

  @Test
  public void issue_on_removed_file() {
    RuleDto rule = newIssueRule();
    ComponentDto project =
      db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY").setKey("PROJECT_KEY")).getMainBranchComponent();
    indexPermissions();
    ComponentDto removedFile = db.components().insertComponent(newFileDto(project).setUuid("REMOVED_FILE_ID")
      .setKey("REMOVED_FILE_KEY")
      .setEnabled(false));

    IssueDto issue = newIssue(rule, project, removedFile)
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
    RuleDto rule = newIssueRule();
    ComponentDto project =
      db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY").setKey("PROJECT_KEY")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    for (int i = 0; i < SearchOptions.MAX_PAGE_SIZE + 1; i++) {
      IssueDto issue = newIssue(rule, project, file).setAssigneeUuid(null).setChecksum(null);
      dbClient.issueDao().insert(session, issue);
    }
    session.commit();
    indexIssues();

    ws.newRequest().setParam(PARAM_COMPONENTS, file.getKey()).execute()
      .assertJson(this.getClass(), "apply_paging_with_one_component.json");
  }

  @Test
  public void filter_by_assigned_to_me() {
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    ComponentDto project = db.components().insertPublicProject(c -> c.setUuid("PROJECT_ID").setKey("PROJECT_KEY").setBranchUuid(
      "PROJECT_ID")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newIssue(rule, project, file)
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
  public void filter_by_new_code_period() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID")).getMainBranchComponent();
    db.components().insertSnapshot(project, s -> s.setLast(true).setPeriodDate(parseDateTime("2014-09-05T00:00:00+0100").getTime()));
    indexPermissions();

    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setMessage(null)
      .setTags(null)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setChecksum(null)
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDateTime("2014-09-06T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setMessage(null)
      .setTags(null)
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setChecksum(null)
      .setAssigneeUuid(alice.getUuid());
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();

    userSession.logIn(john);

    ws.newRequest()
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .setParam(PARAM_COMPONENTS, "PROJECT_KEY")
      .execute()
      .assertJson(this.getClass(), "filter_by_leak_period.json");

  }

  @Test
  public void explicit_false_value_for_new_code_period_parameters_has_no_effect() {
    ws.newRequest()
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "false")
      .execute()
      .assertJson(this.getClass(), "default_page_size_is_100.json");
  }

  @Test
  public void filter_by_leak_period_without_a_period() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY")).getMainBranchComponent();
    SnapshotDto snapshotDto = db.components().insertSnapshot(project);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setChecksum(null)
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setChecksum(null)
      .setAssigneeUuid(alice.getUuid());
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();

    userSession.logIn(john);

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, "PROJECT_KEY")
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "empty_result.json");
  }

  @Test
  public void filter_by_leak_period_has_no_effect_on_prs() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY")).getMainBranchComponent();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("pr"));
    SnapshotDto snapshotDto = db.components().insertSnapshot(pr);
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(pr, null, "FILE_ID", project.uuid()).setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, pr, file)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setTags(null)
      .setMessage(null)
      .setStatus("OPEN")
      .setAuthorLogin("john")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, pr, file)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2017-12-04T00:00:00+0100"))
      .setEffort(10L)
      .setTags(null)
      .setMessage(null)
      .setStatus("OPEN")
      .setAuthorLogin("john")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(alice.getUuid());
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();

    userSession.logIn(john);

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, "PROJECT_KEY")
      .setParam(PARAM_PULL_REQUEST, "pr")
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "filter_by_leak_period_has_no_effect_on_prs.json");
  }

  @Test
  public void return_empty_when_login_is_unknown() {
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, project, file)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newIssue(rule, project, file)
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
    UserDto alice = db.users().insertUser(u -> u.setLogin("alice").setName("Alice").setEmail("alice@email.com"));
    UserDto john = db.users().insertUser(u -> u.setLogin("john").setName("John").setEmail("john@email.com"));
    UserDto poy = db.users().insertUser(u -> u.setLogin("poy").setName("poypoy").setEmail("poypoy@email.com"));
    userSession.logIn(poy);
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, file)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setAssigneeUuid(john.getUuid());
    IssueDto issue2 = newIssue(rule, project, file)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setAssigneeUuid(alice.getUuid());
    IssueDto issue3 = newIssue(rule, project, file)
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
    userSession.logIn();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("leia"));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("luke"));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("han, solo"));
    indexPermissionsAndIssues();

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
  public void hide_author_if_not_logged_in() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("leia"));
    db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("luke"));
    db.issues().insertIssue(rule, project, file, i -> i.setAuthorLogin("han, solo"));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setMultiParam("author", asList("leia", "han, solo"))
      .setParam(FACETS, "author")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getAuthor)
      .containsExactlyInAnyOrder("", "");
   assertThat(response.getFacets().getFacetsList()).isEmpty();
  }

  @Test
  public void filter_by_test_scope() {
    ProjectData projectData = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID"));
    ComponentDto project = projectData.getMainBranchComponent();
    indexPermissions();
    ComponentDto mainCodeFile = db.components().insertComponent(
      newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    ComponentDto testCodeFile = db.components().insertComponent(
      newFileDto(project, null, "ANOTHER_FILE_ID").setKey("ANOTHER_FILE_KEY").setQualifier(UNIT_TEST_FILE));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    IssueDto issue2 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR");
    IssueDto issue3 = newIssue(rule, project, testCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam("scopes", "TEST")
      .setParam(FACETS, "scopes")
      .execute()
      .assertJson(this.getClass(), "filter_by_test_scope.json");
  }

  @Test
  public void filter_by_main_scope() {
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID")).getMainBranchComponent();
    indexPermissions();
    ComponentDto mainCodeFile = db.components().insertComponent(
      newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    ComponentDto testCodeFile = db.components().insertComponent(
      newFileDto(project, null, "ANOTHER_FILE_ID").setKey("ANOTHER_FILE_KEY").setQualifier(UNIT_TEST_FILE));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("83ec1d05-9397-4137-9978-85368bcc3b90")
      .setSeverity("MAJOR");
    IssueDto issue2 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR");
    IssueDto issue3 = newIssue(rule, project, testCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("82fd47d4-4037-b650-80bc-7b112bd4eac2")
      .setSeverity("MAJOR");
    dbClient.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam("scopes", "MAIN")
      .setParam(FACETS, "scopes")
      .execute()
      .assertJson(this.getClass(), "filter_by_main_scope.json");
  }

  @Test
  public void filter_by_scope_always_returns_all_scope_facet_values() {
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID")).getMainBranchComponent();
    indexPermissions();
    ComponentDto mainCodeFile = db.components().insertComponent(
      newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    RuleDto rule = newIssueRule();
    IssueDto issue1 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("83ec1d05-9397-4137-9978-85368bcc3b90")
      .setSeverity("MAJOR");
    IssueDto issue2 = newIssue(rule, project, mainCodeFile)
      .setIssueCreationDate(parseDate("2014-09-04"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setEffort(10L)
      .setStatus("OPEN")
      .setKee("7b112bd4-b650-4037-80bc-82fd47d4eac2")
      .setSeverity("MAJOR");
    dbClient.issueDao().insert(session, issue1, issue2);
    session.commit();
    indexIssues();

    ws.newRequest()
      .setParam("scopes", "MAIN")
      .setParam(FACETS, "scopes")
      .execute()
      .assertJson(this.getClass(), "filter_by_main_scope_2.json");
  }

  @Test
  public void sort_by_updated_at() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID",
      c -> c.setKey("PROJECT_KEY").setName("NAME_PROJECT_ID").setLongName("LONG_NAME_PROJECT_ID")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    dbClient.issueDao().insert(session, newIssue(rule, project, file)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac1")
      .setIssueUpdateDate(parseDateTime("2014-11-02T00:00:00+0100")));
    dbClient.issueDao().insert(session, newIssue(rule, project, file)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2")
      .setIssueUpdateDate(parseDateTime("2014-11-01T00:00:00+0100")));
    dbClient.issueDao().insert(session, newIssue(rule, project, file)
      .setKee("82fd47d4-b650-4037-80bc-7b112bd4eac3")
      .setIssueUpdateDate(parseDateTime("2014-11-03T00:00:00+0100")));
    session.commit();
    indexIssues();

    TestResponse response = ws.newRequest()
      .setParam("s", IssueQuery.SORT_BY_UPDATE_DATE)
      .setParam("asc", "false")
      .execute();

    JsonElement parse = JsonParser.parseString(response.getInput());

    assertThat(parse.getAsJsonObject().get("issues").getAsJsonArray())
      .extracting(o -> o.getAsJsonObject().get("key").getAsString())
      .containsExactly("82fd47d4-b650-4037-80bc-7b112bd4eac3", "82fd47d4-b650-4037-80bc-7b112bd4eac1", "82fd47d4-b650-4037-80bc" +
        "-7b112bd4eac2");
  }

  @Test
  public void only_vulnerabilities_are_returned_by_owaspAsvs40() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "owaspTop10:a1", "pciDss-3.2:6.5.3", "owaspAsvs-4.0:12.3.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "12.3.1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());

    result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "12")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_owaspAsvs40_with_level() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto issueRule1 = db.rules().insertIssueRule(r -> r.setSecurityStandards(Set.of("owaspAsvs-4.0:1.7.2", "owaspAsvs-4.0:12.3.1")));
    RuleDto issueRule2 = db.rules().insertIssueRule(r -> r.setSecurityStandards(Set.of("owaspAsvs-4.0:2.2.5")));
    RuleDto issueRule3 = db.rules().insertIssueRule(r -> r.setSecurityStandards(Set.of("owaspAsvs-4.0:2.2.5", "owaspAsvs-4.0:12.1.3")));
    IssueDto issueDto1 = db.issues().insertIssue(issueRule1, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule3, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "1")
      .setParam("owaspAsvsLevel", "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();

    result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "1")
      .setParam("owaspAsvsLevel", "2")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey());

    result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "12")
      .setParam("owaspAsvsLevel", "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey());

    result = ws.newRequest()
      .setParam("owaspAsvs-4.0", "12")
      .setParam("owaspAsvsLevel", "2")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto3.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_pciDss32() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "owaspTop10:a1", "pciDss-3.2:6.5.3", "pciDss-3.2:10.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("pciDss-3.2", "10")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());

    result = ws.newRequest()
      .setParam("pciDss-3.2", "10.1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void multiple_categories_pciDss32() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    // Rule 1
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "owaspTop10:a1", "pciDss-3.2:6.5.3", "pciDss-3.2:10.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    // Rule 2
    ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("pciDss-4.0:6.5.3", "pciDss-3.2:1.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto4 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    // Rule 3
    ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("pciDss-4.0:6.5.3", "pciDss-3.2:2.3", "pciDss-3.2:10.1.2"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto5 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto6 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("pciDss-3.2", "1,10")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey(), issueDto3.getKey(), issueDto4.getKey(), issueDto5.getKey(),
        issueDto6.getKey());

    result = ws.newRequest()
      .setParam("pciDss-3.2", "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto3.getKey(), issueDto4.getKey());

    result = ws.newRequest()
      .setParam("pciDss-3.2", "1,10,4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey(), issueDto3.getKey(), issueDto4.getKey(), issueDto5.getKey(),
        issueDto6.getKey());

    result = ws.newRequest()
      .setParam("pciDss-3.2", "4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();

    result = ws.newRequest()
      .setParam("pciDss-3.2", "4,7,12")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();

    result = ws.newRequest()
      .setParam("pciDss-3.2", "10.1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_pciDss40() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "owaspTop10:a1", "pciDss-4.0:6.5.3", "pciDss-4.0:10.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("pciDss-4.0", "10,6,5")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());

    result = ws.newRequest()
      .setParam("pciDss-4.0", "10.1,6.5,5.5")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void multiple_categories_pciDss40() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    // Rule 1
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "owaspTop10:a1", "pciDss-4.0:6.5.3", "pciDss-4.0:10.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    // Rule 2
    ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("pciDss-4.0:6.5.3", "pciDss-4.0:1.1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto4 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    // Rule 3
    ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("pciDss-3.2:6.5.3", "pciDss-4.0:2.3"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    issueRule = db.rules().insertIssueRule(ruleConsumer);
    db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(RuleType.VULNERABILITY));

    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("pciDss-4.0", "1,10")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey(), issueDto3.getKey(), issueDto4.getKey());

    result = ws.newRequest()
      .setParam("pciDss-4.0", "1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto3.getKey(), issueDto4.getKey());

    result = ws.newRequest()
      .setParam("pciDss-4.0", "1,10,4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey(), issueDto3.getKey(), issueDto4.getKey());

    result = ws.newRequest()
      .setParam("pciDss-4.0", "4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();

    result = ws.newRequest()
      .setParam("pciDss-4.0", "4,7,12")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  public void only_vulnerabilities_are_returned_by_cwe() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:89", "cwe:943", "owaspTop10:a1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("cwe", "20")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_owasp() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:89", "cwe:943", "owaspTop10:a1", "owaspTop10-2021:a2"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("owaspTop10", "a1")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_owasp_2021() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:89", "cwe:943", "owaspTop10:a1", "owaspTop10-2021:a2"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("owaspTop10-2021", "a2")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_stig_R5V3() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "stig-ASD_V5R3:V-222402", "stig-ASD_V5R3:V-222403", "stig-ASD_V5R3:V" +
        "-222404", "ostig-ASD_V5R3:V-222405"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "stig", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "stig", "sans-top25-insecure",
      "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("stig-ASD_V5R3", "V-222402")
      .setParam(FACETS, "stig-ASD_V5R3")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());

    assertThat(result.getFacets().getFacets(0).getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(tuple("V-222402", 2L), tuple("V-222403", 2L), tuple("V-222404", 2L));
  }

  @Test
  public void only_vulnerabilities_are_returned_by_casa() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:639", "cwe:326"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "sans-top25-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("casa", "4.1.2")
      .setParam(FACETS, "casa")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());

    assertThat(result.getFacets().getFacets(0).getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(tuple("4.1.2", 2L), tuple("4.2.1", 2L), tuple("6.2.3", 2L),
        tuple("6.2.4", 2L), tuple("6.2.7", 2L), tuple("9.1.2", 2L));

    result = ws.newRequest()
      .setParam("casa", "4")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .as("We should be able to search with only the prefix '4'")
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_sansTop25() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:266", "cwe:732", "owaspTop10:a5"))
      .setSystemTags(Sets.newHashSet("cert", "cwe", "owasp-a5", "sans-top25-porous"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("cert", "cwe", "owasp-a5", "sans-top25-porous"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("sansTop25", "porous-defenses")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void only_vulnerabilities_are_returned_by_sonarsource_security() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:89", "cwe:943", "owaspTop10:a1"))
      .setSystemTags(Sets.newHashSet("cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueConsumer, issueDto -> issueDto.setType(CODE_SMELL));
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("sonarsourceSecurity", "sql-injection")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void security_hotspots_are_not_returned_by_default() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i.setType(RuleType.BUG));
    db.issues().insertIssue(rule, project, file, i -> i.setType(RuleType.VULNERABILITY));
    db.issues().insertIssue(rule, project, file, i -> i.setType(CODE_SMELL));
    db.issues().insertHotspot(project, file);
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getType)
      .containsExactlyInAnyOrder(BUG, VULNERABILITY, Common.RuleType.CODE_SMELL);
  }

  @Test
  public void security_hotspots_are_not_returned_by_issues_param() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto issueRule = db.rules().insertIssueRule();
    IssueDto bugIssue = db.issues().insertIssue(issueRule, project, file, i -> i.setType(RuleType.BUG));
    IssueDto vulnerabilityIssue = db.issues().insertIssue(issueRule, project, file, i -> i.setType(RuleType.VULNERABILITY));
    IssueDto codeSmellIssue = db.issues().insertIssue(issueRule, project, file, i -> i.setType(CODE_SMELL));
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    IssueDto hotspot = db.issues().insertHotspot(hotspotRule, project, file);
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("issues",
        Stream.of(bugIssue, vulnerabilityIssue, codeSmellIssue, hotspot).map(IssueDto::getKey).collect(Collectors.joining(",")))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getType)
      .containsExactlyInAnyOrder(BUG, VULNERABILITY, Common.RuleType.CODE_SMELL);
  }

  @Test
  public void security_hotspots_are_not_returned_by_cwe() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    Consumer<RuleDto> ruleConsumer = ruleDefinitionDto -> ruleDefinitionDto
      .setSecurityStandards(Sets.newHashSet("cwe:20", "cwe:564", "cwe:89", "cwe:943", "owaspTop10:a1"))
      .setSystemTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25-insecure", "sql"));
    Consumer<IssueDto> issueConsumer = issueDto -> issueDto.setTags(Sets.newHashSet("bad-practice", "cwe", "owasp-a1", "sans-top25" +
      "-insecure", "sql"));
    RuleDto hotspotRule = db.rules().insertHotspotRule(ruleConsumer);
    db.issues().insertHotspot(hotspotRule, project, file, issueConsumer);
    RuleDto issueRule = db.rules().insertIssueRule(ruleConsumer);
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueConsumer,
      issueDto -> issueDto.setType(RuleType.VULNERABILITY));
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("cwe", "20")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey());
  }

  @Test
  public void security_hotspots_are_not_returned_by_assignees() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    db.issues().insertHotspot(hotspotRule, project, file, issueDto -> issueDto.setAssigneeUuid(user.getUuid()));
    RuleDto issueRule = db.rules().insertIssueRule();
    IssueDto issueDto1 = db.issues().insertIssue(issueRule, project, file, issueDto -> issueDto.setAssigneeUuid(user.getUuid()));
    IssueDto issueDto2 = db.issues().insertIssue(issueRule, project, file, issueDto -> issueDto.setAssigneeUuid(user.getUuid()));
    IssueDto issueDto3 = db.issues().insertIssue(issueRule, project, file, issueDto -> issueDto.setAssigneeUuid(user.getUuid()));
    IssueDto issueDto4 = db.issues().insertIssue(issueRule, project, file, issueDto -> issueDto.setAssigneeUuid(user.getUuid()));

    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_ASSIGNEES, user.getLogin())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issueDto1.getKey(), issueDto2.getKey(), issueDto3.getKey(), issueDto4.getKey());
  }

  @Test
  public void security_hotspots_are_not_returned_by_rule() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    db.issues().insertHotspot(hotspotRule, project, file);
    indexPermissionsAndIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("rules", hotspotRule.getKey().toString())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  public void security_hotspots_are_not_returned_by_issues_param_only() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insertHotspotRule();
    List<IssueDto> hotspots = IntStream.range(1, 2 + new Random().nextInt(10))
      .mapToObj(value -> db.issues().insertHotspot(rule, project, file))
      .toList();
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("issues", hotspots.stream().map(IssueDto::getKey).collect(Collectors.joining(",")))
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .isEmpty();
  }

  @Test
  public void fail_if_trying_to_filter_issues_by_hotspots() {
    ComponentDto mainBranch = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    RuleDto hotspotRule = newHotspotRule();
    db.issues().insertHotspot(hotspotRule, mainBranch, file);
    insertIssues(i -> i.setType(RuleType.BUG), i -> i.setType(RuleType.VULNERABILITY),
      i -> i.setType(RuleType.CODE_SMELL));
    indexPermissionsAndIssues();

    TestRequest request = ws.newRequest()
      .setParam("types", RuleType.SECURITY_HOTSPOT.toString());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'types' (SECURITY_HOTSPOT) must be one of: [CODE_SMELL, BUG, VULNERABILITY]");
  }

  @Test
  public void security_hotspot_are_ignored_when_filtering_by_severities() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto issueRule = db.rules().insertIssueRule();
    IssueDto bugIssue = db.issues().insertIssue(issueRule, project, file, i -> i.setType(RuleType.BUG).setSeverity(Severity.MAJOR.name()));
    IssueDto vulnerabilityIssue = db.issues().insertIssue(issueRule, project, file,
      i -> i.setType(RuleType.VULNERABILITY).setSeverity(Severity.MAJOR.name()));
    IssueDto codeSmellIssue = db.issues().insertIssue(issueRule, project, file,
      i -> i.setType(CODE_SMELL).setSeverity(Severity.MAJOR.name()));
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    db.issues().insertHotspot(hotspotRule, project, file, i -> i.setSeverity(Severity.MAJOR.name()));
    indexPermissions();
    indexIssues();

    SearchWsResponse result = ws.newRequest()
      .setParam("severities", Severity.MAJOR.name())
      .setParam(FACETS, "severities")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getType)
      .containsExactlyInAnyOrder(
        tuple(bugIssue.getKey(), BUG),
        tuple(vulnerabilityIssue.getKey(), VULNERABILITY),
        tuple(codeSmellIssue.getKey(), Common.RuleType.CODE_SMELL));
    assertThat(result.getFacets().getFacets(0).getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactlyInAnyOrder(tuple("MAJOR", 3L), tuple("INFO", 0L), tuple("MINOR", 0L), tuple("CRITICAL", 0L), tuple("BLOCKER", 0L));
  }

  @Test
  public void return_total_effort() {
    insertIssues(i -> i.setEffort(10L), i -> i.setEffort(15L));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getEffortTotal()).isEqualTo(25L);
  }

  @Test
  public void givenNotQuickFixableIssue_returnIssueIsNotQuickFixable() {
    insertIssues(i -> i.setQuickFixAvailable(false));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).hasSize(1);
    assertThat(response.getIssuesList().get(0).getQuickFixAvailable()).isFalse();
  }

  @Test
  public void givenQuickFixableIssue_returnIssueIsQuickFixable() {
    insertIssues(i -> i.setQuickFixAvailable(true));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).hasSize(1);
    assertThat(response.getIssuesList().get(0).getQuickFixAvailable()).isTrue();
  }

  @Test
  public void paging() {
    RuleDto rule = newIssueRule();
    ComponentDto project = db.components().insertPublicProject("PROJECT_ID", c -> c.setKey("PROJECT_KEY")).getMainBranchComponent();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "FILE_ID").setKey("FILE_KEY"));
    for (int i = 0; i < 12; i++) {
      IssueDto issue = newIssue(rule, project, file).setChecksum(null);
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
    TestRequest requestWithNegativePageSize = ws.newRequest()
      .setParam(WebService.Param.PAGE, "1")
      .setParam(WebService.Param.PAGE_SIZE, "-1");

    assertThatThrownBy(requestWithNegativePageSize::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page size must be between 1 and 500 (got -1)");
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
      .setParam("components", "foo")
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
    TestRequest invalidFormatRequest = ws.newRequest()
      .setParam(PARAM_CREATED_AFTER, "wrong-date-input");

    assertThatThrownBy(invalidFormatRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Date 'wrong-date-input' cannot be parsed as either a date or date+time");
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
      "additionalFields", "asc", "assigned", "assignees", "author", "components", "branch", "pullRequest", "createdAfter", "createdAt",
      "createdBefore", "createdInLast", "directories", "facets", "files", "issues", "scopes", "languages", "onComponentOnly",
      "p", "projects", "ps", "resolutions", "resolved", "rules", "s", "severities", "statuses", "tags", "types", "pciDss-3.2", "pciDss-4" +
        ".0", "owaspAsvs-4.0",
      "owaspAsvsLevel", "owaspTop10", "owaspTop10-2021", "stig-ASD_V5R3", "casa", "sansTop25", "cwe", "sonarsourceSecurity", "timeZone",
      "inNewCodePeriod", "codeVariants",
      "cleanCodeAttributeCategories", "impactSeverities", "impactSoftwareQualities", "issueStatuses", "fixedInPullRequest",
      "prioritizedRule");

    WebService.Param branch = def.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");

    WebService.Param projectUuids = def.param("projects");
    assertThat(projectUuids.description()).isEqualTo("To retrieve issues associated to a specific list of projects (comma-separated list " +
      "of project keys). " +
      "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. If this parameter is set, " +
      "projectUuids must not be set.");
  }

  @Test
  public void search_when_additional_field_set_return_context_key() {
    insertIssues(issue -> issue.setRuleDescriptionContextKey("spring"));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("additionalFields", "ruleDescriptionContextKey")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).isNotEmpty()
      .extracting(Issue::getRuleDescriptionContextKey).containsExactly("spring");
  }

  @Test
  public void search_when_no_additional_field_return_empty_context_key() {
    insertIssues(issue -> issue.setRuleDescriptionContextKey("spring"));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).isNotEmpty()
      .extracting(Issue::getRuleDescriptionContextKey).containsExactly(EMPTY);
  }

  @Test
  public void search_when_additional_field_but_no_context_key_return_empty_context_key() {
    insertIssues(issue -> issue.setRuleDescriptionContextKey(null));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("additionalFields", "ruleDescriptionContextKey")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).isNotEmpty()
      .extracting(Issue::getRuleDescriptionContextKey).containsExactly(EMPTY);
  }

  @Test
  public void search_when_additional_field_set_to_all_return_context_key() {
    insertIssues(issue -> issue.setRuleDescriptionContextKey("spring"));
    indexPermissionsAndIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("additionalFields", "_all")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList()).isNotEmpty()
      .extracting(Issue::getRuleDescriptionContextKey).containsExactly("spring");
  }

  @Test
  public void search_whenFixedInPullRequestSetAndNoComponentsSet_throwException() {
    TestRequest request = ws.newRequest()
      .setParam("fixedInPullRequest", "1000");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Exactly one project needs to be provided in the 'components' param when used together with 'fixedInPullRequest' param");
  }

  @Test
  public void search_whenFixedInPullRequestSetAndWrongBranchIsSet_throwException() {
    String pullRequestId = "1000";
    String pullRequestUuid = "pullRequestUuid";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey(pullRequestId)
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid("wrongBranchUuid")
      .setProjectUuid(project.projectUuid())
      .setKey("wrongBranch")
      .setIsMain(false)
      .setBranchType(BranchType.BRANCH)
      .setMergeBranchUuid("wrongTargetBranchUuid"));

    session.commit();
    TestRequest request = ws.newRequest().setParam("fixedInPullRequest", pullRequestId).setParam("components", project.projectKey())
      .setParam("branch", "wrongBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Pull request with key '1000' does not target branch 'wrongBranch'");
  }

  @Test
  public void search_whenFixedInPullRequestSetAndProjectDoesNotExist_throwException() {
    String pullRequestId = "1000";
    String pullRequestUuid = "pullRequestUuid";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey(pullRequestId)
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));

    session.commit();
    TestRequest request = ws.newRequest().setParam("fixedInPullRequest", pullRequestId).setParam("components", "nonExistingProjectKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Project with key 'nonExistingProjectKey' does not exist");
  }

  @Test
  public void search_whenWrongFixedInPullRequestSet_throwException() {
    String pullRequestId = "wrongPullRequest";
    String pullRequestUuid = "pullRequestUuid";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey("pullRequestId")
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));

    session.commit();
    TestRequest request = ws.newRequest().setParam("fixedInPullRequest", pullRequestId).setParam("components", project.projectKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Pull request with key 'wrongPullRequest' does not exist for project " + project.projectKey());
  }

  @Test
  public void search_whenFixedInPullRequestSetAndNonExistingBranchIsSet_throwException() {
    String pullRequestId = "1000";
    String pullRequestUuid = "pullRequestUuid";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey(pullRequestId)
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));

    session.commit();
    TestRequest request = ws.newRequest().setParam("fixedInPullRequest", pullRequestId).setParam("components", project.projectKey())
      .setParam("branch", "nonExistingBranch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Branch with key 'nonExistingBranch' does not exist");
  }

  @Test
  public void search_whenFixedInPullRequestSetAndComponentsIsSetButNoIssueFixedInPR_returnZeroIssues() {
    String pullRequestId = "1000";
    String pullRequestUuid = "pullRequestUuid";
    String issueKey = "issueKey";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey(pullRequestId)
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));

    TestRequest request = ws.newRequest().setParam("components", project.projectKey()).setParam("fixedInPullRequest", pullRequestId);
    insertIssues(project.getMainBranchComponent(), i -> i.setKee(issueKey));
    session.commit();
    indexPermissionsAndIssues();

    SearchWsResponse response = request.executeProtobuf(SearchWsResponse.class);

    List<Issue> issuesList = response.getIssuesList();
    assertThat(issuesList).isEmpty();
  }

  @Test
  public void search_whenFixedInPullRequestSetAndComponentsIsSet_returnOneIssueFixedInPR() {
    String pullRequestId = "1000";
    String pullRequestUuid = "pullRequestUuid";
    String issueKey = "issueKey";
    userSession.logIn(db.users().insertUser());
    ProjectData project = db.components().insertPublicProject();
    db.getDbClient().branchDao().insert(session, new BranchDto()
      .setUuid(pullRequestUuid)
      .setProjectUuid(project.projectUuid())
      .setKey(pullRequestId)
      .setIsMain(false)
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.mainBranchUuid()));

    TestRequest request = ws.newRequest().setParam("components", project.projectKey()).setParam("fixedInPullRequest", pullRequestId);
    insertIssues(project.getMainBranchComponent(), i -> i.setKee(issueKey));
    db.getDbClient().issueFixedDao().insert(session, new IssueFixedDto(pullRequestUuid, issueKey));
    session.commit();
    indexPermissionsAndIssues();

    SearchWsResponse response = request.executeProtobuf(SearchWsResponse.class);

    List<Issue> issuesList = response.getIssuesList();
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getKey()).isEqualTo(issueKey);
  }

  private RuleDto newIssueRule() {
    RuleDto rule = newRule(XOO_X1, createDefaultRuleDescriptionSection(uuidFactory.create(), "Rule desc"))
      .setLanguage("xoo")
      .setName("Rule name")
      .setStatus(RuleStatus.READY);
    db.rules().insert(rule);
    return rule;
  }

  private RuleDto newIssueRule(String ruleKey, Consumer<RuleDto> consumer) {
    RuleDto rule = newRule(RuleKey.of("xoo", ruleKey),
      createDefaultRuleDescriptionSection(uuidFactory.create(), "Rule desc"))
      .setLanguage("xoo")
      .setName("Rule name")
      .setStatus(RuleStatus.READY);
    consumer.accept(rule);
    db.rules().insert(rule);
    return rule;
  }

  private RuleDto newHotspotRule() {
    RuleDto rule = newRule(XOO_X2, createDefaultRuleDescriptionSection(uuidFactory.create(), "Rule desc"))
      .setLanguage("xoo")
      .setName("Rule name")
      .setStatus(RuleStatus.READY)
      .setType(SECURITY_HOTSPOT_VALUE);
    db.rules().insert(rule);
    return rule;
  }

  private void indexPermissions() {
    permissionIndexer.indexAll(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  private void grantPermissionToAnyone(ProjectDto project, String permission) {
    dbClient.groupPermissionDao().insert(session,
      new GroupPermissionDto()
        .setUuid(Uuids.createFast())
        .setGroupUuid(null)
        .setEntityUuid(project.getUuid())
        .setEntityName(project.getName())
        .setRole(permission),
      project, null);
    session.commit();
    userSession.logIn().addProjectPermission(permission, project);
  }

  private void insertIssues(Consumer<IssueDto>... populators) {
    UserDto john = db.users().insertUser();
    userSession.logIn(john);
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto branch = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    for (Consumer<IssueDto> populator : populators) {
      db.issues().insertIssue(rule, branch, file, populator);
    }
  }

  private void insertIssues(ComponentDto branch, Consumer<IssueDto>... populators) {
    UserDto john = db.users().insertUser();
    userSession.logIn(john);
    RuleDto rule = db.rules().insertIssueRule();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    for (Consumer<IssueDto> populator : populators) {
      db.issues().insertIssue(rule, branch, file, populator);
    }
  }

  private void indexPermissionsAndIssues() {
    indexPermissions();
    indexIssues();
  }

}
