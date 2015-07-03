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
package org.sonar.db;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.sonar.db.deprecated.ClusterAction;
import org.sonar.db.deprecated.WorkQueue;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BatchSessionTest {
  @Test
  public void shouldCommitWhenReachingBatchSize() {
    SqlSession mybatisSession = mock(SqlSession.class);
    WorkQueue<?> queue = mock(WorkQueue.class);
    BatchSession session = new BatchSession(queue, mybatisSession, 10);

    for (int i = 0; i < 9; i++) {
      session.insert("id" + i);
      verify(mybatisSession).insert("id" + i);
      verify(mybatisSession, never()).commit();
      verify(mybatisSession, never()).commit(anyBoolean());
    }
    session.insert("id9");
    verify(mybatisSession).commit();
    session.close();
  }

  @Test
  public void shouldCommitWhenReachingBatchSizeWithoutCommits() {
    SqlSession mybatisSession = mock(SqlSession.class);
    WorkQueue<?> queue = mock(WorkQueue.class);
    BatchSession session = new BatchSession(queue, mybatisSession, 10);

    ClusterAction action = new ClusterAction() {
      @Override
      public Object call() throws Exception {
        return null;
      }
    };

    for (int i = 0; i < 9; i++) {
      session.enqueue(action);
      verify(mybatisSession, never()).commit();
      verify(mybatisSession, never()).commit(anyBoolean());
    }
    session.enqueue(action);
    verify(mybatisSession).commit();
    session.close();
  }

  @Test
  public void shouldResetCounterAfterCommit() {
    SqlSession mybatisSession = mock(SqlSession.class);
    WorkQueue<?> queue = mock(WorkQueue.class);
    BatchSession session = new BatchSession(queue, mybatisSession, 10);

    for (int i = 0; i < 35; i++) {
      session.insert("id" + i);
    }
    verify(mybatisSession, times(3)).commit();
    session.close();
  }
}
