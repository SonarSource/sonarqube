/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.notification;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Date;
import java.util.List;

/**
 * @since 2.10
 */
public class DefaultNotificationManager implements NotificationManager {

  private DatabaseSessionFactory sessionFactory;

  public DefaultNotificationManager(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void scheduleForSending(Notification notification) {
    NotificationQueueElement notificationQueueElement = new NotificationQueueElement();
    notificationQueueElement.setCreatedAt(new Date());
    notificationQueueElement.setNotification(notification);
    DatabaseSession session = sessionFactory.getSession();
    session.save(notificationQueueElement);
    session.commit();
  }

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

  public boolean isEnabled(String username, String channelKey, String dispatcherKey) {
    DatabaseSession session = sessionFactory.getSession();
    User user = session.getSingleResult(User.class, "login", username);
    String notificationKey = "notification." + dispatcherKey + "." + channelKey;
    Property property = session.getSingleResult(Property.class, "userId", user.getId(), "key", notificationKey);
    return property != null && "true".equals(property.getValue());
  }

}
