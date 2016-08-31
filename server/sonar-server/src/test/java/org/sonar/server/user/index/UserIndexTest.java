/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.user.index.UserIndexDefinition.INDEX;
import static org.sonar.server.user.index.UserIndexDefinition.TYPE_USER;

public class UserIndexTest {

  private static final String USER1_LOGIN = "user1";
  private static final String USER2_LOGIN = "user2";
  private static final long DATE_1 = 1_500_000_000_000L;
  private static final long DATE_2 = 1_500_000_000_001L;

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings()));

  private UserIndex index;

  @Before
  public void setUp() {
    index = new UserIndex(esTester.client());
  }

  @Test
  public void get_nullable_by_login() throws Exception {
    UserDoc user1 = newUser(USER1_LOGIN, asList("scmA", "scmB"));
    esTester.putDocuments(INDEX, TYPE_USER, user1);
    esTester.putDocuments(INDEX, TYPE_USER, newUser(USER2_LOGIN, Collections.<String>emptyList()));

    UserDoc userDoc = index.getNullableByLogin(USER1_LOGIN);
    assertThat(userDoc).isNotNull();
    assertThat(userDoc.login()).isEqualTo(user1.login());
    assertThat(userDoc.name()).isEqualTo(user1.name());
    assertThat(userDoc.email()).isEqualTo(user1.email());
    assertThat(userDoc.active()).isTrue();
    assertThat(userDoc.scmAccounts()).isEqualTo(user1.scmAccounts());
    assertThat(userDoc.createdAt()).isEqualTo(user1.createdAt());
    assertThat(userDoc.updatedAt()).isEqualTo(user1.updatedAt());

    assertThat(index.getNullableByLogin("")).isNull();
    assertThat(index.getNullableByLogin("unknown")).isNull();
  }

  @Test
  public void get_nullable_by_login_should_be_case_sensitive() throws Exception {
    UserDoc user1 = newUser(USER1_LOGIN, asList("scmA", "scmB"));
    esTester.putDocuments(INDEX, TYPE_USER, user1);

    assertThat(index.getNullableByLogin(USER1_LOGIN)).isNotNull();
    assertThat(index.getNullableByLogin("UsEr1")).isNull();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount() throws Exception {
    UserDoc user1 = newUser("user1", asList("user_1", "u1"));
    UserDoc user2 = newUser("user_with_same_email_as_user1", asList("user_2")).setEmail(user1.email());
    UserDoc user3 = newUser("inactive_user_with_same_scm_as_user1", user1.scmAccounts()).setActive(false);
    esTester.putDocuments(INDEX, TYPE_USER, user1);
    esTester.putDocuments(INDEX, TYPE_USER, user2);
    esTester.putDocuments(INDEX, TYPE_USER, user3);

    assertThat(index.getAtMostThreeActiveUsersForScmAccount(user1.scmAccounts().get(0))).extractingResultOf("login").containsOnly(user1.login());
    assertThat(index.getAtMostThreeActiveUsersForScmAccount(user1.login())).extractingResultOf("login").containsOnly(user1.login());

    // both users share the same email
    assertThat(index.getAtMostThreeActiveUsersForScmAccount(user1.email())).extractingResultOf("login").containsOnly(user1.login(), user2.login());

    assertThat(index.getAtMostThreeActiveUsersForScmAccount("")).isEmpty();
    assertThat(index.getAtMostThreeActiveUsersForScmAccount("unknown")).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_ignore_inactive_user() throws Exception {
    String scmAccount = "scmA";
    UserDoc user = newUser(USER1_LOGIN, asList(scmAccount)).setActive(false);
    esTester.putDocuments(INDEX, TYPE_USER, user);

    assertThat(index.getAtMostThreeActiveUsersForScmAccount(user.login())).isEmpty();
    assertThat(index.getAtMostThreeActiveUsersForScmAccount(scmAccount)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_max_three() throws Exception {
    String email = "user@mail.com";
    UserDoc user1 = newUser("user1", Collections.<String>emptyList()).setEmail(email);
    UserDoc user2 = newUser("user2", Collections.<String>emptyList()).setEmail(email);
    UserDoc user3 = newUser("user3", Collections.<String>emptyList()).setEmail(email);
    UserDoc user4 = newUser("user4", Collections.<String>emptyList()).setEmail(email);
    esTester.putDocuments(INDEX, TYPE_USER, user1);
    esTester.putDocuments(INDEX, TYPE_USER, user2);
    esTester.putDocuments(INDEX, TYPE_USER, user3);
    esTester.putDocuments(INDEX, TYPE_USER, user4);

    // restrict results to 3 users
    assertThat(index.getAtMostThreeActiveUsersForScmAccount(email)).hasSize(3);
  }

  @Test
  public void searchUsers() throws Exception {
    esTester.putDocuments(INDEX, TYPE_USER, newUser(USER1_LOGIN, Arrays.asList("user_1", "u1")));
    esTester.putDocuments(INDEX, TYPE_USER, newUser(USER2_LOGIN, Collections.<String>emptyList()));

    assertThat(index.search(null, new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search("user", new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search("ser", new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(USER1_LOGIN, new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(USER2_LOGIN, new SearchOptions()).getDocs()).hasSize(1);
  }

  private static UserDoc newUser(String login, List<String> scmAccounts) {
    return new UserDoc()
      .setLogin(login)
      .setName(login.toUpperCase(Locale.ENGLISH))
      .setEmail(login + "@mail.com")
      .setActive(true)
      .setScmAccounts(scmAccounts)
      .setCreatedAt(DATE_1)
      .setUpdatedAt(DATE_2);
  }
}
