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
package org.sonar.server.permission.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.template.TemplateGroupsAction;
import org.sonar.server.permission.ws.template.TemplateUsersAction;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class PermissionsWsTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final GroupWsSupport groupWsSupport = new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()));
  private final PermissionWsSupport wsSupport = new PermissionWsSupport(db.getDbClient(), new ComponentFinder(db.getDbClient(), resourceTypes), groupWsSupport);

  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);
  private RequestValidator requestValidator = new RequestValidator(permissionService);


  private WsTester underTest = new WsTester(new PermissionsWs(
    new TemplateUsersAction(db.getDbClient(), userSession, wsSupport, new AvatarResolverImpl(), wsParameters, requestValidator),
    new TemplateGroupsAction(db.getDbClient(), userSession, wsSupport, wsParameters, requestValidator)));

  @Test
  public void define_controller() {
    WebService.Controller controller = controller();
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.7");
    assertThat(controller.actions()).hasSize(2);
  }

  @Test
  public void define_template_users() {
    WebService.Action action = controller().action("template_users");

    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.param(PARAM_PERMISSION).isRequired()).isFalse();
  }

  @Test
  public void define_template_groups() {
    WebService.Action action = controller().action("template_groups");

    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("5.2");
  }

  private WebService.Controller controller() {
    return underTest.controller("api/permissions");
  }
}
