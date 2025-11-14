/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.notification.NotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.server.property.InternalProperties.DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL;

public class DefaultAdminCredentialsVerifierImplIT {

  private static final String ADMIN_LOGIN = "admin";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final NotificationManager notificationManager = mock(NotificationManager.class);

  private final DefaultAdminCredentialsVerifierImpl underTest = new DefaultAdminCredentialsVerifierImpl(db.getDbClient(), localAuthentication, notificationManager);

  @Test
  public void correctly_detect_if_admin_account_is_used_with_default_credential() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "admin");
    assertThat(underTest.hasDefaultCredentialUser()).isTrue();

    changePassword(admin, "1234");
    assertThat(underTest.hasDefaultCredentialUser()).isFalse();
  }

  @Test
  public void does_not_break_if_admin_account_does_not_exist() {
    assertThat(underTest.hasDefaultCredentialUser()).isFalse();
  }

  @Test
  public void set_reset_flag_to_true_and_add_log_when_admin_account_with_default_credential_is_detected() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "admin");

    underTest.runAtStart();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isTrue();
    assertThat(logTester.logs(Level.WARN)).contains("Default Administrator credentials are still being used. Make sure to change the password or deactivate the account.");
    assertThat(db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL)).contains("true");
    verify(notificationManager).scheduleForSending(any(Notification.class));
  }

  @Test
  public void do_not_send_email_to_admins_when_already_sent() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "admin");
    db.getDbClient().internalPropertiesDao().save(db.getSession(), DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL, "true");
    db.commit();

    underTest.runAtStart();

    verifyNoMoreInteractions(notificationManager);
  }

  @Test
  public void do_nothing_when_admin_is_not_using_default_credential() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "something_else");

    underTest.runAtStart();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
    verifyNoMoreInteractions(notificationManager);
  }

  @Test
  public void do_nothing_when_no_admin_account_with_default_credential_detected() {
    UserDto otherUser = db.users().insertUser();
    changePassword(otherUser, "admin");

    underTest.runAtStart();

    assertThat(db.users().selectUserByLogin(otherUser.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
    verifyNoMoreInteractions(notificationManager);
  }

  @Test
  public void do_nothing_when_admin_account_with_default_credential_is_disabled() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN).setActive(false));
    changePassword(admin, "admin");

    underTest.runAtStart();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
    verifyNoMoreInteractions(notificationManager);
  }

  private void changePassword(UserDto user, String password) {
    localAuthentication.storeHashPassword(user, password);
    db.getDbClient().userDao().update(db.getSession(), user);
    db.commit();
  }
}
