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

package org.sonar.server.group;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.user.GroupMembership;
import org.sonar.core.user.GroupMembershipDao;
import org.sonar.core.user.UserDao;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Use Test with DB because there's no IT on this feature for the moment
 */
public class InternalGroupMembershipQueryServiceTest extends AbstractDaoTestCase {

  private InternalGroupMembershipQueryService service;

  @Before
  public void before() throws Exception {
    GroupMembershipDao groupMembershipDao = new GroupMembershipDao(getMyBatis());
    UserDao userDao = new UserDao(getMyBatis());
    service = new InternalGroupMembershipQueryService(userDao, groupMembershipDao);
  }

  @Test
  public void find_all_member_groups() {
    setupData("shared");

    Map<String, Object> params = newHashMap();
    params.put("user", "user1");
    params.put("selected", "all");

    List<GroupMembership> result = service.find(params);
    assertThat(result).hasSize(3);
    check(result.get(0), "sonar-administrators", false);
    check(result.get(1), "sonar-reviewers", false);
    check(result.get(2), "sonar-users", true);
  }

  @Test
  public void find_member_groups() {
    setupData("shared");

    Map<String, Object> params = newHashMap();
    params.put("user", "user1");
    params.put("selected", "selected");

    List<GroupMembership> result = service.find(params);
    assertThat(result).hasSize(1);
    check(result.get(0), "sonar-users", true);
  }

  @Test
  public void find_not_member_groups() {
    setupData("shared");

    Map<String, Object> params = newHashMap();
    params.put("user", "user1");
    params.put("selected", "deselected");

    List<GroupMembership> result = service.find(params);
    assertThat(result).hasSize(2);
    check(result.get(0), "sonar-administrators", false);
    check(result.get(1), "sonar-reviewers", false);
  }

  @Test
  public void fail_if_user_not_found() {
    setupData("shared");

    Map<String, Object> params = newHashMap();
    params.put("user", "user_not_existing");
    params.put("selected", "all");

    try {
      service.find(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("User 'user_not_existing' does not exists.");
    }
  }

  private void check(GroupMembership groupMembership, String expectedName, boolean isMember) {
    assertThat(groupMembership.name()).isEqualTo(expectedName);
    assertThat(groupMembership.isMember()).isEqualTo(isMember);
  }
}
