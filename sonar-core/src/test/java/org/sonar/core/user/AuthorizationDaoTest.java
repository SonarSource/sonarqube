/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.user;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class AuthorizationDaoTest extends AbstractDaoTestCase {

  private static final int USER = 100;
  private static final long PROJECT = 300l, PACKAGE = 301l, FILE = 302l, FILE_IN_OTHER_PROJECT = 999l;

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    setupData("user_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Long> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // user does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    setupData("group_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Long> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void group_should_have_global_authorization() {
    // user is in a group that has authorized access to all projects
    setupData("group_should_have_global_authorization");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Long> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    setupData("anonymous_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Long> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Long>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void should_return_root_project_ids_for_user() {
    setupData("should_return_root_project_ids_for_user");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> rootProjectIds = authorization.selectAuthorizedRootProjectsIds(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsIds(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_ids_for_group() {
    // but user is not in an authorized group
    setupData("should_return_root_project_ids_for_group");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> rootProjectIds = authorization.selectAuthorizedRootProjectsIds(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsIds(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_ids_for_anonymous() {
    setupData("should_return_root_project_ids_for_anonymous");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> rootProjectIds = authorization.selectAuthorizedRootProjectsIds(null, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // group does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsIds(null, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_user_global_permissions() {
    setupData("should_return_user_global_permissions");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_group_global_permissions() {
    setupData("should_return_group_global_permissions");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_global_permissions_for_anonymous() {
    setupData("should_return_global_permissions_for_anonymous");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    assertThat(authorization.selectGlobalPermissions(null)).containsOnly("user", "admin");
  }
}
