/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.util;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.util.GlobalLockManager.DEFAULT_LOCK_DURATION_SECONDS;

public class GlobalLockManagerTest {

  private final System2 system2 = mock(System2.class);

  @Rule
  public final DbTester dbTester = DbTester.create(system2);

  private final GlobalLockManager underTest = new GlobalLockManager(dbTester.getDbClient());

  @Test
  public void tryLock_succeeds_when_created_for_the_first_time() {
    assertThat(underTest.tryLock("newName")).isTrue();
  }

  @Test
  public void tryLock_fails_when_previous_lock_is_too_recent() {
    String name = "newName";
    assertThat(underTest.tryLock(name)).isTrue();
    assertThat(underTest.tryLock(name)).isFalse();
  }

  @Test
  public void tryLock_succeeds_when_previous_lock_is_old_enough() {
    String name = "newName";
    long firstLock = 0;
    long longEnoughAfterFirstLock = firstLock + DEFAULT_LOCK_DURATION_SECONDS * 1000;
    long notLongEnoughAfterFirstLock = longEnoughAfterFirstLock - 1;

    when(system2.now()).thenReturn(firstLock);
    assertThat(underTest.tryLock(name)).isTrue();

    when(system2.now()).thenReturn(notLongEnoughAfterFirstLock);
    assertThat(underTest.tryLock(name)).isFalse();

    when(system2.now()).thenReturn(longEnoughAfterFirstLock);
    assertThat(underTest.tryLock(name)).isTrue();
  }

  @Test
  public void locks_with_different_name_are_independent() {
    assertThat(underTest.tryLock("newName1")).isTrue();
    assertThat(underTest.tryLock("newName2")).isTrue();
  }

}
