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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.notifications.DefaultNotificationManager;
import org.sonar.jpa.entity.NotificationQueueElement;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @since 2.10
 */
public class NotificationService implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
  private static final TimeProfiler TIME_PROFILER = new TimeProfiler(LOG);

  private ScheduledExecutorService executorService;
  private long period = 10; // FIXME small value just for tests

  private DatabaseSessionFactory sessionFactory;
  private DefaultNotificationManager manager;
  private NotificationChannel[] channels;
  private NotificationDispatcher[] dispatchers;

  /**
   * Default constructor when no channels.
   */
  public NotificationService(DatabaseSessionFactory sessionFactory, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers) {
    this(sessionFactory, manager, dispatchers, new NotificationChannel[0]);
    LOG.warn("There is no channels - all notifications would be ignored!");
  }

  public NotificationService(DatabaseSessionFactory sessionFactory, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers, NotificationChannel[] channels) {
    this.sessionFactory = sessionFactory;
    this.manager = manager;
    this.channels = channels;
    this.dispatchers = dispatchers;
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void setPeriod(long milliseconds) {
    this.period = milliseconds;
  }

  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        processQueue();
      }
    }, 0, period, TimeUnit.MILLISECONDS);
    LOG.info("Notification service started");
  }

  public void stop() {
    try {
      executorService.awaitTermination(period, TimeUnit.MILLISECONDS);
      executorService.shutdown();
    } catch (InterruptedException e) {
      LOG.error("Error during stop of notification service", e);
    }
    LOG.info("Notification service stopped");
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void processQueue() {
    NotificationQueueElement queueElement = manager.getFromQueue();
    while (queueElement != null) {
      deliver(queueElement.getNotification());
      queueElement = manager.getFromQueue();
    }
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void deliver(Notification notification) {
    TIME_PROFILER.start("Delivering notification " + notification);
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
        dispatcher.dispatch(notification, context);
        for (String username : possibleRecipients) {
          if (isEnabled(username, channel, dispatcher)) {
            recipients.put(username, channel);
          }
        }
      }
    }
    for (Map.Entry<String, Collection<NotificationChannel>> entry : recipients.asMap().entrySet()) {
      String username = entry.getKey();
      Collection<NotificationChannel> channels = entry.getValue();
      LOG.info("For user {} via {}", username, channels);
      for (NotificationChannel channel : channels) {
        channel.deliver(notification, username);
      }
    }
    TIME_PROFILER.stop();
  }

  public List<NotificationDispatcher> getDispatchers() {
    return Arrays.asList(dispatchers);
  }

  public List<NotificationChannel> getChannels() {
    return Arrays.asList(channels);
  }

  /**
   * Visibility has been relaxed for tests.
   */
  boolean isEnabled(String username, NotificationChannel channel, NotificationDispatcher dispatcher) {
    DatabaseSession session = sessionFactory.getSession();
    User user = session.getSingleResult(User.class, "login", username);
    String notificationKey = "notification." + dispatcher.getKey() + "." + channel.getKey();
    Property property = session.getSingleResult(Property.class, "userId", user.getId(), "key", notificationKey);
    return property != null && "true".equals(property.getValue());
  }

}
