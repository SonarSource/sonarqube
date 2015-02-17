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
package org.sonar.core.user;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationDaoTest extends AbstractDaoTestCase {

  private static final int USER = 100;
  private static final Long PROJECT_ID = 300L, EMPTY_PROJECT_ID = 400L;
  private static final String PROJECT = "pj-w-snapshot";

  DbSession session;

  @Before
  public void setUp() throws Exception {
    session = getMyBatis().openSession(false);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    setupData("user_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, EMPTY_PROJECT_ID);

    // user does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void is_authorized_component_key_for_user() {
    // but user is not in an authorized group
    setupData("user_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();

    // user does not have the role "admin"
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    setupData("group_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, EMPTY_PROJECT_ID);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void group_should_have_global_authorization() {
    // user is in a group that has authorized access to all projects
    setupData("group_should_have_global_authorization");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, EMPTY_PROJECT_ID);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    setupData("anonymous_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, EMPTY_PROJECT_ID),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, EMPTY_PROJECT_ID);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_user() {
    setupData("should_return_root_project_keys_for_user");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_group() {
    // but user is not in an authorized group
    setupData("should_return_root_project_keys_for_group");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_anonymous() {
    setupData("should_return_root_project_keys_for_anonymous");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // group does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_user() {
    setupData("should_return_root_project_keys_for_user");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_group() {
    // but user is not in an authorized group
    setupData("should_return_root_project_keys_for_group");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_anonymous() {
    setupData("should_return_root_project_keys_for_anonymous");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(null, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // group does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectUuids).isEmpty();
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

  @Test
  public void should_return_global_permissions_for_group_anyone() throws Exception {
    setupData("should_return_global_permissions_for_group_anyone");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    assertThat(authorization.selectGlobalPermissions("anyone_user")).containsOnly("user", "profileadmin");
  }

}
