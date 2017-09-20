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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
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
  public Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, String projectUuid) {
    requireNonNull(projectUuid, "ProjectUUID is mandatory");
    String dispatcherKey = dispatcher.getKey();

    Set<SubscriberAndChannel> subscriberAndChannels = Arrays.stream(notificationChannels)
      .flatMap(notificationChannel -> toSubscriberAndChannels(dispatcherKey, projectUuid, notificationChannel))
      .collect(Collectors.toSet());

    if (subscriberAndChannels.isEmpty()) {
      return ImmutableMultimap.of();
    }

    ImmutableSetMultimap.Builder<String, NotificationChannel> builder = ImmutableSetMultimap.builder();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> authorizedLogins = keepAuthorizedLogins(projectUuid, subscriberAndChannels, dbSession);
      subscriberAndChannels.stream()
        .filter(subscriberAndChannel -> authorizedLogins.contains(subscriberAndChannel.getSubscriber().getLogin()))
        .forEach(subscriberAndChannel -> builder.put(subscriberAndChannel.getSubscriber().getLogin(), subscriberAndChannel.getChannel()));
    }
    return builder.build();
  }

  private Stream<SubscriberAndChannel> toSubscriberAndChannels(String dispatcherKey, String projectUuid, NotificationChannel notificationChannel) {
    return dbClient.propertiesDao().findUsersForNotification(dispatcherKey, notificationChannel.getKey(), projectUuid)
      .stream()
      .map(login -> new SubscriberAndChannel(login, notificationChannel));
  }

  private Set<String> keepAuthorizedLogins(String projectUuid, Set<SubscriberAndChannel> subscriberAndChannels, DbSession dbSession) {
    Set<String> logins = subscriberAndChannels.stream()
      .map(DefaultNotificationManager.SubscriberAndChannel::getSubscriber)
      .map(Subscriber::getLogin)
      .collect(Collectors.toSet());
    return dbClient.authorizationDao().keepAuthorizedLoginsOnProject(dbSession, logins, projectUuid, UserRole.USER);
  }

  private final class SubscriberAndChannel {
    private final Subscriber subscriber;
    private final NotificationChannel channel;

    private SubscriberAndChannel(Subscriber subscriber, NotificationChannel channel) {
      this.subscriber = subscriber;
      this.channel = channel;
    }

    public Subscriber getSubscriber() {
      return subscriber;
    }

    public NotificationChannel getChannel() {
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

  private static void addUsersToRecipientListForChannel(Collection<String> users, SetMultimap<String, NotificationChannel> recipients, NotificationChannel channel) {
    for (String username : users) {
      recipients.put(username, channel);
    }
  }
}
