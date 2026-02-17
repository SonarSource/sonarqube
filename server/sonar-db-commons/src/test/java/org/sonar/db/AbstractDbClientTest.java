/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonar.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractDbClientTest {

  private final Database mockDatabase = mock(Database.class);
  private final MyBatis mockMyBatis = mock(MyBatis.class);
  private final DBSessions mockDbSessions = mock(DBSessions.class);

  @Test
  void constructor_shouldStoreDependencies() {
    TestDao dao1 = new TestDao();
    TestDao2 dao2 = new TestDao2();

    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions, dao1, dao2);

    assertThat(underTest.getDatabase()).isSameAs(mockDatabase);
    assertThat(underTest.getMyBatis()).isSameAs(mockMyBatis);
  }

  @Test
  void openSession_whenBatchIsTrue_shouldDelegateToDbSessions() {
    DbSession expectedSession = mock(DbSession.class);
    when(mockDbSessions.openSession(true)).thenReturn(expectedSession);

    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions);
    DbSession actualSession = underTest.openSession(true);

    assertThat(actualSession).isSameAs(expectedSession);
    verify(mockDbSessions).openSession(true);
  }

  @Test
  void openSession_whenBatchIsFalse_shouldDelegateToDbSessions() {
    DbSession expectedSession = mock(DbSession.class);
    when(mockDbSessions.openSession(false)).thenReturn(expectedSession);

    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions);
    DbSession actualSession = underTest.openSession(false);

    assertThat(actualSession).isSameAs(expectedSession);
    verify(mockDbSessions).openSession(false);
  }

  @Test
  void getDatabase_shouldReturnDatabase() {
    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions);

    assertThat(underTest.getDatabase()).isSameAs(mockDatabase);
  }

  @Test
  void getMyBatis_shouldReturnMyBatis() {
    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions);

    assertThat(underTest.getMyBatis()).isSameAs(mockMyBatis);
  }

  @Test
  void getDao_shouldReturnDaoByClass() {
    TestDao dao1 = new TestDao();
    TestDao2 dao2 = new TestDao2();

    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions, dao1, dao2);

    assertThat(underTest.getTestDao()).isSameAs(dao1);
    assertThat(underTest.getTestDao2()).isSameAs(dao2);
  }

  @Test
  void getDao_shouldReturnNullForUnknownDao() {
    TestDao dao1 = new TestDao();

    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions, dao1);

    assertThat(underTest.getTestDao2()).isNull();
  }

  @Test
  void constructor_whenNoDaos_shouldWork() {
    TestDbClient underTest = new TestDbClient(mockDatabase, mockMyBatis, mockDbSessions);

    assertThat(underTest.getTestDao()).isNull();
    assertThat(underTest.getTestDao2()).isNull();
  }

  // Test implementation of AbstractDbClient
  private static class TestDbClient extends AbstractDbClient {
    protected TestDbClient(Database database, MyBatis myBatis, DBSessions dbSessions, Dao... daos) {
      super(database, myBatis, dbSessions, daos);
    }

    public TestDao getTestDao() {
      return getDao(TestDao.class);
    }

    public TestDao2 getTestDao2() {
      return getDao(TestDao2.class);
    }
  }

  // Test DAO implementations
  private static class TestDao implements Dao {
  }

  private static class TestDao2 implements Dao {
  }
}
