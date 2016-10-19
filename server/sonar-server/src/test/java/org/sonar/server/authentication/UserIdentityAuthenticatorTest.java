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
package org.sonar.server.authentication;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.DefaultOrganizationProviderRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserIdentityAuthenticatorTest {

  private static String USER_LOGIN = "github-johndoo";
  private static String DEFAULT_GROUP = "default";

  private static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin(USER_LOGIN)
    .setName("John")
    .setEmail("john@email.com")
    .build();

  private static TestIdentityProvider IDENTITY_PROVIDER = new TestIdentityProvider()
    .setKey("github")
    .setEnabled(true)
    .setAllowsUsersToSignUp(true);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private Settings settings = new MapSettings();
  private DefaultOrganizationProvider defaultOrganizationProvider = DefaultOrganizationProviderRule.create(db);
  private UserUpdater userUpdater = new UserUpdater(
    mock(NewUserNotifier.class),
    settings,
    db.getDbClient(),
    mock(UserIndexer.class),
    system2,
    defaultOrganizationProvider);
  private UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(db.getDbClient(), userUpdater, defaultOrganizationProvider);
  private GroupDto defaultGroup;

  @Before
  public void setUp() throws Exception {
    settings.setProperty("sonar.defaultGroup", DEFAULT_GROUP);
    defaultGroup = db.users().insertGroup(db.getDefaultOrganization(), DEFAULT_GROUP);
  }

  @Test
  public void authenticate_new_user() throws Exception {
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);

    UserDto user = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(user).isNotNull();
    assertThat(user.isActive()).isTrue();
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("john@email.com");
    assertThat(user.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(defaultGroup.getId());
  }

  @Test
  public void authenticate_new_user_with_groups() throws Exception {
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // group3 doesn't exist in db, it will be ignored
      .setGroups(newHashSet("group1", "group2", "group3"))
      .build(), IDENTITY_PROVIDER);

    Optional<UserDto> user = db.users().selectUserByLogin(USER_LOGIN);
    assertThat(user).isPresent();

    assertThat(db.users().selectGroupIdsOfUser(user.get())).containsOnly(group1.getId(), group2.getId());
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

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
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

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);

    UserDto userDto = db.users().selectUserByLogin(USER_LOGIN).get();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void authenticate_existing_user_and_add_new_groups() throws Exception {
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // group3 doesn't exist in db, it will be ignored
      .setGroups(newHashSet("group1", "group2", "group3"))
      .build(), IDENTITY_PROVIDER);

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group1.getId(), group2.getId());
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

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // Only group1 is returned by the id provider => group2 will be removed
      .setGroups(newHashSet("group1"))
      .build(), IDENTITY_PROVIDER);

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group1.getId());
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups() throws Exception {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(user.getLogin())
      .setName(user.getName())
      // No group => group1 and group2 will be removed
      .setGroups(Collections.emptySet())
      .build(), IDENTITY_PROVIDER);

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void ignore_groups_on_non_default_organizations() throws Exception {
    OrganizationDto org = OrganizationTesting.insert(db, newOrganizationDto());
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
      .build(), IDENTITY_PROVIDER);

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(groupInDefaultOrg.getId());
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() throws Exception {
    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("'github' users are not allowed to sign up");
    underTest.authenticate(USER_IDENTITY, identityProvider);
  }

  @Test
  public void fail_to_authenticate_new_user_when_email_already_exists() throws Exception {
    db.users().insertUser(newUserDto()
      .setLogin("Existing user with same email")
      .setActive(true)
      .setEmail("john@email.com"));

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("You can't sign up because email 'john@email.com' is already used by an existing user. " +
      "This means that you probably already registered with another account.");
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);
  }

}
