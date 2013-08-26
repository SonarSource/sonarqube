/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.notification;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.core.notification.db.NotificationQueueDao;
import org.sonar.core.notification.db.NotificationQueueDto;
import org.sonar.core.properties.PropertiesDao;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @since 2.10
 */
public class DefaultNotificationManager implements NotificationManager {

  private NotificationChannel[] notificationChannels;
  private NotificationQueueDao notificationQueueDao;
  private PropertiesDao propertiesDao;

  /**
   * Default constructor used by Pico
   */
  public DefaultNotificationManager(NotificationChannel[] channels, NotificationQueueDao notificationQueueDao, PropertiesDao propertiesDao) {
    this.notificationChannels = channels;
    this.notificationQueueDao = notificationQueueDao;
    this.propertiesDao = propertiesDao;
  }

  /**
   * Constructor if no notification channel
   */
  public DefaultNotificationManager(NotificationQueueDao notificationQueueDao, PropertiesDao propertiesDao) {
    this(new NotificationChannel[0], notificationQueueDao, propertiesDao);
  }

  /**
   * {@inheritDoc}
   */
  public void scheduleForSending(Notification notification) {
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    notificationQueueDao.insert(dto);
  }

  /**
   * Give the notification queue so that it can be processed
   */
  public Notification getFromQueue() {
    int batchSize = 1;
    List<NotificationQueueDto> notifications = notificationQueueDao.findOldest(batchSize);
    if (notifications.isEmpty()) {
      return null;
    }
    notificationQueueDao.delete(notifications);

    // If batchSize is increased then we should return a list instead of a single element
    return notifications.get(0).toNotification();

  }

  /**
   * {@inheritDoc}
   */
  public Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, @Nullable Integer resourceId) {
    String dispatcherKey = dispatcher.getKey();

    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : notificationChannels) {
      String channelKey = channel.getKey();

      // Find users subscribed globally to the dispatcher (i.e. not on a specific project)
      addUsersToRecipientListForChannel(propertiesDao.findUsersForNotification(dispatcherKey, channelKey, null), recipients, channel);

      if (resourceId != null) {
        // Find users subscribed to the dispatcher specifically for the resource
        addUsersToRecipientListForChannel(propertiesDao.findUsersForNotification(dispatcherKey, channelKey, resourceId.longValue()), recipients, channel);
      }
    }

    return recipients;
  }

  @Override
  public Multimap<String, NotificationChannel> findNotificationSubscribers(NotificationDispatcher dispatcher, @Nullable String componentKey) {
    String dispatcherKey = dispatcher.getKey();

    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : notificationChannels) {
      addUsersToRecipientListForChannel(propertiesDao.findNotificationSubscribers(dispatcherKey, channel.getKey(), componentKey), recipients, channel);
    }

    return recipients;
  }

  @VisibleForTesting
  protected List<NotificationChannel> getChannels() {
    return Arrays.asList(notificationChannels);
  }

  private void addUsersToRecipientListForChannel(List<String> users, SetMultimap<String, NotificationChannel> recipients, NotificationChannel channel) {
    for (String username : users) {
      recipients.put(username, channel);
    }
  }

}
