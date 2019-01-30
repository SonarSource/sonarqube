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

package org.sonar.server.authentication;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserLastConnectionDatesUpdaterImplTest {

  private static final long NOW = 10_000_000_000L;
  private static final long ONE_MINUTE = 60_000L;
  private static final long ONE_HOUR = ONE_MINUTE * 60L;
  private static final long TWO_HOUR = ONE_HOUR * 2L;

  @Rule
  public DbTester db = DbTester.create();

  private System2 system2 = new TestSystem2().setNow(NOW);

  private UserLastConnectionDatesUpdaterImpl underTest = new UserLastConnectionDatesUpdaterImpl(db.getDbClient(), system2);

  @Test
  public void update_last_connection_date_from_user_when_last_connection_was_more_than_one_hour() {
    UserDto user = db.users().insertUser();
    db.users().updateLastConnectionDate(user, NOW - TWO_HOUR);

    underTest.updateLastConnectionDateIfNeeded(user);

    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLastConnectionDate()).isEqualTo(NOW);
  }

  @Test
  public void update_last_connection_date_from_user_when_no_last_connection_date() {
    UserDto user = db.users().insertUser();

    underTest.updateLastConnectionDateIfNeeded(user);

    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLastConnectionDate()).isEqualTo(NOW);
  }

  @Test
  public void do_not_update_when_last_connection_from_user_was_less_than_one_hour() {
    UserDto user = db.users().insertUser();
    db.users().updateLastConnectionDate(user, NOW - ONE_MINUTE);

    underTest.updateLastConnectionDateIfNeeded(user);

    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLastConnectionDate()).isEqualTo(NOW - ONE_MINUTE);
  }

  @Test
  public void update_last_connection_date_from_user_token_when_last_connection_was_more_than_one_hour() {
    UserDto user = db.users().insertUser();
    UserTokenDto userToken = db.users().insertToken(user);
    db.getDbClient().userTokenDao().update(db.getSession(), userToken.setLastConnectionDate(NOW - TWO_HOUR));
    db.commit();

    underTest.updateLastConnectionDateIfNeeded(userToken);

    UserTokenDto userTokenReloaded = db.getDbClient().userTokenDao().selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertThat(userTokenReloaded.getLastConnectionDate()).isEqualTo(NOW);
  }

  @Test
  public void update_last_connection_date_from_user_token_when_no_last_connection_date() {
    UserDto user = db.users().insertUser();
    UserTokenDto userToken = db.users().insertToken(user);

    underTest.updateLastConnectionDateIfNeeded(userToken);

    UserTokenDto userTokenReloaded = db.getDbClient().userTokenDao().selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertThat(userTokenReloaded.getLastConnectionDate()).isEqualTo(NOW);
  }

  @Test
  public void do_not_update_when_last_connection_from_user_token_was_less_than_one_hour() {
    UserDto user = db.users().insertUser();
    UserTokenDto userToken = db.users().insertToken(user);
    db.getDbClient().userTokenDao().update(db.getSession(), userToken.setLastConnectionDate(NOW - ONE_MINUTE));
    db.commit();

    underTest.updateLastConnectionDateIfNeeded(userToken);

    UserTokenDto userTokenReloaded = db.getDbClient().userTokenDao().selectByTokenHash(db.getSession(), userToken.getTokenHash());
    assertThat(userTokenReloaded.getLastConnectionDate()).isEqualTo(NOW - ONE_MINUTE);
  }
}
