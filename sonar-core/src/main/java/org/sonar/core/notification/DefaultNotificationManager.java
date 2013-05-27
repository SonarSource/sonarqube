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
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @since 2.10
 */
public class DefaultNotificationManager implements NotificationManager {

  private NotificationChannel[] notificationChannels;
  private DatabaseSessionFactory sessionFactory;
  private PropertiesDao propertiesDao;

  /**
   * Default constructor used by Pico
   */
  public DefaultNotificationManager(NotificationChannel[] channels, DatabaseSessionFactory sessionFactory, PropertiesDao propertiesDao) {
    this.notificationChannels = channels;
    this.sessionFactory = sessionFactory;
    this.propertiesDao = propertiesDao;
  }

  /**
   * Constructor if no notification channel
   */
  public DefaultNotificationManager(DatabaseSessionFactory sessionFactory, PropertiesDao propertiesDao) {
    this(new NotificationChannel[0], sessionFactory, propertiesDao);
  }

  /**
   * {@inheritDoc}
   */
  public void scheduleForSending(Notification notification) {
    NotificationQueueElement notificationQueueElement = new NotificationQueueElement();
    notificationQueueElement.setCreatedAt(new Date());
    notificationQueueElement.setNotification(notification);
    DatabaseSession session = sessionFactory.getSession();
    session.save(notificationQueueElement);
    session.commit();
  }

  /**
   * Give the notification queue so that it can be processed
   */
  public NotificationQueueElement getFromQueue() {
    DatabaseSession session = sessionFactory.getSession();
    String hql = "FROM " + NotificationQueueElement.class.getSimpleName() + " ORDER BY createdAt ASC";
    List<NotificationQueueElement> notifications = session.createQuery(hql).setMaxResults(1).getResultList();
    if (notifications.isEmpty()) {
      // UGLY - waiting for a clean way to manage JDBC connections without Hibernate - myBatis is coming soon
      // This code is highly coupled to org.sonar.server.notifications.NotificationService, which periodically executes
      // several times the methods getFromQueue() and isEnabled(). The session is closed only at the end of the task -
      // when there are no more notifications to process - to ensure "better" performances.
      sessionFactory.clear();
      return null;
    }
    NotificationQueueElement notification = notifications.get(0);
    session.removeWithoutFlush(notification);
    session.commit();
    return notification;

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
