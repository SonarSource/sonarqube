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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;


public class UserGroupDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  UserGroupDao dao = dbTester.getDbClient().userGroupDao();

  @Test
  public void insert() {
    UserGroupDto userGroupDto = new UserGroupDto().setUserId(1L).setGroupId(2L);
    dao.insert(dbTester.getSession(), userGroupDto);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", "groups_users");
  }

  @Test
  public void delete_members_by_group_id() {
    dbTester.prepareDbUnit(getClass(), "delete_members_by_group_id.xml");
    dao.deleteMembersByGroupId(dbTester.getSession(), 1L);
    dbTester.getSession().commit();
    dbTester.assertDbUnit(getClass(), "delete_members_by_group_id-result.xml", "groups_users");
  }
}
