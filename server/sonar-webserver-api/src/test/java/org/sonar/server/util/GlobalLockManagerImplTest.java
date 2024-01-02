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
package org.sonar.server.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.InternalPropertiesDao;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.InternalPropertiesDao.LOCK_NAME_MAX_LENGTH;
import static org.sonar.server.util.GlobalLockManager.DEFAULT_LOCK_DURATION_SECONDS;

@RunWith(DataProviderRunner.class)
public class GlobalLockManagerImplTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final InternalPropertiesDao internalPropertiesDao = mock(InternalPropertiesDao.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final GlobalLockManager underTest = new GlobalLockManagerImpl(dbClient);

  @Before
  public void wire_db_mocks() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao()).thenReturn(internalPropertiesDao);
  }

  @Test
  public void tryLock_fails_with_IAE_if_name_is_empty() {
    String badLockName = "";

    expectBadLockNameIAE(() -> underTest.tryLock(badLockName), badLockName);
  }

  @Test
  public void tryLock_fails_with_IAE_if_name_length_is_more_than_max_or_more() {
    String badLockName = RandomStringUtils.random(LOCK_NAME_MAX_LENGTH + 1 + new Random().nextInt(96));

    expectBadLockNameIAE(() -> underTest.tryLock(badLockName), badLockName);
  }

  @Test
  public void tryLock_accepts_name_with_allowed_length() {
    for (int i = 1; i <= LOCK_NAME_MAX_LENGTH; i++) {
      String lockName = RandomStringUtils.random(i);
      assertThatNoException().isThrownBy(() -> underTest.tryLock(lockName));
    }
  }

  @Test
  @UseDataProvider("randomValidLockName")
  public void tryLock_delegates_to_internalPropertiesDao_and_commits(String randomValidLockName) {
    boolean expected = new Random().nextBoolean();
    when(internalPropertiesDao.tryLock(dbSession, randomValidLockName, DEFAULT_LOCK_DURATION_SECONDS))
      .thenReturn(expected);

    assertThat(underTest.tryLock(randomValidLockName)).isEqualTo(expected);

    verify(dbClient).openSession(false);
    verify(internalPropertiesDao).tryLock(dbSession, randomValidLockName, DEFAULT_LOCK_DURATION_SECONDS);
    verify(dbSession).commit();
    verifyNoMoreInteractions(internalPropertiesDao);
  }

  @Test
  @UseDataProvider("randomValidDuration")
  public void tryLock_with_duration_fails_with_IAE_if_name_is_empty(int randomValidDuration) {
    String badLockName = "";

    expectBadLockNameIAE(() -> underTest.tryLock(badLockName, randomValidDuration), badLockName);
  }

  @Test
  @UseDataProvider("randomValidDuration")
  public void tryLock_with_duration_accepts_name_with_length_15_or_less(int randomValidDuration) {
    for (int i = 1; i <= 15; i++) {
      underTest.tryLock(RandomStringUtils.random(i), randomValidDuration);
    }
  }

  @Test
  @UseDataProvider("randomValidDuration")
  public void tryLock_with_duration_fails_with_IAE_if_name_length_is_36_or_more(int randomValidDuration) {
    String badLockName = RandomStringUtils.random(LOCK_NAME_MAX_LENGTH + 1 + new Random().nextInt(65));

    expectBadLockNameIAE(() -> underTest.tryLock(badLockName, randomValidDuration), badLockName);
  }

  @Test
  @UseDataProvider("randomValidLockName")
  public void tryLock_with_duration_fails_with_IAE_if_duration_is_0(String randomValidLockName) {
    expectBadDuration(() -> underTest.tryLock(randomValidLockName, 0), 0);
  }

  @Test
  @UseDataProvider("randomValidLockName")
  public void tryLock_with_duration_fails_with_IAE_if_duration_is_less_than_0(String randomValidLockName) {
    int negativeDuration = -1 - new Random().nextInt(100);

    expectBadDuration(() -> underTest.tryLock(randomValidLockName, negativeDuration), negativeDuration);
  }

  @Test
  @UseDataProvider("randomValidDuration")
  public void tryLock_with_duration_delegates_to_InternalPropertiesDao_and_commits(int randomValidDuration) {
    String lockName = "foo";
    boolean expected = new Random().nextBoolean();
    when(internalPropertiesDao.tryLock(dbSession, lockName, randomValidDuration))
      .thenReturn(expected);

    assertThat(underTest.tryLock(lockName, randomValidDuration)).isEqualTo(expected);

    verify(dbClient).openSession(false);
    verify(internalPropertiesDao).tryLock(dbSession, lockName, randomValidDuration);
    verify(dbSession).commit();
    verifyNoMoreInteractions(internalPropertiesDao);
  }

  @DataProvider
  public static Object[][] randomValidLockName() {
    return new Object[][] {
      {randomAlphabetic(1 + new Random().nextInt(LOCK_NAME_MAX_LENGTH))}
    };
  }

  @DataProvider
  public static Object[][] randomValidDuration() {
    return new Object[][] {
      {1 + new Random().nextInt(2_00)}
    };
  }

  private void expectBadLockNameIAE(ThrowingCallable callback, String badLockName) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("name's length must be > 0 and <= " + LOCK_NAME_MAX_LENGTH + ": '" + badLockName + "'");
  }

  private void expectBadDuration(ThrowingCallable callback, int badDuration) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("duration must be > 0: " + badDuration);
  }

}
