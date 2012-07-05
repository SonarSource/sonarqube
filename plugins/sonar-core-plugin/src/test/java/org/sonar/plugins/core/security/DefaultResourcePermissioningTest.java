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
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultResourcePermissioningTest extends AbstractDaoTestCase {

  private Resource project = new Project("project").setId(123);

  @Test
  public void addGroupPermissions() {
    setupData("addGroupPermissions");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.addGroupPermissions(project, "sonar-administrators", "admin");

    checkTables("addGroupPermissions", "group_roles");
  }

  @Test
  public void addGroupPermissions_anyone() {
    setupData("addGroupPermissions_anyone");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.addGroupPermissions(project, DefaultGroups.ANYONE, "admin");

    checkTables("addGroupPermissions_anyone", "group_roles");
  }

  @Test
  public void addGroupPermissions_ignore_if_group_not_found() {
    setupData("addGroupPermissions_ignore_if_group_not_found");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    permissioning.addGroupPermissions(project, "not_found", "admin");

    checkTables("addGroupPermissions_ignore_if_group_not_found", "group_roles");
  }

  @Test
  public void addGroupPermissions_ignore_if_not_persisted() {
    setupData("addGroupPermissions_ignore_if_not_persisted");

    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());
    Project resourceWithoutId = new Project("");
    permissioning.addGroupPermissions(resourceWithoutId, "sonar-users", "admin");

    checkTables("addGroupPermissions_ignore_if_not_persisted", "group_roles");
  }

  @Test
  public void grantDefaultPermissions() {
    setupData("grantDefaultPermissions");

    Settings settings = new Settings(new PropertyDefinitions(DefaultResourcePermissioning.class));
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultPermissions(project);

    checkTables("grantDefaultPermissions", "user_roles", "group_roles");
  }

  @Test
  public void grantDefaultPermissions_unknown_group() {
    setupData("grantDefaultPermissions_unknown_group");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultGroups", "sonar-administrators,unknown");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultPermissions(project);

    checkTables("grantDefaultPermissions_unknown_group", "group_roles");
  }

  @Test
  public void grantDefaultPermissions_users() {
    setupData("grantDefaultPermissions_users");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultUsers", "marius,disabled,notfound");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(settings, getMyBatis());
    permissioning.grantDefaultPermissions(project);

    checkTables("grantDefaultPermissions_users", "user_roles");
  }

  @Test
  public void hasPermissions() {
    setupData("hasPermissions");
    DefaultResourcePermissioning permissioning = new DefaultResourcePermissioning(new Settings(), getMyBatis());

    // no groups and at least one user
    assertThat(permissioning.hasPermissions(new Project("only_users").setId(1))).isTrue();

    // no users and at least one group
    assertThat(permissioning.hasPermissions(new Project("only_groups").setId(2))).isTrue();

    // groups and users
    assertThat(permissioning.hasPermissions(new Project("groups_and_users").setId(3))).isTrue();

    // no groups, no users
    assertThat(permissioning.hasPermissions(new Project("no_groups_no_users").setId(4))).isFalse();

    // does not exist
    assertThat(permissioning.hasPermissions(new Project("not_found"))).isFalse();
  }
}