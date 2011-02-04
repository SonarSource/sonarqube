/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RoleManagerTest extends AbstractDbUnitTestCase {

  @Test
  public void affectDefaultRolesToResource() {
    setupData("affectDefaultRolesToResource");
    new RoleManager(getSession()).affectDefaultRolesToResource(10);
    checkTables("affectDefaultRolesToResource", "user_roles", "group_roles");
  }

  @Test
  public void affectZeroDefaultRolesToResource() {
    setupData("affectZeroDefaultRolesToResource");
    new RoleManager(getSession()).affectDefaultRolesToResource(10);
    checkTables("affectZeroDefaultRolesToResource", "user_roles", "group_roles");
  }

  @Test
  public void affectAnyoneDefaultRoleToResource() {
    setupData("affectAnyoneDefaultRoleToResource");
    new RoleManager(getSession()).affectDefaultRolesToResource(10);
    checkTables("affectAnyoneDefaultRoleToResource", "group_roles");
  }

  @Test
  public void convertDefaultRoleName() {
    assertThat(RoleManager.convertDefaultRoleName(RoleManager.DEFAULT_ROLE_PREFIX + "admin"), is("admin"));
  }
}
