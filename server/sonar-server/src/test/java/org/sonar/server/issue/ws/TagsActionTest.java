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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.TagsResponse;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;

public class TagsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);

  private WsActionTester ws = new WsActionTester(new TagsAction(issueIndex, db.getDbClient(), new ComponentFinder(db.getDbClient(), resourceTypes)));

  @Test
  public void search_tags() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project);

    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2", "tag3", "tag4", "tag5");
  }

  @Test
  public void search_tags_by_query() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag12", "tag4", "tag5")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project);

    TagsResponse result = ws.newRequest()
      .setParam("q", "ag1")
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag12");
  }

  @Test
  public void search_tags_by_organization() {
    RuleDefinitionDto rule = db.rules().insert();
    // Tags on issues of organization 1
    OrganizationDto organization1 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization1);
    db.issues().insert(rule, project1, project1, issue -> issue.setTags(asList("tag1", "tag2")));
    // Tags on issues of organization 2
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project2 = db.components().insertPrivateProject(organization2);
    db.issues().insert(rule, project2, project2, issue -> issue.setTags(singletonList("tag3")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project1, project2);

    TagsResponse result = ws.newRequest()
      .setParam("organization", organization1.getKey())
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2");
  }

  @Test
  public void search_tags_by_project() {
    RuleDefinitionDto rule = db.rules().insert();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    db.issues().insert(rule, project1, project1, issue -> issue.setTags(singletonList("tag1")));
    db.issues().insert(rule, project2, project2, issue -> issue.setTags(singletonList("tag2")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project1, project2);

    TagsResponse result = ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project1.getKey())
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1");
  }

  @Test
  public void search_tags_by_portfolio() {
    OrganizationDto organization = db.getDefaultOrganization();
    ComponentDto portfolio = db.components().insertPrivatePortfolio(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.components().insertComponent(newProjectCopy(project, portfolio));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    issueIndexer.indexOnStartup(emptySet());
    viewIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(organization);

    TagsResponse result = ws.newRequest()
      .setParam("project", portfolio.getKey())
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("cwe");
  }

  @Test
  public void search_tags_by_application() {
    OrganizationDto organization = db.getDefaultOrganization();
    ComponentDto application = db.components().insertPrivateApplication(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.components().insertComponent(newProjectCopy(project, application));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    issueIndexer.indexOnStartup(emptySet());
    viewIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(organization);

    TagsResponse result = ws.newRequest()
      .setParam("project", application.getKey())
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("cwe");
  }

  @Test
  public void return_limited_size() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project);

    TagsResponse result = ws.newRequest()
      .setParam("ps", "2")
      .executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2");
  }

  @Test
  public void do_not_return_issues_without_permission() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.issues().insert(rule, project1, project1, issue -> issue.setTags(asList("tag1", "tag2")));
    db.issues().insert(rule, project2, project2, issue -> issue.setTags(asList("tag3", "tag4", "tag5")));
    issueIndexer.indexOnStartup(emptySet());
    // Project 2 is not visible to current user
    permissionIndexer.allowOnlyAnyone(project1);

    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2");
  }

  @Test
  public void without_organization_parameter_is_cross_organization() {
    RuleDefinitionDto rule = db.rules().insert();
    // Tags on issues of organization 1
    OrganizationDto organization1 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization1);
    db.issues().insert(rule, project1, project1, issue -> issue.setTags(asList("tag1", "tag2")));
    // Tags on issues of organization 2
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project2 = db.components().insertPrivateProject(organization2);
    db.issues().insert(rule, project2, project2, issue -> issue.setTags(singletonList("tag3")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project1, project2);

    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).containsExactly("tag1", "tag2", "tag3");
  }

  @Test
  public void empty_list() {
    TagsResponse result = ws.newRequest().executeProtobuf(TagsResponse.class);

    assertThat(result.getTagsList()).isEmpty();
  }

  @Test
  public void fail_when_project_does_not_belong_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(otherOrganization);
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Project '%s' is not part of the organization '%s'", project.getKey(), organization.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_parameter_does_not_match_a_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Component '%s' must be a project", file.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", file.getKey())
      .execute();
  }

  @Test
  public void json_example() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    db.issues().insert(rule, project, project, issue -> issue.setTags(asList("convention", "security")));
    db.issues().insert(rule, project, project, issue -> issue.setTags(singletonList("cwe")));
    issueIndexer.indexOnStartup(emptySet());
    permissionIndexer.allowOnlyAnyone(project);

    String result = ws.newRequest().execute().getInput();

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
    assertThat(action.params())
      .extracting(Param::key, Param::defaultValue, Param::since, Param::isRequired, Param::isInternal)
      .containsExactlyInAnyOrder(
        tuple("q", null, null, false, false),
        tuple("ps", "10", null, false, false),
        tuple("organization", null, "6.4", false, true),
        tuple("project", null, "7.4", false, false));
  }

}
