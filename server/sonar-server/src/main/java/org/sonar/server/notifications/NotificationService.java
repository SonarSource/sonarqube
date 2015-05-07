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
package org.sonar.server.notifications;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import org.picocontainer.Startable;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.ServerSide;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.server.db.DbClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @since 2.10
 */
@Properties({
  @Property(
    key = NotificationService.PROPERTY_DELAY,
    defaultValue = "60",
    name = "Delay of notifications, in seconds",
    project = false,
    global = false),
  @Property(
    key = NotificationService.PROPERTY_DELAY_BEFORE_REPORTING_STATUS,
    defaultValue = "600",
    name = "Delay before reporting notification status, in seconds",
    project = false,
    global = false)
})
@ServerSide
public class NotificationService implements Startable {

  private static final Logger LOG = Loggers.get(NotificationService.class);

  public static final String PROPERTY_DELAY = "sonar.notifications.delay";
  public static final String PROPERTY_DELAY_BEFORE_REPORTING_STATUS = "sonar.notifications.runningDelayBeforeReportingStatus";

  private final long delayInSeconds;
  private final long delayBeforeReportingStatusInSeconds;
  private final DefaultNotificationManager manager;
  private final List<NotificationDispatcher> dispatchers;
  private final DbClient dbClient;

  private ScheduledExecutorService executorService;
  private boolean stopping = false;

  /**
   * Constructor for {@link NotificationService}
   */
  public NotificationService(Settings settings, DefaultNotificationManager manager, DbClient dbClient, NotificationDispatcher[] dispatchers) {
    this.delayInSeconds = settings.getLong(PROPERTY_DELAY);
    this.delayBeforeReportingStatusInSeconds = settings.getLong(PROPERTY_DELAY_BEFORE_REPORTING_STATUS);
    this.manager = manager;
    this.dbClient = dbClient;
    this.dispatchers = ImmutableList.copyOf(dispatchers);
  }

  /**
   * Default constructor when no dispatchers.
   */
  public NotificationService(Settings settings, DefaultNotificationManager manager, DbClient dbClient) {
    this(settings, manager, dbClient, new NotificationDispatcher[0]);
  }

  @Override
  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          processQueue();
        } catch (Exception e) {
          LOG.error("Error in NotificationService", e);
        }
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
    }
    LOG.info("Notification service stopped");
  }

  @VisibleForTesting
  synchronized void processQueue() {
    long start = now();
    long lastLog = start;
    long notifSentCount = 0;

    Notification notifToSend = manager.getFromQueue();
    while (notifToSend != null) {
      deliver(notifToSend);
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
    LOG.info("{} notifications sent during the past {} minutes and {} still waiting to be sent", new Object[] {notifSentCount, spentTimeInMinutes, remainingNotifCount});
  }

  @VisibleForTesting
  long now() {
    return System.currentTimeMillis();
  }

  public void deliver(Notification notification) {
    final SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationDispatcher dispatcher : dispatchers) {
      NotificationDispatcher.Context context = new NotificationDispatcher.Context() {
        @Override
        public void addUser(String username) {
          // This method is not used anymore
        }

        @Override
        public void addUser(String userLogin, NotificationChannel notificationChannel) {
          if (userLogin != null) {
            recipients.put(userLogin, notificationChannel);
          }
        }
      };
      try {
        dispatcher.performDispatch(notification, context);
      } catch (Exception e) {
        // catch all exceptions in order to dispatch using other dispatchers
        LOG.warn("Unable to dispatch notification " + notification + " using " + dispatcher, e);
      }
    }
    dispatch(notification, recipients);
  }

  private void dispatch(Notification notification, SetMultimap<String, NotificationChannel> recipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> entry : recipients.asMap().entrySet()) {
      String username = entry.getKey();
      Collection<NotificationChannel> userChannels = entry.getValue();
      LOG.debug("For user {} via {}", username, userChannels);
      for (NotificationChannel channel : userChannels) {
        try {
          channel.deliver(notification, username);
        } catch (Exception e) {
          // catch all exceptions in order to deliver via other channels
          LOG.warn("Unable to deliver notification " + notification + " for user " + username + " via " + channel, e);
        }
      }
    }
  }

  @VisibleForTesting
  protected List<NotificationDispatcher> getDispatchers() {
    return dispatchers;
  }

  /**
   * Returns true if at least one user is subscribed to at least one notifications with given types.
   * Subscription can be globally or on the specific project.
   */
  public boolean hasProjectSubscribersForTypes(String projectUuid, Set<String> notificationTypes) {
    Collection<String> dispatcherKeys = new ArrayList<>();
    for (NotificationDispatcher dispatcher : dispatchers) {
      if (notificationTypes.contains(dispatcher.getType())) {
        dispatcherKeys.add(dispatcher.getKey());
      }
    }

    return dbClient.propertiesDao().hasProjectNotificationSubscribersForDispatchers(projectUuid, dispatcherKeys);
  }
}
