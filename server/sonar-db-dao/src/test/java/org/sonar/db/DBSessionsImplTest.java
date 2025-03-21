/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.lang.Math.abs;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DBSessionsImplTest {
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final MyBatis myBatis = mock(MyBatis.class);
  private final DbSession myBatisDbSession = mock(DbSession.class);
  private final Random random = new Random();
  private final DBSessionsImpl underTest = new DBSessionsImpl(myBatis);

  @AfterEach
  void tearDown() {
    underTest.disableCaching();
  }

  @Test
  void openSession_without_caching_always_returns_a_new_regular_session_when_parameter_is_false() {
    DbSession[] expected = {mock(DbSession.class), mock(DbSession.class), mock(DbSession.class), mock(DbSession.class)};
    when(myBatis.openSession(false))
      .thenReturn(expected[0])
      .thenReturn(expected[1])
      .thenReturn(expected[2])
      .thenReturn(expected[3])
      .thenThrow(oneCallTooMuch());

    assertThat(Arrays.stream(expected).map(ignored -> underTest.openSession(false)).toList())
      .containsExactly(expected);
  }

  @Test
  void openSession_without_caching_always_returns_a_new_batch_session_when_parameter_is_true() {
    DbSession[] expected = {mock(DbSession.class), mock(DbSession.class), mock(DbSession.class), mock(DbSession.class)};
    when(myBatis.openSession(true))
      .thenReturn(expected[0])
      .thenReturn(expected[1])
      .thenReturn(expected[2])
      .thenReturn(expected[3])
      .thenThrow(oneCallTooMuch());

    assertThat(Arrays.stream(expected).map(ignored -> underTest.openSession(true)).toList())
      .containsExactly(expected);
  }

  @Test
  void openSession_with_caching_always_returns_the_same_regular_session_when_parameter_is_false() {
    DbSession expected = mock(DbSession.class);
    when(myBatis.openSession(false))
      .thenReturn(expected)
      .thenThrow(oneCallTooMuch());
    underTest.enableCaching();

    int size = 1 + abs(random.nextInt(10));
    Set<DbSession> dbSessions = IntStream.range(0, size).mapToObj(ignored -> underTest.openSession(false)).collect(Collectors.toSet());
    assertThat(dbSessions).hasSize(size);
    assertThat(getWrappedDbSessions(dbSessions))
      .hasSize(1)
      .containsOnly(expected);
  }

  @Test
  void openSession_with_caching_always_returns_the_same_batch_session_when_parameter_is_true() {
    DbSession expected = mock(DbSession.class);
    when(myBatis.openSession(true))
      .thenReturn(expected)
      .thenThrow(oneCallTooMuch());
    underTest.enableCaching();

    int size = 1 + abs(random.nextInt(10));
    Set<DbSession> dbSessions = IntStream.range(0, size).mapToObj(ignored -> underTest.openSession(true)).collect(Collectors.toSet());
    assertThat(dbSessions).hasSize(size);
    assertThat(getWrappedDbSessions(dbSessions))
      .hasSize(1)
      .containsOnly(expected);
  }

  @Test
  void openSession_with_caching_returns_a_session_per_thread() {
    boolean batchOrRegular = random.nextBoolean();
    DbSession[] expected = {mock(DbSession.class), mock(DbSession.class), mock(DbSession.class), mock(DbSession.class)};
    when(myBatis.openSession(batchOrRegular))
      .thenReturn(expected[0])
      .thenReturn(expected[1])
      .thenReturn(expected[2])
      .thenReturn(expected[3])
      .thenThrow(oneCallTooMuch());
    List<DbSession> collector = new ArrayList<>();
    Runnable runnable = () -> {
      underTest.enableCaching();
      collector.add(underTest.openSession(batchOrRegular));
      underTest.disableCaching();
    };
    Thread[] threads = {new Thread(runnable, "T1"), new Thread(runnable, "T2"), new Thread(runnable, "T3")};

    executeThreadsAndCurrent(runnable, threads);

    // verify each DbSession was closed and then reset mock for next verification
    Arrays.stream(expected).forEach(s -> {
      verify(s).close();
      reset(s);
    });
    // verify whether each thread got the expected DbSession from MyBatis
    // by verifying to which each returned DbSession delegates to
    DbSession[] dbSessions = collector.toArray(new DbSession[0]);
    for (int i = 0; i < expected.length; i++) {
      dbSessions[i].rollback();
      verify(expected[i]).rollback();

      List<DbSession> sub = new ArrayList<>(Arrays.asList(expected));
      sub.remove(expected[i]);
      sub.forEach(Mockito::verifyNoMoreInteractions);
      reset(expected);
    }
  }

  private static void executeThreadsAndCurrent(Runnable runnable, Thread[] threads) {
    Arrays.stream(threads).forEach(thread -> {
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        Throwables.propagate(e);
      }
    });
    runnable.run();
  }

  @Test
  void openSession_with_caching_returns_wrapper_of_MyBatis_DbSession_which_delegates_all_methods_but_close() {
    boolean batchOrRegular = random.nextBoolean();

    underTest.enableCaching();

    verifyFirstDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      dbSession.rollback();
      verify(myBatisDbSession).rollback();
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      boolean flag = random.nextBoolean();
      dbSession.rollback(flag);
      verify(myBatisDbSession).rollback(flag);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      dbSession.commit();
      verify(myBatisDbSession).commit();
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      boolean flag = random.nextBoolean();
      dbSession.commit(flag);
      verify(myBatisDbSession).commit(flag);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      dbSession.selectOne(str);
      verify(myBatisDbSession).selectOne(str);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object object = new Object();
      dbSession.selectOne(str, object);
      verify(myBatisDbSession).selectOne(str, object);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      dbSession.selectList(str);
      verify(myBatisDbSession).selectList(str);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object object = new Object();
      dbSession.selectList(str, object);
      verify(myBatisDbSession).selectList(str, object);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object parameter = new Object();
      RowBounds rowBounds = new RowBounds();
      dbSession.selectList(str, parameter, rowBounds);
      verify(myBatisDbSession).selectList(str, parameter, rowBounds);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      String mapKey = secure().nextAlphabetic(10);
      dbSession.selectMap(str, mapKey);
      verify(myBatisDbSession).selectMap(str, mapKey);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object parameter = new Object();
      String mapKey = secure().nextAlphabetic(10);
      dbSession.selectMap(str, parameter, mapKey);
      verify(myBatisDbSession).selectMap(str, parameter, mapKey);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object parameter = new Object();
      String mapKey = secure().nextAlphabetic(10);
      RowBounds rowBounds = new RowBounds();
      dbSession.selectMap(str, parameter, mapKey, rowBounds);
      verify(myBatisDbSession).selectMap(str, parameter, mapKey, rowBounds);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      ResultHandler handler = mock(ResultHandler.class);
      dbSession.select(str, handler);
      verify(myBatisDbSession).select(str, handler);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object parameter = new Object();
      ResultHandler handler = mock(ResultHandler.class);
      dbSession.select(str, parameter, handler);
      verify(myBatisDbSession).select(str, parameter, handler);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object parameter = new Object();
      ResultHandler handler = mock(ResultHandler.class);
      RowBounds rowBounds = new RowBounds();
      dbSession.select(str, parameter, rowBounds, handler);
      verify(myBatisDbSession).select(str, parameter, rowBounds, handler);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      dbSession.insert(str);
      verify(myBatisDbSession).insert(str);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object object = new Object();
      dbSession.insert(str, object);
      verify(myBatisDbSession).insert(str, object);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      dbSession.update(str);
      verify(myBatisDbSession).update(str);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object object = new Object();
      dbSession.update(str, object);
      verify(myBatisDbSession).update(str, object);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      dbSession.delete(str);
      verify(myBatisDbSession).delete(str);
    });
    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      String str = secure().nextAlphabetic(10);
      Object object = new Object();
      dbSession.delete(str, object);
      verify(myBatisDbSession).delete(str, object);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      dbSession.flushStatements();
      verify(myBatisDbSession).flushStatements();
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      dbSession.clearCache();
      verify(myBatisDbSession).clearCache();
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      Configuration expected = mock(Configuration.class);
      when(myBatisDbSession.getConfiguration()).thenReturn(expected);
      assertThat(dbSession.getConfiguration()).isSameAs(expected);
      verify(myBatisDbSession).getConfiguration();
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      Class<Object> clazz = Object.class;
      Object expected = new Object();
      when(myBatisDbSession.getMapper(clazz)).thenReturn(expected);
      assertThat(dbSession.getMapper(clazz)).isSameAs(expected);
      verify(myBatisDbSession).getMapper(clazz);
    });

    verifyDelegation(batchOrRegular, (myBatisDbSession, dbSession) -> {
      Connection connection = mock(Connection.class);
      when(myBatisDbSession.getConnection()).thenReturn(connection);
      assertThat(dbSession.getConnection()).isSameAs(connection);
      verify(myBatisDbSession).getConnection();
    });
  }

  @Test
  void openSession_with_caching_returns_DbSession_that_rolls_back_on_close_if_any_mutation_call_was_not_followed_by_commit_nor_rollback() throws SQLException {
    DbSession dbSession = openSessionAndDoSeveralMutatingAndNeutralCalls();

    dbSession.close();

    verify(myBatisDbSession).rollback();
  }

  @Test
  void openSession_with_caching_returns_DbSession_that_does_not_roll_back_on_close_if_any_mutation_call_was_followed_by_commit() throws SQLException {
    DbSession dbSession = openSessionAndDoSeveralMutatingAndNeutralCalls();
    COMMIT_CALLS[random.nextBoolean() ? 0 : 1].consume(dbSession);

    dbSession.close();

    verify(myBatisDbSession, times(0)).rollback();
  }

  @Test
  void openSession_with_caching_returns_DbSession_that_does_not_roll_back_on_close_if_any_mutation_call_was_followed_by_rollback_without_parameters() throws SQLException {
    DbSession dbSession = openSessionAndDoSeveralMutatingAndNeutralCalls();
    dbSession.rollback();

    dbSession.close();

    verify(myBatisDbSession, times(1)).rollback();
  }

  @Test
  void openSession_with_caching_returns_DbSession_that_does_not_roll_back_on_close_if_any_mutation_call_was_followed_by_rollback_with_parameters() throws SQLException {
    boolean force = random.nextBoolean();
    DbSession dbSession = openSessionAndDoSeveralMutatingAndNeutralCalls();
    dbSession.rollback(force);

    dbSession.close();

    verify(myBatisDbSession, times(1)).rollback(force);
    verify(myBatisDbSession, times(0)).rollback();
  }

  private DbSession openSessionAndDoSeveralMutatingAndNeutralCalls() throws SQLException {
    boolean batchOrRegular = random.nextBoolean();
    when(myBatis.openSession(batchOrRegular))
      .thenReturn(myBatisDbSession)
      .thenThrow(oneCallTooMuch());
    underTest.enableCaching();
    DbSession dbSession = underTest.openSession(batchOrRegular);

    int dirtyingCallsCount = 1 + abs(random.nextInt(5));
    int neutralCallsCount = abs(random.nextInt(5));
    int[] dirtyCallIndices = IntStream.range(0, dirtyingCallsCount).map(ignored -> abs(random.nextInt(DIRTYING_CALLS.length))).toArray();
    int[] neutralCallsIndices = IntStream.range(0, neutralCallsCount).map(ignored -> abs(random.nextInt(NEUTRAL_CALLS.length))).toArray();
    for (int index : dirtyCallIndices) {
      DIRTYING_CALLS[index].consume(dbSession);
    }
    for (int index : neutralCallsIndices) {
      NEUTRAL_CALLS[index].consume(dbSession);
    }
    return dbSession;
  }

  private static final DbSessionCaller[] DIRTYING_CALLS = {
    session -> session.insert(secure().nextAlphabetic(3)),
    session -> session.insert(secure().nextAlphabetic(2), new Object()),
    session -> session.update(secure().nextAlphabetic(3)),
    session -> session.update(secure().nextAlphabetic(3), new Object()),
    session -> session.delete(secure().nextAlphabetic(3)),
    session -> session.delete(secure().nextAlphabetic(3), new Object()),
  };

  private static final DbSessionCaller[] COMMIT_CALLS = {SqlSession::commit,
    session -> session.commit(new Random().nextBoolean()),
  };

  private static final DbSessionCaller[] NEUTRAL_CALLS = {
    session -> session.selectOne(secure().nextAlphabetic(3)),
    session -> session.selectOne(secure().nextAlphabetic(3), new Object()),
    session -> session.select(secure().nextAlphabetic(3), mock(ResultHandler.class)),
    session -> session.select(secure().nextAlphabetic(3), new Object(), mock(ResultHandler.class)),
    session -> session.select(secure().nextAlphabetic(3), new Object(), new RowBounds(), mock(ResultHandler.class)),
    session -> session.selectList(secure().nextAlphabetic(3)),
    session -> session.selectList(secure().nextAlphabetic(3), new Object()),
    session -> session.selectList(secure().nextAlphabetic(3), new Object(), new RowBounds()),
    session -> session.selectMap(secure().nextAlphabetic(3), secure().nextAlphabetic(3)),
    session -> session.selectMap(secure().nextAlphabetic(3), new Object(), secure().nextAlphabetic(3)),
    session -> session.selectMap(secure().nextAlphabetic(3), new Object(), secure().nextAlphabetic(3), new RowBounds()),
    session -> session.getMapper(Object.class), SqlSession::getConfiguration, SqlSession::getConnection, SqlSession::clearCache, SqlSession::flushStatements
  };

  private interface DbSessionCaller {
    void consume(DbSession t) throws SQLException;
  }

  @Test
  void disableCaching_does_not_open_DB_connection_if_openSession_was_never_called() {
    when(myBatis.openSession(anyBoolean()))
      .thenThrow(oneCallTooMuch());
    underTest.enableCaching();

    underTest.disableCaching();

    verifyNoMoreInteractions(myBatis);
  }

  @Test
  void disableCaching_has_no_effect_if_enabledCaching_has_not_been_called() {
    underTest.disableCaching();

    verifyNoMoreInteractions(myBatis);
  }

  @Test
  void disableCaching_does_not_fail_but_logs_if_closing_MyBatis_session_close_throws_an_exception() {
    boolean batchOrRegular = random.nextBoolean();
    IllegalStateException toBeThrown = new IllegalStateException("Faking MyBatisSession#close failing");

    when(myBatis.openSession(batchOrRegular))
      .thenReturn(myBatisDbSession)
      .thenThrow(oneCallTooMuch());
    Mockito.doThrow(toBeThrown)
      .when(myBatisDbSession).close();
    underTest.enableCaching();
    underTest.openSession(batchOrRegular);

    underTest.disableCaching();

    List<String> errorLogs = logTester.logs(Level.ERROR);
    assertThat(errorLogs)
      .hasSize(1)
      .containsOnly("Failed to close " + (batchOrRegular ? "batch" : "regular") + " connection in " + Thread.currentThread());
  }

  private void verifyFirstDelegation(boolean batchOrRegular, BiConsumer<DbSession, DbSession> r) {
    verifyDelegation(batchOrRegular, true, r);
  }

  private void verifyDelegation(boolean batchOrRegular, BiConsumer<DbSession, DbSession> r) {
    verifyDelegation(batchOrRegular, false, r);
  }

  private void verifyDelegation(boolean batchOrRegular, boolean firstCall, BiConsumer<DbSession, DbSession> r) {
    when(myBatis.openSession(batchOrRegular))
      .thenReturn(myBatisDbSession)
      .thenThrow(oneCallTooMuch());
    r.accept(myBatisDbSession, underTest.openSession(batchOrRegular));
    verify(myBatisDbSession, times(firstCall ? 1 : 0)).getSqlSession();
    verifyNoMoreInteractions(myBatisDbSession);
    reset(myBatis, myBatisDbSession);
  }

  private static IllegalStateException oneCallTooMuch() {
    return new IllegalStateException("one call too much");
  }

  private Set<DbSession> getWrappedDbSessions(Set<DbSession> dbSessions) {
    return dbSessions.stream()
      .filter(NonClosingDbSession.class::isInstance)
      .map(NonClosingDbSession.class::cast)
      .map(NonClosingDbSession::getDelegate)
      .collect(Collectors.toSet());
  }
}
