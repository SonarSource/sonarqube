/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.usertoken.notification;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.DbClient;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDao;
import org.sonar.db.user.UserTokenDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenExpirationNotificationSenderTest {
  @Rule
  public LogTester logTester = new LogTester();
  private final DbClient dbClient = mock(DbClient.class);
  private final TokenExpirationEmailComposer emailComposer = mock(TokenExpirationEmailComposer.class);
  private final TokenExpirationNotificationSender underTest = new TokenExpirationNotificationSender(dbClient, emailComposer);

  @Test
  public void no_notification_when_email_setting_is_not_set() {
    logTester.setLevel(Level.DEBUG);
    when(emailComposer.areEmailSettingsSet()).thenReturn(false);
    underTest.sendNotifications();
    assertThat(logTester.getLogs(Level.DEBUG))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("Emails for token expiration notification have not been sent because email settings are not configured.");
  }

  @Test
  public void send_notification() {
    var expiringToken = new UserTokenDto().setUserUuid("admin");
    var expiredToken = new UserTokenDto().setUserUuid("admin");
    var user = mock(UserDto.class);
    when(user.getUuid()).thenReturn("admin");
    when(user.getEmail()).thenReturn("admin@admin.com");
    var userTokenDao = mock(UserTokenDao.class);
    var userDao = mock(UserDao.class);
    when(userDao.selectByUuids(any(), any())).thenReturn(List.of(user));
    when(userTokenDao.selectTokensExpiredInDays(any(), eq(7L))).thenReturn(List.of(expiringToken));
    when(userTokenDao.selectTokensExpiredInDays(any(), eq(0L))).thenReturn(List.of(expiredToken));
    when(dbClient.userTokenDao()).thenReturn(userTokenDao);
    when(dbClient.userDao()).thenReturn(userDao);
    when(emailComposer.areEmailSettingsSet()).thenReturn(true);

    underTest.sendNotifications();

    var argumentCaptor = ArgumentCaptor.forClass(TokenExpirationEmail.class);
    verify(emailComposer, times(2)).send(argumentCaptor.capture());
    List<TokenExpirationEmail> emails = argumentCaptor.getAllValues();
    assertThat(emails).hasSize(2);
    assertThat(emails.get(0).getRecipients()).containsOnly("admin@admin.com");
    assertThat(emails.get(0).getUserToken()).isEqualTo(expiringToken);
    assertThat(emails.get(1).getRecipients()).containsOnly("admin@admin.com");
    assertThat(emails.get(1).getUserToken()).isEqualTo(expiredToken);
  }

}
