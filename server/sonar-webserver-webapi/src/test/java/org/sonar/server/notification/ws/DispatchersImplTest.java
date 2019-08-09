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
package org.sonar.server.notification.ws;

import org.junit.Test;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.issue.notification.FPOrWontFixNotificationHandler;
import org.sonar.server.issue.notification.MyNewIssuesNotificationHandler;
import org.sonar.server.issue.notification.NewIssuesNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.qualitygate.notification.QGChangeNotificationHandler;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;

public class DispatchersImplTest {

  private NotificationCenter notificationCenter = new NotificationCenter(
    new NotificationDispatcherMetadata[] {
      NotificationDispatcherMetadata.create(MyNewIssuesNotificationHandler.KEY)
        .setProperty(GLOBAL_NOTIFICATION, "true")
        .setProperty(PER_PROJECT_NOTIFICATION, "true"),
      NotificationDispatcherMetadata.create(NewIssuesNotificationHandler.KEY)
        .setProperty(GLOBAL_NOTIFICATION, "false"),
      NotificationDispatcherMetadata.create(QGChangeNotificationHandler.KEY)
        .setProperty(GLOBAL_NOTIFICATION, "true")
        .setProperty(PER_PROJECT_NOTIFICATION, "true"),
      NotificationDispatcherMetadata.create(FPOrWontFixNotificationHandler.KEY)
        .setProperty(GLOBAL_NOTIFICATION, "false")
        .setProperty(PER_PROJECT_NOTIFICATION, "true")
    },
    new NotificationChannel[] {});

  private DispatchersImpl underTest = new DispatchersImpl(notificationCenter);

  @Test
  public void get_sorted_global_dispatchers() {
    underTest.start();

    assertThat(underTest.getGlobalDispatchers()).containsExactly(
      QGChangeNotificationHandler.KEY, MyNewIssuesNotificationHandler.KEY);
  }

  @Test
  public void get_sorted_project_dispatchers() {
    underTest.start();

    assertThat(underTest.getProjectDispatchers()).containsExactly(
      QGChangeNotificationHandler.KEY, FPOrWontFixNotificationHandler.KEY, MyNewIssuesNotificationHandler.KEY);
  }

}
