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
package org.sonar.server.authentication;

import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent.Method;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.core.config.CorePropertyDefinitions.ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.ALLOW;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.FORBID;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.WARN;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class UserIdentityAuthenticatorTest {

  private static String USER_LOGIN = "github-johndoo";

  private static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin(USER_LOGIN)
    .setName("John")
    .setEmail("john@email.com")
    .build();

  private static TestIdentityProvider IDENTITY_PROVIDER = new TestIdentityProvider()
    .setKey("github")
    .setName("name of github")
    .setEnabled(true)
    .setAllowsUsersToSignUp(true);

  private MapSettings settings = new MapSettings();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(settings.asConfig()));
  private UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationCreation organizationCreation = mock(OrganizationCreation.class);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private UserUpdater userUpdater = new UserUpdater(
    mock(NewUserNotifier.class),
    db.getDbClient(),
    userIndexer,
    organizationFlags,
    defaultOrganizationProvider,
    organizationCreation,
    new DefaultGroupFinder(db.getDbClient()),
    settings.asConfig());

  private UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(db.getDbClient(), userUpdater, defaultOrganizationProvider, organizationFlags,
    new DefaultGroupFinder(db.getDbClient()));

  @Test
  public void authenticate_new_user() {
    organizationFlags.setEnabled(true);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.realm(Method.BASIC, IDENTITY_PROVIDER.getName()), FORBID);

    UserDto user = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.isRoot()).isFalse();
    checkGroupMembership(user);
  }

  @Test
  public void authenticate_new_user_with_groups() {
    organizationFlags.setEnabled(true);
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");

    authenticate(USER_LOGIN, "group1", "group2", "group3");

    Optional<UserDto> user = db.users().selectUserByLogin(USER_LOGIN);
    checkGroupMembership(user.get(), group1, group2);
  }

  @Test
  public void authenticate_new_user_and_force_default_group_when_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto defaultGroup = insertDefaultGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getLogin(), "group1");

    checkGroupMembership(user, group1, defaultGroup);
  }

  @Test
  public void does_not_force_default_group_when_authenticating_new_user_if_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto defaultGroup = insertDefaultGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getLogin(), "group1");

    checkGroupMembership(user, group1);
  }

  @Test
  public void authenticate_new_user_sets_onboarded_flag_to_false_when_onboarding_setting_is_set_to_true() {
    organizationFlags.setEnabled(true);
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, true);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.realm(Method.BASIC, IDENTITY_PROVIDER.getName()), FORBID);

    assertThat(db.users().selectUserByLogin(USER_LOGIN).get().isOnboarded()).isFalse();
  }

  @Test
  public void authenticate_new_user_sets_onboarded_flag_to_true_when_onboarding_setting_is_set_to_false() {
    organizationFlags.setEnabled(true);
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, false);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.realm(Method.BASIC, IDENTITY_PROVIDER.getName()), FORBID);

    assertThat(db.users().selectUserByLogin(USER_LOGIN).get().isOnboarded()).isTrue();
  }

  @Test
  public void authenticate_new_user_update_existing_user_email_when_strategy_is_ALLOW() {
    organizationFlags.setEnabled(true);
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserIdentity newUser = UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin("new_login")
      .setName(existingUser.getName())
      .setEmail(existingUser.getEmail())
      .build();

    underTest.authenticate(newUser, IDENTITY_PROVIDER, Source.local(Method.BASIC), ALLOW);

    UserDto newUserReloaded = db.users().selectUserByLogin(newUser.getLogin()).get();
    assertThat(newUserReloaded.getEmail()).isEqualTo(existingUser.getEmail());
    UserDto existingUserReloaded = db.users().selectUserByLogin(existingUser.getLogin()).get();
    assertThat(existingUserReloaded.getEmail()).isNull();
  }

  @Test
  public void throw_EmailAlreadyExistException_when_authenticating_new_user_when_email_already_exists_and_strategy_is_WARN() {
    organizationFlags.setEnabled(true);
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserIdentity newUser = UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin("new_login")
      .setName(existingUser.getName())
      .setEmail(existingUser.getEmail())
      .build();

    expectedException.expect(EmailAlreadyExistsException.class);

    underTest.authenticate(newUser, IDENTITY_PROVIDER, Source.local(Method.BASIC), WARN);
  }

  @Test
  public void throw_AuthenticationException_when_authenticating_new_user_when_email_already_exists_and_strategy_is_FORBID() {
    db.users().insertUser(newUserDto()
      .setLogin("Existing user with same email")
      .setActive(true)
      .setEmail("john@email.com"));
    Source source = Source.realm(Method.FORM, IDENTITY_PROVIDER.getName());

    expectedException.expect(authenticationException().from(source)
      .withLogin(USER_IDENTITY.getLogin())
      .andPublicMessage("You can't sign up because email 'john@email.com' is already used by an existing user. " +
        "This means that you probably already registered with another account."));
    expectedException.expectMessage("Email 'john@email.com' is already used");

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, source, FORBID);
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() {
    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);
    Source source = Source.realm(Method.FORM, identityProvider.getName());

    expectedException.expect(authenticationException().from(source).withLogin(USER_IDENTITY.getLogin()).andPublicMessage("'github' users are not allowed to sign up"));
    expectedException.expectMessage("User signup disabled for provider 'github'");
    underTest.authenticate(USER_IDENTITY, identityProvider, source, FORBID);
  }

  @Test
  public void authenticate_existing_user() {
    db.users().insertUser(u -> u
      .setLogin(USER_LOGIN)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC), FORBID);

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(userDto.isRoot()).isFalse();
  }

  @Test
  public void authenticate_existing_disabled_user() {
    organizationFlags.setEnabled(true);
    db.users().insertUser(u -> u
      .setLogin(USER_LOGIN)
      .setActive(false)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC_TOKEN), FORBID);

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(userDto.isRoot()).isFalse();
  }

  @Test
  public void authenticate_existing_user_when_email_already_exists_and_strategy_is_ALLOW() {
    organizationFlags.setEnabled(true);
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserDto currentUser = db.users().insertUser(u -> u.setEmail(null));
    UserIdentity userIdentity = UserIdentity.builder()
      .setLogin(currentUser.getLogin())
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    underTest.authenticate(userIdentity, IDENTITY_PROVIDER, Source.local(Method.BASIC), ALLOW);

    UserDto currentUserReloaded = db.users().selectUserByLogin(currentUser.getLogin()).get();
    assertThat(currentUserReloaded.getEmail()).isEqualTo("john@email.com");
    UserDto existingUserReloaded = db.users().selectUserByLogin(existingUser.getLogin()).get();
    assertThat(existingUserReloaded.getEmail()).isNull();
  }

  @Test
  public void throw_EmailAlreadyExistException_when_authenticating_existing_user_when_email_already_exists_and_strategy_is_WARN() {
    organizationFlags.setEnabled(true);
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserDto currentUser = db.users().insertUser(u -> u.setEmail(null));
    UserIdentity userIdentity = UserIdentity.builder()
      .setLogin(currentUser.getLogin())
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    expectedException.expect(EmailAlreadyExistsException.class);

    underTest.authenticate(userIdentity, IDENTITY_PROVIDER, Source.local(Method.BASIC), WARN);
  }

  @Test
  public void throw_AuthenticationException_when_authenticating_existing_user_when_email_already_exists_and_strategy_is_FORBID() {
    organizationFlags.setEnabled(true);
    UserDto existingUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserDto currentUser = db.users().insertUser(u -> u.setEmail(null));
    UserIdentity userIdentity = UserIdentity.builder()
      .setLogin(currentUser.getLogin())
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    expectedException.expect(authenticationException().from(Source.realm(Method.FORM, IDENTITY_PROVIDER.getName()))
      .withLogin(userIdentity.getLogin())
      .andPublicMessage("You can't sign up because email 'john@email.com' is already used by an existing user. " +
        "This means that you probably already registered with another account."));
    expectedException.expectMessage("Email 'john@email.com' is already used");

    underTest.authenticate(userIdentity, IDENTITY_PROVIDER, Source.realm(Method.FORM, IDENTITY_PROVIDER.getName()), FORBID);
  }

  @Test
  public void does_not_fail_to_authenticate_user_when_email_has_not_changed_and_strategy_is_FORBID() {
    organizationFlags.setEnabled(true);
    UserDto currentUser = db.users().insertUser(u -> u.setEmail("john@email.com"));
    UserIdentity userIdentity = UserIdentity.builder()
      .setLogin(currentUser.getLogin())
      .setProviderLogin("johndoo")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    underTest.authenticate(userIdentity, IDENTITY_PROVIDER, Source.local(Method.BASIC), FORBID);

    UserDto currentUserReloaded = db.users().selectUserByLogin(currentUser.getLogin()).get();
    assertThat(currentUserReloaded.getEmail()).isEqualTo("john@email.com");
  }

  @Test
  public void authenticate_existing_user_and_add_new_groups() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");

    authenticate(USER_LOGIN, "group1", "group2", "group3");

    checkGroupMembership(user, group1, group2);
  }

  @Test
  public void authenticate_existing_user_and_remove_groups() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    authenticate(USER_LOGIN, "group1");

    checkGroupMembership(user, group1);
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups_expect_default_when_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    GroupDto defaultGroup = insertDefaultGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getLogin());

    checkGroupMembership(user, defaultGroup);
  }

  @Test
  public void does_not_force_default_group_when_authenticating_existing_user_when_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto defaultGroup = insertDefaultGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getLogin(), "group1");

    checkGroupMembership(user, group1);
  }

  @Test
  public void ignore_groups_on_non_default_organizations() {
    organizationFlags.setEnabled(true);
    OrganizationDto org = db.organizations().insert();
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    String groupName = "a-group";
    GroupDto groupInDefaultOrg = db.users().insertGroup(db.getDefaultOrganization(), groupName);
    GroupDto groupInOrg = db.users().insertGroup(org, groupName);

    // adding a group with the same name than in non-default organization
    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setGroups(newHashSet(groupName))
      .build(), IDENTITY_PROVIDER, Source.sso(), FORBID);

    checkGroupMembership(user, groupInDefaultOrg);
  }

  private void authenticate(String login, String... groups) {
    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(login)
      .setName("John")
      // No group
      .setGroups(stream(groups).collect(MoreCollectors.toSet()))
      .build(), IDENTITY_PROVIDER, Source.sso(), FORBID);
  }

  private void checkGroupMembership(UserDto user, GroupDto... expectedGroups) {
    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(stream(expectedGroups).map(GroupDto::getId).collect(Collectors.toList()).toArray(new Integer[] {}));
  }

  private GroupDto insertDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }
}
