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

import com.google.common.collect.Multimap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPropertyDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.process.ProcessProperties.Property.ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS;

public class UserUpdaterReactivateTest {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private DbSession session = db.getSession();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private OrganizationUpdater organizationUpdater = mock(OrganizationUpdater.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private MapSettings settings = new MapSettings();
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());
  private UserUpdater underTest = new UserUpdater(system2, newUserNotifier, dbClient, userIndexer, organizationFlags, defaultOrganizationProvider, organizationUpdater,
    new DefaultGroupFinder(dbClient), settings.asConfig(), localAuthentication);;

  @Test
  public void reactivate_user() {
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin("marius")
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build(),
      u -> {
      });

    UserDto reloaded = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(reloaded.isActive()).isTrue();
    assertThat(reloaded.getLogin()).isEqualTo("marius");
    assertThat(reloaded.getName()).isEqualTo("Marius2");
    assertThat(reloaded.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(reloaded.getScmAccounts()).isNull();
    assertThat(reloaded.isLocal()).isTrue();
    assertThat(reloaded.getSalt()).isNull();
    assertThat(reloaded.getHashMethod()).isEqualTo(HashMethod.BCRYPT.name());
    assertThat(reloaded.getCryptedPassword()).isNotNull().isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(reloaded.getCreatedAt()).isEqualTo(user.getCreatedAt());
    assertThat(reloaded.getUpdatedAt()).isGreaterThan(user.getCreatedAt());
  }

  @Test
  public void reactivate_user_without_providing_login() {
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build(),
      u -> {
      });

    UserDto reloaded = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(reloaded.isActive()).isTrue();
    assertThat(reloaded.getLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void reactivate_user_not_having_password() {
    UserDto user = db.users().insertDisabledUser(u -> u.setSalt(null).setCryptedPassword(null));
    createDefaultGroup();

    UserDto dto = underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(),
      u -> {
      });

    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo(user.getName());
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getCreatedAt()).isEqualTo(user.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isGreaterThan(user.getCreatedAt());
  }

  @Test
  public void reactivate_user_with_external_provider() {
    UserDto user = db.users().insertDisabledUser(u -> u.setLocal(true));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setExternalIdentity(new ExternalIdentity("github", "john", "ABCD"))
      .build(), u -> {
      });
    session.commit();

