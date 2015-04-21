/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.ui.ws.ComponentConfigurationPages.ConfigPage;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentConfigurationPagesTest {

  @Mock
  private I18n i18n;

  @Mock
  private ResourceTypes resourceTypes;

  @Before
  public void before() {
    when(i18n.message(Matchers.any(Locale.class), Matchers.anyString(), Matchers.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArgumentAt(1, String.class);
      }
    });
  }

  @Test
  public void pages_for_project() throws Exception {
    String uuid = "abcd";
    ComponentDto component = ComponentTesting.newProjectDto(uuid).setKey("org.codehaus.sonar:sonar");
    UserSession userSession = MockUserSession.set().setLogin("obiwan").addProjectUuidPermissions(UserRole.ADMIN, uuid);

    List<ConfigPage> pages = new ComponentConfigurationPages(i18n, resourceTypes).getConfigPages(component, userSession);
    assertThat(pages).extracting("visible").containsExactly(
      false, true, true, true, true, true, false, false, false, false);
    assertThat(pages).extracting("url").containsExactly(
        "/project/settings?id=org.codehaus.sonar%3Asonar",
        "/project/profile?id=org.codehaus.sonar%3Asonar",
        "/project/qualitygate?id=org.codehaus.sonar%3Asonar",
        "/manual_measures/index?id=org.codehaus.sonar%3Asonar",
        "/action_plans/index?id=org.codehaus.sonar%3Asonar",
        "/project/links?id=org.codehaus.sonar%3Asonar",
        "/project_roles/index?id=org.codehaus.sonar%3Asonar",
        "/project/history?id=org.codehaus.sonar%3Asonar",
        "/project/key?id=org.codehaus.sonar%3Asonar",
        "/project/deletion?id=org.codehaus.sonar%3Asonar"
      );
  }

  @Test
  public void pages_for_project_with_resource_type_property() throws Exception {
    String uuid = "abcd";
    ComponentDto component = ComponentTesting.newProjectDto(uuid);
    UserSession userSession = MockUserSession.set().setLogin("obiwan").addProjectUuidPermissions(UserRole.ADMIN, uuid);
    when(resourceTypes.get(component.qualifier())).thenReturn(
      ResourceType.builder(component.qualifier()).setProperty("configurable", true).build());

    List<ConfigPage> pages = new ComponentConfigurationPages(i18n, resourceTypes).getConfigPages(component, userSession);
    assertThat(pages).extracting("visible").containsExactly(
      true, true, true, true, true, true, false, false, false, false);
  }

  @Test
  public void pages_for_module() throws Exception {
    String uuid = "abcd";
    ComponentDto project = ComponentTesting.newProjectDto(uuid);
    ComponentDto module = ComponentTesting.newModuleDto(project);
    UserSession userSession = MockUserSession.set().setLogin("obiwan").addProjectUuidPermissions(UserRole.ADMIN, uuid);

    List<ConfigPage> pages = new ComponentConfigurationPages(i18n, resourceTypes).getConfigPages(module, userSession);
    assertThat(pages).extracting("visible").containsExactly(
      false, false, false, true, false, false, false, false, false, false);
  }

  @Test
  public void pages_for_non_admin() throws Exception {
    String uuid = "abcd";
    ComponentDto project = ComponentTesting.newProjectDto(uuid);
    UserSession userSession = MockUserSession.set().setLogin("obiwan").addProjectUuidPermissions(UserRole.USER, uuid);

    List<ConfigPage> pages = new ComponentConfigurationPages(i18n, resourceTypes).getConfigPages(project, userSession);
    assertThat(pages).extracting("visible").containsExactly(
      false, true, true, false, false, false, false, false, false, false);
  }
}
