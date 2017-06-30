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
package org.sonar.server.user;

import com.google.common.collect.Multimap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.core.config.CorePropertyDefinitions.ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS;
import static org.sonar.db.user.UserTesting.newDisabledUser;
import static org.sonar.db.user.UserTesting.newLocalUser;

public class UserUpdaterReactivateTest {

  private static final long NOW = 1418215735482L;
  private static final long PAST = 1000000000000L;
  private static final String DEFAULT_LOGIN = "marius";

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private DbSession session = db.getSession();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private OrganizationCreation organizationCreation = mock(OrganizationCreation.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private MapSettings settings = new MapSettings();
  private UserUpdater underTest = new UserUpdater(newUserNotifier, dbClient, userIndexer, system2, organizationFlags, defaultOrganizationProvider, organizationCreation,
    new DefaultGroupFinder(dbClient), settings);

  @Test
  public void reactivate_user_when_creating_user_with_existing_login() {
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(false)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build());
    session.commit();

    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.isLocal()).isTrue();

    assertThat(dto.getSalt()).isNotNull().isNotEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isNotNull().isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(PAST);
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN).isActive()).isTrue();
  }

  @Test
  public void reactivate_user_not_having_password() {
    db.users().insertUser(newDisabledUser("marius").setName("Marius").setEmail("marius@lesbronzes.fr")
      .setSalt(null)
      .setCryptedPassword(null)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .build());
    session.commit();

    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccounts()).isNull();

    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getCreatedAt()).isEqualTo(PAST);
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void reactivate_user_with_external_provider() {
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(true)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setExternalIdentity(new ExternalIdentity("github", "john"))
      .build());
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void reactivate_user_with_local_provider() {
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(true)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setPassword("password")
      .build());
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isLocal()).isTrue();
    assertThat(dto.getExternalIdentity()).isEqualTo(DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
  }

  @Test
  public void fail_to_reactivate_user_if_not_disabled() {
    db.users().insertUser(newLocalUser("marius", "Marius", "marius@lesbronzes.fr")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An active user with login 'marius' already exists");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build());
  }

  @Test
  public void associate_default_groups_when_reactivating_user_and_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    UserDto userDto = db.users().insertUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(true)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    db.organizations().insertForUuid("org1");
    GroupDto groupDto = db.users().insertGroup(GroupTesting.newGroupDto().setName("sonar-devs").setOrganizationUuid("org1"));
    db.users().insertMember(groupDto, userDto);
    GroupDto defaultGroup = createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build());
    session.commit();

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isTrue();
  }

  @Test
  public void does_not_associate_default_groups_when_reactivating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    UserDto userDto = db.users().insertUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(true)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    db.organizations().insertForUuid("org1");
    GroupDto groupDto = db.users().insertGroup(GroupTesting.newGroupDto().setName("sonar-devs").setOrganizationUuid("org1"));
    db.users().insertMember(groupDto, userDto);
    GroupDto defaultGroup = createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .build());
    session.commit();

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isFalse();
  }

  @Test
  public void add_user_as_member_of_default_organization_when_reactivating_user_and_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN));
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Name")
      .setPassword("password")
      .build());
    session.commit();

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isPresent();
  }

  @Test
  public void does_not_add_user_as_member_of_default_organization_when_reactivating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN));
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Name")
      .setPassword("password")
      .build());
    session.commit();

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isNotPresent();
  }

  @Test
  public void reactivate_not_onboarded_user_if_onboarding_setting_is_set_to_false() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, false);
    UserDto user = db.users().insertUser(u -> u
      .setActive(false)
      .setOnboarded(false));
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(user.getLogin())
      .setName("name")
      .setPassword("password")
      .build());

    assertThat(dbClient.userDao().selectByLogin(session, user.getLogin()).isOnboarded()).isTrue();
  }

  @Test
  public void reactivate_onboarded_user_if_onboarding_setting_is_set_to_true() {
    settings.setProperty(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, true);
    UserDto user = db.users().insertUser(u -> u
      .setActive(false)
      .setOnboarded(true));
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(user.getLogin())
      .setName("name")
      .setPassword("password")
      .build());

    assertThat(dbClient.userDao().selectByLogin(session, user.getLogin()).isOnboarded()).isFalse();
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

}
