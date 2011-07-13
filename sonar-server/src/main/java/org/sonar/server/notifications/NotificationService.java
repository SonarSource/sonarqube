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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.server.notifications.NotificationQueue.Element;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @since 2.10
 */
public class NotificationService implements ServerComponent {

  private static Logger LOG = LoggerFactory.getLogger(NotificationService.class);

  private ScheduledExecutorService executorService;
  private long period = 10; // FIXME small value just for tests
  private NotificationQueue queue;

  private Map<String, NotificationChannel> channels = Maps.newHashMap();

  /**
   * Default constructor when no channels.
   */
  public NotificationService(NotificationQueue queue) {
    this(queue, new NotificationChannel[0]);
    LOG.warn("There is no channels - all notifications would be ignored!");
  }

  public NotificationService(NotificationQueue queue, NotificationChannel[] channels) {
    this.queue = queue;
    for (NotificationChannel channel : channels) {
      this.channels.put(channel.getKey(), channel);
    }
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
    NotificationQueue.Element element = queue.get();
    while (element != null) {
      deliver(element);
      element = queue.get();
    }
  }

  /**
   * Visibility has been relaxed for tests.
   */
  void deliver(Element element) {
    NotificationChannel channel = channels.get(element.channelKey);
    if (channel != null) {
      channel.deliver(element.notificationData);
    }
  }

}
