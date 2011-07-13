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
import org.sonar.api.utils.TimeProfiler;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * @since 2.10
 */
public class NotificationManager implements ServerComponent { // TODO should be available on batch side too

  private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);
  private static final TimeProfiler TIME_PROFILER = new TimeProfiler().setLogger(LOG);

  private NotificationQueue queue;
  private NotificationDispatcher[] dispatchers;
  private NotificationChannel[] channels;

  public NotificationManager(NotificationQueue queue, NotificationDispatcher[] dispatchers, NotificationChannel[] channels) {
    this.queue = queue;
    this.dispatchers = dispatchers;
    this.channels = channels;
  }

  public void scheduleForSending(Notification notification) {
    TIME_PROFILER.start("Scheduling " + notification);
    SetMultimap<Integer, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : channels) {
      for (NotificationDispatcher dispatcher : dispatchers) {
        final Set<Integer> possibleRecipients = Sets.newHashSet();
        NotificationDispatcher.Context context = new NotificationDispatcher.Context() {
          public void addUser(Integer userId) {
            possibleRecipients.add(userId);
          }
        };
        dispatcher.dispatch(notification, context);
        for (Integer userId : possibleRecipients) {
          if (isEnabled(userId, channel, dispatcher)) {
            recipients.put(userId, channel);
          }
        }
      }
    }
    for (Map.Entry<Integer, NotificationChannel> entry : recipients.entries()) {
      Integer userId = entry.getKey();
      NotificationChannel channel = entry.getValue();
      LOG.info("For user {} via {}", userId, channel.getKey());
      Serializable notificationData = channel.createDataForPersistance(notification, userId);
      queue.add(notificationData, channel);
    }
    TIME_PROFILER.stop();
  }

  boolean isEnabled(Integer userId, NotificationChannel channel, NotificationDispatcher dispatcher) {
    return true; // FIXME for the moment we will accept everything
  }

}