    UserDto dto = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalId()).isEqualTo("ABCD");
    assertThat(dto.getExternalLogin()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void reactivate_user_using_same_external_info_but_was_local() {
    UserDto user = db.users().insertDisabledUser(u -> u.setLocal(true)
      .setExternalId("ABCD")
      .setExternalLogin("john")
      .setExternalIdentityProvider("github"));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setExternalIdentity(new ExternalIdentity("github", "john", "ABCD"))
      .build(), u -> {
      });
    session.commit();

    UserDto dto = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalId()).isEqualTo("ABCD");
    assertThat(dto.getExternalLogin()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void reactivate_user_with_local_provider() {
    UserDto user = db.users().insertDisabledUser(u -> u.setLocal(false)
      .setExternalId("ABCD")
      .setExternalLogin("john")
      .setExternalIdentityProvider("github"));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });
    session.commit();

    UserDto dto = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(dto.isLocal()).isTrue();
    assertThat(dto.getExternalId()).isEqualTo(user.getLogin());
    assertThat(dto.getExternalLogin()).isEqualTo(user.getLogin());
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
  }

  @Test
  public void fail_to_reactivate_user_if_active() {
    UserDto user = db.users().insertUser();
    createDefaultGroup();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("An active user with login '%s' already exists", user.getLogin()));

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });
  }

  @Test
  public void associate_default_groups_when_reactivating_user_and_organizations_are_disabled() {
    UserDto userDto = db.users().insertDisabledUser();
    db.organizations().insertForUuid("org1");
    GroupDto groupDto = db.users().insertGroup(GroupTesting.newGroupDto().setName("sonar-devs").setOrganizationUuid("org1"));
    db.users().insertMember(groupDto, userDto);
    GroupDto defaultGroup = createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), userDto, NewUser.builder()
      .setLogin(userDto.getLogin())
      .setName(userDto.getName())
      .build(), u -> {
      });
    session.commit();

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, singletonList(userDto.getLogin()));
    assertThat(groups.get(userDto.getLogin()).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isTrue();
  }

  @Test
  public void does_not_associate_default_groups_when_reactivating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    UserDto userDto = db.users().insertDisabledUser();
    db.organizations().insertForUuid("org1");
    GroupDto groupDto = db.users().insertGroup(GroupTesting.newGroupDto().setName("sonar-devs").setOrganizationUuid("org1"));
    db.users().insertMember(groupDto, userDto);
    GroupDto defaultGroup = createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), userDto, NewUser.builder()
      .setLogin(userDto.getLogin())
      .setName(userDto.getName())
      .build(), u -> {
      });
    session.commit();

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, singletonList(userDto.getLogin()));
    assertThat(groups.get(userDto.getLogin()).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isFalse();
  }

  @Test
  public void add_user_as_member_of_default_organization_when_reactivating_user_and_organizations_are_disabled() {
    UserDto user = db.users().insertDisabledUser();
    createDefaultGroup();

    UserDto dto = underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder().setLogin(user.getLogin()).setName(user.getName()).build(), u -> {
    });

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isPresent();
  }

  @Test
  public void does_not_add_user_as_member_of_default_organization_when_reactivating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertDisabledUser();
    createDefaultGroup();

    UserDto dto = underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder().setLogin(user.getLogin()).setName(user.getName()).build(), u -> {
    });

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isNotPresent();
  }

  @Test
  public void reactivate_not_onboarded_user_if_onboarding_setting_is_set_to_false() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS.getKey(), false);
    UserDto user = db.users().insertDisabledUser(u -> u.setOnboarded(false));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, user.getLogin()).isOnboarded()).isTrue();
  }

  @Test
  public void reactivate_onboarded_user_if_onboarding_setting_is_set_to_true() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS.getKey(), true);
    UserDto user = db.users().insertDisabledUser(u -> u.setOnboarded(true));
    createDefaultGroup();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, user.getLogin()).isOnboarded()).isFalse();
  }

  @Test
  public void set_notifications_readDate_setting_when_reactivating_user_on_sonar_cloud() {
    long now = system2.now();
    organizationFlags.setEnabled(true);
    createDefaultGroup();
    UserDto user = db.users().insertDisabledUser();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });

    UserPropertyDto notificationReadDateSetting = dbClient.userPropertiesDao().selectByUser(session, user).get(0);
    assertThat(notificationReadDateSetting.getKey()).isEqualTo("notifications.readDate");
    assertThat(Long.parseLong(notificationReadDateSetting.getValue())).isGreaterThanOrEqualTo(now);
  }

  @Test
  public void does_not_set_notifications_readDate_setting_when_reactivating_user_when_not_on_sonar_cloud() {
    createDefaultGroup();
    UserDto user = db.users().insertDisabledUser();

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .build(), u -> {
      });

    assertThat(dbClient.userPropertiesDao().selectByUser(session, user)).isEmpty();
  }

  @Test
  public void fail_to_reactivate_user_when_login_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setLogin("existing_login"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with login 'existing_login' already exists");

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(existingUser.getLogin())
      .setName("Marius2")
      .setPassword("password2")
      .build(),
      u -> {
      });
  }

  @Test
  public void fail_to_reactivate_user_when_external_id_and_external_provider_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setExternalId("existing_external_id").setExternalIdentityProvider("existing_external_provider"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with provider id 'existing_external_id' and identity provider 'existing_external_provider' already exists");

    underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
      .setLogin(user.getLogin())
      .setName("Marius2")
      .setExternalIdentity(new ExternalIdentity(existingUser.getExternalIdentityProvider(), existingUser.getExternalLogin(), existingUser.getExternalId()))
      .build(),
      u -> {
      });
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

}
