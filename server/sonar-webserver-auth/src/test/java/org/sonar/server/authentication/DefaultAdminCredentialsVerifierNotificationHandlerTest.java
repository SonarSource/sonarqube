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

import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;

public class DefaultAdminCredentialsVerifierNotificationHandlerTest {

  @Rule
  public DbTester db = DbTester.create();

  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);

  private DefaultAdminCredentialsVerifierNotificationHandler underTest = new DefaultAdminCredentialsVerifierNotificationHandler(db.getDbClient(),
    emailNotificationChannel);

  @Before
  public void setUp() {
    when(emailNotificationChannel.deliverAll(anySet()))
      .then((Answer<Integer>) invocationOnMock -> ((Set<EmailNotificationChannel.EmailDeliveryRequest>) invocationOnMock.getArguments()[0]).size());
  }

  @Test
  public void deliver_to_all_admins_having_emails() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    DefaultAdminCredentialsVerifierNotification detectActiveAdminAccountWithDefaultCredentialNotification = mock(DefaultAdminCredentialsVerifierNotification.class);
    // Users granted admin permission directly
    UserDto admin1 = db.users().insertUser(u -> u.setEmail("admin1"));
    UserDto adminWithNoEmail = db.users().insertUser(u -> u.setEmail(null));
    db.users().insertPermissionOnUser(admin1, ADMINISTER);
    db.users().insertPermissionOnUser(adminWithNoEmail, ADMINISTER);
    // User granted admin permission by group membership
    UserDto admin2 = db.users().insertUser(u -> u.setEmail("admin2"));
    GroupDto adminGroup = db.users().insertGroup();
    db.users().insertPermissionOnGroup(adminGroup, ADMINISTER);
    db.users().insertMember(adminGroup, admin2);
    db.users().insertUser(u -> u.setEmail("otherUser"));

    int deliver = underTest.deliver(singletonList(detectActiveAdminAccountWithDefaultCredentialNotification));

    // Only 2 admins have there email defined
    assertThat(deliver).isEqualTo(2);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(anySet());
    verifyNoMoreInteractions(detectActiveAdminAccountWithDefaultCredentialNotification);
  }

  @Test
  public void deliver_to_no_one_when_no_admins() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    DefaultAdminCredentialsVerifierNotification detectActiveAdminAccountWithDefaultCredentialNotification = mock(DefaultAdminCredentialsVerifierNotification.class);
    db.users().insertUser(u -> u.setEmail("otherUser"));

    int deliver = underTest.deliver(singletonList(detectActiveAdminAccountWithDefaultCredentialNotification));

    assertThat(deliver).isZero();
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    verifyNoMoreInteractions(detectActiveAdminAccountWithDefaultCredentialNotification);
  }

  @Test
  public void do_nothing_if_emailNotificationChannel_is_disabled() {
    when(emailNotificationChannel.isActivated()).thenReturn(false);
    DefaultAdminCredentialsVerifierNotification detectActiveAdminAccountWithDefaultCredentialNotification = mock(
      DefaultAdminCredentialsVerifierNotification.class);

    int deliver = underTest.deliver(singletonList(detectActiveAdminAccountWithDefaultCredentialNotification));

    assertThat(deliver).isZero();
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    verifyNoMoreInteractions(detectActiveAdminAccountWithDefaultCredentialNotification);
  }

  @Test
  public void getMetadata_returns_empty() {
    assertThat(underTest.getMetadata()).isEmpty();
  }

  @Test
  public void getNotificationClass() {
    assertThat(underTest.getNotificationClass()).isEqualTo(DefaultAdminCredentialsVerifierNotification.class);
  }

}
