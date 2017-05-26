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
package org.sonar.server.issue.ws;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
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
import org.sonar.server.organization.TestDefaultOrganizationProvider;
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

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings()), new RuleIndexDefinition(new MapSettings()));

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), new IssueIteratorFactory(db.getDbClient()));
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private PermissionIndexerTester permissionIndexerTester = new PermissionIndexerTester(es, issueIndexer);
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));
  private RuleIndex ruleIndex = new RuleIndex(es.client());

  private WsActionTester tester = new WsActionTester(new TagsAction(issueIndex, ruleIndex, db.getDbClient(), TestDefaultOrganizationProvider.from(db)));
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = db.organizations().insert();
  }

  @Test
  public void return_tags_from_issues() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]}");
  }

  @Test
  public void return_tags_from_rules() throws Exception {
    userSession.logIn();
    RuleDefinitionDto r = db.rules().insert(setSystemTags("tag1"));
    ruleIndexer.indexRuleDefinition(r.getKey());
    db.rules().insertOrUpdateMetadata(r, organization, setTags("tag2"));
    ruleIndexer.indexRuleExtension(organization, r.getKey());

    RuleDefinitionDto r2 = db.rules().insert(setSystemTags("tag3"));
    ruleIndexer.indexRuleDefinition(r2.getKey());
    db.rules().insertOrUpdateMetadata(r2, organization, setTags("tag4", "tag5"));
    ruleIndexer.indexRuleExtension(organization, r2.getKey());

    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]}");
  }

  @Test
  public void return_tags_from_issue_and_rule_tags() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    RuleDefinitionDto r = db.rules().insert(setSystemTags("tag6"));
    ruleIndexer.indexRuleDefinition(r.getKey());
    db.rules().insertOrUpdateMetadata(r, organization, setTags("tag7"));
    ruleIndexer.indexRuleExtension(organization, r.getKey());

    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\", \"tag6\", \"tag7\"]}");
  }

  @Test
  public void return_limited_size() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4", "tag5");

    String result = tester.newRequest()
      .setParam("ps", "2")
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\"]}");
  }

  @Test
  public void return_tags_matching_query() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag12", "tag4", "tag5");

    String result = tester.newRequest()
      .setParam("q", "ag1")
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag12\"]}");
  }

  @Test
  public void do_not_return_issues_without_permission() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "tag1", "tag2");
    insertIssueWithoutBrowsePermission(insertRuleWithoutTags(), "tag3", "tag4");

    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag1\", \"tag2\"]}");
    assertThat(result).doesNotContain("tag3").doesNotContain("tag4");
  }

  @Test
  public void return_empty_list() throws Exception {
    userSession.logIn();
    String result = tester.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[]}");
  }

  @Test
  public void test_example() throws Exception {
    userSession.logIn();
    insertIssueWithBrowsePermission(insertRuleWithoutTags(), "convention");

    RuleDefinitionDto r = db.rules().insert(setSystemTags("cwe"));
    ruleIndexer.indexRuleDefinition(r.getKey());
    db.rules().insertOrUpdateMetadata(r, organization, setTags("security"));
    ruleIndexer.indexRuleExtension(organization, r.getKey());

    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .execute().getInput();
    assertJson(result).isSimilarTo(tester.getDef().responseExampleAsString());
  }

  @Test
  public void test_definition() {
    userSession.logIn();
    Action action = tester.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("q", "ps", "organization");

    Param query = action.param("q");
    assertThat(query).isNotNull();
    assertThat(query.isRequired()).isFalse();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();

    Param pageSize = action.param("ps");
    assertThat(pageSize).isNotNull();
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
    return db.rules().insert(setSystemTags());
  }

  private void insertIssueWithBrowsePermission(RuleDefinitionDto rule, String... tags) {
    IssueDto issue = insertIssueWithoutBrowsePermission(rule, tags);
    grantAccess(issue);
  }

  private IssueDto insertIssueWithoutBrowsePermission(RuleDefinitionDto rule, String... tags) {
    IssueDto issue = db.issues().insertIssue(organization, i -> i.setRule(rule).setTags(asList(tags)));
    ComponentDto project = db.getDbClient().componentDao().selectByUuid(db.getSession(), issue.getProjectUuid()).get();
    userSession.addProjectPermission(USER, project);
    issueIndexer.index(Collections.singletonList(issue.getKey()));
    return issue;
  }

  private void grantAccess(IssueDto issue) {
    PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(issue.getProjectUuid(), System2.INSTANCE.now(), "TRK");
    access.addUserId(userSession.getUserId());
    permissionIndexerTester.allow(access);
  }
}
