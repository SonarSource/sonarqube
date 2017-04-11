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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.MapSettings;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newDisabledUser;
import static org.sonar.db.user.UserTesting.newLocalUser;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

public class UserUpdaterTest {

  private static final long NOW = 1418215735482L;
  private static final long PAST = 1000000000000L;
  private static final String DEFAULT_LOGIN = "marius";

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private ArgumentCaptor<NewUserHandler.Context> newUserHandler = ArgumentCaptor.forClass(NewUserHandler.Context.class);
  private DbSession session = db.getSession();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private OrganizationCreation organizationCreation = mock(OrganizationCreation.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private UserUpdater underTest = new UserUpdater(newUserNotifier, dbClient, userIndexer, system2, organizationFlags, defaultOrganizationProvider, organizationCreation,
    new DefaultGroupFinder(dbClient));

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void create_user() {
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .setScmAccounts(ImmutableList.of("u1", "u_1", "User 1"))
      .build());

    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("user");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isEqualTo("user@mail.com");
    assertThat(dto.getScmAccountsAsList()).containsOnly("u1", "u_1", "User 1");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.isLocal()).isTrue();

    assertThat(dto.getSalt()).isNotNull();
    assertThat(dto.getCryptedPassword()).isNotNull();
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735482L);

    assertThat(dbClient.userDao().selectByLogin(session, "user").getId()).isEqualTo(dto.getId());
    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSource())
      .contains(
        entry("login", "user"),
        entry("name", "User"),
        entry("email", "user@mail.com"));
  }

  @Test
  public void create_user_with_sq_authority_when_no_authority_set() throws Exception {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .build());

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto.getExternalIdentity()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(dto.isLocal()).isTrue();
  }

  @Test
  public void create_user_with_identity_provider() throws Exception {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity("github", "github-user"))
      .build());

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalIdentity()).isEqualTo("github-user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
  }

  @Test
  public void create_user_with_sonarqube_external_identity() throws Exception {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setExternalIdentity(new ExternalIdentity(SQ_AUTHORITY, "user"))
      .build());

    UserDto dto = dbClient.userDao().selectByLogin(session, "user");
    assertThat(dto.isLocal()).isFalse();
    assertThat(dto.getExternalIdentity()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
  }

  @Test
  public void create_user_with_minimum_fields() {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("us")
      .setName("User")
      .build());

    UserDto dto = dbClient.userDao().selectByLogin(session, "us");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("us");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isNull();
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.isActive()).isTrue();
  }

  @Test
  public void create_user_with_scm_accounts_containing_blank_or_null_entries() {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "", null))
      .build());

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void create_user_with_scm_accounts_containing_one_blank_entry() {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList(""))
      .build());

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccounts()).isNull();
  }

  @Test
  public void create_user_with_scm_accounts_containing_duplications() {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u1"))
      .build());

    assertThat(dbClient.userDao().selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void fail_to_create_user_with_missing_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login can't be empty");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(null)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_invalid_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("/marius/")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_space_in_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("mari us")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_too_short_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login is too short (minimum is 2 characters)");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("m")
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_too_long_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login is too long (maximum is 255 characters)");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(Strings.repeat("m", 256))
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_missing_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name can't be empty");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName(null)
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_too_long_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name is too long (maximum is 200 characters)");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName(Strings.repeat("m", 201))
      .setEmail("marius@mail.com")
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_too_long_email() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Email is too long (maximum is 100 characters)");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail(Strings.repeat("m", 101))
      .setPassword("password")
      .build());
  }

  @Test
  public void fail_to_create_user_with_many_errors() {
    try {
      underTest.create(db.getSession(), NewUser.builder()
        .setLogin("")
        .setName("")
        .setEmail("marius@mail.com")
        .setPassword("")
        .build());
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors()).hasSize(3);
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("jo")));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'jo' is already used by user(s) : 'John (john)'");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("jo"))
      .build());
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used_by_many_users() {
    db.users().insertUser(newLocalUser("john", "John", null).setScmAccounts(singletonList("john@email.com")));
    db.users().insertUser(newLocalUser("technical-account", "Technical account", null).setScmAccounts(singletonList("john@email.com")));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'john@email.com' is already used by user(s) : 'John (john), Technical account (technical-account)'");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius")
      .setEmail("marius@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("john@email.com"))
      .build());
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_login() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList(DEFAULT_LOGIN))
      .build());
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_email() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("marius2@mail.com"))
      .build());
  }

  @Test
  public void notify_new_user() {
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u_1"))
      .build());

    verify(newUserNotifier).onNewUser(newUserHandler.capture());
    assertThat(newUserHandler.getValue().getLogin()).isEqualTo("user");
    assertThat(newUserHandler.getValue().getName()).isEqualTo("User");
    assertThat(newUserHandler.getValue().getEmail()).isEqualTo("user@mail.com");
  }

  @Test
  public void associate_default_group_when_creating_user_and_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    GroupDto defaultGroup = createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .build());

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList("user"));
    assertThat(groups.get("user")).containsOnly(defaultGroup.getName());
  }

  @Test
  public void does_not_associate_default_group_when_creating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    createDefaultGroup();

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .build());

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList("user"));
    assertThat(groups.get("user")).isEmpty();
  }

  @Test
  public void fail_to_associate_default_group_when_default_group_does_not_exist() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group cannot be found");

    underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u_1"))
      .build());
  }

  @Test
  public void create_personal_organization_when_creating_user() {
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build());

    verify(organizationCreation).createForUser(any(DbSession.class), eq(dto));
  }

  @Test
  public void add_user_as_member_of_default_organization_when_creating_user_and_organizations_are_disabled() {
    organizationFlags.setEnabled(false);
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build());

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isPresent();
  }

  @Test
  public void does_not_add_user_as_member_of_default_organization_when_creating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .build());

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isNotPresent();
  }

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
  public void update_external_provider_when_reactivating_user() {
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
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.isLocal()).isFalse();
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

    UserDto dto = underTest.create(db.getSession(), NewUser.builder().setLogin(DEFAULT_LOGIN).setName("Name").build());
    session.commit();

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isPresent();
  }

  @Test
  public void does_not_add_user_as_member_of_default_organization_when_reactivating_user_and_organizations_are_enabled() {
    organizationFlags.setEnabled(true);
    db.users().insertUser(newDisabledUser(DEFAULT_LOGIN));
    createDefaultGroup();

    UserDto dto = underTest.create(db.getSession(), NewUser.builder().setLogin(DEFAULT_LOGIN).setName("Name").build());
    session.commit();

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), defaultOrganizationProvider.get().getUuid(), dto.getId())).isNotPresent();
  }

  @Test
  public void update_user() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365")
      .setCryptedPassword("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    UserDto updatedUser = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(updatedUser.isActive()).isTrue();
    assertThat(updatedUser.getName()).isEqualTo("Marius2");
    assertThat(updatedUser.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(updatedUser.getScmAccountsAsList()).containsOnly("ma2");

    assertThat(updatedUser.getSalt()).isNotEqualTo(user.getSalt());
    assertThat(updatedUser.getCryptedPassword()).isNotEqualTo(user.getCryptedPassword());
    assertThat(updatedUser.getCreatedAt()).isEqualTo(PAST);
    assertThat(updatedUser.getUpdatedAt()).isEqualTo(NOW);

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSource())
      .contains(
        entry("login", DEFAULT_LOGIN),
        entry("name", "Marius2"),
        entry("email", "marius2@mail.com"));
  }

  @Test
  public void update_user_external_identity_when_user_was_not_local() {
    db.users().insertUser(UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setPassword(null)
      .setExternalIdentity(new ExternalIdentity("github", "john")));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void update_user_external_identity_when_user_was_local() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setPassword(null)
      .setExternalIdentity(new ExternalIdentity("github", "john")));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    // Password must be removed
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void reactivate_user_on_update() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");

    assertThat(dto.getSalt()).isNotEqualTo(user.getSalt());
    assertThat(dto.getCryptedPassword()).isNotEqualTo(user.getCryptedPassword());
    assertThat(dto.getCreatedAt()).isEqualTo(PAST);
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSource())
      .contains(
        entry("login", DEFAULT_LOGIN),
        entry("name", "Marius2"),
        entry("email", "marius2@mail.com"));
  }

  @Test
  public void update_user_with_scm_accounts_containing_blank_entry() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2", "", null)));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");
  }

  @Test
  public void update_only_user_name() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2"));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getName()).isEqualTo("Marius2");

    // Following fields has not changed
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("salt");
    assertThat(dto.getCryptedPassword()).isEqualTo("crypted password");
  }

  @Test
  public void update_only_user_email() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setEmail("marius2@mail.com"));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("salt");
    assertThat(dto.getCryptedPassword()).isEqualTo("crypted password");
  }

  @Test
  public void update_only_scm_accounts() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.getSalt()).isEqualTo("salt");
    assertThat(dto.getCryptedPassword()).isEqualTo("crypted password");
  }

  @Test
  public void update_scm_accounts_with_same_values() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(newArrayList("ma", "marius33")));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
  }

  @Test
  public void remove_scm_accounts() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(null));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccounts()).isNull();
  }

  @Test
  public void update_only_user_password() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setPassword("password2"));
    session.commit();

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getSalt()).isNotEqualTo("salt");
    assertThat(dto.getCryptedPassword()).isNotEqualTo("crypted password");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
  }

  @Test
  public void update_only_external_identity_id() {
    db.users().insertUser(UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalIdentity("john")
      .setExternalIdentityProvider("github")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setExternalIdentity(new ExternalIdentity("github", "john.smith")));
    session.commit();

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN))
      .extracting(UserDto::getExternalIdentity, UserDto::getExternalIdentityProvider, UserDto::getUpdatedAt)
      .containsOnly("john.smith", "github", NOW);
  }

  @Test
  public void update_only_external_identity_provider() {
    db.users().insertUser(UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalIdentity("john")
      .setExternalIdentityProvider("github")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST));
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setExternalIdentity(new ExternalIdentity("bitbucket", "john")));
    session.commit();

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN))
      .extracting(UserDto::getExternalIdentity, UserDto::getExternalIdentityProvider, UserDto::getUpdatedAt)
      .containsOnly("john", "bitbucket", NOW);
  }

  @Test
  public void does_not_update_user_when_no_change() {
    UserDto user = UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalIdentity("john")
      .setExternalIdentityProvider("github")
      .setScmAccounts(asList("ma1", "ma2"))
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    db.users().insertUser(user);
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(user.getLogin())
      .setName(user.getName())
      .setEmail(user.getEmail())
      .setScmAccounts(user.getScmAccountsAsList())
      .setExternalIdentity(new ExternalIdentity(user.getExternalIdentityProvider(), user.getExternalIdentity())));
    session.commit();

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN).getUpdatedAt()).isEqualTo(PAST);
  }

  @Test
  public void does_not_update_user_when_no_change_and_scm_account_reordered() {
    UserDto user = UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalIdentity("john")
      .setExternalIdentityProvider("github")
      .setScmAccounts(asList("ma1", "ma2"))
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    db.users().insertUser(user);
    createDefaultGroup();

    underTest.update(session, UpdateUser.create(user.getLogin())
      .setName(user.getName())
      .setEmail(user.getEmail())
      .setScmAccounts(asList("ma2", "ma1"))
      .setExternalIdentity(new ExternalIdentity(user.getExternalIdentityProvider(), user.getExternalIdentity())));
    session.commit();

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN).getUpdatedAt()).isEqualTo(PAST);
  }

  @Test
  public void fail_to_set_null_password_when_local_user() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Password can't be empty");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setPassword(null));
  }

  @Test
  public void fail_to_update_password_when_user_is_not_local() {
    db.users().insertUser(newUserDto()
      .setLogin(DEFAULT_LOGIN)
      .setLocal(false));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Password cannot be changed when external authentication is used");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setPassword("password2"));
  }

  @Test
  public void not_associate_default_group_when_updating_user() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    GroupDto defaultGroup = createDefaultGroup();

    // Existing user, he has no group, and should not be associated to the default one
    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isFalse();
  }

  @Test
  public void not_associate_default_group_when_updating_user_if_already_existing() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    GroupDto defaultGroup = createDefaultGroup();
    db.users().insertMember(defaultGroup, user);

    // User is already associate to the default group
    Multimap<String, String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).as("Current user groups : %s", groups.get(defaultGroup.getName())).isTrue();

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    // Nothing as changed
    groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isTrue();
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_already_used() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com").setScmAccounts(singletonList("ma")));
    db.users().insertUser(newLocalUser("john", "John", "john@email.com").setScmAccounts(singletonList("jo")));
    createDefaultGroup();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'jo' is already used by user(s) : 'John (john)'");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("jo")));
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_user_login() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setScmAccounts(newArrayList(DEFAULT_LOGIN)));
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_existing_user_email() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN).setScmAccounts(newArrayList("marius@lesbronzes.fr")));
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_new_user_email() {
    db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.update(session, UpdateUser.create(DEFAULT_LOGIN)
      .setEmail("marius@newmail.com")
      .setScmAccounts(newArrayList("marius@newmail.com")));
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

}
