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
package org.sonar.api.notifications;

import org.sonar.api.ServerExtension;

/**
 * <p>
 * Plugins should extend this class to provide logic to determine which users are interested in receiving notifications.
 * It has no knowledge about the way of delivery.
 * </p>
 * For example:
 * <ul>
 * <li>notify me when someone comments on review created by me</li>
 * <li>notify me when someone comments on review assigned to me</li>
 * <li>notify me when someone mentions me in comment for review</li>
 * <li>send me system notifications (like password reset, account creation, ...)</li>
 * </ul> 
 * 
 * @since 2.10
 */
public abstract class NotificationDispatcher implements ServerExtension {

  /**
   * Additional information related to the notification, which will be used
   * to know who should receive the notification.
   */
  public interface Context {
    /**
     * Adds a user that will be notified.
     * 
     * @param userLogin the user login
     */
    void addUser(String userLogin);
  }

  /**
   * Returns the unique key of this dispatcher. 
   * 
   * @return the key
   */
  public String getKey() {
    return getClass().getSimpleName();
  }

  /**
   * <p>
   * Implements the logic that defines which users will receive the notification.
   * </p>
   * The purpose of this method is to populate the context object with users, based on the type of notification and the content of the notification.
   * 
   * @param notification the notification that will be sent
   * @param the context linked to this notification
   */
  public abstract void dispatch(Notification notification, Context context);

  @Override
  public String toString() {
    return getKey();
  }

}
