/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;

public class UserRegistrarImplTest {
  private static final String USER_LOGIN = "johndoo";

  private static final UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderId("ABCD")
    .setProviderLogin(USER_LOGIN)
    .setName("John")
    .setEmail("john@email.com")
    .build();

  private static final TestIdentityProvider GH_IDENTITY_PROVIDER = new TestIdentityProvider()
    .setKey("github")
    .setName("name of github")
    .setEnabled(true)
    .setAllowsUsersToSignUp(true);

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final DefaultGroupFinder groupFinder = new DefaultGroupFinder(db.getDbClient());
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(), userIndexer, groupFinder, settings.asConfig(), auditPersister, localAuthentication);

  private final UserRegistrarImpl underTest = new UserRegistrarImpl(db.getDbClient(), userUpdater, groupFinder);
  private GroupDto defaultGroup;

  @Before
  public void setUp() {
    defaultGroup = insertDefaultGroup();
  }

  @Test
  public void authenticate_new_user() {
    UserDto createdUser = underTest.register(newUserRegistration());

    UserDto user = db.users().selectUserByLogin(createdUser.getLogin()).get();
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalLogin()).isEqualTo(USER_LOGIN);
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.getExternalId()).isEqualTo("ABCD");
    checkGroupMembership(user, defaultGroup);
  }

  @Test
  public void authenticate_new_user_with_sq_identity() {
    TestIdentityProvider sqIdentityProvider = new TestIdentityProvider()
      .setKey("sonarqube")
      .setName("sonarqube identity name")
      .setEnabled(true)
      .setAllowsUsersToSignUp(true);

    UserDto createdUser = underTest.register(UserRegistration.builder()
      .setUserIdentity(USER_IDENTITY)
      .setProvider(sqIdentityProvider)
      .setSource(Source.realm(BASIC, sqIdentityProvider.getName()))
      .build());

    UserDto user = db.users().selectUserByLogin(createdUser.getLogin()).get();
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getLogin()).isEqualTo(USER_LOGIN);
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalLogin()).isEqualTo(USER_LOGIN);
    assertThat(user.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(user.getExternalId()).isEqualTo("ABCD");
    assertThat(user.isLocal()).isFalse();
    checkGroupMembership(user, defaultGroup);
  }

  @Test
  public void authenticate_new_user_generates_login() {
    underTest.register(newUserRegistration(UserIdentity.builder()
      .setProviderId("ABCD")
      .setProviderLogin(USER_LOGIN)
      .setName("John Doe")
      .setEmail("john@email.com")
      .build()));

    UserDto user = db.getDbClient().userDao().selectByEmail(db.getSession(), "john@email.com").get(0);
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getLogin()).isNotEqualTo("John Doe").startsWith("john-doe");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalLogin()).isEqualTo(USER_LOGIN);
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.getExternalId()).isEqualTo("ABCD");
  }

  @Test
  public void authenticate_new_user_assigns_user_to_groups() {
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");

    UserDto loggedInUser = authenticate(USER_LOGIN, USER_IDENTITY.getEmail(), "group1", "group2", "group3");

    Optional<UserDto> user = db.users().selectUserByLogin(loggedInUser.getLogin());
    checkGroupMembership(user.get(), group1, group2, defaultGroup);
  }

  @Test
  public void authenticate_new_user_sets_external_id_to_provider_login_when_id_is_null() {
    UserIdentity newUser = UserIdentity.builder()
      .setProviderId(null)
      .setProviderLogin("johndoo")
      .setName("JOhn")
      .build();

    UserDto user = underTest.register(newUserRegistration(newUser));

    assertThat(db.users().selectUserByLogin(user.getLogin()).get())
      .extracting(UserDto::getLogin, UserDto::getExternalId, UserDto::getExternalLogin)
      .contains(user.getLogin(), "johndoo", "johndoo");
  }

  @Test
  public void authenticate_new_user_with_gitlab_provider() {
    UserRegistration registration = UserRegistration.builder()
      .setUserIdentity(USER_IDENTITY)
      .setProvider(new TestIdentityProvider()
        .setKey("gitlab")
        .setName("name of gitlab")
        .setEnabled(true)
        .setAllowsUsersToSignUp(true))
      .setSource(Source.local(BASIC))
      .build();

    UserDto newUser = underTest.register(registration);
    assertThat(newUser)
      .extracting(UserDto::getExternalIdentityProvider, UserDto::getExternalLogin)
      .containsExactly("gitlab", USER_IDENTITY.getProviderLogin());
  }

  @Test
  public void authenticate_new_user_throws_AuthenticationException_when_when_email_already_exists() {
    db.users().insertUser(u -> u.setEmail("john@email.com"));
    Source source = Source.local(BASIC);

    assertThatThrownBy(() -> underTest.register(newUserRegistration()))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("Email 'john@email.com' is already used")
      .hasFieldOrPropertyWithValue("source", source)
      .hasFieldOrPropertyWithValue("login", USER_IDENTITY.getProviderLogin())
      .hasFieldOrPropertyWithValue("publicMessage", "This account is already associated with another authentication method."
        + " Sign in using the current authentication method,"
        + " or contact your administrator to transfer your account to a different authentication method.");
  }

  @Test
  public void authenticate_new_user_throws_AuthenticationException_when_email_already_exists_multiple_times() {
    db.users().insertUser(u -> u.setEmail("john@email.com"));
    db.users().insertUser(u -> u.setEmail("john@email.com"));
    Source source = Source.realm(AuthenticationEvent.Method.FORM, GH_IDENTITY_PROVIDER.getName());

    assertThatThrownBy(() -> underTest.register(newUserRegistration(source)))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("Email 'john@email.com' is already used")
      .hasFieldOrPropertyWithValue("source", source)
      .hasFieldOrPropertyWithValue("login", USER_IDENTITY.getProviderLogin())
      .hasFieldOrPropertyWithValue("publicMessage", "This account is already associated with another authentication method."
          + " Sign in using the current authentication method,"
          + " or contact your administrator to transfer your account to a different authentication method.");
  }

  @Test
  public void authenticate_new_user_fails_when_allow_users_to_signup_is_false() {
    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);
    Source source = Source.realm(AuthenticationEvent.Method.FORM, identityProvider.getName());

    assertThatThrownBy(() -> {
      underTest.register(UserRegistration.builder()
        .setUserIdentity(USER_IDENTITY)
        .setProvider(identityProvider)
        .setSource(source)
        .build());
    })
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("User signup disabled for provider 'github'")
      .hasFieldOrPropertyWithValue("source", source)
      .hasFieldOrPropertyWithValue("login", USER_IDENTITY.getProviderLogin())
      .hasFieldOrPropertyWithValue("publicMessage", "'github' users are not allowed to sign up");
  }

  @Test
  public void authenticate_existing_user_doesnt_change_group_membership() {
    UserDto user = db.users().insertUser(u -> u.setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));
    GroupDto group1 = db.users().insertGroup("group1");
    db.users().insertMember(group1, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getExternalLogin(), user.getEmail(), "group1");

    checkGroupMembership(user, group1, defaultGroup);
  }

  @Test
  public void authenticate_and_update_existing_user_matching_external_id() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setEmail("Old email")
      .setExternalId(USER_IDENTITY.getProviderId())
      .setExternalLogin("old identity")
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalId, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        UserDto::isActive)
      .contains(USER_LOGIN, "John", "john@email.com", "ABCD", "johndoo", "github", true);
  }

  @Test
  public void authenticate_and_update_existing_user_matching_external_login_and_email() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setEmail(USER_IDENTITY.getEmail())
      .setExternalId("Old id")
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getName, UserDto::getEmail, UserDto::getExternalId, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        UserDto::isActive)
      .contains("John", "john@email.com", "ABCD", "johndoo", "github", true);
  }

  @Test
  public void authenticate_existing_user_should_not_update_login() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("old login")
      .setName(USER_IDENTITY.getName())
      .setEmail(USER_IDENTITY.getEmail())
      .setExternalId(USER_IDENTITY.getProviderId())
      .setExternalLogin("old identity")
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    // no new user should be created
    assertThat(db.countRowsOfTable(db.getSession(), "users")).isOne();
    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalId, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        UserDto::isActive)
      .containsExactly("old login", USER_IDENTITY.getName(), USER_IDENTITY.getEmail(), USER_IDENTITY.getProviderId(), USER_IDENTITY.getProviderLogin(),
        GH_IDENTITY_PROVIDER.getKey(), true);
    verify(auditPersister, never()).updateUserPassword(any(), any());
  }

  @Test
  public void authenticate_existing_user_matching_external_login_and_email_when_external_id_is_null() {
    UserDto user = db.users().insertUser(u -> u
      .setName("Old name")
      .setExternalId("Old id")
      .setEmail("john@email.com")
      .setExternalLogin("johndoo")
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration(UserIdentity.builder()
      .setProviderId(null)
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build()));

    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalId, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        UserDto::isActive)
      .contains(user.getLogin(), "John", "john@email.com", "johndoo", "johndoo", "github", true);
  }

  @Test
  public void do_not_authenticate_gitlab_user_matching_external_login() {
    db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setEmail(USER_IDENTITY.getEmail())
      .setExternalId("Old id")
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider("gitlab"));

    UserRegistration registration = UserRegistration.builder()
      .setUserIdentity(USER_IDENTITY)
      .setProvider(new TestIdentityProvider()
        .setKey("gitlab")
        .setName("name of gitlab")
        .setEnabled(true))
      .setSource(Source.local(BASIC))
      .build();

    assertThatThrownBy(() -> underTest.register(registration))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(String.format("Failed to authenticate with login '%s'", USER_IDENTITY.getProviderLogin()));
  }

  @Test
  public void do_not_authenticate_and_update_existing_github_user_matching_external_login_if_emails_do_not_match() {
    db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setEmail("another-email@sonarsource.com")
      .setExternalId("Old id")
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    assertThatThrownBy(() -> underTest.register(newUserRegistration()))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(String.format("Failed to authenticate with login '%s'", USER_IDENTITY.getProviderLogin()));

    assertThat(logTester.logs()).contains(String.format("User with login '%s' tried to login with email '%s' which doesn't match the email on record '%s'",
      USER_IDENTITY.getProviderLogin(), USER_IDENTITY.getEmail(), "another-email@sonarsource.com"));
  }

  @Test
  public void authenticate_and_update_existing_user_matching_external_login_and_emails_mismatch() {
    UserRegistration registration = UserRegistration.builder()
      .setUserIdentity(USER_IDENTITY)
      .setProvider(new TestIdentityProvider()
        .setKey("other")
        .setName("name of other")
        .setEnabled(true))
      .setSource(Source.local(BASIC))
      .build();

    UserDto user = db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setEmail("another-email@sonarsource.com")
      .setExternalId("Old id")
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider(registration.getProvider().getKey()));

    underTest.register(registration);
    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalId, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        UserDto::isActive)
      .contains(user.getLogin(), "John", "john@email.com", "ABCD", "johndoo", "other", true);
  }

  @Test
  public void authenticate_and_update_existing_user_matching_external_login_if_email_is_missing() {
    db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setExternalId("Old id")
      .setEmail(null)
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    Optional<UserDto> user = db.users().selectUserByLogin("Old login");
    assertThat(user).isPresent();
    assertThat(user.get().getEmail()).isEqualTo(USER_IDENTITY.getEmail());
  }

  @Test
  public void do_not_authenticate_and_update_existing_user_matching_external_id_if_external_provider_does_not_match() {
    db.users().insertUser(u -> u
      .setLogin("Old login")
      .setName("Old name")
      .setExternalId(USER_IDENTITY.getProviderId())
      .setEmail(null)
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider("Old provider"));

    underTest.register(newUserRegistration());
    assertThat(db.countRowsOfTable("users")).isEqualTo(2);
  }

  @Test
  public void authenticate_existing_user_should_update_login() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("Old login")
      .setExternalId(USER_IDENTITY.getProviderId())
      .setExternalLogin("old identity")
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    assertThat(db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid()))
      .extracting(UserDto::getLogin, UserDto::getExternalLogin)
      .contains(USER_LOGIN, USER_IDENTITY.getProviderLogin());
  }

  @Test
  public void authenticate_existing_disabled_user_should_reactivate_it() {
    db.users().insertUser(u -> u
      .setLogin(USER_LOGIN)
      .setActive(false)
      .setName("Old name")
      .setEmail(USER_IDENTITY.getEmail())
      .setExternalId("Old id")
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));

    underTest.register(newUserRegistration());

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo(USER_IDENTITY.getName());
    assertThat(userDto.getEmail()).isEqualTo(USER_IDENTITY.getEmail());
    assertThat(userDto.getExternalId()).isEqualTo(USER_IDENTITY.getProviderId());
    assertThat(userDto.getExternalLogin()).isEqualTo(USER_IDENTITY.getProviderLogin());
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo(GH_IDENTITY_PROVIDER.getKey());
  }

  @Test
  public void authenticating_existing_user_throws_AuthenticationException_when_email_already_exists() {
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserDto currentUser = db.users().insertUser(u -> u.setEmail(null));
    UserIdentity userIdentity = UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    Source source = Source.realm(AuthenticationEvent.Method.FORM, GH_IDENTITY_PROVIDER.getName());

    assertThatThrownBy(() -> underTest.register(newUserRegistration(userIdentity, source)))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("Email 'john@email.com' is already used")
      .hasFieldOrPropertyWithValue("source", source)
      .hasFieldOrPropertyWithValue("login", USER_IDENTITY.getProviderLogin())
      .hasFieldOrPropertyWithValue("publicMessage", "This account is already associated with another authentication method."
          + " Sign in using the current authentication method,"
          + " or contact your administrator to transfer your account to a different authentication method.");
  }

  @Test
  public void authenticate_existing_user_succeeds_when_email_has_not_changed() {
    UserDto currentUser = db.users().insertUser(u -> u.setEmail("john@email.com")
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey()));
    UserIdentity userIdentity = UserIdentity.builder()
      .setProviderId(currentUser.getExternalId())
      .setProviderLogin(currentUser.getExternalLogin())
      .setName("John")
      .setEmail("john@email.com")
      .build();

    underTest.register(newUserRegistration(userIdentity));

    UserDto currentUserReloaded = db.users().selectUserByLogin(currentUser.getLogin()).get();
    assertThat(currentUserReloaded.getEmail()).isEqualTo("john@email.com");
  }

  @Test
  public void authenticate_existing_user_and_add_new_groups() {
    UserDto user = db.users().insertUser(newUserDto()
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey())
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setEmail(USER_IDENTITY.getEmail())
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");

    authenticate(USER_IDENTITY.getProviderLogin(), USER_IDENTITY.getEmail(), "group1", "group2", "group3");

    checkGroupMembership(user, group1, group2);
  }

  @Test
  public void authenticate_existing_user_and_remove_groups() {
    UserDto user = db.users().insertUser(newUserDto()
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey())
      .setExternalLogin(USER_IDENTITY.getProviderLogin())
      .setEmail(USER_IDENTITY.getEmail())
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    authenticate(USER_IDENTITY.getProviderLogin(), USER_IDENTITY.getEmail(), "group1");

    checkGroupMembership(user, group1);
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups_expect_default() {
    UserDto user = db.users().insertUser(newUserDto()
      .setExternalIdentityProvider(GH_IDENTITY_PROVIDER.getKey())
      .setExternalLogin(USER_IDENTITY.getProviderLogin()));
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getExternalLogin(), user.getEmail());

    checkGroupMembership(user, defaultGroup);
  }

  private static UserRegistration newUserRegistration(UserIdentity userIdentity) {
    return newUserRegistration(userIdentity, Source.local(BASIC));
  }

  private static UserRegistration newUserRegistration(UserIdentity userIdentity, Source source) {
    return UserRegistration.builder()
      .setUserIdentity(userIdentity)
      .setProvider(GH_IDENTITY_PROVIDER)
      .setSource(source)
      .build();
  }

  private static UserRegistration newUserRegistration(Source source) {
    return newUserRegistration(USER_IDENTITY, source);
  }

  private static UserRegistration newUserRegistration() {
    return newUserRegistration(USER_IDENTITY, Source.local(BASIC));
  }

  private UserDto authenticate(String providerLogin, @Nullable String email, String... groups) {
    return underTest.register(newUserRegistration(UserIdentity.builder()
      .setProviderLogin(providerLogin)
      .setName("John")
      .setEmail(email)
      .setGroups(Set.of(groups))
      .build()));
  }

  private void checkGroupMembership(UserDto user, GroupDto... expectedGroups) {
    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(stream(expectedGroups).map(GroupDto::getUuid).collect(Collectors.toList()).toArray(new String[]{}));
  }

  private GroupDto insertDefaultGroup() {
    return db.users().insertDefaultGroup();
  }

}
