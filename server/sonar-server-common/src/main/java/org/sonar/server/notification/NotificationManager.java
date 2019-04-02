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

import com.google.common.collect.Multimap;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;

import static java.util.Objects.requireNonNull;

/**
 * The notification manager receives notifications and is in charge of storing them so that they are processed by the notification service.
 * <p>
 * Pico provides an instance of this class, and plugins just need to create notifications and pass them to this manager with
 * the {@link NotificationManager#scheduleForSending(Notification)} method.
 * </p>
 */
public interface NotificationManager {

  /**
   * Receives a notification and stores it so that it is processed by the notification service.
   *
   * @param notification the notification.
   */
  <T extends Notification> void scheduleForSending(T notification);

  /**
   * <p>
   * Returns the list of users who subscribed to the given dispatcher, along with the notification channels (email, twitter, ...) that they choose
   * for this dispatcher.
   * </p>
   * <p>
   * The resource ID can be null in case of notifications that have nothing to do with a specific project (like system notifications).
   * </p>
   *
   * @param dispatcher the dispatcher for which this list of users is requested
   * @param projectKey key of the project
   * @param subscriberPermissionsOnProject the required permission for global and project subscribers
   *
   * @return the list of user login along with the subscribed channels
   */
  Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, String projectKey,
    SubscriberPermissionsOnProject subscriberPermissionsOnProject);


  @Immutable
  final class EmailRecipient {
    private final String login;
    private final String email;

    public EmailRecipient(String login, String email) {
      this.login = requireNonNull(login, "login can't be null");
      this.email = requireNonNull(email, "email can't be null");
    }

    public String getLogin() {
      return login;
    }

    public String getEmail() {
      return email;
    }

    @Override
    public String toString() {
      return "EmailRecipient{" + "'" + login + '\'' + ":'" + email + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EmailRecipient that = (EmailRecipient) o;
      return login.equals(that.login) && email.equals(that.email);
    }

    @Override
    public int hashCode() {
      return Objects.hash(login, email);
    }
  }

  /**
   * Find login and email of users which have subscribed to the email notification of the specified {@code dispatcherKey}.
   * <p>
   * Obviously, only subscribers which have an email are returned.
   */
  Set<EmailRecipient> findSubscribedEmailRecipients(String dispatcherKey, String projectKey, SubscriberPermissionsOnProject subscriberPermissionsOnProject);

  /**
   * Find email of users with the specified {@code logins} which have subscribed to the email notification of the
   * specified {@code dispatcherKey}.
   * <p>
   * Obviously, only subscribers which have an email are returned.
   */
  Set<EmailRecipient> findSubscribedEmailRecipients(String dispatcherKey, String projectKey, Set<String> logins, SubscriberPermissionsOnProject subscriberPermissionsOnProject);

  final class SubscriberPermissionsOnProject {
    public static final SubscriberPermissionsOnProject ALL_MUST_HAVE_ROLE_USER = new SubscriberPermissionsOnProject(UserRole.USER);

    private final String globalSubscribers;
    private final String projectSubscribers;

    public SubscriberPermissionsOnProject(String globalAndProjectSubscribers) {
      this(globalAndProjectSubscribers, globalAndProjectSubscribers);
    }

    public SubscriberPermissionsOnProject(String globalSubscribers, String projectSubscribers) {
      this.globalSubscribers = requireNonNull(globalSubscribers, "global subscribers's required permission can't be null");
      this.projectSubscribers = requireNonNull(projectSubscribers, "project subscribers's required permission can't be null");
    }

    public String getGlobalSubscribers() {
      return globalSubscribers;
    }

    public String getProjectSubscribers() {
      return projectSubscribers;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SubscriberPermissionsOnProject that = (SubscriberPermissionsOnProject) o;
      return globalSubscribers.equals(that.globalSubscribers) && projectSubscribers.equals(that.projectSubscribers);
    }

    @Override
    public int hashCode() {
      return Objects.hash(globalSubscribers, projectSubscribers);
    }
  }
}
