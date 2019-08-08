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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues.AuthorsResponse;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.test.JsonAssert.assertJson;

public class AuthorsActionTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);
  private ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT);

  private WsActionTester ws = new WsActionTester(new AuthorsAction(userSession, db.getDbClient(), issueIndex,
    new ComponentFinder(db.getDbClient(), resourceTypes), defaultOrganizationProvider));

  @Test
  public void search_authors() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(db.getDefaultOrganization());

    AuthorsResponse result = ws.newRequest().executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList()).containsExactlyInAnyOrder(leia, luke);
  }

  @Test
  public void search_authors_by_query() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(db.getDefaultOrganization());

    AuthorsResponse result = ws.newRequest()
      .setParam(TEXT_QUERY, "leia")
      .executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList())
      .containsExactlyInAnyOrder(leia)
      .doesNotContain(luke);
  }

  @Test
  public void search_authors_by_organization() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization1);
    ComponentDto project2 = db.components().insertPrivateProject(organization2);
    permissionIndexer.allowOnlyAnyone(project1, project2);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project1, project1, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project2, project2, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(organization1);

    assertThat(ws.newRequest()
      .setParam("organization", organization1.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("organization", organization1.getKey())
      .setParam(TEXT_QUERY, "eia")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("organization", organization1.getKey())
      .setParam(TEXT_QUERY, "luke")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .isEmpty();
  }

  @Test
  public void search_authors_by_project() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    permissionIndexer.allowOnlyAnyone(project1, project2);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project1, project1, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project2, project2, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(organization);

    assertThat(ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project1.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project1.getKey())
      .setParam(TEXT_QUERY, "eia")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
    assertThat(ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project1.getKey())
      .setParam(TEXT_QUERY, "luke")
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .isEmpty();
  }

  @Test
  public void search_authors_by_portfolio() {
    String leia = "leia.organa";
    OrganizationDto organization = db.getDefaultOrganization();
    ComponentDto portfolio = db.components().insertPrivatePortfolio(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.components().insertComponent(newProjectCopy(project, portfolio));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    issueIndexer.indexOnStartup(emptySet());
    viewIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(organization);

    assertThat(ws.newRequest()
      .setParam("project", portfolio.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
        .containsExactlyInAnyOrder(leia);
  }

  @Test
  public void search_authors_by_application() {
    String leia = "leia.organa";
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    ComponentDto application = db.components().insertPrivateApplication(defaultOrganization);
    ComponentDto project = db.components().insertPrivateProject(defaultOrganization);
    db.components().insertComponent(newProjectCopy(project, application));
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    issueIndexer.indexOnStartup(emptySet());
    viewIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(defaultOrganization);

    assertThat(ws.newRequest()
      .setParam("project", application.getKey())
      .executeProtobuf(AuthorsResponse.class).getAuthorsList())
      .containsExactlyInAnyOrder(leia);
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter_set() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project1 = db.components().insertPrivateProject(db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    ComponentDto project2 = db.components().insertPrivateProject(otherOrganization);
    permissionIndexer.allowOnlyAnyone(project1, project2);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project1, project1, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project2, project2, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(db.getDefaultOrganization());

    AuthorsResponse result = ws.newRequest().executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList())
      .containsExactlyInAnyOrder(leia)
      .doesNotContain(luke);
  }

  @Test
  public void set_page_size() {
    String han = "han.solo";
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(han));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(db.getDefaultOrganization());

    AuthorsResponse result = ws.newRequest()
      .setParam(PAGE_SIZE, "2")
      .executeProtobuf(AuthorsResponse.class);

    assertThat(result.getAuthorsList())
      .containsExactlyInAnyOrder(han, leia)
      .doesNotContain(luke);
  }

  @Test
  public void return_only_authors_from_issues_visible_by_current_user() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(leia));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn(user).addMembership(db.getDefaultOrganization());

    // User has no permission on project
    assertThat(ws.newRequest().executeProtobuf(AuthorsResponse.class).getAuthorsList()).isEmpty();

    // User has no browse permission on project
    permissionIndexer.allowOnlyUser(project, user);
    assertThat(ws.newRequest().executeProtobuf(AuthorsResponse.class).getAuthorsList()).isNotEmpty();
  }

  @Test
  public void fail_when_user_is_not_logged() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_user_is_not_member_of_the_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    userSession.logIn().addMembership(otherOrganization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_organization_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'unknown'");

    ws.newRequest()
      .setParam("organization", "unknown")
      .execute();
  }

  @Test
  public void fail_when_project_does_not_belong_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    userSession.logIn()
      .addMembership(organization)
      .addMembership(otherOrganization);
    ComponentDto project = db.components().insertPrivateProject(otherOrganization);
    permissionIndexer.allowOnlyAnyone(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Project '%s' is not part of the organization '%s'", project.getKey(), organization.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_is_not_a_project() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn().addMembership(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    permissionIndexer.allowOnlyAnyone(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Component '%s' must be a project", file.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", file.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn().addMembership(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown' not found");

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("project", "unknown")
      .execute();
  }

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject();
    permissionIndexer.allowOnlyAnyone(project);
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin("luke.skywalker"));
    db.issues().insert(rule, project, project, issue -> issue.setAuthorLogin("leia.organa"));
    issueIndexer.indexOnStartup(emptySet());
    userSession.logIn().addMembership(db.getDefaultOrganization());

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("authors");
    assertThat(definition.since()).isEqualTo("5.1");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();

    assertThat(definition.params())
      .extracting(Param::key, Param::isRequired, Param::isInternal)
      .containsExactlyInAnyOrder(
        tuple("q", false, false),
        tuple("ps", false, false),
        tuple("organization", false, true),
        tuple("project", false, false));

    assertThat(definition.param("ps"))
      .extracting(Param::defaultValue, Param::maximumValue)
      .containsExactly("10", 100);
  }
}
