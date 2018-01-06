/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbTester;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.notification.NotificationDispatcher.Context;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BuiltInQualityProfilesNotificationDispatcherTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  private Context context = mock(Context.class);
  private EmailNotificationChannel channel = mock(EmailNotificationChannel.class);
  private BuiltInQualityProfilesNotificationDispatcher underTest = new BuiltInQualityProfilesNotificationDispatcher(channel, dbTester.getDbClient());

  @Test
  public void send_email_to_quality_profile_administrator() {
    UserDto profileAdmin = dbTester.users().insertUser();
    dbTester.users().insertPermissionOnUser(profileAdmin, OrganizationPermission.ADMINISTER_QUALITY_PROFILES);

    underTest.dispatch(mock(Notification.class), context);

    verify(context).addUser(profileAdmin.getLogin(), channel);
  }

  @Test
  public void does_not_send_email_to_user_that_is_not_quality_profile_administrator() {
    dbTester.users().insertUser();

    underTest.dispatch(mock(Notification.class), context);

    verifyZeroInteractions(context);
  }
}
