/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.notifications;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.ServerComponent;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.notifications.DefaultNotificationManager;
import org.sonar.jpa.entity.NotificationQueueElement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * @since 2.10
 */
public class NotificationService implements ServerComponent {

  private static final TimeProfiler TIME_PROFILER = new TimeProfiler(Logs.INFO).setLevelToDebug();

  private static final String DELAY = "sonar.notifications.delay";
  private static final long DELAY_DEFAULT = 60;

  private ScheduledExecutorService executorService;
  private long delay;

  private DefaultNotificationManager manager;
  private NotificationChannel[] channels;
  private NotificationDispatcher[] dispatchers;

  /**
   * Default constructor when no channels.
   */
  public NotificationService(Configuration configuration, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers) {
    this(configuration, manager, dispatchers, new NotificationChannel[0]);
    Logs.INFO.warn("There is no channels - all notifications would be ignored!");
  }

  public NotificationService(Configuration configuration, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers, NotificationChannel[] channels) {
    delay = configuration.getLong(DELAY, DELAY_DEFAULT);
    this.manager = manager;
    this.channels = channels;
    this.dispatchers = dispatchers;
  }

  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        processQueue();
      }
    }, 0, delay, TimeUnit.SECONDS);
    Logs.INFO.info("Notification service started (delay {} sec.)", delay);
  }

  public void stop() {
    try {
      executorService.awaitTermination(delay, TimeUnit.SECONDS);
      executorService.shutdown();
    } catch (InterruptedException e) {
      Logs.INFO.error("Error during stop of notification service", e);
    }
    Logs.INFO.info("Notification service stopped");
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void processQueue() {
    TIME_PROFILER.start("Processing notifications queue");
    NotificationQueueElement queueElement = manager.getFromQueue();
    while (queueElement != null) {
      deliver(queueElement.getNotification());
      queueElement = manager.getFromQueue();
    }
    TIME_PROFILER.stop();
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void deliver(Notification notification) {
    Logs.INFO.debug("Delivering notification " + notification);
    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : channels) {
      for (NotificationDispatcher dispatcher : dispatchers) {
        final Set<String> possibleRecipients = Sets.newHashSet();
        NotificationDispatcher.Context context = new NotificationDispatcher.Context() {
          public void addUser(String username) {
            if (username != null) {
              possibleRecipients.add(username);
            }
          }
        };
        try {
          dispatcher.dispatch(notification, context);
        } catch (Exception e) { // catch all exceptions in order to dispatch using other dispatchers
          Logs.INFO.warn("Unable to dispatch notification " + notification + " using " + dispatcher, e);
        }
        for (String username : possibleRecipients) {
          if (manager.isEnabled(username, channel.getKey(), dispatcher.getKey())) {
            recipients.put(username, channel);
          }
        }
      }
    }
    for (Map.Entry<String, Collection<NotificationChannel>> entry : recipients.asMap().entrySet()) {
      String username = entry.getKey();
      Collection<NotificationChannel> userChannels = entry.getValue();
      Logs.INFO.debug("For user {} via {}", username, userChannels);
      for (NotificationChannel channel : userChannels) {
        try {
          channel.deliver(notification, username);
        } catch (Exception e) { // catch all exceptions in order to deliver via other channels
          Logs.INFO.warn("Unable to deliver notification " + notification + " for user " + username + " via " + channel, e);
        }
      }
    }
  }

  public List<NotificationDispatcher> getDispatchers() {
    return Arrays.asList(dispatchers);
  }

  public List<NotificationChannel> getChannels() {
    return Arrays.asList(channels);
  }

}
