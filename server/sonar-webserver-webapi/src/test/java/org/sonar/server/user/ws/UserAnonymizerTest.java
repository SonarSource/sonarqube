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
package org.sonar.server.user.ws;

import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ExternalIdentity;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAnonymizerTest {
  private final System2 system2 = new AlwaysIncreasingSystem2();
  @Rule
  public DbTester db = DbTester.create(system2);
  private final UserAnonymizer userAnonymizer = new UserAnonymizer(db.getDbClient());

  @Test
  public void anonymize_user() {
    UserDto user = db.users().insertUser(u -> u.setLogin("login1"));
    userAnonymizer.anonymize(db.getSession(), user);
    assertThat(user.getLogin()).startsWith("sq-removed-");
    assertThat(user.getExternalIdentityProvider()).isEqualTo(ExternalIdentity.SQ_AUTHORITY);
    assertThat(user.getExternalId()).isEqualTo(user.getLogin());
    assertThat(user.getExternalLogin()).isEqualTo(user.getLogin());
    assertThat(user.getName()).isEqualTo(user.getLogin());
  }

  @Test
  public void try_avoid_login_collisions() {
    List<String> logins = List.of("login1", "login2", "login3");
    Iterator<String> randomGeneratorIt = logins.iterator();
    UserAnonymizer userAnonymizer = new UserAnonymizer(db.getDbClient(), randomGeneratorIt::next);

    UserDto user1 = db.users().insertUser(u -> u.setLogin("login1"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("login2"));
    UserDto userToAnonymize = db.users().insertUser(u -> u.setLogin("toAnonymize").setActive(false));

    userAnonymizer.anonymize(db.getSession(), userToAnonymize);
    assertThat(userToAnonymize.getLogin()).isEqualTo("login3");
  }

}
