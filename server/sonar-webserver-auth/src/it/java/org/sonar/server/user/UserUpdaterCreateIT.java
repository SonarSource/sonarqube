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
package org.sonar.server.user;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.user.UserTesting.newLocalUser;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

@RunWith(DataProviderRunner.class)
public class UserUpdaterCreateIT {

  private static final String DEFAULT_LOGIN = "marius";

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private final ArgumentCaptor<NewUserHandler.Context> newUserHandler = ArgumentCaptor.forClass(NewUserHandler.Context.class);
  private final DbSession session = db.getSession();
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final UserUpdater underTest = new UserUpdater(newUserNotifier, dbClient,
    new DefaultGroupFinder(dbClient), settings.asConfig(), null, localAuthentication);

  @Test
  public void create_user() {
    createDefaultGroup();

    UserDto dto = underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .setScmAccounts(ImmutableList.of("u1", "u_1", "User 1"))
      .build(), u -> {
    });

    assertThat(dto.getUuid()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("user");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isEqualTo("user@mail.com");
    assertThat(dto.getSortedScmAccounts()).containsOnly("u1", "u_1", "User 1");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.isLocal()).isTrue();

    assertThat(dto.getSalt()).isNotNull();
    assertThat(dto.getHashMethod()).isEqualTo(HashMethod.PBKDF2.name());
    assertThat(dto.getCryptedPassword()).isNotNull();
    assertThat(dto.getCreatedAt())
      .isPositive()
      .isEqualTo(dto.getUpdatedAt());

