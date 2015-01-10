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
package org.sonar.core.resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResourcePermissionsTest extends AbstractDaoTestCase {

  private final static Long PROJECT_ID = 123L;

  DbSession session;
  Resource project;
  Settings settings;
  DefaultResourcePermissions permissions;
  PermissionFacade permissionFacade;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void initResourcePermissions() {
    session = getMyBatis().openSession(false);
    project = new Project("project").setId(PROJECT_ID.intValue());
    settings = new Settings();
    permissionFacade = new PermissionFacade(new RoleDao(), new UserDao(getMyBatis(), System2.INSTANCE), new ResourceDao(getMyBatis(), System2.INSTANCE),
      new PermissionTemplateDao(getMyBatis(), System2.INSTANCE), settings);
    permissions = new DefaultResourcePermissions(getMyBatis(), permissionFacade);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void grantGroupRole() {
    setupData("grantGroupRole");

    permissions.grantGroupRole(project, "sonar-administrators", "admin");

    // do not insert duplicated rows
    permissions.grantGroupRole(project, "sonar-administrators", "admin");

    checkTables("grantGroupRole", new String[] {"id"}, "group_roles");
  }

  @Test
  public void grantGroupRole_anyone() {
    setupData("grantGroupRole_anyone");

    permissions.grantGroupRole(project, DefaultGroups.ANYONE, "admin");

    checkTables("grantGroupRole_anyone", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_group_not_found() {
    setupData("grantGroupRole_ignore_if_group_not_found");

    permissions.grantGroupRole(project, "not_found", "admin");

    checkTables("grantGroupRole_ignore_if_group_not_found", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_not_persisted() {
    setupData("grantGroupRole_ignore_if_not_persisted");

    Project resourceWithoutId = new Project("");
    permissions.grantGroupRole(resourceWithoutId, "sonar-users", "admin");

    checkTables("grantGroupRole_ignore_if_not_persisted", "group_roles");
  }

  @Test
  public void grantUserRole() {
    setupData("grantUserRole");

    permissions.grantUserRole(project, "marius", "admin");

    // do not insert duplicated rows
    permissions.grantUserRole(project, "marius", "admin");

    checkTables("grantUserRole", new String[] {"id"}, "user_roles");
  }

  @Test
  public void grantDefaultRoles_qualifier_independent() {
    setupData("grantDefaultRoles");

    settings.setProperty("sonar.permission.template.default", "default_template_20130101_010203");

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions(session, "marius", PROJECT_ID)).isEmpty();

    permissions.grantDefaultRoles(session, project);

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).containsOnly("admin");
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).containsOnly("user", "codeviewer");
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", PROJECT_ID)).containsOnly("user", "codeviewer");

    assertThat(permissionFacade.selectUserPermissions(session, "marius", PROJECT_ID)).isEmpty();
  }

  @Test
  public void grantDefaultRoles_pattern() {
    setupData("grantDefaultRolesPattern");

    settings.setProperty("sonar.permission.template.default", "default");

    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRolesPattern", "user_roles", "group_roles");
  }

  @Test
  public void grantDefaultRoles_several_matching_pattern() {
    setupData("grantDefaultRolesSeveralPattern");

    settings.setProperty("sonar.permission.template.default", "default");

    throwable.expect(IllegalStateException.class);
    throwable
      .expectMessage("The \"foo.project\" key matches multiple permission templates: \"Start with foo\", \"Start with foo again\". A system administrator must update these templates so that only one of them matches the key.");

    permissions.grantDefaultRoles(project);
  }

  @Test
  public void grantDefaultRoles_qualifier_specific() {
    setupData("grantDefaultRolesProject");

    settings.setProperty("sonar.permission.template.default", "default_20130101_010203");
    settings.setProperty("sonar.permission.template.TRK.default", "default_for_trk_20130101_010203");

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", PROJECT_ID)).isEmpty();
    assertThat(permissionFacade.selectUserPermissions(session, "marius", PROJECT_ID)).isEmpty();

    permissions.grantDefaultRoles(session, project);

    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-administrators", PROJECT_ID)).containsOnly("admin", "user", "codeviewer");
    assertThat(permissionFacade.selectGroupPermissions(session, "sonar-users", PROJECT_ID)).containsOnly("admin");
    assertThat(permissionFacade.selectGroupPermissions(session, "Anyone", PROJECT_ID)).containsOnly("codeviewer");

    assertThat(permissionFacade.selectUserPermissions(session, "marius", PROJECT_ID)).containsOnly("codeviewer");
  }

  @Test
  public void grantDefaultRoles_unknown_group() {
    setupData("grantDefaultRoles_unknown_group");

    settings.setProperty("sonar.permission.template.TRK.default", "default_template_20130101_010203");
    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_unknown_group", "group_roles");
  }

  @Test
  public void grantDefaultRoles_users() {
    setupData("grantDefaultRoles_users");

    settings.setProperty("sonar.permission.template.TRK.default", "default_for_trk_20130101_010203");
    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_users", "user_roles");
  }

  @Test
  public void hasRoles() {
    setupData("hasRoles");

    // no groups and at least one user
    assertThat(permissions.hasRoles(new Project("only_users").setId(1))).isTrue();

    // no users and at least one group
    assertThat(permissions.hasRoles(new Project("only_groups").setId(2))).isTrue();

    // groups and users
    assertThat(permissions.hasRoles(new Project("groups_and_users").setId(3))).isTrue();

    // no groups, no users
    assertThat(permissions.hasRoles(new Project("no_groups_no_users").setId(4))).isFalse();

    // does not exist
    assertThat(permissions.hasRoles(new Project("not_found"))).isFalse();
  }

  @Test
  public void should_fail_when_no_default_template_is_defined() throws Exception {
    throwable.expect(IllegalStateException.class);

    permissions.grantDefaultRoles(project);
  }
}
