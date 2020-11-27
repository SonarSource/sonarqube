/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

package org.sonar.server.startup;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;

import static org.assertj.core.api.Assertions.assertThat;

public class DetectActiveAdminAccountWithDefaultCredentialTest {

  private static final String ADMIN_LOGIN = "admin";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());

  private final DetectActiveAdminAccountWithDefaultCredential underTest = new DetectActiveAdminAccountWithDefaultCredential(db.getDbClient(), localAuthentication);

  @After
  public void after() {
    underTest.stop();
  }

  @Test
  public void set_reset_flag_to_true_and_add_log_when_admin_account_with_default_credential_is_detected() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "admin");

    underTest.start();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isTrue();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Default Administrator credentials are still being used. Make sure to change the password or deactivate the account.");
  }

  @Test
  public void do_nothing_when_admin_is_not_using_default_credential() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN));
    changePassword(admin, "something_else");

    underTest.start();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_admin_account_with_default_credential_detected() {
    UserDto otherUser = db.users().insertUser();
    changePassword(otherUser, "admin");

    underTest.start();

    assertThat(db.users().selectUserByLogin(otherUser.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void do_nothing_when_admin_account_with_default_credential_is_disabled() {
    UserDto admin = db.users().insertUser(u -> u.setLogin(ADMIN_LOGIN).setActive(false));
    changePassword(admin, "admin");

    underTest.start();

    assertThat(db.users().selectUserByLogin(admin.getLogin()).get().isResetPassword()).isFalse();
    assertThat(logTester.logs()).isEmpty();
  }

  private void changePassword(UserDto user, String password) {
    localAuthentication.storeHashPassword(user, password);
    db.getDbClient().userDao().update(db.getSession(), user);
    db.commit();
  }
}
