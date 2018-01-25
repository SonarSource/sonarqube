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
package org.sonar.server.issue.ws;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.test.JsonAssert.assertJson;

public class TagsActionTest {

  private MapSettings settings = new MapSettings();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(settings.asConfig()), new RuleIndexDefinition(settings.asConfig()));

  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbTester.getDbClient(), new IssueIteratorFactory(dbTester.getDbClient()));
  private RuleIndexer ruleIndexer = new RuleIndexer(esTester.client(), dbTester.getDbClient());
  private PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(esTester, issueIndexer);
  private IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));
  private RuleIndex ruleIndex = new RuleIndex(esTester.client(), System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new TagsAction(issueIndex, ruleIndex, dbTester.getDbClient()));
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
  }

  @Test
  public void return_tags_from_issues() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]}");
  }

  @Test
  public void return_tags_from_rules() {
    userSession.logIn();
    RuleDefinitionDto r = dbTester.rules().insert(setSystemTags("tag1"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId());
    dbTester.rules().insertOrUpdateMetadata(r, organization, setTags("tag2"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId(), organization);

    RuleDefinitionDto r2 = dbTester.rules().insert(setSystemTags("tag3"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r2.getId());
    dbTester.rules().insertOrUpdateMetadata(r2, organization, setTags("tag4", "tag5"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r2.getId(), organization);

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]}");
  }

  @Test
  public void return_tags_from_issue_and_rule_tags() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    RuleDefinitionDto r = dbTester.rules().insert(setSystemTags("tag6"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId());
    dbTester.rules().insertOrUpdateMetadata(r, organization, setTags("tag7"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId(), organization);

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\", \"tag6\", \"tag7\"]}");
  }

  @Test
  public void return_limited_size() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    String result = ws.newRequest()
      .setParam("ps", "2")
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\"]}");
  }

  @Test
  public void return_tags_matching_query() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag12", "tag4", "tag5");

    String result = ws.newRequest()
      .setParam("q", "ag1")
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag12\"]}");
  }

  @Test
  public void do_not_return_issues_without_permission() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithoutBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4");

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\"]}");
    assertThat(result).doesNotContain("tag3").doesNotContain("tag4");
  }

  @Test
  public void empty_list() {
    userSession.logIn();
    String result = ws.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[]}");
  }

  @Test
  public void without_organization_parameter_is_cross_organization() {
    userSession.logIn();
    OrganizationDto organization = dbTester.organizations().insert();
    OrganizationDto anotherOrganization = dbTester.organizations().insert();
    insertIssueWithBrowsePermission(organization, insertRuleWithoutTags(), "tag1");
    insertIssueWithBrowsePermission(anotherOrganization, insertRuleWithoutTags(), "tag2");

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\"]}");
  }

  @Test
  public void json_example() {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "convention");

    RuleDefinitionDto r = dbTester.rules().insert(setSystemTags("cwe"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId());
    dbTester.rules().insertOrUpdateMetadata(r, organization, setTags("security"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getId(), organization);

    String result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    userSession.logIn();
    Action action = ws.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("q", "ps", "organization");

    Param query = action.param("q");
    assertThat(query.isRequired()).isFalse();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();

    Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("10");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();

    Param organization = action.param("organization");
    assertThat(organization).isNotNull();
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.description()).isNotEmpty();
    assertThat(organization.exampleValue()).isNotEmpty();
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.since()).isEqualTo("6.4");
  }

  private RuleDefinitionDto insertRuleWithoutTags() {
    return dbTester.rules().insert(setSystemTags());
  }

  private void insertIssueWithBrowsePermission(OrganizationDto organization, RuleDefinitionDto rule, String... tags) {
    IssueDto issue = insertIssueWithoutBrowsePermission(organization, rule, tags);
    grantAccess(issue);
  }

  private void insertIssueWithBrowsePermission(RuleDefinitionDto rule, String... tags) {
    IssueDto issue = insertIssueWithoutBrowsePermission(rule, tags);
    grantAccess(issue);
  }

  private IssueDto insertIssueWithoutBrowsePermission(RuleDefinitionDto rule, String... tags) {
    return insertIssueWithoutBrowsePermission(organization, rule, tags);
  }

  private IssueDto insertIssueWithoutBrowsePermission(OrganizationDto organization, RuleDefinitionDto rule, String... tags) {
    IssueDto issue = dbTester.issues().insertIssue(organization, i -> i.setRule(rule).setTags(asList(tags)));
    ComponentDto project = dbTester.getDbClient().componentDao().selectByUuid(dbTester.getSession(), issue.getProjectUuid()).get();
    userSession.addProjectPermission(USER, project);
    issueIndexer.commitAndIndexIssues(dbTester.getSession(), Collections.singletonList(issue));
    return issue;
  }

  private void grantAccess(IssueDto issue) {
    PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(issue.getProjectUuid(), "TRK");
    access.addUserId(userSession.getUserId());
    permissionIndexerTester.allow(access);
  }
}
