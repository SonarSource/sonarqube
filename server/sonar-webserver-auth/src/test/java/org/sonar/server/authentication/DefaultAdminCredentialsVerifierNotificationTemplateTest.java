/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.authentication;

import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.EmailMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAdminCredentialsVerifierNotificationTemplateTest {

  private DefaultAdminCredentialsVerifierNotificationTemplate underTest = new DefaultAdminCredentialsVerifierNotificationTemplate();

  @Test
  public void do_not_format_other_notifications() {
    assertThat(underTest.format(new Notification("foo"))).isNull();
  }

  @Test
  public void format_notification() {
    Notification notification = new Notification(DefaultAdminCredentialsVerifierNotification.TYPE);

    EmailMessage emailMessage = underTest.format(notification);

    assertThat(emailMessage.getSubject()).isEqualTo("Default Administrator credentials are still used");
    assertThat(emailMessage.getMessage()).isEqualTo("""
      Hello,

      Your SonarQube instance is still using default administrator credentials.
      Make sure to change the password for the 'admin' account or deactivate this account.""");
  }

}
