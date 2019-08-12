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
package org.sonar.server.user;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.sonar.db.user.UserPropertyDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.user.UserTesting.newLocalUser;
import static org.sonar.process.ProcessProperties.Property.ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

public class UserUpdaterCreateTest {

  private static final String DEFAULT_LOGIN = "marius";

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private ArgumentCaptor<NewUserHandler.Context> newUserHandler = ArgumentCaptor.forClass(NewUserHandler.Context.class);
  private DbSession session = db.getSession();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private MapSettings settings = new MapSettings();
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());

  private UserUpdater underTest = new UserUpdater(system2, newUserNotifier, dbClient, userIndexer, organizationFlags, defaultOrganizationProvider,
    new DefaultGroupFinder(dbClient), settings.asConfig(), localAuthentication);

  @Test
  public void create_user() {
    createDefaultGroup();

    UserDto dto = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .setScmAccounts(ImmutableList.of("u1", "u_1", "User 1"))
      .build(), u -> {
      });

    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("user");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isEqualTo("user@mail.com");
    assertThat(dto.getScmAccountsAsList()).containsOnly("u1", "u_1", "User 1");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.isLocal()).isTrue();

    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getHashMethod()).isEqualTo(HashMethod.BCRYPT.name());
    assertThat(dto.getCryptedPassword()).isNotNull();
    assertThat(dto.getCreatedAt())
      .isPositive()
      .isEqualTo(dto.getUpdatedAt());

    assertThat(dbClient.userDao().selectByLogin(session, "user").getId()).isEqualTo(dto.getId());
    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSourceAsMap())
      .contains(
        entry("login", "user"),
        entry("name", "User"),
        entry("email", "user@mail.com"));
  }

  @Test
  public void create_user_with_minimum_fields() {
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("us")
      .setName("User")
      .build(), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, "us");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("us");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isNull();
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.isActive()).isTrue();
  }

  @Test
  public void create_user_generates_unique_login_no_login_provided() {
    createDefaultGroup();

    UserDto user = underTest.createAndCommit(db.getSession(), NewUser.builder()
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

    UserDto user = underTest.createAndCommit(db.getSession(), NewUser.builder()
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

    underTest.createAndCommit(db.getSession(), NewUser.builder()
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

    underTest.createAndCommit(db.getSession(), NewUser.builder()
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

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity(SQ_AUTHORITY, "user", "user"))
      .build(), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
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

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(asList("u1", "", null))
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void create_user_with_scm_accounts_containing_one_blank_entry() {
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(asList(""))
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccounts()).isNull();
  }

  @Test
  public void create_user_with_scm_accounts_containing_duplications() {
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(asList("u1", "u1"))
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void create_not_onboarded_user_if_onboarding_setting_is_set_to_false() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS.getKey(), false);
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, "user").isOnboarded()).isTrue();
  }

  @Test
  public void create_onboarded_user_if_onboarding_setting_is_set_to_true() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS.getKey(), true);
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, "user").isOnboarded()).isFalse();
  }

  @Test
  public void set_notifications_readDate_setting_when_creating_user_and_organization_enabled() {
    long now = system2.now();
    organizationFlags.setEnabled(true);
    createDefaultGroup();

    UserDto user = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("userLogin")
      .setName("UserName")
      .build(), u -> {
      });

    UserPropertyDto notificationReadDateSetting = dbClient.userPropertiesDao().selectByUser(session, user).get(0);
    assertThat(notificationReadDateSetting.getKey()).isEqualTo("notifications.readDate");
    assertThat(Long.parseLong(notificationReadDateSetting.getValue())).isGreaterThanOrEqualTo(now);
  }

  @Test
  public void does_not_set_notifications_readDate_setting_when_creating_user_when_not_on_and_organization_disabled() {
    createDefaultGroup();

    UserDto user = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("userLogin")
      .setName("UserName")
      .build(), u -> {
      });

    assertThat(dbClient.userPropertiesDao().selectByUser(session, user)).isEmpty();
  }

  @Test
  public void create_user_and_index_other_user() {
    createDefaultGroup();
    UserDto otherUser = db.users().insertUser();

    UserDto created = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build(), u -> {
      }, otherUser);

    assertThat(es.getIds(UserIndexDefinition.TYPE_USER)).containsExactlyInAnyOrder(created.getUuid(), otherUser.getUuid());
  }

  @Test
  public void fail_to_create_user_with_invalid_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("/marius/")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_space_in_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("mari us")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_too_short_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login is too short (minimum is 2 characters)");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("m")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_too_long_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login is too long (maximum is 255 characters)");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(Strings.repeat("m", 256))
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_missing_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name can't be empty");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName(null)
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_too_long_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name is too long (maximum is 200 characters)");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName(Strings.repeat("m", 201))
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_too_long_email() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Email is too long (maximum is 100 characters)");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail(Strings.repeat("m", 101))
      .setPassword("password")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_with_many_errors() {
    try {
      underTest.createAndCommit(db.getSession(), NewUser.builder()
        .setLogin("")
        .setName("")
        .setEmail("marius@mail.com")
        .setPassword("")
        .build(), u -> {
        });
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors()).containsExactlyInAnyOrder("Name can't be empty", "Password can't be empty");
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("jo")));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'jo' is already used by user(s) : 'John (john)'");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .setScmAccounts(asList("jo"))
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used_by_many_users() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("john@email.com")));
    db.users().insertUser(newLocalUser("technical-account", "Technical account", null).setScmAccounts(singletonList("john@email.com")));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'john@email.com' is already used by user(s) : 'John (john), Technical account (technical-account)'");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .setScmAccounts(asList("john@email.com"))
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList(DEFAULT_LOGIN))
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_email() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("marius2@mail.com"))
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_when_login_already_exists() {
    createDefaultGroup();
    UserDto existingUser = db.users().insertUser(u -> u.setLogin("existing_login"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with login 'existing_login' already exists");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin(existingUser.getLogin())
      .setName("User")
      .setPassword("PASSWORD")
      .build(), u -> {
      });
  }

  @Test
  public void fail_to_create_user_when_external_id_and_external_provider_already_exists() {
    createDefaultGroup();
    UserDto existingUser = db.users().insertUser(u -> u.setExternalId("existing_external_id").setExternalIdentityProvider("existing_external_provider"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with provider id 'existing_external_id' and identity provider 'existing_external_provider' already exists");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("new_login")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity(existingUser.getExternalIdentityProvider(), existingUser.getExternalLogin(), existingUser.getExternalId()))
      .build(), u -> {
      });
  }

  @Test
  public void notify_new_user() {
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
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
  public void associate_default_group_when_creating_user_and_organizations_are_disabled() {
    GroupDto defaultGroup = createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .build(), u -> {
      });

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList("user"));
    assertThat(groups.get("user")).containsOnly(defaultGroup.getName());
  }

  @Test
  public void does_not_associate_default_group_when_creating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    createDefaultGroup();

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .build(), u -> {
      });

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList("user"));
    assertThat(groups.get("user")).isEmpty();
  }

  @Test
  public void fail_to_associate_default_group_when_default_group_does_not_exist() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group cannot be found");

    underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(asList("u1", "u_1"))
      .build(), u -> {
      });
  }

  @Test
  public void add_user_as_member_of_default_organization_when_creating_user_and_organizations_are_disabled() {
    createDefaultGroup();

    UserDto dto = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build(), u -> {
      });

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isPresent();
  }

  @Test
  public void does_not_add_user_as_member_of_default_organization_when_creating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    createDefaultGroup();

    UserDto dto = underTest.createAndCommit(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build(), u -> {
      });

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isNotPresent();
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization());
  }
}
