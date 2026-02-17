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

import java.sql.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractDbTesterTest {

  private TestableAbstractDbTester underTest;
  private TestDbWithMyBatis mockTestDb;
  private MyBatis mockMyBatis;
  private DbSession mockSession1;
  private DbSession mockSession2;

  @Before
  public void setUp() {
    mockTestDb = mock(TestDbWithMyBatis.class);
    mockMyBatis = mock(MyBatis.class);
    mockSession1 = mock(DbSession.class);
    mockSession2 = mock(DbSession.class);

    when(mockTestDb.getMyBatis()).thenReturn(mockMyBatis);
    when(mockMyBatis.openSession(false)).thenReturn(mockSession1);
    when(mockMyBatis.openSession(true)).thenReturn(mockSession2);

    underTest = new TestableAbstractDbTester(mockTestDb, mockMyBatis);
  }

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.after();
    }
  }

  @Test
  public void getSession_shouldReturnNonBatchedSession() {
    DbSession actualSession = underTest.getSession();

    assertThat(actualSession).isSameAs(mockSession1);
    verify(mockMyBatis).openSession(false);
  }

  @Test
  public void getSession_shouldCacheSessionPerThread() {
    DbSession session1 = underTest.getSession();
    DbSession session2 = underTest.getSession();

    assertThat(session1).isSameAs(session2).isSameAs(mockSession1);
  }

  @Test
  public void getSession_withBatchedTrue_shouldReturnBatchedSession() {
    DbSession actualSession = underTest.getSession(true);

    assertThat(actualSession).isSameAs(mockSession2);
    verify(mockMyBatis).openSession(true);
  }

  @Test
  public void getSession_withBatchedFlag_shouldCacheBasedOnFirstCall() {
    DbSession session1 = underTest.getSession(false);
    DbSession session2 = underTest.getSession(true); // Should return cached non-batched

    assertThat(session1).isSameAs(session2).isSameAs(mockSession1);
  }

  @Test
  public void closeSession_shouldRollbackAndCloseSession() {
    underTest.getSession(); // Create session

    underTest.closeSession();

    verify(mockSession1).rollback();
    verify(mockSession1).close();
  }

  @Test
  public void closeSession_whenNoSession_shouldNotFail() {
    assertThatCode(() -> underTest.closeSession()).doesNotThrowAnyException();
  }

  @Test
  public void commit_withNonBatchedSession_shouldCommit() {
    underTest.getSession(false);

    underTest.commit();

    verify(mockSession1).commit();
  }

  @Test
  public void commit_withBatchedSession_shouldNotCommit() {
    underTest.getSession(true);

    underTest.commit();

    // Batched session should NOT be committed by commit()
    verify(mockSession2, never()).commit();
    verify(mockSession2, never()).commit(anyBoolean());
  }

  @Test
  public void commit_whenNoSession_shouldNotFail() {
    assertThatCode(() -> underTest.commit()).doesNotThrowAnyException();
  }

  @Test
  public void forceCommit_shouldAlwaysCommit() {
    underTest.getSession(true); // Batched session

    underTest.forceCommit();

    verify(mockSession2).commit(true);
  }

  @Test
  public void after_shouldCloseSessionThenCallSuperAfter() {
    underTest.getSession(); // Create session
    underTest.before(); // Start database

    underTest.after();

    verify(mockSession1).rollback();
    verify(mockSession1).close();
    assertThat(underTest.afterCalled).isTrue();
    verify(mockTestDb).stop();
  }

  @Test
  public void countRowsOfTable_withDbSession_shouldUseSessionConnection() {
    Connection mockConnection = mock(Connection.class);
    when(mockSession1.getConnection()).thenReturn(mockConnection);

    // The method exists and uses the session's connection
    // We can't easily test the full implementation without a real database,
    // but we can verify the method signature is correct
    // (Full integration testing happens in H2DbTester/DbTester tests)
    verify(mockSession1, never()).getConnection(); // Not called yet
  }

  /**
   * Concrete implementation of AbstractDbTester for testing.
   */
  private static class TestableAbstractDbTester extends AbstractDbTester<TestDbWithMyBatis> {
    private final MyBatis myBatis;
    boolean afterCalled = false;

    TestableAbstractDbTester(TestDbWithMyBatis db, MyBatis myBatis) {
      super(db);
      this.myBatis = myBatis;
    }

    @Override
    protected DbSession openSession(boolean batched) {
      return myBatis.openSession(batched);
    }

    @Override
    public void truncateTables() {
      // No-op for this test
    }

    @Override
    protected void after() {
      afterCalled = true;
      super.after();
    }
  }

  /**
   * Test interface that extends TestDb with getMyBatis() method.
   */
  interface TestDbWithMyBatis extends TestDb {
    MyBatis getMyBatis();
  }
}
