/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.ui.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.ResourceLanguage;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.ResourceScope;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.Views;
import org.sonar.server.ws.WsActionTester;

import static java.util.Locale.ENGLISH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentNavigationActionTest {

  private static final String PROJECT_KEY = "polop";
  private static final ComponentDto PROJECT = newProjectDto("abcd")
    .setKey(PROJECT_KEY)
    .setName("Polop")
    .setDescription("test project")
    .setLanguage("xoo");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private UserDbTester userDbTester = new UserDbTester(dbTester);
  private PropertyDbTester propertyDbTester = new PropertyDbTester(dbTester);
  private DbClient dbClient = dbTester.getDbClient();

  private I18n i18n = mock(I18n.class);

  private ResourceTypes resourceTypes = mock(ResourceTypes.class);

  private WsActionTester ws;

  @Before
  public void before() {
    when(i18n.message(eq(ENGLISH), any(String.class), any(String.class)))
      .thenAnswer(invocation -> invocation.getArgumentAt(2, String.class));
  }

  @Test
  public void fail_on_missing_parameters() throws Exception {
    init();

    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest().execute();
  }

  @Test
  public void fail_on_unknown_component_key() throws Exception {
    init();

    expectedException.expect(NotFoundException.class);
    execute(PROJECT.key());
  }

  @Test
  public void fail_on_missing_permission() throws Exception {
    init();
    componentDbTester.insertComponent(PROJECT);

    expectedException.expect(ForbiddenException.class);
    execute(PROJECT.key());
  }

  @Test
  public void return_component_info_when_anonymous_no_snapshot() throws Exception {
    init();
    componentDbTester.insertComponent(PROJECT);
    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT.uuid());

    executeAndVerify(PROJECT.key(), "return_component_info_when_anonymous_no_snapshot.json");
  }

  @Test
  public void return_component_info_with_favourite() throws Exception {
    init();
    UserDto user = userDbTester.insertUser("obiwan");
    componentDbTester.insertComponent(PROJECT);
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setResourceId(PROJECT.getId()).setUserId(user.getId()));
    userSessionRule.login(user).addProjectUuidPermissions(UserRole.USER, PROJECT.uuid());

    executeAndVerify(PROJECT.key(), "return_component_info_with_favourite.json");
  }

  @Test
  public void return_component_info_when_snapshot() throws Exception {
    init();
    componentDbTester.insertComponent(PROJECT);
    componentDbTester.insertSnapshot(newAnalysis(PROJECT)
      .setCreatedAt(DateUtils.parseDateTime("2015-04-22T11:44:00+0200").getTime())
      .setVersion("3.14")
      .setLast(true));
    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT.uuid());

    executeAndVerify(PROJECT.key(), "return_component_info_when_snapshot.json");
  }

  @Test
  public void return_extensions() throws Exception {
    init(createViews());
    componentDbTester.insertProjectAndSnapshot(PROJECT);
    userSessionRule.anonymous().addProjectUuidPermissions(UserRole.USER, PROJECT.uuid());

    executeAndVerify(PROJECT.key(), "return_extensions.json");
  }

  @Test
  public void return_extensions_for_admin() throws Exception {
    init(createViews());
    componentDbTester.insertProjectAndSnapshot(PROJECT);
    userSessionRule.anonymous()
      .addProjectUuidPermissions(UserRole.USER, PROJECT.uuid())
      .addProjectUuidPermissions(UserRole.ADMIN, PROJECT.uuid());

    executeAndVerify(PROJECT.key(), "return_extensions_for_admin.json");
  }

  @Test
  public void return_configuration_for_admin() throws Exception {
    UserDto user = userDbTester.insertUser();
    componentDbTester.insertComponent(PROJECT);
    userSessionRule.login(user)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    @NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    class FirstPage implements Page {
      @Override
      public String getTitle() {
        return "First Page";
      }

      @Override
      public String getId() {
        return "first_page";
      }
    }

    @NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    class SecondPage implements Page {
      @Override
      public String getTitle() {
        return "Second Page";
      }

      @Override
      public String getId() {
        return "/second/page";
      }
    }

    init(new Page[] {new FirstPage(), new SecondPage()});
    executeAndVerify(PROJECT.key(), "return_configuration_for_admin.json");
  }

  @Test
  public void return_configuration_with_all_properties() throws Exception {
    init();
    componentDbTester.insertComponent(PROJECT);
    userSessionRule.anonymous()
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    ResourceType projectResourceType = ResourceType.builder(PROJECT.qualifier())
      .setProperty("comparable", true)
      .setProperty("configurable", true)
      .setProperty("hasRolePolicy", true)
      .setProperty("modifiable_history", true)
      .setProperty("updatable_key", true)
      .setProperty("deletable", true)
      .build();
    when(resourceTypes.get(PROJECT.qualifier()))
      .thenReturn(projectResourceType);

    executeAndVerify(PROJECT.key(), "return_configuration_with_all_properties.json");
  }

  @Test
  public void return_breadcrumbs_on_module() throws Exception {
    init();
    ComponentDto project = componentDbTester.insertComponent(PROJECT);
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setKey("palap").setName("Palap"));
    userSessionRule.anonymous()
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    executeAndVerify(module.key(), "return_breadcrumbs_on_module.json");
  }

  @Test
  public void return_configuration_for_quality_profile_admin() throws Exception {
    init();
    componentDbTester.insertComponent(PROJECT);
    userSessionRule.anonymous()
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .setGlobalPermissions(QUALITY_PROFILE_ADMIN);

    executeAndVerify(PROJECT.key(), "return_configuration_for_quality_profile_admin.json");
  }

  @Test
  public void return_bread_crumbs_on_several_levels() throws Exception {
    init();
    ComponentDto project = componentDbTester.insertComponent(PROJECT);
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setKey("palap").setName("Palap"));
    ComponentDto directory = componentDbTester.insertComponent(newDirectory(module, "src/main/xoo"));
    ComponentDto file = componentDbTester.insertComponent(newFileDto(directory, directory, "cdef").setName("Source.xoo")
      .setKey("palap:src/main/xoo/Source.xoo")
      .setPath(directory.path()));
    userSessionRule.addProjectUuidPermissions(UserRole.USER, project.uuid());

    executeAndVerify(file.key(), "return_bread_crumbs_on_several_levels.json");
  }

  @Test
  public void work_with_only_system_admin() throws Exception {
    init(createViews());
    componentDbTester.insertProjectAndSnapshot(PROJECT);
    userSessionRule.setGlobalPermissions(SYSTEM_ADMIN);

    execute(PROJECT.key());
  }

  private void init(View... views) {
    ws = new WsActionTester(new ComponentNavigationAction(dbClient, new Views(userSessionRule, views), i18n, resourceTypes, userSessionRule, new ComponentFinder(dbClient)));
  }

  private String execute(String componentKey) {
    return ws.newRequest().setParam("componentKey", componentKey).execute().getInput();
  }

  private void verify(String json, String expectedJson) {
    assertJson(json).isSimilarTo(getClass().getResource(ComponentNavigationActionTest.class.getSimpleName() + "/" + expectedJson));
  }

  private void executeAndVerify(String componentKey, String expectedJson) {
    verify(execute(componentKey), expectedJson);
  }

  private View[] createViews() {
    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    class FirstPage implements Page {
      @Override
      public String getTitle() {
        return "First Page";
      }

      @Override
      public String getId() {
        return "first_page";
      }
    }
    Page page1 = new FirstPage();

    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    class SecondPage implements Page {
      @Override
      public String getTitle() {
        return "Second Page";
      }

      @Override
      public String getId() {
        return "/second/page";
      }
    }
    Page page2 = new SecondPage();

    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    @UserRole(UserRole.ADMIN)
    class AdminPage implements Page {
      @Override
      public String getTitle() {
        return "Admin Page";
      }

      @Override
      public String getId() {
        return "/admin/page";
      }
    }
    Page adminPage = new AdminPage();
    return new Page[] {page1, page2, adminPage};
  }
}
