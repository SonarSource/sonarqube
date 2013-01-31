/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.notifications.violations;

import com.google.common.collect.Multimap;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.core.properties.PropertiesDao;

import java.util.Collection;
import java.util.List;

/**
 * This dispatcher means: "notify me when new violations are introduced on projects that I flagged as favourite".
 * 
 * @since 2.14
 */
public class NewViolationsOnMyFavouriteProject extends NotificationDispatcher {

  private NotificationManager notificationManager;
  private PropertiesDao propertiesDao;

  public NewViolationsOnMyFavouriteProject(NotificationManager notificationManager, PropertiesDao propertiesDao) {
    super("new-violations");
    this.notificationManager = notificationManager;
    this.propertiesDao = propertiesDao;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    // "null" is passed as a 2nd argument because this dispatcher is not a per-project dispatcher
    Multimap<String, NotificationChannel> subscribedRecipients = notificationManager.findSubscribedRecipientsForDispatcher(this, null);

    List<String> userLogins = propertiesDao.findUserIdsForFavouriteResource(Long.parseLong(notification.getFieldValue("projectId")));
    for (String userLogin : userLogins) {
      Collection<NotificationChannel> channels = subscribedRecipients.get(userLogin);
      for (NotificationChannel channel : channels) {
        context.addUser(userLogin, channel);
      }
    }
  }

}
