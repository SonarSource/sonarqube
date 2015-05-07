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
package org.sonar.api.notifications;

import com.google.common.collect.Multimap;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.batch.InstantiationStrategy;

import javax.annotation.Nullable;

import java.util.List;

/**
 * <p>
 * The notification manager receives notifications and is in charge of storing them so that they are processed by the notification service.
 * </p>
 * <p>
 * Pico provides an instance of this class, and plugins just need to create notifications and pass them to this manager with
 * the {@link NotificationManager#scheduleForSending(Notification)} method.
 * </p>
 *
 * @since 2.10
 */
@ServerSide
@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public interface NotificationManager {

  /**
   * Receives a notification and stores it so that it is processed by the notification service.
   *
   * @param notification the notification.
   */
  void scheduleForSending(Notification notification);

  /**
   * Receives notifications and stores them so that they are processed by the notification service.
   *
   * @param notifications the notifications.
   * @since 3.7.1
   */
  void scheduleForSending(List<Notification> notifications);

  /**
   * <p>
   * Returns the list of users who subscribed to the given dispatcher, along with the notification channels (email, twitter, ...) that they choose
   * for this dispatcher.
   * </p>
   * <p>
   * The resource ID can be null in case of notifications that have nothing to do with a specific project (like system notifications).
   * </p>
   *
   * @param dispatcher the dispatcher for which this list of users is requested
   * @param resourceId the optional resource which is concerned by this request
   * @return the list of user login along with the subscribed channels
   */
  Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, @Nullable Integer resourceId);

  Multimap<String, NotificationChannel> findNotificationSubscribers(NotificationDispatcher dispatcher, @Nullable String componentKey);
}
