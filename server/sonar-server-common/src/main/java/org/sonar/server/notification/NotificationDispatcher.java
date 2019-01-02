/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.server.ServerSide;

/**
 * <p>
 * Plugins should extend this class to provide logic to determine which users are interested in receiving notifications,
 * along with which delivery channels they selected.
 * </p>
 * For example:
 * <ul>
 * <li>notify me by email when someone comments an issue reported by me</li>
 * <li>notify me by twitter when someone comments an issue assigned to me</li>
 * <li>notify me by Jabber when someone mentions me in an issue comment</li>
 * <li>send me by SMS when there are system notifications (like password reset, account creation, ...)</li>
 * </ul> 
  */
@ServerSide
@ComputeEngineSide
@ExtensionPoint
public abstract class NotificationDispatcher {

  private final String notificationType;

  /**
   * Additional information related to the notification, which will be used
   * to know who should receive the notification.
   */
  public interface Context {
    /**
     * Adds a user that will be notified through the given notification channel.
     * 
     * @param userLogin the user login
     * @param notificationChannel the notification channel to use for this user
     */
    void addUser(String userLogin, NotificationChannel notificationChannel);
  }

  /**
   * Creates a new dispatcher for notifications of the given type.
   * 
   * @param notificationType the type of notifications handled by this dispatcher
   */
  public NotificationDispatcher(String notificationType) {
    this.notificationType = notificationType;
  }

  /**
   * Creates a new generic dispatcher, used for any kind of notification. 
   * <p/>
   * Should be avoided and replaced by the other constructor - as it is easier to understand that a
   * dispatcher listens for a specific type of notification.
   */
  public NotificationDispatcher() {
    this("");
  }

  /**
   * The unique key of this dispatcher. By default it's the class name without the package prefix.
   * <p/>
   * The related label in l10n bundles is 'notification.dispatcher.<key>', for example 'notification.dispatcher.NewFalsePositive'.
   */
  public String getKey() {
    return getClass().getSimpleName();
  }

  /**
   * @since 5.1
   */
  public String getType() {
    return notificationType;
  }

  /**
   * <p>
   * Performs the dispatch.
   * </p>
   */
  public final void performDispatch(Notification notification, Context context) {
    if (StringUtils.equals(notification.getType(), notificationType) || StringUtils.equals("", notificationType)) {
      dispatch(notification, context);
    }
  }

  /**
   * <p>
   * Implements the logic that defines which users will receive the notification.
   * </p>
   * The purpose of this method is to populate the context object with users, based on the type of notification and the content of the notification.
   */
  public abstract void dispatch(Notification notification, Context context);

  @Override
  public String toString() {
    return getKey();
  }

}
