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
package org.sonar.server.issue.notification;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static java.util.Collections.emptySet;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class MyNewIssuesNotificationHandler extends EmailNotificationHandler<MyNewIssuesNotification> {
  public static final String KEY = "SQ-MyNewIssues";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private final NotificationManager notificationManager;

  public MyNewIssuesNotificationHandler(NotificationManager notificationManager, EmailNotificationChannel emailNotificationChannel) {
    super(emailNotificationChannel);
    this.notificationManager = notificationManager;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<MyNewIssuesNotification> getNotificationClass() {
    return MyNewIssuesNotification.class;
  }

  @Override
  public Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<MyNewIssuesNotification> notifications) {
    Multimap<String, MyNewIssuesNotification> notificationsByProjectKey = notifications.stream()
      .filter(t -> t.getProjectKey() != null)
      .filter(t -> t.getAssignee() != null)
      .collect(index(MyNewIssuesNotification::getProjectKey));
    if (notificationsByProjectKey.isEmpty()) {
      return emptySet();
    }

    return notificationsByProjectKey.asMap().entrySet()
      .stream()
      .flatMap(e -> toEmailDeliveryRequests(e.getKey(), e.getValue()))
      .collect(MoreCollectors.toSet(notifications.size()));
  }

  private Stream<? extends EmailDeliveryRequest> toEmailDeliveryRequests(String projectKey, Collection<MyNewIssuesNotification> notifications) {
    Set<String> assignees = notifications.stream()
      .map(MyNewIssuesNotification::getAssignee)
      .collect(Collectors.toSet());
    Map<String, NotificationManager.EmailRecipient> recipientsByLogin = notificationManager
      .findSubscribedEmailRecipients(KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER)
      .stream()
      .collect(MoreCollectors.uniqueIndex(NotificationManager.EmailRecipient::getLogin));
    return notifications.stream()
      .map(notification -> toEmailDeliveryRequest(recipientsByLogin, notification))
      .filter(Objects::nonNull);
  }

  @CheckForNull
  private static EmailDeliveryRequest toEmailDeliveryRequest(Map<String, NotificationManager.EmailRecipient> recipientsByLogin,
    MyNewIssuesNotification notification) {
    String assignee = notification.getAssignee();

    NotificationManager.EmailRecipient emailRecipient = recipientsByLogin.get(assignee);
    if (emailRecipient != null) {
      return new EmailDeliveryRequest(emailRecipient.getEmail(), notification);
    }
    return null;
  }

}
