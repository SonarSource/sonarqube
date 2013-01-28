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
package org.sonar.plugins.emailnotifications.newviolations;

import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.core.properties.PropertiesDao;

import java.util.List;

/**
 * This dispatcher means: "notify me when new violations are introduced on projects that I flagged as favourite".
 * 
 * @since 2.14
 */
public class NewViolationsOnMyFavouriteProject extends NotificationDispatcher {

  private PropertiesDao propertiesDao;

  public NewViolationsOnMyFavouriteProject(PropertiesDao propertiesDao) {
    super("new-violations");
    this.propertiesDao = propertiesDao;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    Long projectId = Long.parseLong(notification.getFieldValue("projectId"));
    List<String> userLogins = propertiesDao.findUserIdsForFavouriteResource(projectId);
    for (String userLogin : userLogins) {
      context.addUser(userLogin);
    }
  }

}
