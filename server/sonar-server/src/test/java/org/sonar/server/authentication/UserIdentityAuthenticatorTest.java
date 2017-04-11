/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
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

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationCreation organizationCreation = mock(OrganizationCreation.class);
  private UserUpdater userUpdater = new UserUpdater(
    mock(NewUserNotifier.class),
    db.getDbClient(),
    mock(UserIndexer.class),
    System2.INSTANCE,
    defaultOrganizationProvider,
    organizationCreation,
    new DefaultGroupFinder(db.getDbClient()));
  private UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(db.getDbClient(), userUpdater, defaultOrganizationProvider);
  private GroupDto defaultGroup;

  @Before
  public void setUp() throws Exception {
    defaultGroup = db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }

  @Test
  public void authenticate_new_user() throws Exception {
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.realm(Method.BASIC, IDENTITY_PROVIDER.getName()));

    UserDto user = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.isRoot()).isFalse();

    checkGroupMembership(user, defaultGroup);
  }

  @Test
  public void authenticate_new_user_with_groups() throws Exception {
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");

    authenticate(USER_LOGIN, "group1", "group2", "group3");

    Optional<UserDto> user = db.users().selectUserByLogin(USER_LOGIN);
    checkGroupMembership(user.get(), group1, group2, defaultGroup);
  }

  @Test
  public void authenticate_existing_user() throws Exception {
    db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC));

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(userDto.isRoot()).isFalse();
  }

  @Test
  public void authenticate_existing_disabled_user() throws Exception {
    db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(false)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC_TOKEN));

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(userDto.isRoot()).isFalse();
  }

  @Test
  public void authenticate_existing_user_and_add_new_groups() throws Exception {
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(defaultGroup, user);

    authenticate(USER_LOGIN, "group1", "group2", "group3");

    checkGroupMembership(user, group1, group2, defaultGroup);
  }

  @Test
  public void authenticate_existing_user_and_remove_groups() throws Exception {
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(USER_LOGIN, "group1");

    checkGroupMembership(user, group1, defaultGroup);
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups_expect_default() throws Exception {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    db.users().insertMember(defaultGroup, user);

    authenticate(user.getLogin());

    checkGroupMembership(user, defaultGroup);
  }

  @Test
  public void ignore_groups_on_non_default_organizations() throws Exception {
    OrganizationDto org = db.organizations().insert();
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    db.users().insertMember(defaultGroup, user);
    String groupName = "a-group";
    GroupDto groupInDefaultOrg = db.users().insertGroup(db.getDefaultOrganization(), groupName);
    GroupDto groupInOrg = db.users().insertGroup(org, groupName);

    // adding a group with the same name than in non-default organization
    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setGroups(newHashSet(groupName))
      .build(), IDENTITY_PROVIDER, Source.sso());

    checkGroupMembership(user, groupInDefaultOrg, defaultGroup);
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() throws Exception {
    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);
    Source source = Source.realm(Method.FORM, identityProvider.getName());

    thrown.expect(authenticationException().from(source).withLogin(USER_IDENTITY.getLogin()).andPublicMessage("'github' users are not allowed to sign up"));
    thrown.expectMessage("User signup disabled for provider 'github'");
    underTest.authenticate(USER_IDENTITY, identityProvider, source);
  }

  @Test
  public void fail_to_authenticate_new_user_when_email_already_exists() throws Exception {
    db.users().insertUser(newUserDto()
      .setLogin("Existing user with same email")
      .setActive(true)
      .setEmail("john@email.com"));
    Source source = Source.realm(Method.FORM, IDENTITY_PROVIDER.getName());

    thrown.expect(authenticationException().from(source)
      .withLogin(USER_IDENTITY.getLogin())
      .andPublicMessage("You can't sign up because email 'john@email.com' is already used by an existing user. " +
        "This means that you probably already registered with another account."));
    thrown.expectMessage("Email 'john@email.com' is already used");
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, source);
  }

  private void authenticate(String login, String... groups) {
    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(login)
      .setName("John")
      // No group
      .setGroups(stream(groups).collect(MoreCollectors.toSet()))
      .build(), IDENTITY_PROVIDER, Source.sso());
  }

  private void checkGroupMembership(UserDto user, GroupDto... expectedGroups) {
    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(stream(expectedGroups).map(GroupDto::getId).collect(Collectors.toList()).toArray(new Integer[] {}));
  }
}
