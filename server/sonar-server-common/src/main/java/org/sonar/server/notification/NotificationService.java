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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;

@ServerSide
@ComputeEngineSide
public class NotificationService {

  private static final Logger LOG = Loggers.get(NotificationService.class);

  private final List<NotificationDispatcher> dispatchers;
  private final DbClient dbClient;

  public NotificationService(DbClient dbClient, NotificationDispatcher[] dispatchers) {
    this.dbClient = dbClient;
    this.dispatchers = ImmutableList.copyOf(dispatchers);
  }

  /**
   * Default constructor when no dispatchers.
   */
  public NotificationService(DbClient dbClient) {
    this(dbClient, new NotificationDispatcher[0]);
  }

  public void deliver(Notification notification) {
    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationDispatcher dispatcher : dispatchers) {
      NotificationDispatcher.Context context = new ContextImpl(recipients);
      try {
        dispatcher.performDispatch(notification, context);
      } catch (Exception e) {
        // catch all exceptions in order to dispatch using other dispatchers
        LOG.warn(String.format("Unable to dispatch notification %s using %s", notification, dispatcher), e);
      }
    }
    dispatch(notification, recipients);
  }

  private static void dispatch(Notification notification, SetMultimap<String, NotificationChannel> recipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> entry : recipients.asMap().entrySet()) {
      String username = entry.getKey();
      Collection<NotificationChannel> userChannels = entry.getValue();
      LOG.debug("For user {} via {}", username, userChannels);
      for (NotificationChannel channel : userChannels) {
        try {
          channel.deliver(notification, username);
        } catch (Exception e) {
          // catch all exceptions in order to deliver via other channels
          LOG.warn("Unable to deliver notification " + notification + " for user " + username + " via " + channel, e);
        }
      }
    }
  }

  @VisibleForTesting
  List<NotificationDispatcher> getDispatchers() {
    return dispatchers;
  }

  /**
   * Returns true if at least one user is subscribed to at least one notifications with given types.
   * Subscription can be globally or on the specific project.
   */
  public boolean hasProjectSubscribersForTypes(String projectUuid, Set<String> notificationTypes) {
    Collection<String> dispatcherKeys = new ArrayList<>();
    for (NotificationDispatcher dispatcher : dispatchers) {
      if (notificationTypes.contains(dispatcher.getType())) {
        dispatcherKeys.add(dispatcher.getKey());
      }
    }

    return dbClient.propertiesDao().hasProjectNotificationSubscribersForDispatchers(projectUuid, dispatcherKeys);
  }

  private static class ContextImpl implements NotificationDispatcher.Context {
    private final Multimap<String, NotificationChannel> recipients;

    ContextImpl(Multimap<String, NotificationChannel> recipients) {
      this.recipients = recipients;
    }

    @Override
    public void addUser(@Nullable String userLogin, NotificationChannel notificationChannel) {
      if (userLogin != null) {
        recipients.put(userLogin, notificationChannel);
      }
    }
  }
}
