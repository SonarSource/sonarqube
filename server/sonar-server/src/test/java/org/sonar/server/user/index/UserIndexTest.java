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
package org.sonar.server.user.index;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.user.index.UserIndexDefinition.INDEX_TYPE_USER;

public class UserIndexTest {

  private static final String ORGANIZATION_UUID = "my-organization";
  private static final String USER1_LOGIN = "user1";
  private static final String USER2_LOGIN = "user2";
  private static final long DATE_1 = 1_500_000_000_000L;
  private static final long DATE_2 = 1_500_000_000_001L;

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  private UserIndex underTest;

  private UserQuery.Builder userQuery = UserQuery.builder();

  @Before
  public void setUp() {
    underTest = new UserIndex(esTester.client(), System2.INSTANCE);
  }

  @Test
  public void get_nullable_by_login()  {
    UserDoc user1 = newUser(USER1_LOGIN, asList("scmA", "scmB"));
    esTester.putDocuments(INDEX_TYPE_USER, user1);
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER2_LOGIN, Collections.emptyList()));

    UserDoc userDoc = underTest.getNullableByLogin(USER1_LOGIN);
    assertThat(userDoc).isNotNull();
    assertThat(userDoc.login()).isEqualTo(user1.login());
    assertThat(userDoc.name()).isEqualTo(user1.name());
    assertThat(userDoc.email()).isEqualTo(user1.email());
    assertThat(userDoc.active()).isTrue();
    assertThat(userDoc.scmAccounts()).isEqualTo(user1.scmAccounts());

    assertThat(underTest.getNullableByLogin("")).isNull();
    assertThat(underTest.getNullableByLogin("unknown")).isNull();
  }

  @Test
  public void getNullableByLogin_is_case_sensitive()  {
    UserDoc user1 = newUser(USER1_LOGIN, asList("scmA", "scmB"));
    esTester.putDocuments(INDEX_TYPE_USER, user1);

    assertThat(underTest.getNullableByLogin(USER1_LOGIN)).isNotNull();
    assertThat(underTest.getNullableByLogin("UsEr1")).isNull();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_returns_the_users_with_specified_scm_account()  {
    UserDoc user1 = newUser("user1", asList("user_1", "u1"));
    UserDoc user2 = newUser("user_with_same_email_as_user1", asList("user_2")).setEmail(user1.email());
    UserDoc user3 = newUser("inactive_user_with_same_scm_as_user1", user1.scmAccounts()).setActive(false);
    esTester.putDocuments(INDEX_TYPE_USER, user1);
    esTester.putDocuments(INDEX_TYPE_USER, user2);
    esTester.putDocuments(INDEX_TYPE_USER, user3);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(user1.scmAccounts().get(0), ORGANIZATION_UUID)).extractingResultOf("login").containsOnly(user1.login());
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(user1.login(), ORGANIZATION_UUID)).extractingResultOf("login").containsOnly(user1.login());

    // both users share the same email
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(user1.email(), ORGANIZATION_UUID)).extracting(UserDoc::login).containsOnly(user1.login(), user2.login());

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("", ORGANIZATION_UUID)).isEmpty();
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("unknown", ORGANIZATION_UUID)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_ignores_inactive_user()  {
    String scmAccount = "scmA";
    UserDoc user = newUser(USER1_LOGIN, singletonList(scmAccount)).setActive(false);
    esTester.putDocuments(INDEX_TYPE_USER, user);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(user.login(), ORGANIZATION_UUID)).isEmpty();
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(scmAccount, ORGANIZATION_UUID)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_returns_maximum_three_users()  {
    String email = "user@mail.com";
    UserDoc user1 = newUser("user1", Collections.emptyList()).setEmail(email);
    UserDoc user2 = newUser("user2", Collections.emptyList()).setEmail(email);
    UserDoc user3 = newUser("user3", Collections.emptyList()).setEmail(email);
    UserDoc user4 = newUser("user4", Collections.emptyList()).setEmail(email);
    esTester.putDocuments(INDEX_TYPE_USER, user1);
    esTester.putDocuments(INDEX_TYPE_USER, user2);
    esTester.putDocuments(INDEX_TYPE_USER, user3);
    esTester.putDocuments(INDEX_TYPE_USER, user4);

    // restrict results to 3 users
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount(email, ORGANIZATION_UUID)).hasSize(3);
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_is_case_sensitive_for_login()  {
    UserDoc user = newUser("the_login", singletonList("John.Smith"));
    esTester.putDocuments(INDEX_TYPE_USER, user);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("the_login", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("the_Login", ORGANIZATION_UUID)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_is_case_insensitive_for_email()  {
    UserDoc user = newUser("the_login", "the_EMAIL@corp.com", singletonList("John.Smith"));
    esTester.putDocuments(INDEX_TYPE_USER, user);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("the_EMAIL@corp.com", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("the_email@corp.com", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("email", ORGANIZATION_UUID)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_is_case_insensitive_for_scm_account()  {
    UserDoc user = newUser("the_login", singletonList("John.Smith"));
    esTester.putDocuments(INDEX_TYPE_USER, user);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("John.Smith", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("JOHN.SMIth", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("JOHN.SMITH", ORGANIZATION_UUID)).hasSize(1);
    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("JOHN", ORGANIZATION_UUID)).isEmpty();
  }

  @Test
  public void getAtMostThreeActiveUsersForScmAccount_search_only_user_within_given_organization()  {
    UserDoc user1 = newUser("user1", singletonList("same_scm")).setOrganizationUuids(singletonList(ORGANIZATION_UUID));
    UserDoc user2 = newUser("user2", singletonList("same_scm")).setOrganizationUuids(singletonList("another_organization"));
    esTester.putDocuments(INDEX_TYPE_USER, user1);
    esTester.putDocuments(INDEX_TYPE_USER, user2);

    assertThat(underTest.getAtMostThreeActiveUsersForScmAccount("same_scm", ORGANIZATION_UUID)).extractingResultOf("login").containsOnly(user1.login());
  }

  @Test
  public void searchUsers()  {
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER1_LOGIN, asList("user_1", "u1")).setEmail("email1"));
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER2_LOGIN, Collections.emptyList()).setEmail("email2"));

    assertThat(underTest.search(userQuery.build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(userQuery.setTextQuery("user").build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(userQuery.setTextQuery("ser").build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(userQuery.setTextQuery(USER1_LOGIN).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(userQuery.setTextQuery(USER2_LOGIN).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(userQuery.setTextQuery("mail").build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(userQuery.setTextQuery("EMAIL1").build(), new SearchOptions()).getDocs()).hasSize(1);
  }

  @Test
  public void search_users_filter_by_organization_uuid() {
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER1_LOGIN, asList("user_1", "u1")).setEmail("email1")
      .setOrganizationUuids(newArrayList("O1", "O2")));
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER2_LOGIN, emptyList()).setEmail("email2")
      .setOrganizationUuids(newArrayList("O2")));

    assertThat(underTest.search(userQuery.setOrganizationUuid("O42").build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(underTest.search(userQuery.setOrganizationUuid("O2").build(), new SearchOptions()).getDocs()).extracting(UserDoc::login).containsOnly(USER1_LOGIN, USER2_LOGIN);
    assertThat(underTest.search(userQuery.setOrganizationUuid("O1").build(), new SearchOptions()).getDocs()).extracting(UserDoc::login).containsOnly(USER1_LOGIN);
  }

  @Test
  public void search_users_filter_by_excluded_organization_uuid() {
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER1_LOGIN, asList("user_1", "u1")).setEmail("email1")
      .setOrganizationUuids(newArrayList("O1", "O2")));
    esTester.putDocuments(INDEX_TYPE_USER, newUser(USER2_LOGIN, emptyList()).setEmail("email2")
      .setOrganizationUuids(newArrayList("O2")));

    assertThat(underTest.search(userQuery.setExcludedOrganizationUuid("O42").build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(userQuery.setExcludedOrganizationUuid("O2").build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(underTest.search(userQuery.setExcludedOrganizationUuid("O1").build(), new SearchOptions()).getDocs()).hasSize(1);
  }

  private static UserDoc newUser(String login, List<String> scmAccounts) {
    return new UserDoc()
      .setLogin(login)
      .setName(login.toUpperCase(Locale.ENGLISH))
      .setEmail(login + "@mail.com")
      .setActive(true)
      .setScmAccounts(scmAccounts)
      .setOrganizationUuids(singletonList(ORGANIZATION_UUID));
  }

  private static UserDoc newUser(String login, String email, List<String> scmAccounts) {
    return new UserDoc()
      .setLogin(login)
      .setName(login.toUpperCase(Locale.ENGLISH))
      .setEmail(email)
      .setActive(true)
      .setScmAccounts(scmAccounts)
      .setOrganizationUuids(singletonList(ORGANIZATION_UUID));
  }
}