    assertThat(dbClient.userDao().selectByLogin(session, "user").getUuid()).isEqualTo(dto.getUuid());
  }

  @Test
  public void create_user_with_minimum_fields() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("us")
      .setName("User")
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, "us");
    assertThat(dto.getUuid()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("us");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isNull();
    assertThat(dto.getSortedScmAccounts()).isEmpty();
    assertThat(dto.isActive()).isTrue();
  }

  @Test
  public void create_user_generates_unique_login_no_login_provided() {
    createDefaultGroup();

    UserDto user = underTest.createAndCommit(session, NewUser.builder()
      .setName("John Doe")
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, user.getLogin());
    assertThat(dto.getLogin()).startsWith("john-doe");
    assertThat(dto.getName()).isEqualTo("John Doe");
  }

  @Test
  public void create_user_generates_unique_login_when_login_is_empty() {
    createDefaultGroup();

    UserDto user = underTest.createAndCommit(session, NewUser.builder()
      .setLogin("")
      .setName("John Doe")
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, user.getLogin());
    assertThat(dto.getLogin()).startsWith("john-doe");
    assertThat(dto.getName()).isEqualTo("John Doe");
  }

  @Test
  public void create_user_with_sq_authority_when_no_authority_set() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto.getExternalLogin()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(dto.isLocal()).isTrue();
  }

  @Test
  public void create_user_with_identity_provider() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity("github", "github-user", "ABCD"))
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalId()).isEqualTo("ABCD");
    assertThat(dto.getExternalLogin()).isEqualTo("github-user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
  }

  @Test
  public void create_user_with_sonarqube_external_identity() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity(SQ_AUTHORITY, "user", "user"))
      .build(), u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto).isNotNull();
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalId()).isEqualTo("user");
    assertThat(dto.getExternalLogin()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
  }

  @Test
  public void create_user_with_scm_accounts_containing_blank_or_null_entries() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(asList("u1", "", null))
      .build(), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getSortedScmAccounts()).containsOnly("u1");
  }

  @Test
  public void create_user_with_scm_accounts_containing_one_blank_entry() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(List.of(""))
      .build(), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getSortedScmAccounts()).isEmpty();
  }

  @Test
  public void create_user_with_scm_accounts_containing_duplications() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(asList("u1", "u1"))
      .build(), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getSortedScmAccounts()).containsOnly("u1");
  }

  @Test
  @UseDataProvider("loginWithAuthorizedSuffix")
  public void createAndCommit_should_createUserWithoutException_when_loginHasAuthorizedSuffix(String login) {
    createDefaultGroup();

    NewUser user = NewUser.builder().setLogin(login).setName("aName").build();
    underTest.createAndCommit(session, user, u -> {
    });

    UserDto dto = dbClient.userDao().selectByLogin(session, login);
    assertNotNull(dto);
  }

  @DataProvider
  public static Object[][] loginWithAuthorizedSuffix() {
    return new Object[][] {
      {"1Login"},
      {"AnotherLogin"},
      {"alogin"},
      {"_technicalUser"},
      {"_.toto"}
    };
  }

  @Test
  public void fail_to_create_user_with_invalid_characters_in_login() {
    NewUser newUser = newUserBuilder().setLogin("_amarius/").build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login should contain only letters, numbers, and .-_@");
  }

  @Test
  public void fail_to_create_user_with_space_in_login() {
    NewUser newUser = newUserBuilder().setLogin("mari us").build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login should contain only letters, numbers, and .-_@");
  }

  @Test
  public void fail_to_create_user_with_too_short_login() {
    NewUser newUser = newUserBuilder().setLogin("m").build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login is too short (minimum is 2 characters)");
  }


  @Test
  @UseDataProvider("loginWithUnauthorizedSuffix")
  public void createAndCommit_should_ThrowBadRequestExceptionWithSpecificMessage_when_loginHasUnauthorizedSuffix(String login) {

    NewUser newUser = NewUser.builder().setLogin(login).build();

    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login should start with _ or alphanumeric.");

  }

  @DataProvider
  public static Object[][] loginWithUnauthorizedSuffix() {
    return new Object[][] {
      {".Toto"},
      {"@Toto"},
      {"-Tutu"},
      {" Tutut"},
      {"#nesp"},
    };
  }

  @Test
  public void create_user_login_contains_underscore() {
    createDefaultGroup();
    String login = "name_with_underscores";
    NewUser newUser = newUserBuilder().setLogin(login).build();

    underTest.createAndCommit(session, newUser, u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, login)).isNotNull();
  }

  @Test
  public void fail_to_create_user_with_too_long_login() {
    NewUser newUser = newUserBuilder().setLogin(randomAlphabetic(256)).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login is too long (maximum is 255 characters)");
  }

  @Test
  public void fail_to_create_user_with_missing_name() {
    NewUser newUser = NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Name can't be empty");
  }

  @Test
  public void fail_to_create_user_with_too_long_name() {
    NewUser newUser = newUserBuilder().setName(randomAlphabetic(201)).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Name is too long (maximum is 200 characters)");
  }

  @Test
  public void fail_to_create_user_with_too_long_email() {
    NewUser newUser = newUserBuilder().setEmail(randomAlphabetic(101)).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Email is too long (maximum is 100 characters)");
  }

  @Test
  public void fail_to_create_user_with_many_errors() {
    NewUser newUser = NewUser.builder()
      .setLogin("")
      .setName("")
      .setEmail("marius@mail.com")
      .setPassword("")
      .build();

    try {
      underTest.createAndCommit(session, newUser, u -> {
      });
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors()).containsExactlyInAnyOrder("Name can't be empty", "Password can't be empty");
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("jo")));
    NewUser newUser = newUserBuilder().setScmAccounts(singletonList("jo")).build();

    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The scm account 'jo' is already used by user(s) : 'John (john)'");
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used_by_many_users() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("john@email.com")));
    db.users().insertUser(newLocalUser("technical-account", "Technical account", null).setScmAccounts(singletonList("john@email.com")));
    NewUser newUser = newUserBuilder().setScmAccounts(List.of("john@email.com")).build();

    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The scm account 'john@email.com' is already used by user(s) : 'John (john), Technical account (technical-account)'");
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_login() {
    NewUser newUser = newUserBuilder().setLogin(DEFAULT_LOGIN).setScmAccounts(singletonList(DEFAULT_LOGIN)).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login and email are automatically considered as SCM accounts");
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_email() {
    NewUser newUser = newUserBuilder().setEmail("marius2@mail.com").setScmAccounts(singletonList("marius2@mail.com")).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Login and email are automatically considered as SCM accounts");
  }

  @Test
  public void fail_to_create_user_when_login_already_exists() {
    createDefaultGroup();
    UserDto existingUser = db.users().insertUser(u -> u.setLogin("existing_login"));

    NewUser newUser = newUserBuilder().setLogin(existingUser.getLogin()).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A user with login 'existing_login' already exists");
  }

  @Test
  public void fail_to_create_user_when_external_id_and_external_provider_already_exists() {
    createDefaultGroup();
    UserDto existingUser = db.users().insertUser(u -> u.setExternalId("existing_external_id").setExternalIdentityProvider("existing_external_provider"));

    NewUser newUser = NewUser.builder()
      .setLogin("new_login")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity(existingUser.getExternalIdentityProvider(), existingUser.getExternalLogin(), existingUser.getExternalId()))
      .build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A user with provider id 'existing_external_id' and identity provider 'existing_external_provider' already exists");
  }

  @Test
  public void notify_new_user() {
    createDefaultGroup();

    underTest.createAndCommit(session, NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(asList("u1", "u_1"))
      .build(), u -> {
    });

    verify(newUserNotifier).onNewUser(newUserHandler.capture());
    assertThat(newUserHandler.getValue().getLogin()).isEqualTo("user");
    assertThat(newUserHandler.getValue().getName()).isEqualTo("User");
    assertThat(newUserHandler.getValue().getEmail()).isEqualTo("user@mail.com");
  }

  @Test
  public void associate_default_group_when_creating_user() {
    GroupDto defaultGroup = createDefaultGroup();

    NewUser newUser = newUserBuilder().build();
    underTest.createAndCommit(session, newUser, u -> {
    });

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, singletonList(newUser.login()));
    assertThat(groups.get(newUser.login())).containsOnly(defaultGroup.getName());
  }

  @Test
  public void fail_to_associate_default_group_when_default_group_does_not_exist() {
    NewUser newUser = newUserBuilder().setScmAccounts(asList("u1", "u_1")).build();
    assertThatThrownBy(() -> underTest.createAndCommit(session, newUser, u -> {
    }))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default group cannot be found");
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup();
  }

  private NewUser.Builder newUserBuilder() {
    return NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password");
  }
}
