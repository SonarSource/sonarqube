/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DbSessionImplTest {
  private SqlSession sqlSessionMock = mock(SqlSession.class);

  private DbSessionImpl underTest = new DbSessionImpl(sqlSessionMock);

  @Test
  public void all_methods_to_wrapped_SqlSession() {
    Random random = new Random();
    boolean randomBoolean = random.nextBoolean();
    int randomInt = random.nextInt(200);
    String randomStatement = randomAlphabetic(10);
    Object randomParameter = new Object();
    Cursor<Object> mockCursor = mock(Cursor.class);
    RowBounds rowBounds = new RowBounds();
    Object randomObject = new Object();
    List<Object> randomList = new ArrayList<>();
    Map<Object, Object> randomMap = new HashMap<>();
    String randomMapKey = randomAlphabetic(10);
    ResultHandler randomResultHandler = resultContext -> {
      // don't care
    };
    List<BatchResult> randomBatchResults = new ArrayList<>();
    Configuration randomConfiguration = new Configuration();

    verifyDelegation(DbSessionImpl::commit, s -> verify(s).commit());
    verifyDelegation(t -> t.commit(randomBoolean), s -> verify(s).commit(randomBoolean));

    verifyDelegation(
      sqlSession -> when(sqlSession.selectCursor(randomStatement)).thenReturn(mockCursor),
      dbSession -> dbSession.selectCursor(randomStatement),
      sqlSession -> {
        verify(sqlSession).selectCursor(randomStatement);
        return mockCursor;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectCursor(randomStatement, randomParameter)).thenReturn(mockCursor),
      dbSession -> dbSession.selectCursor(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).selectCursor(randomStatement, randomParameter);
        return mockCursor;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectCursor(randomStatement, randomParameter, rowBounds)).thenReturn(mockCursor),
      dbSession -> dbSession.selectCursor(randomStatement, randomParameter, rowBounds),
      sqlSession -> {
        verify(sqlSessionMock).selectCursor(randomStatement, randomParameter, rowBounds);
        return mockCursor;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.selectOne(randomStatement)).thenReturn(randomObject),
      dbSession -> dbSession.selectOne(randomStatement),
      sqlSession -> {
        verify(sqlSession).selectOne(randomStatement);
        return randomObject;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectOne(randomStatement, randomParameter)).thenReturn(randomObject),
      dbSession -> dbSession.selectOne(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).selectOne(randomStatement, randomParameter);
        return randomObject;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.selectList(randomStatement)).thenReturn(randomList),
      dbSession -> dbSession.selectList(randomStatement),
      sqlSession -> {
        verify(sqlSession).selectList(randomStatement);
        return randomList;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectList(randomStatement, randomParameter)).thenReturn(randomList),
      dbSession -> dbSession.selectList(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).selectList(randomStatement, randomParameter);
        return randomList;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectList(randomStatement, randomParameter, rowBounds)).thenReturn(randomList),
      dbSession -> dbSession.selectList(randomStatement, randomParameter, rowBounds),
      sqlSession -> {
        verify(sqlSessionMock).selectList(randomStatement, randomParameter, rowBounds);
        return randomList;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.selectMap(randomStatement, randomMapKey)).thenReturn(randomMap),
      dbSession -> dbSession.selectMap(randomStatement, randomMapKey),
      sqlSession -> {
        verify(sqlSession).selectMap(randomStatement, randomMapKey);
        return randomMap;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectMap(randomStatement, randomParameter, randomMapKey)).thenReturn(randomMap),
      dbSession -> dbSession.selectMap(randomStatement, randomParameter, randomMapKey),
      sqlSession -> {
        verify(sqlSessionMock).selectMap(randomStatement, randomParameter, randomMapKey);
        return randomMap;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.selectMap(randomStatement, randomParameter, randomMapKey, rowBounds)).thenReturn(randomMap),
      dbSession -> dbSession.selectMap(randomStatement, randomParameter, randomMapKey, rowBounds),
      sqlSession -> {
        verify(sqlSessionMock).selectMap(randomStatement, randomParameter, randomMapKey, rowBounds);
        return randomMap;
      });

    verifyDelegation(
      dbSession -> dbSession.select(randomStatement, randomResultHandler),
      sqlSession -> verify(sqlSessionMock).select(randomStatement, randomResultHandler));
    verifyDelegation(
      dbSession -> dbSession.select(randomStatement, randomParameter, randomResultHandler),
      sqlSession -> verify(sqlSession).select(randomStatement, randomParameter, randomResultHandler));
    verifyDelegation(
      dbSession -> dbSession.select(randomStatement, randomParameter, rowBounds, randomResultHandler),
      sqlSession -> verify(sqlSessionMock).select(randomStatement, randomParameter, rowBounds, randomResultHandler));

    verifyDelegation(
      sqlSession -> when(sqlSession.insert(randomStatement)).thenReturn(randomInt),
      dbSession -> dbSession.insert(randomStatement),
      sqlSession -> {
        verify(sqlSession).insert(randomStatement);
        return randomInt;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.insert(randomStatement, randomParameter)).thenReturn(randomInt),
      dbSession -> dbSession.insert(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).insert(randomStatement, randomParameter);
        return randomInt;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.update(randomStatement)).thenReturn(randomInt),
      dbSession -> dbSession.update(randomStatement),
      sqlSession -> {
        verify(sqlSession).update(randomStatement);
        return randomInt;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.update(randomStatement, randomParameter)).thenReturn(randomInt),
      dbSession -> dbSession.update(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).update(randomStatement, randomParameter);
        return randomInt;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.delete(randomStatement)).thenReturn(randomInt),
      dbSession -> dbSession.delete(randomStatement),
      sqlSession -> {
        verify(sqlSession).delete(randomStatement);
        return randomInt;
      });
    verifyDelegation(
      sqlSession -> when(sqlSession.delete(randomStatement, randomParameter)).thenReturn(randomInt),
      dbSession -> dbSession.delete(randomStatement, randomParameter),
      sqlSession -> {
        verify(sqlSessionMock).delete(randomStatement, randomParameter);
        return randomInt;
      });

    verifyDelegation(DbSessionImpl::rollback, s -> verify(s).rollback());
    verifyDelegation(t -> t.rollback(randomBoolean), s -> verify(s).rollback(randomBoolean));

    verifyDelegation(
      sqlSession -> when(sqlSession.flushStatements()).thenReturn(randomBatchResults),
      DbSessionImpl::flushStatements,
      sqlSession -> {
        verify(sqlSession).flushStatements();
        return randomBatchResults;
      });

    verifyDelegation(DbSessionImpl::close, s -> verify(s).close());

    verifyDelegation(DbSessionImpl::clearCache, s -> verify(s).clearCache());

    verifyDelegation(
      sqlSession -> when(sqlSession.getConfiguration()).thenReturn(randomConfiguration),
      DbSessionImpl::getConfiguration,
      sqlSession -> {
        verify(sqlSession).getConfiguration();
        return randomConfiguration;
      });

    verifyDelegation(
      sqlSession -> when(sqlSession.getMapper(DbSessionImplTest.class)).thenReturn(DbSessionImplTest.this),
      dbSession -> dbSession.getMapper(DbSessionImplTest.class),
      sqlSession -> {
        verify(sqlSession).getMapper(DbSessionImplTest.class);
        return DbSessionImplTest.this;
      });

    verifyDelegation(DbSessionImpl::getConnection, s -> verify(s).getConnection());
  }

  @Test
  public void getSqlSession_returns_wrapped_SqlSession_object() {
    assertThat(underTest.getSqlSession()).isSameAs(sqlSessionMock);
  }

  private void verifyDelegation(Consumer<DbSessionImpl> t, Consumer<SqlSession> s) {
    reset(sqlSessionMock);
    t.accept(underTest);
    s.accept(sqlSessionMock);
    verifyNoMoreInteractions(sqlSessionMock);
  }

  private <T> void verifyDelegation(Consumer<SqlSession> prepare, Function<DbSessionImpl, T> t, Function<SqlSession, T> s) {
    reset(sqlSessionMock);
    prepare.accept(sqlSessionMock);
    T value = t.apply(underTest);
    T expected = s.apply(sqlSessionMock);
    verifyNoMoreInteractions(sqlSessionMock);
    if (expected instanceof Number) {
      assertThat(value).isEqualTo(expected);
    } else {
      assertThat(value).isSameAs(expected);
    }
  }
}
