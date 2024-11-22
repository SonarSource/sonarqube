/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.SonarException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class DefaultNotificationManager implements NotificationManager {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationManager.class);

  private static final String UNABLE_TO_READ_NOTIFICATION = "Unable to read notification";

  private final NotificationChannel[] notificationChannels;
  private final DbClient dbClient;
  private boolean alreadyLoggedDeserializationIssue = false;

  public DefaultNotificationManager(NotificationChannel[] channels, DbClient dbClient) {
    this.notificationChannels = channels;
    this.dbClient = dbClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Notification> void scheduleForSending(T notification) {
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    dbClient.notificationQueueDao().insert(singletonList(dto));
  }

  /**
   * Give the notification queue so that it can be processed
   */
  public <T extends Notification> T getFromQueue() {
    int batchSize = 1;
    List<NotificationQueueDto> notificationDtos = dbClient.notificationQueueDao().selectOldest(batchSize);
    if (notificationDtos.isEmpty()) {
      return null;
    }
    dbClient.notificationQueueDao().delete(notificationDtos);

    return convertToNotification(notificationDtos);
  }

  private <T extends Notification> T convertToNotification(List<NotificationQueueDto> notifications) {
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

  private static void verifyProjectKey(String projectKey) {
    requireNonNull(projectKey, "projectKey is mandatory");
  }

  @Override
  public Set<EmailRecipient> findSubscribedEmailRecipients(String dispatcherKey, String projectKey,
    SubscriberPermissionsOnProject subscriberPermissionsOnProject) {
    verifyProjectKey(projectKey);

    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<EmailSubscriberDto> emailSubscribers = dbClient.propertiesDao().findEnabledEmailSubscribersForNotification(
        dbSession, dispatcherKey, EmailNotificationChannel.class.getSimpleName(), projectKey);

      return keepAuthorizedEmailSubscribers(dbSession, projectKey, subscriberPermissionsOnProject, emailSubscribers);
    }
  }

  @Override
  public Set<EmailRecipient> findSubscribedEmailRecipients(String dispatcherKey, String projectKey, Set<String> logins,
    SubscriberPermissionsOnProject subscriberPermissionsOnProject) {
    verifyProjectKey(projectKey);
    requireNonNull(logins, "logins can't be null");
    if (logins.isEmpty()) {
      return emptySet();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<EmailSubscriberDto> emailSubscribers = dbClient.propertiesDao().findEnabledEmailSubscribersForNotification(
        dbSession, dispatcherKey, EmailNotificationChannel.class.getSimpleName(), projectKey, logins);

      return keepAuthorizedEmailSubscribers(dbSession, projectKey, subscriberPermissionsOnProject, emailSubscribers);
    }
  }

  private Set<EmailRecipient> keepAuthorizedEmailSubscribers(DbSession dbSession, String projectKey,
    SubscriberPermissionsOnProject subscriberPermissionsOnProject, Set<EmailSubscriberDto> emailSubscribers) {
    if (emailSubscribers.isEmpty()) {
      return emptySet();
    }

    return keepAuthorizedEmailSubscribers(dbSession, projectKey, emailSubscribers, subscriberPermissionsOnProject)
      .map(emailSubscriber -> new EmailRecipient(emailSubscriber.getLogin(), emailSubscriber.getEmail()))
      .collect(Collectors.toSet());
  }

  private Stream<EmailSubscriberDto> keepAuthorizedEmailSubscribers(DbSession dbSession, String projectKey,
    Set<EmailSubscriberDto> emailSubscribers,
    SubscriberPermissionsOnProject requiredPermissions) {
    if (requiredPermissions.getGlobalSubscribers().equals(requiredPermissions.getProjectSubscribers())) {
      return keepAuthorizedEmailSubscribers(dbSession, projectKey, emailSubscribers, null, requiredPermissions.getGlobalSubscribers());
    } else {
      return Stream.concat(
        keepAuthorizedEmailSubscribers(dbSession, projectKey, emailSubscribers, true, requiredPermissions.getGlobalSubscribers()),
        keepAuthorizedEmailSubscribers(dbSession, projectKey, emailSubscribers, false, requiredPermissions.getProjectSubscribers()));
    }
  }

  private Stream<EmailSubscriberDto> keepAuthorizedEmailSubscribers(DbSession dbSession, String projectKey,
    Set<EmailSubscriberDto> emailSubscribers,
    @Nullable Boolean global, String permission) {
    Set<EmailSubscriberDto> subscribers = emailSubscribers.stream()
      .filter(s -> global == null || s.isGlobal() == global)
      .collect(Collectors.toSet());
    if (subscribers.isEmpty()) {
      return Stream.empty();
    }

    Set<String> logins = subscribers.stream()
      .map(EmailSubscriberDto::getLogin)
      .collect(Collectors.toSet());
    Set<String> authorizedLogins = dbClient.authorizationDao().keepAuthorizedLoginsOnEntity(dbSession, logins, projectKey, permission);
    return subscribers.stream()
      .filter(s -> authorizedLogins.contains(s.getLogin()));
  }

  @VisibleForTesting
  protected List<NotificationChannel> getChannels() {
    return Arrays.asList(notificationChannels);
  }

}
