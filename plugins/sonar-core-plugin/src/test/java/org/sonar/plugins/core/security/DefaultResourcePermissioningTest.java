/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.security;

import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultResourcePermissioningTest extends AbstractDaoTestCase {

  private Resource project = new Project("project").setId(123);

  @Test
  public void grantGroupRole() {
    setupData("grantGroupRole");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.grantGroupRole(project, "sonar-administrators", "admin");

    checkTables("grantGroupRole", "group_roles");
  }

  @Test
  public void grantGroupRole_anyone() {
    setupData("grantGroupRole_anyone");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.grantGroupRole(project, DefaultGroups.ANYONE, "admin");

    checkTables("grantGroupRole_anyone", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_group_not_found() {
    setupData("grantGroupRole_ignore_if_group_not_found");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.grantGroupRole(project, "not_found", "admin");

    checkTables("grantGroupRole_ignore_if_group_not_found", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_not_persisted() {
    setupData("grantGroupRole_ignore_if_not_persisted");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    Project resourceWithoutId = new Project("");
    permissioning.grantGroupRole(resourceWithoutId, "sonar-users", "admin");

    checkTables("grantGroupRole_ignore_if_not_persisted", "group_roles");
  }

  @Test
  public void grantDefaultRoles() {
    setupData("grantDefaultRoles");

    Settings settings = new Settings(new PropertyDefinitions(DefaultResourcePermissioning.class));
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultRoles(project);

    checkTables("grantDefaultRoles", "user_roles", "group_roles");
  }

  @Test
  public void grantDefaultRoles_unknown_group() {
    setupData("grantDefaultRoles_unknown_group");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultGroups", "sonar-administrators,unknown");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_unknown_group", "group_roles");
  }

  @Test
  public void grantDefaultRoles_users() {
    setupData("grantDefaultRoles_users");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultUsers", "marius,disabled,notfound");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_users", "user_roles");
  }

  @Test
  public void hasRoles() {
    setupData("hasRoles");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());

    // no groups and at least one user
    assertThat(permissioning.hasRoles(new Project("only_users").setId(1))).isTrue();

    // no users and at least one group
    assertThat(permissioning.hasRoles(new Project("only_groups").setId(2))).isTrue();

    // groups and users
    assertThat(permissioning.hasRoles(new Project("groups_and_users").setId(3))).isTrue();

    // no groups, no users
    assertThat(permissioning.hasRoles(new Project("no_groups_no_users").setId(4))).isFalse();

    // does not exist
    assertThat(permissioning.hasRoles(new Project("not_found"))).isFalse();
  }

  @Test
  public void use_default_project_roles_when_old_version_of_views_plugin() {
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    Resource view = mock(Resource.class);
    when(view.getQualifier()).thenReturn(Qualifiers.VIEW);

    assertThat(permissioning.getStrategy(view)).isEqualTo(Qualifiers.PROJECT);
  }

  @Test
  public void use_existing_view_roles() {
    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.VW.defaultUsers", "sonar-administrators");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    Resource view = mock(Resource.class);
    when(view.getQualifier()).thenReturn(Qualifiers.VIEW);

    assertThat(permissioning.getStrategy(view)).isEqualTo(Qualifiers.VIEW);
  }

  @Test
  public void use_existing_default_view_roles() {
    Settings settings = new Settings(new PropertyDefinitions(RecentViewPlugin.class));

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    Resource view = mock(Resource.class);
    when(view.getQualifier()).thenReturn(Qualifiers.VIEW);

    assertThat(permissioning.getStrategy(view)).isEqualTo(Qualifiers.VIEW);
  }

  @Properties({
    @Property(key = "sonar.role.user.VW.defaultUsers", defaultValue = "sonar-users", name = "")
  })
  static class RecentViewPlugin {

  }
}