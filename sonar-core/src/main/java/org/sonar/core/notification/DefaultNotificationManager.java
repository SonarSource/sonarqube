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
package org.sonar.core.notification;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.utils.SonarException;
import org.sonar.core.notification.db.NotificationQueueDao;
import org.sonar.core.notification.db.NotificationQueueDto;
import org.sonar.core.properties.PropertiesDao;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.List;

/**
 * @since 2.10
 */
@RequiresDB
public class DefaultNotificationManager implements NotificationManager {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationManager.class);

  private static final String UNABLE_TO_READ_NOTIFICATION = "Unable to read notification";

  private NotificationChannel[] notificationChannels;
  private NotificationQueueDao notificationQueueDao;
  private PropertiesDao propertiesDao;

  private boolean alreadyLoggedDeserializationIssue = false;

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
  @Override
  public void scheduleForSending(Notification notification) {
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    notificationQueueDao.insert(Arrays.asList(dto));
  }

  @Override
  public void scheduleForSending(List<Notification> notification) {
    notificationQueueDao.insert(Lists.transform(notification, new Function<Notification, NotificationQueueDto>() {
      @Override
      public NotificationQueueDto apply(Notification notification) {
        return NotificationQueueDto.toNotificationQueueDto(notification);
      }
    }));
  }

  /**
   * Give the notification queue so that it can be processed
   */
  public Notification getFromQueue() {
    int batchSize = 1;
    List<NotificationQueueDto> notificationDtos = notificationQueueDao.findOldest(batchSize);
    if (notificationDtos.isEmpty()) {
      return null;
    }
    notificationQueueDao.delete(notificationDtos);

    return convertToNotification(notificationDtos);
  }

  private Notification convertToNotification(List<NotificationQueueDto> notifications) {
    try {
      // If batchSize is increased then we should return a list instead of a single element
      return notifications.get(0).toNotification();
    } catch (InvalidClassException e) {
      // SONAR-4739
      if (!alreadyLoggedDeserializationIssue) {
        logDeserializationIssue();
        alreadyLoggedDeserializationIssue = true;
      }
      return null;
    } catch (IOException e) {
      throw new SonarException(UNABLE_TO_READ_NOTIFICATION, e);

    } catch (ClassNotFoundException e) {
      throw new SonarException(UNABLE_TO_READ_NOTIFICATION, e);
    }
  }

  @VisibleForTesting
  void logDeserializationIssue() {
    LOG.warn("It is impossible to send pending notifications which existed prior to the upgrade of SonarQube. They will be ignored.");
  }

  public long count() {
    return notificationQueueDao.count();
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
