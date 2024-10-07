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
package org.sonar.server.user.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.USER;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECTS;

@RunWith(DataProviderRunner.class)
public class CurrentActionHomepageIT {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();

  private final PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
  private final HomepageTypesImpl homepageTypes = new HomepageTypesImpl();
  private final PermissionService permissionService = new PermissionServiceImpl(new ResourceTypes(new ResourceTypeTree[] {
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()}));
  private final WsActionTester ws = new WsActionTester(
    new CurrentAction(userSessionRule, dbClient, new AvatarResolverImpl(), homepageTypes, platformEditionProvider, permissionService));

  @Test
  public void return_homepage_when_set_to_portfolios() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIOS"));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .isEqualTo(CurrentWsResponse.HomepageType.PORTFOLIOS);
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void return_homepage_when_set_to_a_portfolio(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter(portfolio.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, portfolio);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.PORTFOLIO, portfolio.getKey());
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void return_default_when_set_to_a_portfolio_but_no_rights_on_this_portfolio(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter(portfolio.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .isEqualTo(PROJECTS);
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void return_homepage_when_set_to_an_application(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, application);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.APPLICATION, application.getKey());
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void return_default_homepage_when_set_to_an_application_but_no_rights_on_this_application(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .isEqualTo(PROJECTS);
  }

  @Test
  @UseDataProvider("allEditions")
  public void return_homepage_when_set_to_a_project(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(mainBranch.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, projectData.getProjectDto());

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECT, mainBranch.getKey());
  }

  @Test
  @UseDataProvider("allEditions")
  public void return_default_homepage_when_set_to_a_project_but_no_rights_on_this_project(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(project.uuid()));
    userSessionRule.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType)
      .isEqualTo(PROJECTS);
  }

  @Test
  public void return_homepage_when_set_to_a_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter(branch.uuid()));
    userSessionRule.logIn(user).addProjectPermission(USER, projectData.getProjectDto());

    CurrentWsResponse response = call();

    assertThat(response.getHomepage())
      .extracting(CurrentWsResponse.Homepage::getType, CurrentWsResponse.Homepage::getComponent, CurrentWsResponse.Homepage::getBranch)
      .containsExactly(CurrentWsResponse.HomepageType.PROJECT, branch.getKey(), branchName);
  }

  @Test
  public void fallback_when_user_homepage_project_does_not_exist_in_db() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter("not-existing-project-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void fallback_when_user_homepage_portfolio_does_not_exist_in_db(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PORTFOLIO").setHomepageParameter("not-existing-portfolio-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  public void fallback_when_edition_is_null() {
    setPlatformEdition(null);
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
    assertThat(response.getHomepage().getType()).isEqualTo(PROJECTS);
  }

  @Test
  @UseDataProvider("enterpriseAndAbove")
  public void fallback_when_user_homepage_application_does_not_exist_in_db(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter("not-existing-application-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage()).isNotNull();
  }

  @Test
  @UseDataProvider("belowEnterprise")
  public void fallback_when_user_homepage_application_and_edition_below_enterprise(EditionProvider.Edition edition) {
    setPlatformEdition(edition);
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    UserDto user = db.users().insertUser(u -> u.setHomepageType("APPLICATION").setHomepageParameter(application.uuid()));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage().getType()).hasToString("PROJECTS");
  }

  @Test
  public void fallback_to_PROJECTS() {
    UserDto user = db.users().insertUser(u -> u.setHomepageType("PROJECT").setHomepageParameter("not-existing-project-uuid"));
    userSessionRule.logIn(user.getLogin());

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);

    assertThat(response.getHomepage().getType()).hasToString("PROJECTS");
  }

  private CurrentWsResponse call() {
    return ws.newRequest().executeProtobuf(CurrentWsResponse.class);
  }

  private void setPlatformEdition(@Nullable EditionProvider.Edition edition) {
    when(platformEditionProvider.get()).thenReturn(Optional.ofNullable(edition));
  }

  @DataProvider
  public static Object[][] enterpriseAndAbove() {
    return new Object[][] {
      {EditionProvider.Edition.ENTERPRISE},
      {EditionProvider.Edition.DATACENTER}
    };
  }

  @DataProvider
  public static Object[][] belowEnterprise() {
    return new Object[][] {
      {EditionProvider.Edition.COMMUNITY},
      {EditionProvider.Edition.DEVELOPER}
    };
  }

  @DataProvider
  public static Object[][] allEditions() {
    return new Object[][] {
      {EditionProvider.Edition.COMMUNITY},
      {EditionProvider.Edition.DEVELOPER},
      {EditionProvider.Edition.ENTERPRISE},
      {EditionProvider.Edition.DATACENTER}
    };
  }
}
