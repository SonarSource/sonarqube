/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.user;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public GroupDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public GroupDto insertGroup() {
    return insertGroup(newGroupDto());
  }

  public GroupDto insertGroup(GroupDto groupDto) {
    GroupDto updatedGroup = dbClient.groupDao().insert(dbSession, groupDto);
    db.commit();

    return updatedGroup;
  }

  public UserGroupDto addUserToGroup(long userId, long groupId) {
    UserGroupDto dto = dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupId(groupId).setUserId(userId));
    db.commit();

    return dto;
  }
}
