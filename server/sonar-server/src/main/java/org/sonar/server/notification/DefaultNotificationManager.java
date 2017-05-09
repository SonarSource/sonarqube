/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.property.PropertiesDao;

import static java.util.Collections.singletonList;

public class DefaultNotificationManager implements NotificationManager {

  private static final Logger LOG = Loggers.get(DefaultNotificationManager.class);

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
    notificationQueueDao.insert(singletonList(dto));
  }
  /**
   * Give the notification queue so that it can be processed
   */
  public Notification getFromQueue() {
    int batchSize = 1;
    List<NotificationQueueDto> notificationDtos = notificationQueueDao.selectOldest(batchSize);
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
    } catch (IOException | ClassNotFoundException e) {
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
  public Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher,
    @Nullable String projectUuid) {
    String dispatcherKey = dispatcher.getKey();

    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : notificationChannels) {
      String channelKey = channel.getKey();

      // Find users subscribed globally to the dispatcher (i.e. not on a specific project)
      addUsersToRecipientListForChannel(propertiesDao.selectUsersForNotification(dispatcherKey, channelKey, null), recipients, channel);

      if (projectUuid != null) {
        // Find users subscribed to the dispatcher specifically for the project
        addUsersToRecipientListForChannel(propertiesDao.selectUsersForNotification(dispatcherKey, channelKey, projectUuid), recipients, channel);
      }
    }

    return recipients;
  }

  @Override
  public Multimap<String, NotificationChannel> findNotificationSubscribers(NotificationDispatcher dispatcher, @Nullable String componentKey) {
    String dispatcherKey = dispatcher.getKey();

    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : notificationChannels) {
      addUsersToRecipientListForChannel(propertiesDao.selectNotificationSubscribers(dispatcherKey, channel.getKey(), componentKey), recipients, channel);
    }

    return recipients;
  }

  @VisibleForTesting
  protected List<NotificationChannel> getChannels() {
    return Arrays.asList(notificationChannels);
  }

  private static void addUsersToRecipientListForChannel(List<String> users, SetMultimap<String, NotificationChannel> recipients, NotificationChannel channel) {
    for (String username : users) {
      recipients.put(username, channel);
    }
  }

}
