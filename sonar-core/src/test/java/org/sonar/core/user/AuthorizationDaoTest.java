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
package org.sonar.core.user;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class AuthorizationDaoTest extends AbstractDaoTestCase {

  private static final int USER = 100;
  private static final int PROJECT = 300, PACKAGE = 301, FILE = 302, FILE_IN_OTHER_PROJECT = 999;

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    setupData("user_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Integer> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // user does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    setupData("group_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Integer> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void group_should_have_global_authorization() {
    // user is in a group that has authorized access to all projects
    setupData("group_should_have_global_authorization");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Integer> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    setupData("anonymous_should_be_authorized");

    AuthorizationDao authorization = new AuthorizationDao(getMyBatis());
    Set<Integer> componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT, PACKAGE, FILE);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedComponentIds(
      Sets.<Integer>newHashSet(PROJECT, PACKAGE, FILE, FILE_IN_OTHER_PROJECT),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }
}
