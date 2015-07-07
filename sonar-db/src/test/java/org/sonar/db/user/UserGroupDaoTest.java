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

package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class UserGroupDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  UserGroupDao dao = dbTester.getDbClient().userGroupDao();

  @Test
  public void insert() {
    dbTester.truncateTables();

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
