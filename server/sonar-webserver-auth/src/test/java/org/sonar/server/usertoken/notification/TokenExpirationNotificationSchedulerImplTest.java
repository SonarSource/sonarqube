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
package org.sonar.server.usertoken.notification;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.util.GlobalLockManager;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TokenExpirationNotificationSchedulerImplTest {
  @Rule
  public LogTester logTester = new LogTester();
  private final TokenExpirationNotificationExecutorService executorService = mock(TokenExpirationNotificationExecutorService.class);
  private final GlobalLockManager lockManager = mock(GlobalLockManager.class);
  private final TokenExpirationNotificationSender notificationSender = mock(TokenExpirationNotificationSender.class);
  private final TokenExpirationNotificationSchedulerImpl underTest = new TokenExpirationNotificationSchedulerImpl(executorService, lockManager,
    notificationSender);

  @Test
  public void startScheduling() {
    underTest.startScheduling();
    verify(executorService, times(1)).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(DAYS.toSeconds(1)), eq(SECONDS));
  }

  @Test
  public void no_notification_if_it_is_already_sent() {
    when(lockManager.tryLock(anyString(), anyInt())).thenReturn(false);
    underTest.notifyTokenExpiration();
    verifyNoInteractions(notificationSender);
  }

  @Test
  public void log_error_if_exception_in_sending_notification() {
    when(lockManager.tryLock(anyString(), anyInt())).thenReturn(true);
    doThrow(new IllegalStateException()).when(notificationSender).sendNotifications();
    underTest.notifyTokenExpiration();
    assertThat(logTester.getLogs(LoggerLevel.ERROR))
      .extracting(LogAndArguments::getFormattedMsg)
      .containsExactly("Error in sending token expiration notification");
  }
}
