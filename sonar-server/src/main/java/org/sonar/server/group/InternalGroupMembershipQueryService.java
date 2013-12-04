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

import org.sonar.api.ServerComponent;
import org.sonar.core.user.*;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used by ruby code <pre>Internal.groupmembership</pre>
 */
public class InternalGroupMembershipQueryService implements ServerComponent {

  private final UserDao userDao;
  private final GroupMembershipDao groupMembershipDao;

  public InternalGroupMembershipQueryService(UserDao userDao, GroupMembershipDao groupMembershipDao) {
    this.userDao = userDao;
    this.groupMembershipDao = groupMembershipDao;
  }

  public List<GroupMembership> find(Map<String, String> params) {
    List<GroupMembership> groupMemberships = newArrayList();
    String user = user(params);
    UserDto userDto = userDao.selectActiveUserByLogin(user);
    if (userDto == null) {
      throw new NotFoundException("User '"+ user +"' does not exists.");
    }
    List<GroupMembershipDto> dtos = groupMembershipDao.selectGroups(parseQuery(params, userDto.getId()));
    for (GroupMembershipDto dto : dtos){
      groupMemberships.add(dto.toDefaultGroupMembership());
    }
    return groupMemberships;
  }

  private GroupMembershipQuery parseQuery(Map<String, String> params, Long userId) {
    GroupMembershipQuery.Builder builder = GroupMembershipQuery.builder();
    builder.memberShip(memberShip(params));
    builder.searchText(params.get("query"));
    builder.userId(userId);
    return builder.build();
  }

  private String user(Map<String, String> params){
    return params.get("user");
  }

  private String memberShip(Map<String, String> params){
    String selected = params.get("selected");
    if ("selected".equals(selected)) {
      return GroupMembershipQuery.MEMBER_ONLY;
    } else if ("deselected".equals(selected)) {
      return GroupMembershipQuery.NOT_MEMBER;
    } else {
      return GroupMembershipQuery.ALL;
    }
  }
}
