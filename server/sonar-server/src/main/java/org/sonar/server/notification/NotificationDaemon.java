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
package org.sonar.server.notification;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Configuration;
import org.sonar.api.notifications.Notification;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Properties({
  @Property(
    key = NotificationDaemon.PROPERTY_DELAY,
    defaultValue = "60",
    name = "Delay of notifications, in seconds",
    global = false),
  @Property(
    key = NotificationDaemon.PROPERTY_DELAY_BEFORE_REPORTING_STATUS,
    defaultValue = "600",
    name = "Delay before reporting notification status, in seconds",
    global = false)
})
@ServerSide
public class NotificationDaemon implements Startable {
  private static final String THREAD_NAME_PREFIX = "sq-notification-service-";

  private static final Logger LOG = Loggers.get(NotificationDaemon.class);

  public static final String PROPERTY_DELAY = "sonar.notifications.delay";
  public static final String PROPERTY_DELAY_BEFORE_REPORTING_STATUS = "sonar.notifications.runningDelayBeforeReportingStatus";

  private final long delayInSeconds;
  private final long delayBeforeReportingStatusInSeconds;
  private final DefaultNotificationManager manager;
  private final NotificationService service;

  private ScheduledExecutorService executorService;
  private boolean stopping = false;

  public NotificationDaemon(Configuration config, DefaultNotificationManager manager, NotificationService service) {
    this.delayInSeconds = config.getLong(PROPERTY_DELAY).get();
    this.delayBeforeReportingStatusInSeconds = config.getLong(PROPERTY_DELAY_BEFORE_REPORTING_STATUS).get();
    this.manager = manager;
    this.service = service;
  }

  @Override
  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
        .setNameFormat(THREAD_NAME_PREFIX + "%d")
        .setPriority(Thread.MIN_PRIORITY)
        .build());
    executorService.scheduleWithFixedDelay(() -> {
      try {
        processQueue();
      } catch (Exception e) {
        LOG.error("Error in NotificationService", e);
      }
    }, 0, delayInSeconds, TimeUnit.SECONDS);
    LOG.info("Notification service started (delay {} sec.)", delayInSeconds);
  }

  @Override
  public void stop() {
    try {
      stopping = true;
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOG.error("Error during stop of notification service", e);
      Thread.currentThread().interrupt();
    }
    LOG.info("Notification service stopped");
  }

  private synchronized void processQueue() {
    long start = now();
    long lastLog = start;
    long notifSentCount = 0;

    Notification notifToSend = manager.getFromQueue();
    while (notifToSend != null) {
      service.deliver(notifToSend);
      notifSentCount++;
      if (stopping) {
        break;
      }
      long now = now();
      if (now - lastLog > delayBeforeReportingStatusInSeconds * 1000) {
        long remainingNotifCount = manager.count();
        lastLog = now;
        long spentTimeInMinutes = (now - start) / (60 * 1000);
        log(notifSentCount, remainingNotifCount, spentTimeInMinutes);
      }
      notifToSend = manager.getFromQueue();
    }
  }

  @VisibleForTesting
  void log(long notifSentCount, long remainingNotifCount, long spentTimeInMinutes) {
    LOG.info("{} notifications sent during the past {} minutes and {} still waiting to be sent",
      notifSentCount, spentTimeInMinutes, remainingNotifCount);
  }

  @VisibleForTesting
  long now() {
    return System.currentTimeMillis();
  }
}
