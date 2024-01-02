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
package org.sonar.server.usertoken.notification;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.LocalDateTime;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.util.GlobalLockManager;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class TokenExpirationNotificationSchedulerImpl implements TokenExpirationNotificationScheduler {
  // Lock 23 hours in case of server restart or multiple nodes in data center edition
  private static int LOCK_DURATION = 23 * 60 * 60;
  private static String LOCK_NAME = "token-notif";
  private static final Logger LOG = Loggers.get(TokenExpirationNotificationSchedulerImpl.class);
  private final TokenExpirationNotificationExecutorService executorService;
  private final GlobalLockManager lockManager;
  private final TokenExpirationNotificationSender notificationSender;

  public TokenExpirationNotificationSchedulerImpl(TokenExpirationNotificationExecutorService executorService, GlobalLockManager lockManager,
    TokenExpirationNotificationSender notificationSender) {
    this.executorService = executorService;
    this.lockManager = lockManager;
    this.notificationSender = notificationSender;
  }

  @Override
  public void startScheduling() {
    LocalDateTime now = LocalDateTime.now();
    // schedule run at midnight everyday
    LocalDateTime nextRun = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
    long initialDelay = Duration.between(now, nextRun).getSeconds();
    executorService.scheduleAtFixedRate(this::notifyTokenExpiration, initialDelay, DAYS.toSeconds(1), SECONDS);
  }

  @VisibleForTesting
  void notifyTokenExpiration() {
    try {
      // Avoid notification multiple times in case of data center edition
      if (!lockManager.tryLock(LOCK_NAME, LOCK_DURATION)) {
        return;
      }
      notificationSender.sendNotifications();
    } catch (RuntimeException e) {
      LOG.error("Error in sending token expiration notification", e);
    }
  }

}
