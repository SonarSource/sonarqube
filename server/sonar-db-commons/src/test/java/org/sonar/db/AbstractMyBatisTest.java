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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.dialect.Dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractMyBatisTest {

  private static final int DEFAULT_FETCH_SIZE = 200;
  private static final String TEST_SQL = "SELECT * FROM test";

  private final Database mockDatabase = mock(Database.class);
  private final Dialect mockDialect = mock(Dialect.class);
  private final DataSource mockDataSource = mock(DataSource.class);
  private final Connection mockConnection = mock(Connection.class);

  @BeforeEach
  void setUp() throws SQLException {
    when(mockDatabase.getDialect()).thenReturn(mockDialect);
    when(mockDatabase.getDataSource()).thenReturn(mockDataSource);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockDialect.getId()).thenReturn("h2");
    when(mockDialect.getTrueSqlValue()).thenReturn("true");
    when(mockDialect.getFalseSqlValue()).thenReturn("false");
    when(mockDialect.getSqlFromDual()).thenReturn("");
    when(mockDialect.getScrollDefaultFetchSize()).thenReturn(DEFAULT_FETCH_SIZE);
  }

  @Test
  void constructor_withoutExtensions_shouldWork() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);

    assertThat(underTest.getDatabase()).isSameAs(mockDatabase);
  }

  @Test
  void constructor_withNullExtensions_shouldWork() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase, null);

    assertThat(underTest.getDatabase()).isSameAs(mockDatabase);
  }

  @Test
  void constructor_withExtensions_shouldWork() {
    BaseMyBatisConfExtension extension = new TestMyBatisConfExtension();

    TestMyBatis underTest = new TestMyBatis(mockDatabase, new BaseMyBatisConfExtension[]{extension});

    assertThat(underTest.getDatabase()).isSameAs(mockDatabase);
  }

  @Test
  void start_shouldCreateSessionFactory() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    assertThat(underTest.getSessionFactory()).isNotNull();
  }

  @Test
  void start_shouldCallConfigureBeforeMappersOnStart() {
    TestMyBatisWithHooks underTest = new TestMyBatisWithHooks(mockDatabase);
    underTest.start();

    assertThat(underTest.beforeMappersCalled.get()).isTrue();
  }

  @Test
  void start_shouldCallConfigureAfterMappersOnStart() {
    TestMyBatisWithHooks underTest = new TestMyBatisWithHooks(mockDatabase);
    underTest.start();

    assertThat(underTest.afterMappersCalled.get()).isTrue();
  }

  @Test
  void start_shouldCallHooksInCorrectOrder() {
    TestMyBatisWithHooks underTest = new TestMyBatisWithHooks(mockDatabase);
    underTest.start();

    // Both hooks should be called
    assertThat(underTest.beforeMappersCalled.get()).isTrue();
    assertThat(underTest.afterMappersCalled.get()).isTrue();
    // beforeMappers should be called before afterMappers
    assertThat(underTest.beforeMappersCalledFirst).isTrue();
  }

  @Test
  void start_whenExtensions_shouldLoadExtensionAliases() {
    TestMyBatisConfExtension extension = new TestMyBatisConfExtension();
    TestMyBatis underTest = new TestMyBatis(mockDatabase, new BaseMyBatisConfExtension[]{extension});
    underTest.start();

    assertThat(extension.loadAliasesCalled).isTrue();
  }

  @Test
  void start_whenExtensions_shouldLoadExtensionMappers() {
    TestMyBatisConfExtension extension = new TestMyBatisConfExtension();
    TestMyBatis underTest = new TestMyBatis(mockDatabase, new BaseMyBatisConfExtension[]{extension});
    underTest.start();

    assertThat(extension.getMapperClassesCalled).isTrue();
  }

  @Test
  void openSession_whenBatchIsFalse_shouldReturnDbSessionImpl() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession actualSession = underTest.openSession(false);

    assertThat(actualSession)
      .isNotNull()
      .isInstanceOf(DbSessionImpl.class);
    actualSession.close();
  }

  @Test
  void openSession_whenBatchIsTrue_shouldReturnBatchSession() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession actualSession = underTest.openSession(true);

    assertThat(actualSession)
      .isNotNull()
      .isInstanceOf(BatchSession.class);
    actualSession.close();
  }

  @Test
  void newScrollingSelectStatement_shouldUseDialectFetchSize() throws SQLException {
    PreparedStatement mockStmt = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mockStmt);

    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession session = underTest.openSession(false);
    PreparedStatement actualStmt = underTest.newScrollingSelectStatement(session, TEST_SQL);

    verify(mockStmt).setFetchSize(DEFAULT_FETCH_SIZE);
    assertThat(actualStmt).isSameAs(mockStmt);
    session.close();
  }

  @Test
  void newScrollingSelectStatement_shouldCreateForwardOnlyStatement() throws SQLException {
    PreparedStatement mockStmt = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mockStmt);

    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession session = underTest.openSession(false);
    underTest.newScrollingSelectStatement(session, TEST_SQL);

    verify(mockConnection).prepareStatement(TEST_SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    session.close();
  }

  @Test
  void newScrollingSelectStatement_whenSqlException_shouldThrow() throws SQLException {
    when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt()))
      .thenThrow(new SQLException("Test exception"));

    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession session = underTest.openSession(false);
    assertThatThrownBy(() -> underTest.newScrollingSelectStatement(session, TEST_SQL))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to create SQL statement: " + TEST_SQL)
      .hasCauseInstanceOf(SQLException.class);

    session.close();
  }

  @Test
  void getSessionFactory_shouldReturnSessionFactoryAfterStart() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    SqlSessionFactory actualSessionFactory = underTest.getSessionFactory();

    assertThat(actualSessionFactory).isNotNull();
  }

  @Test
  void multipleSessions_canBeOpened() {
    TestMyBatis underTest = new TestMyBatis(mockDatabase);
    underTest.start();

    DbSession session1 = underTest.openSession(false);
    DbSession session2 = underTest.openSession(true);
    DbSession session3 = underTest.openSession(false);

    assertThat(session1)
      .isNotSameAs(session2)
      .isNotSameAs(session3);
    assertThat(session2).isNotSameAs(session3);

    session1.close();
    session2.close();
    session3.close();
  }

  private static class TestMyBatis extends AbstractMyBatis {
    protected TestMyBatis(Database database) {
      super(database);
    }

    protected TestMyBatis(Database database, BaseMyBatisConfExtension[] confExtensions) {
      super(database, confExtensions);
    }
  }

  private static class TestMyBatisWithHooks extends AbstractMyBatis {
    final AtomicBoolean beforeMappersCalled = new AtomicBoolean(false);
    final AtomicBoolean afterMappersCalled = new AtomicBoolean(false);
    boolean beforeMappersCalledFirst = false;

    protected TestMyBatisWithHooks(Database database) {
      super(database);
    }

    @Override
    protected void configureBeforeMappersOnStart(MyBatisConfBuilder confBuilder) {
      if (!afterMappersCalled.get()) {
        beforeMappersCalledFirst = true;
      }
      beforeMappersCalled.set(true);
    }

    @Override
    protected void configureAfterMappersOnStart(MyBatisConfBuilder confBuilder) {
      afterMappersCalled.set(true);
    }
  }
  
  private static class TestMyBatisConfExtension implements BaseMyBatisConfExtension {
    boolean loadAliasesCalled = false;
    boolean getMapperClassesCalled = false;

    @Override
    public void loadAliases(LoadAliasContext context) {
      loadAliasesCalled = true;
    }

    @Override
    public Stream<Class<?>> getMapperClasses() {
      getMapperClassesCalled = true;
      return Stream.empty();
    }
  }
}
