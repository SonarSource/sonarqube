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
package org.sonar.server.issue.index;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.sql.Connection;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueAuthorizationDaoTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  IssueAuthorizationDao dao = new IssueAuthorizationDao();
  DbClient client;
  Connection connection;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    client = new DbClient(dbTester.database(), dbTester.myBatis());
    connection = dbTester.openConnection();
  }

  @After
  public void tearDown() throws Exception {
    DbUtils.closeQuietly(connection);
  }

  @Test
  public void select_all() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    Collection<IssueAuthorizationDao.Dto> dtos = dao.selectAfterDate(client, connection, 0L);
    assertThat(dtos).hasSize(2);

    IssueAuthorizationDao.Dto abc = Iterables.find(dtos, new ProjectPredicate("ABC"));
    assertThat(abc.getGroups()).containsOnly("Anyone", "devs");
    assertThat(abc.getUsers()).containsOnly("user1");
    assertThat(abc.getUpdatedAt()).isNotNull();

    IssueAuthorizationDao.Dto def = Iterables.find(dtos, new ProjectPredicate("DEF"));
    assertThat(def.getGroups()).containsOnly("Anyone");
    assertThat(def.getUsers()).containsOnly("user1", "user2");
    assertThat(def.getUpdatedAt()).isNotNull();
  }

  @Test
  public void select_after_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    Collection<IssueAuthorizationDao.Dto> dtos = dao.selectAfterDate(client, connection, 1500000000L);

    // only project DEF was updated in this period
    assertThat(dtos).hasSize(1);
    IssueAuthorizationDao.Dto def = Iterables.find(dtos, new ProjectPredicate("DEF"));
    assertThat(def).isNotNull();
    assertThat(def.getGroups()).containsOnly("Anyone");
    assertThat(def.getUsers()).containsOnly("user1", "user2");
  }

  @Test
  public void no_authorization() throws Exception {
    dbTester.prepareDbUnit(getClass(), "no_authorization.xml");

    Collection<IssueAuthorizationDao.Dto> dtos = dao.selectAfterDate(client, connection, 0L);

    assertThat(dtos).hasSize(1);
    IssueAuthorizationDao.Dto abc = Iterables.find(dtos, new ProjectPredicate("ABC"));
    assertThat(abc.getGroups()).isEmpty();
    assertThat(abc.getUsers()).isEmpty();
    assertThat(abc.getUpdatedAt()).isNotNull();
  }

  private static class ProjectPredicate implements Predicate<IssueAuthorizationDao.Dto> {

    private final String projectUuid;

    ProjectPredicate(String projectUuid) {
      this.projectUuid = projectUuid;
    }

    @Override
    public boolean apply(IssueAuthorizationDao.Dto input) {
      return input.getProjectUuid().equals(projectUuid);
    }

    @Override
    public boolean equals(Object object) {
      return true;
    }
  }
}
