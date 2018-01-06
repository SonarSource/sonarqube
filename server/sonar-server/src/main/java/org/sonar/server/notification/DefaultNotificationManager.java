/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.property.Subscriber;

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
  public Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher,
    String projectKey, SubscriberPermissionsOnProject subscriberPermissionsOnProject) {
    requireNonNull(projectKey, "projectKey is mandatory");
    String dispatcherKey = dispatcher.getKey();

    Set<SubscriberAndChannel> subscriberAndChannels = Arrays.stream(notificationChannels)
      .flatMap(notificationChannel -> toSubscriberAndChannels(dispatcherKey, projectKey, notificationChannel))
      .collect(Collectors.toSet());

    if (subscriberAndChannels.isEmpty()) {
      return ImmutableMultimap.of();
    }

    ImmutableSetMultimap.Builder<String, NotificationChannel> builder = ImmutableSetMultimap.builder();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> authorizedLogins = keepAuthorizedLogins(dbSession, projectKey, subscriberAndChannels, subscriberPermissionsOnProject);
      subscriberAndChannels.stream()
        .filter(subscriberAndChannel -> authorizedLogins.contains(subscriberAndChannel.getSubscriber().getLogin()))
        .forEach(subscriberAndChannel -> builder.put(subscriberAndChannel.getSubscriber().getLogin(), subscriberAndChannel.getChannel()));
    }
    return builder.build();
  }

  private Stream<SubscriberAndChannel> toSubscriberAndChannels(String dispatcherKey, String projectKey, NotificationChannel notificationChannel) {
    Set<Subscriber> usersForNotification = dbClient.propertiesDao().findUsersForNotification(dispatcherKey, notificationChannel.getKey(), projectKey);
    return usersForNotification
      .stream()
      .map(login -> new SubscriberAndChannel(login, notificationChannel));
  }

  private Set<String> keepAuthorizedLogins(DbSession dbSession, String projectKey, Set<SubscriberAndChannel> subscriberAndChannels,
    SubscriberPermissionsOnProject requiredPermissions) {
    if (requiredPermissions.getGlobalSubscribers().equals(requiredPermissions.getProjectSubscribers())) {
      return keepAuthorizedLogins(dbSession, projectKey, subscriberAndChannels, null, requiredPermissions.getGlobalSubscribers());
    } else {
      return Stream
        .concat(
          keepAuthorizedLogins(dbSession, projectKey, subscriberAndChannels, true, requiredPermissions.getGlobalSubscribers()).stream(),
          keepAuthorizedLogins(dbSession, projectKey, subscriberAndChannels, false, requiredPermissions.getProjectSubscribers()).stream())
        .collect(Collectors.toSet());
    }
  }

  private Set<String> keepAuthorizedLogins(DbSession dbSession, String projectKey, Set<SubscriberAndChannel> subscriberAndChannels,
    @Nullable Boolean global, String permission) {
    Set<String> logins = subscriberAndChannels.stream()
      .filter(s -> global == null || s.getSubscriber().isGlobal() == global)
      .map(s -> s.getSubscriber().getLogin())
      .collect(Collectors.toSet());
    if (logins.isEmpty()) {
      return Collections.emptySet();
    }
    return dbClient.authorizationDao().keepAuthorizedLoginsOnProject(dbSession, logins, projectKey, permission);
  }

  private static final class SubscriberAndChannel {
    private final Subscriber subscriber;
    private final NotificationChannel channel;

    private SubscriberAndChannel(Subscriber subscriber, NotificationChannel channel) {
      this.subscriber = subscriber;
      this.channel = channel;
    }

    Subscriber getSubscriber() {
      return subscriber;
    }

    NotificationChannel getChannel() {
      return channel;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SubscriberAndChannel that = (SubscriberAndChannel) o;
      return Objects.equals(subscriber, that.subscriber) &&
        Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subscriber, channel);
    }
  }

  @VisibleForTesting
  protected List<NotificationChannel> getChannels() {
    return Arrays.asList(notificationChannels);
  }

}
