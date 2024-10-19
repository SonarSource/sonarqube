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
package org.sonar.server.authentication.purge;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.user.SamlMessageIdDto;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.util.AbstractStoppableExecutorService;
import org.sonar.server.util.GlobalLockManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpiredSessionsCleanerIT {

  private static final long NOW = 1_000_000_000L;

  private TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public LogTester logTester = new LogTester();

  private GlobalLockManager lockManager = mock(GlobalLockManager.class);

  private SyncSessionTokensCleanerExecutorService executorService = new SyncSessionTokensCleanerExecutorService();

  private ExpiredSessionsCleaner underTest = new ExpiredSessionsCleaner(executorService, db.getDbClient(), lockManager);

  @Test
  public void purge_expired_session_tokens() {
    when(lockManager.tryLock(anyString())).thenReturn(true);
    UserDto user = db.users().insertUser();
    SessionTokenDto validSessionToken = db.users().insertSessionToken(user, st -> st.setExpirationDate(NOW + 1_000_000L));
    SessionTokenDto expiredSessionToken = db.users().insertSessionToken(user, st -> st.setExpirationDate(NOW - 1_000_000L));
    underTest.start();

    executorService.runCommand();

    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(db.getSession(), validSessionToken.getUuid())).isPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(db.getSession(), expiredSessionToken.getUuid())).isNotPresent();
    assertThat(logTester.getLogs(LoggerLevel.INFO))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("Purge of expired session tokens has removed 1 elements");
  }

  @Test
  public void purge_expired_saml_message_ids() {
    when(lockManager.tryLock(anyString())).thenReturn(true);
    db.getDbClient().samlMessageIdDao().insert(db.getSession(), new SamlMessageIdDto().setMessageId("MESSAGE_1").setExpirationDate(NOW + 1_000_000L));
    db.getDbClient().samlMessageIdDao().insert(db.getSession(), new SamlMessageIdDto().setMessageId("MESSAGE_2").setExpirationDate(NOW - 1_000_000L));
    db.commit();
    underTest.start();

    executorService.runCommand();

    assertThat(db.getDbClient().samlMessageIdDao().selectByMessageId(db.getSession(), "MESSAGE_1")).isPresent();
    assertThat(db.getDbClient().samlMessageIdDao().selectByMessageId(db.getSession(), "MESSAGE_2")).isNotPresent();
    assertThat(logTester.getLogs(LoggerLevel.INFO))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("Purge of expired SAML message ids has removed 1 elements");
  }

  @Test
  public void do_not_execute_purge_when_fail_to_get_lock() {
    when(lockManager.tryLock(anyString())).thenReturn(false);
    SessionTokenDto expiredSessionToken = db.users().insertSessionToken(db.users().insertUser(), st -> st.setExpirationDate(NOW - 1_000_000L));
    underTest.start();

    executorService.runCommand();

    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(db.getSession(), expiredSessionToken.getUuid())).isPresent();
  }

  private static class SyncSessionTokensCleanerExecutorService extends AbstractStoppableExecutorService<ScheduledExecutorService> implements ExpiredSessionsCleanerExecutorService {

    private Runnable command;

    public SyncSessionTokensCleanerExecutorService() {
      super(null);
    }

    public void runCommand() {
      command.run();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      this.command = command;
      return null;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return null;
    }

  }
}
