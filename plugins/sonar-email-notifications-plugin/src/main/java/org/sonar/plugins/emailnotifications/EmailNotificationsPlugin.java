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
package org.sonar.plugins.emailnotifications;

import com.google.common.collect.ImmutableList;
import org.sonar.api.SonarPlugin;
import org.sonar.api.notifications.NotificationDispatcherMetadata;
import org.sonar.plugins.emailnotifications.alerts.AlertsEmailTemplate;
import org.sonar.plugins.emailnotifications.alerts.AlertsOnMyFavouriteProject;
import org.sonar.plugins.emailnotifications.newviolations.NewViolationsEmailTemplate;
import org.sonar.plugins.emailnotifications.newviolations.NewViolationsOnMyFavouriteProject;
import org.sonar.plugins.emailnotifications.reviews.ChangesInReviewAssignedToMeOrCreatedByMe;
import org.sonar.plugins.emailnotifications.reviews.ReviewEmailTemplate;

import java.util.List;

public class EmailNotificationsPlugin extends SonarPlugin {
  public List<?> getExtensions() {
    return ImmutableList.of(
        EmailNotificationChannel.class,

        // Notify incoming violations on my favourite projects
        NewViolationsOnMyFavouriteProject.class,
        NotificationDispatcherMetadata.create("NewViolationsOnMyFavouriteProject")
            .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, "true"),
        NewViolationsEmailTemplate.class,

        // Notify reviews changes
        ChangesInReviewAssignedToMeOrCreatedByMe.class,
        NotificationDispatcherMetadata.create("ChangesInReviewAssignedToMeOrCreatedByMe")
            .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, "true"),
        ReviewEmailTemplate.class,

        // Notify alerts on my favourite projects
        AlertsOnMyFavouriteProject.class,
        NotificationDispatcherMetadata.create("AlertsOnMyFavouriteProject")
            .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, "true"),
        AlertsEmailTemplate.class

        );
  }
}
