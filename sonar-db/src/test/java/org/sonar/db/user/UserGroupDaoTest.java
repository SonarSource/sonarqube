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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class UserGroupDaoTest {

  @ClassRule
  public static DbTester db = new DbTester();

  private UserGroupDao dao;
  private DbSession session;

  @Before
  public void before() {
    db.truncateTables();
    this.session = db.myBatis().openSession(false);
    this.dao = new UserGroupDao();
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void insert() {
    UserGroupDto userGroupDto = new UserGroupDto().setUserId(1L).setGroupId(2L);
    dao.insert(session, userGroupDto);
    session.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", "groups_users");
  }

  @Test
  public void delete_members_by_group_id() {
    db.prepareDbUnit(getClass(), "delete_members_by_group_id.xml");
    dao.deleteMembersByGroupId(session, 1L);
    session.commit();
    db.assertDbUnit(getClass(), "delete_members_by_group_id-result.xml", "groups_users");
  }
}
