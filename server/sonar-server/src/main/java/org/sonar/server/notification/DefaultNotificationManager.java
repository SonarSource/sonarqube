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
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationQueueDto;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class DefaultNotificationManager implements NotificationManager {

  private static final Logger LOG = Loggers.get(DefaultNotificationManager.class);

  private static final String UNABLE_TO_READ_NOTIFICATION = "Unable to read notification";

  private NotificationChannel[] notificationChannels;
  private final DbClient dbClient;

  private boolean alreadyLoggedDeserializationIssue = false;

  /**
   * Default constructor used by Pico
   */
  public DefaultNotificationManager(NotificationChannel[] channels,
    DbClient dbClient) {
    this.notificationChannels = channels;
    this.dbClient = dbClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void scheduleForSending(Notification notification) {
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    dbClient.notificationQueueDao().insert(singletonList(dto));
  }
  /**
   * Give the notification queue so that it can be processed
   */
  public Notification getFromQueue() {
    int batchSize = 1;
    List<NotificationQueueDto> notificationDtos = dbClient.notificationQueueDao().selectOldest(batchSize);
    if (notificationDtos.isEmpty()) {
      return null;
    }
    dbClient.notificationQueueDao().delete(notificationDtos);

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
    return dbClient.notificationQueueDao().count();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, String projectUuid) {
    requireNonNull(projectUuid, "ProjectUUID is mandatory");
    String dispatcherKey = dispatcher.getKey();

    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : notificationChannels) {
      String channelKey = channel.getKey();

      // Find users subscribed globally to the dispatcher (i.e. not on a specific project)
      // And users subscribed to the dispatcher specifically for the project
      Set<String> subscribedUsers = dbClient.propertiesDao().findUsersForNotification(dispatcherKey, channelKey, projectUuid);

      if (!subscribedUsers.isEmpty()) {
        try (DbSession dbSession = dbClient.openSession(false)) {
          Set<String> filteredSubscribedUsers = dbClient.authorizationDao().keepAuthorizedLoginsOnProject(dbSession, subscribedUsers, projectUuid, UserRole.USER);
          addUsersToRecipientListForChannel(filteredSubscribedUsers, recipients, channel);
        }
      }
    }

    return recipients;
  }

  @VisibleForTesting
  protected List<NotificationChannel> getChannels() {
    return Arrays.asList(notificationChannels);
  }

  private static void addUsersToRecipientListForChannel(Collection<String> users, SetMultimap<String, NotificationChannel> recipients, NotificationChannel channel) {
    for (String username : users) {
      recipients.put(username, channel);
    }
  }
}
