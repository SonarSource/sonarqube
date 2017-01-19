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

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

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
    .setName("name of github")
    .setEnabled(true)
    .setAllowsUsersToSignUp(true);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private Settings settings = new MapSettings();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private UserUpdater userUpdater = new UserUpdater(
    mock(NewUserNotifier.class),
    settings,
    db.getDbClient(),
    mock(UserIndexer.class),
    System2.INSTANCE,
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
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.realm(Method.BASIC, IDENTITY_PROVIDER.getName()));

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

    authenticate(USER_LOGIN, "group1", "group2", "group3");

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

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC));

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

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, Source.local(Method.BASIC_TOKEN));

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

    authenticate(USER_LOGIN, "group1", "group2", "group3");

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

    authenticate(USER_LOGIN, "group1");

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(group1.getId());
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups() throws Exception {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    authenticate(user.getLogin());

    assertThat(db.users().selectGroupIdsOfUser(user)).isEmpty();
  }

  @Test
  public void authenticate_new_user_and_add_it_to_no_group_sets_root_flag_to_false() {
    authenticate(USER_LOGIN);

    db.rootFlag().verify(USER_LOGIN, false);
  }

  @Test
  public void authenticate_new_user_and_add_it_to_admin_group_of_default_organization_sets_root_flag_to_true() {
    GroupDto adminGroup = db.users().insertAdminGroup(db.getDefaultOrganization());

    authenticate(USER_LOGIN, adminGroup.getName());

    db.rootFlag().verify(USER_LOGIN, true);
  }

  @Test
  public void authenticate_new_user_and_add_it_to_admin_group_of_other_organization_does_not_set_root_flag_to_true() {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto adminGroup = db.users().insertAdminGroup(otherOrganization);

    authenticate(USER_LOGIN, adminGroup.getName());

    db.rootFlag().verify(USER_LOGIN, false);
  }

  @Test
  public void authenticate_existing_user_and_add_it_to_no_group_sets_root_flag_to_false() {
    UserDto userDto = db.users().insertUser();

    authenticate(userDto.getLogin());

    db.rootFlag().verify(userDto, false);
  }

  @Test
  public void authenticate_existing_user_and_add_it_to_admin_group_of_default_organization_sets_root_flag_to_true() {
    GroupDto adminGroup = db.users().insertAdminGroup(db.getDefaultOrganization());
    UserDto userDto = db.users().insertUser();

    authenticate(userDto.getLogin(), adminGroup.getName());

    db.rootFlag().verify(userDto, true);
  }

  @Test
  public void authenticate_existing_user_and_add_it_to_admin_group_of_other_organization_sets_root_flag_to_false() {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto adminGroup = db.users().insertAdminGroup(otherOrganization);
    UserDto userDto = db.users().insertUser();

    authenticate(userDto.getLogin(), adminGroup.getName());

    db.rootFlag().verify(userDto, false);
  }

  @Test
  public void authenticate_existing_user_and_remove_it_from_admin_group_of_default_organization_sets_root_flag_to_false() {
    GroupDto adminGroup = db.users().insertAdminGroup(db.getDefaultOrganization());
    UserDto userDto = db.users().makeRoot(db.users().insertUser());
    db.users().insertMembers(adminGroup, userDto);

    authenticate(userDto.getLogin());

    db.rootFlag().verify(userDto, false);
  }

  @Test
  public void authenticate_existing_user_with_user_permission_admin_on_default_organization_with_no_group_does_not_set_root_flag_to_false() {
    UserDto rootUser = db.users().insertRootByUserPermission();

    authenticate(rootUser.getLogin());

    db.rootFlag().verify(rootUser, true);
  }

  @Test
  public void authenticate_existing_user_with_user_permission_admin_on_default_organization_with_non_admin_groups_does_not_set_root_flag_to_false() {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto defaultOrgGroup = db.users().insertGroup(db.getDefaultOrganization());
    GroupDto otherOrgGroup = db.users().insertGroup(otherOrganization);
    UserDto rootUser = db.users().insertRootByUserPermission();

    authenticate(rootUser.getLogin(), defaultOrgGroup.getName(), otherOrgGroup.getName());

    db.rootFlag().verify(rootUser, true);
  }

  @Test
  public void authenticate_user_multiple_times_sets_root_flag_to_true_only_if_at_least_one_group_is_admin() {
    GroupDto defaultAdminGroup = db.users().insertAdminGroup(db.getDefaultOrganization(), "admin_of_default");
    GroupDto defaultSomeGroup = db.users().insertGroup(db.getDefaultOrganization(), "some_group_of_default");
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto otherAdminGroup = db.users().insertAdminGroup(otherOrganization, "admin_of_other");
    GroupDto otherSomeGroup = db.users().insertGroup(otherOrganization, "some_group_of_other");

    authenticate(USER_LOGIN, defaultAdminGroup.getName(), defaultSomeGroup.getName(), otherAdminGroup.getName(), otherSomeGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, defaultAdminGroup.getName(), defaultSomeGroup.getName(), otherAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, otherAdminGroup.getName(), defaultAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, otherAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, false);

    authenticate(USER_LOGIN, otherAdminGroup.getName(), otherSomeGroup.getName());
    db.rootFlag().verify(USER_LOGIN, false);

    authenticate(USER_LOGIN, otherAdminGroup.getName(), otherSomeGroup.getName());
    db.rootFlag().verify(USER_LOGIN, false);

    authenticate(USER_LOGIN, otherAdminGroup.getName(), defaultAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, defaultSomeGroup.getName(), defaultAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, otherSomeGroup.getName(), defaultAdminGroup.getName());
    db.rootFlag().verify(USER_LOGIN, true);

    authenticate(USER_LOGIN, otherSomeGroup.getName(), defaultSomeGroup.getName());
    db.rootFlag().verify(USER_LOGIN, false);
  }

  @Test
  public void ignore_groups_on_non_default_organizations() throws Exception {
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
      .build(), IDENTITY_PROVIDER, Source.sso());

    assertThat(db.users().selectGroupIdsOfUser(user)).containsOnly(groupInDefaultOrg.getId());
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
      .setGroups(Arrays.stream(groups).collect(Collectors.toSet()))
      .build(), IDENTITY_PROVIDER, Source.sso());
  }

}
