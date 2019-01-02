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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.USER;

public class CurrentActionHomepageTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private MapSettings settings = new MapSettings();
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private HomepageTypesImpl homepageTypes = new HomepageTypesImpl(settings.asConfig(), organizationFlags, db.getDbClient());
  private PermissionService permissionService = new PermissionServiceImpl(new ResourceTypes(new ResourceTypeTree[] {
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()}));

  private WsActionTester ws = new WsActionTester(
    new CurrentAction(userSessionRule, dbClient, defaultOrganizationProvider, new AvatarResolverImpl(), homepageTypes, pluginRepository, permissionService));

  @Test
  public void return_homepage_when_set_to_MY_PROJECTS() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("MY_PROJECTS"));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .containsExactly(CurrentWsResponse.HomepageType.MY_PROJECTS);
  }

  @Test
  public void return_homepage_when_set_to_portfolios() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIOS"));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .containsExactly(CurrentWsResponse.HomepageType.PORTFOLIOS);
  }

  @Test
  public void return_homepage_when_set_to_a_portfolio() {
    withGovernancePlugin();
    ComponentDto portfolio = db.components().insertPrivatePortfolio(db.getDefaultOrganization());
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter(portfolio.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, portfolio);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.PORTFOLIO, portfolio.getKey());
  }

  @Test
  public void return_default_when_set_to_a_portfolio_but_no_rights_on_this_portfolio() {
    withGovernancePlugin();
    ComponentDto portfolio = db.components().insertPrivatePortfolio(db.getDefaultOrganization());
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter(portfolio.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECTS);
  }

  @Test
  public void return_homepage_when_set_to_an_application() {
    withGovernancePlugin();
    ComponentDto application = db.components().insertPrivateApplication(db.getDefaultOrganization());
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, application);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.APPLICATION, application.getKey());
  }

  @Test
  public void return_default_homepage_when_set_to_an_application_but_no_rights_on_this_application() {
    withGovernancePlugin();
    ComponentDto application = db.components().insertPrivateApplication(db.getDefaultOrganization());
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECTS);
  }

  @Test
  public void return_homepage_when_set_to_a_project() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(project.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, project);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECT, project.getKey());
  }

  @Test
  public void return_default_homepage_when_set_to_a_project_but_no_rights_on_this_project() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(project.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECTS);
  }

  @Test
  public void return_homepage_when_set_to_an_organization() {
    OrganizationDto organizationDto = db.organizations().insert();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("ORGANIZATION").setHomepageParameter(organizationDto.getUuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getOrganization)
      .containsExactly(CurrentWsResponse.HomepageType.ORGANIZATION, organizationDto.getKey());
  }

  @Test
  public void return_homepage_when_set_to_a_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(branch.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, project);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent, CurrentWsResponse.Homepage::getBranch)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECT, branch.getKey(), branch.getBranch());
  }

  @Test
  public void fallback_when_user_homepage_project_does_not_exist_in_db() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter("not-existing-project-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  public void fallback_when_user_homepage_organization_does_not_exist_in_db() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("ORGANIZATION").setHomepageParameter("not-existing-organization-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  public void fallback_when_user_homepage_portfolio_does_not_exist_in_db() {
    withGovernancePlugin();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter("not-existing-portfolio-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  public void fallback_when_user_homepage_application_does_not_exist_in_db() {
    withGovernancePlugin();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter("not-existing-application-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  public void fallback_when_user_homepage_application_and_governance_plugin_is_not_installed() {
    withoutGovernancePlugin();
    ComponentDto application = db.components().insertPrivateApplication(db.getDefaultOrganization());
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage().getType().toString()).isEqualTo("PROJECTS");
  }

  @Test
  public void fallback_to_PROJECTS_when_on_SonarQube() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter("not-existing-project-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage().getType().toString()).isEqualTo("PROJECTS");
  }

  @Test
  public void fallback_to_MY_PROJECTS_when_on_SonarCloud() {
    onSonarCloud();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter("not-existing-project-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage().getType().toString()).isEqualTo("MY_PROJECTS");
  }

  private CurrentWsResponse call() {
    return ws.newRequest().executeProtobuf(CurrentWsResponse.class);
  }

  private void onSonarCloud() {
    settings.setProperty("sonar.sonarcloud.enabled", true);
  }

  private void withGovernancePlugin() {
    when(pluginRepository.hasPlugin("governance")).thenReturn(true);
  }

  private void withoutGovernancePlugin() {
    when(pluginRepository.hasPlugin("governance")).thenReturn(false);
  }

}
