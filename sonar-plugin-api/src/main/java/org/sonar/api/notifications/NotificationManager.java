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

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

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
public interface NotificationManager extends ServerComponent, BatchComponent {

  /**
   * Receives a notification and stores it so that it is processed by the notification service.
   * 
   * @param notification the notification.
   */
  void scheduleForSending(Notification notification);

}
