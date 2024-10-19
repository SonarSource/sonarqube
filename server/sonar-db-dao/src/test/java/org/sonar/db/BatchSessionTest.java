/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BatchSessionTest {
  @Test
  void shouldCommitWhenReachingBatchSize() {
    DbSession mybatisSession = mock(DbSession.class);
    BatchSession session = new BatchSession(mybatisSession, 10);

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
  void shouldCommitWhenReachingBatchSizeWithoutCommits() {
    DbSession mybatisSession = mock(DbSession.class);
    BatchSession session = new BatchSession(mybatisSession, 10);

    for (int i = 0; i < 9; i++) {
      session.delete("delete something");
      verify(mybatisSession, never()).commit();
      verify(mybatisSession, never()).commit(anyBoolean());
    }
    session.delete("delete something");
    verify(mybatisSession).commit();
    session.close();
  }

  @Test
  void shouldResetCounterAfterCommit() {
    DbSession mybatisSession = mock(DbSession.class);
    BatchSession session = new BatchSession(mybatisSession, 10);

    for (int i = 0; i < 35; i++) {
      session.insert("id" + i);
    }
    verify(mybatisSession, times(3)).commit();
    session.close();
  }
}
