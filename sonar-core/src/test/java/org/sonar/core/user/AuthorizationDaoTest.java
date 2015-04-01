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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationDaoTest extends AbstractDaoTestCase {

  private static final int USER = 100;
  private static final Long PROJECT_ID = 300L, PROJECT_ID_WITHOUT_SNAPSHOT = 400L;
  private static final String PROJECT = "pj-w-snapshot";
  private static final String PROJECT_WIHOUT_SNAPSHOT = "pj-wo-snapshot";

  DbSession session;

  AuthorizationDao authorization;

  @Before
  public void setUp() throws Exception {
    session = getMyBatis().openSession(false);
    authorization = new AuthorizationDao(getMyBatis());
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    setupData("user_should_be_authorized");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // user does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();

    assertThat(authorization.keepAuthorizedProjectIds(session,
      Collections.<Long>emptySet(),
      USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_user() {
    setupData("keep_authorized_project_ids_for_user");

    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(session, Collections.<Long>emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_group() {
    setupData("keep_authorized_project_ids_for_group");

    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(session, Collections.<Long>emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_anonymous() {
    setupData("keep_authorized_project_ids_for_anonymous");

    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), null, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(session, Sets.newHashSet(PROJECT_ID), null, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(session, Collections.<Long>emptySet(), null, "admin")).isEmpty();
  }

  @Test
  public void is_authorized_component_key_for_user() {
    setupData("keep_authorized_project_ids_for_user");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_group() {
    setupData("keep_authorized_project_ids_for_group");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_anonymous() {
    setupData("keep_authorized_project_ids_for_anonymous");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, null, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, null, "user")).isFalse();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, null, "admin")).isFalse();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    setupData("group_should_be_authorized");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    setupData("anonymous_should_be_authorized");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(session,
      Sets.newHashSet(PROJECT_ID),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_user() {
    setupData("should_return_root_project_keys_for_user");

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

    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_anonymous() {
    setupData("should_return_root_project_keys_for_anonymous");

    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // group does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_user() {
    setupData("should_return_root_project_keys_for_user");

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

    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_anonymous() {
    setupData("should_return_root_project_keys_for_anonymous");

    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(null, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // group does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_user_global_permissions() {
    setupData("should_return_user_global_permissions");

    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_group_global_permissions() {
    setupData("should_return_group_global_permissions");

    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_global_permissions_for_anonymous() {
    setupData("should_return_global_permissions_for_anonymous");

    assertThat(authorization.selectGlobalPermissions(null)).containsOnly("user", "admin");
  }

  @Test
  public void should_return_global_permissions_for_group_anyone() throws Exception {
    setupData("should_return_global_permissions_for_group_anyone");

    assertThat(authorization.selectGlobalPermissions("anyone_user")).containsOnly("user", "profileadmin");
  }

}
