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
package org.sonar.server.user;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.CORE_DEFAULT_GROUP;
import static org.sonar.db.user.UserTesting.newDisabledUser;
import static org.sonar.db.user.UserTesting.newUserDto;

import com.google.common.base.Strings;
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.util.Validation;

public class UserUpdaterTest {

  static final long NOW = 1418215735482L;
  static final long PAST = 1000000000000L;

  static final String DEFAULT_LOGIN = "marius";

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new Settings()));

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  DbClient dbClient = db.getDbClient();

  NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);

  ArgumentCaptor<NewUserHandler.Context> newUserHandler = ArgumentCaptor.forClass(NewUserHandler.Context.class);

  Settings settings = new Settings();
  UserDao userDao = dbClient.userDao();
  GroupDao groupDao = dbClient.groupDao();
  GroupMembershipFinder groupMembershipFinder = new GroupMembershipFinder(userDao, dbClient.groupMembershipDao());
  DbSession session = db.getSession();
  UserIndexer userIndexer;

  UserUpdater userUpdater;

  @Before
  public void setUp() {
    userIndexer = (UserIndexer) new UserIndexer(dbClient, es.client()).setEnabled(true);
    userUpdater = new UserUpdater(newUserNotifier, settings, dbClient,
      userIndexer, system2);

    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void create_user() {
    createDefaultGroup();

    boolean result = userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("PASSWORD")
      .setScmAccounts(newArrayList("u1", "u_1", "User 1")));

    UserDto dto = userDao.selectByLogin(session, "user");
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
    assertThat(result).isFalse();

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER);
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

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setPassword("password"));

    UserDto dto = userDao.selectByLogin(session, "user");
    assertThat(dto.getExternalIdentity()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(dto.isLocal()).isTrue();
  }

  @Test
  public void create_user_with_authority() {
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("ABCD")
      .setName("User")
      .setPassword("password")
      .setExternalIdentity(new ExternalIdentity("github", "user")));

    UserDto dto = userDao.selectByLogin(session, "ABCD");
    assertThat(dto.getExternalIdentity()).isEqualTo("user");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.isLocal()).isFalse();
  }

  @Test
  public void create_user_with_minimum_fields() {
    when(system2.now()).thenReturn(1418215735482L);
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User"));

    UserDto dto = userDao.selectByLogin(session, "user");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLogin()).isEqualTo("user");
    assertThat(dto.getName()).isEqualTo("User");
    assertThat(dto.getEmail()).isNull();
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.isActive()).isTrue();
  }

  @Test
  public void create_user_with_scm_accounts_containing_blank_or_null_entries() {
    when(system2.now()).thenReturn(1418215735482L);
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "", null)));

    assertThat(userDao.selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void create_user_with_scm_accounts_containing_one_blank_entry() {
    when(system2.now()).thenReturn(1418215735482L);
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList("")));

    assertThat(userDao.selectByLogin(session, "user").getScmAccounts()).isNull();
  }

  @Test
  public void create_user_with_scm_accounts_containing_duplications() {
    when(system2.now()).thenReturn(1418215735482L);
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u1")));

    assertThat(userDao.selectByLogin(session, "user").getScmAccountsAsList()).containsOnly("u1");
  }

  @Test
  public void fail_to_create_user_with_missing_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(null)
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, "Login"));
    }
  }

  @Test
  public void fail_to_create_user_with_invalid_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin("/marius/")
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.bad_login"));
    }
  }

  @Test
  public void fail_to_create_user_with_space_in_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin("mari us")
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.bad_login"));
    }
  }

  @Test
  public void fail_to_create_user_with_too_short_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin("ma")
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.IS_TOO_SHORT_MESSAGE, "Login", 3));
    }
  }

  @Test
  public void fail_to_create_user_with_too_long_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(Strings.repeat("m", 256))
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.IS_TOO_LONG_MESSAGE, "Login", 255));
    }
  }

  @Test
  public void fail_to_create_user_with_missing_name() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName(null)
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    }
  }

  @Test
  public void fail_to_create_user_with_too_long_name() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName(Strings.repeat("m", 201))
        .setEmail("marius@mail.com")
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.IS_TOO_LONG_MESSAGE, "Name", 200));
    }
  }

  @Test
  public void fail_to_create_user_with_too_long_email() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius")
        .setEmail(Strings.repeat("m", 101))
        .setPassword("password"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of(Validation.IS_TOO_LONG_MESSAGE, "Email", 100));
    }
  }

  @Test
  public void fail_to_create_user_with_many_errors() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin("")
        .setName("")
        .setEmail("marius@mail.com")
        .setPassword(""));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).hasSize(3);
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used() {
    db.prepareDbUnit(getClass(), "fail_to_create_user_when_scm_account_is_already_used.xml");

    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password")
        .setScmAccounts(newArrayList("jo")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.scm_account_already_used", "jo", "John (john)"));
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_already_used_by_many_user() {
    db.prepareDbUnit(getClass(), "fail_to_create_user_when_scm_account_is_already_used_by_many_user.xml");

    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius")
        .setEmail("marius@mail.com")
        .setPassword("password")
        .setScmAccounts(newArrayList("john@email.com")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.scm_account_already_used", "john@email.com", "John (john), Technical account (technical-account)"));
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_login() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius2")
        .setEmail("marius2@mail.com")
        .setPassword("password2")
        .setScmAccounts(newArrayList(DEFAULT_LOGIN)));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.login_or_email_used_as_scm_account"));
    }
  }

  @Test
  public void fail_to_create_user_when_scm_account_is_user_email() {
    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius2")
        .setEmail("marius2@mail.com")
        .setPassword("password2")
        .setScmAccounts(newArrayList("marius2@mail.com")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.login_or_email_used_as_scm_account"));
    }
  }

  @Test
  public void notify_new_user() {
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    verify(newUserNotifier).onNewUser(newUserHandler.capture());
    assertThat(newUserHandler.getValue().getLogin()).isEqualTo("user");
    assertThat(newUserHandler.getValue().getName()).isEqualTo("User");
    assertThat(newUserHandler.getValue().getEmail()).isEqualTo("user@mail.com");
  }

  @Test
  public void associate_default_group_when_creating_user() {
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setPassword("password")
      .setScmAccounts(newArrayList("u1", "u_1")));

    GroupMembershipFinder.Membership membership = groupMembershipFinder.find(GroupMembershipQuery.builder().login("user").build());
    assertThat(membership.groups()).hasSize(1);
    assertThat(membership.groups().get(0).name()).isEqualTo("sonar-users");
    assertThat(membership.groups().get(0).isMember()).isTrue();
  }

  @Test
  public void fail_to_associate_default_group_to_user_if_no_default_group() {
    settings.setProperty(CORE_DEFAULT_GROUP, (String) null);

    try {
      userUpdater.create(NewUser.create()
        .setLogin("user")
        .setName("User")
        .setEmail("user@mail.com")
        .setPassword("password")
        .setScmAccounts(newArrayList("u1", "u_1")));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ServerException.class).hasMessage("The default group property 'sonar.defaultGroup' is null");
    }
  }

  @Test
  public void fail_to_associate_default_group_when_default_group_does_not_exist() {
    settings.setProperty(CORE_DEFAULT_GROUP, "polop");

    try {
      userUpdater.create(NewUser.create()
        .setLogin("user")
        .setName("User")
        .setEmail("user@mail.com")
        .setPassword("password")
        .setScmAccounts(newArrayList("u1", "u_1")));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ServerException.class)
        .hasMessage("The default group 'polop' for new users does not exist. Please update the general security settings to fix this issue.");
    }
  }

  @Test
  public void reactivate_user_when_creating_user_with_existing_login() {
    addUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(false)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST)
    );
    createDefaultGroup();

    boolean result = userUpdater.create(NewUser.create()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2"));
    session.commit();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccounts()).isNull();
    assertThat(dto.isLocal()).isTrue();

    assertThat(dto.getSalt()).isNotNull().isNotEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isNotNull().isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(PAST);
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);

    assertThat(result).isTrue();
  }

  @Test
  public void reactivate_user_not_having_password() {
    db.prepareDbUnit(getClass(), "reactivate_user_not_having_password.xml");
    when(system2.now()).thenReturn(1418215735486L);
    createDefaultGroup();

    boolean result = userUpdater.create(NewUser.create()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com"));
    session.commit();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccounts()).isNull();

    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735486L);

    assertThat(result).isTrue();
  }

  @Test
  public void update_external_provider_when_reactivating_user() {
    addUser(newDisabledUser(DEFAULT_LOGIN)
      .setLocal(true)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST)
    );
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setPassword("password2")
      .setExternalIdentity(new ExternalIdentity("github", "john")));
    session.commit();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.isLocal()).isFalse();
  }

  @Test
  public void fail_to_reactivate_user_if_not_disabled() {
    db.prepareDbUnit(getClass(), "fail_to_reactivate_user_if_not_disabled.xml");
    createDefaultGroup();

    try {
      userUpdater.create(NewUser.create()
        .setLogin(DEFAULT_LOGIN)
        .setName("Marius2")
        .setEmail("marius2@mail.com")
        .setPassword("password2"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("An active user with login 'marius' already exists");
    }
  }

  @Test
  public void associate_default_groups_when_reactivating_user() {
    db.prepareDbUnit(getClass(), "associate_default_groups_when_reactivating_user.xml");
    createDefaultGroup();

    userUpdater.create(NewUser.create()
      .setLogin(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2"));
    session.commit();

    GroupMembershipFinder.Membership membership = groupMembershipFinder.find(GroupMembershipQuery.builder().login(DEFAULT_LOGIN).groupSearch("sonar-users").build());
    assertThat(membership.groups()).hasSize(1);
    assertThat(membership.groups().get(0).name()).isEqualTo("sonar-users");
    assertThat(membership.groups().get(0).isMember()).isTrue();
  }

  @Test
  public void update_user() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    when(system2.now()).thenReturn(1418215735486L);
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");

    assertThat(dto.getSalt()).isNotEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735486L);

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSource())
      .contains(
        entry("login", DEFAULT_LOGIN),
        entry("name", "Marius2"),
        entry("email", "marius2@mail.com"));
  }

  @Test
  public void update_user_external_identity_when_user_was_not_local() {
    addUser(UserTesting.newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST)
    );
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setPassword(null)
      .setExternalIdentity(new ExternalIdentity("github", "john")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void update_user_external_identity_when_user_was_local() {
    addUser(UserTesting.newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST)
    );
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setPassword(null)
      .setExternalIdentity(new ExternalIdentity("github", "john")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalIdentity()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    // Password must be removed
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void reactivate_user_on_update() {
    db.prepareDbUnit(getClass(), "reactivate_user.xml");
    when(system2.now()).thenReturn(1418215735486L);
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getName()).isEqualTo("Marius2");
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");

    assertThat(dto.getSalt()).isNotEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735486L);

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSource())
      .contains(
        entry("login", DEFAULT_LOGIN),
        entry("name", "Marius2"),
        entry("email", "marius2@mail.com"));
  }

  @Test
  public void update_user_with_scm_accounts_containing_blank_entry() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2", "", null)));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");
  }

  @Test
  public void update_only_user_name() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2"));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getName()).isEqualTo("Marius2");

    // Following fields has not changed
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
  }

  @Test
  public void update_only_user_email() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setEmail("marius2@mail.com"));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getEmail()).isEqualTo("marius2@mail.com");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
  }

  @Test
  public void update_only_scm_accounts() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(newArrayList("ma2")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
  }

  @Test
  public void update_scm_accounts_with_same_values() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(newArrayList("ma", "marius33")));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
  }

  @Test
  public void remove_scm_accounts() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setScmAccounts(null));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccounts()).isNull();
  }

  @Test
  public void update_only_user_password() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setPassword("password2"));
    session.commit();
    session.clearCache();

    UserDto dto = userDao.selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getSalt()).isNotEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
  }

  @Test
  public void fail_to_set_null_password_when_local_user() {
    addUser(UserTesting.newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setPassword(null));
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("errors.cant_be_empty", "Password"));
    }
  }

  @Test
  public void fail_to_update_password_when_user_is_not_local() {
    UserDto user = newUserDto()
      .setLogin(DEFAULT_LOGIN)
      .setLocal(false);
    userDao.insert(session, user);
    session.commit();
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setPassword("password2"));
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.password_cant_be_changed_on_external_auth"));
    }
  }

  @Test
  public void not_associate_default_group_when_updating_user() {
    db.prepareDbUnit(getClass(), "associate_default_groups_when_updating_user.xml");
    createDefaultGroup();

    // Existing user, he has no group, and should not be associated to the default one
    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    GroupMembershipFinder.Membership membership = groupMembershipFinder.find(GroupMembershipQuery.builder().login(DEFAULT_LOGIN).groupSearch("sonar-users").build());
    assertThat(membership.groups()).hasSize(1);
    assertThat(membership.groups().get(0).name()).isEqualTo("sonar-users");
    assertThat(membership.groups().get(0).isMember()).isFalse();
  }

  @Test
  public void not_associate_default_group_when_updating_user_if_already_existing() {
    db.prepareDbUnit(getClass(), "not_associate_default_group_when_updating_user_if_already_existing.xml");
    settings.setProperty(CORE_DEFAULT_GROUP, "sonar-users");
    session.commit();

    // User is already associate to the default group
    GroupMembershipFinder.Membership membership = groupMembershipFinder.find(GroupMembershipQuery.builder().login(DEFAULT_LOGIN).groupSearch("sonar-users").build());
    assertThat(membership.groups()).hasSize(1);
    assertThat(membership.groups().get(0).name()).isEqualTo("sonar-users");
    assertThat(membership.groups().get(0).isMember()).isTrue();

    userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(newArrayList("ma2")));
    session.commit();

    // Nothing as changed
    membership = groupMembershipFinder.find(GroupMembershipQuery.builder().login(DEFAULT_LOGIN).groupSearch("sonar-users").build());
    assertThat(membership.groups()).hasSize(1);
    assertThat(membership.groups().get(0).name()).isEqualTo("sonar-users");
    assertThat(membership.groups().get(0).isMember()).isTrue();
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_already_used() {
    db.prepareDbUnit(getClass(), "fail_to_update_user_when_scm_account_is_already_used.xml");
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setName("Marius2")
        .setEmail("marius2@mail.com")
        .setPassword("password2")
        .setScmAccounts(newArrayList("jo")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.scm_account_already_used", "jo", "John (john)"));
    }
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_user_login() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setScmAccounts(newArrayList(DEFAULT_LOGIN)));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.login_or_email_used_as_scm_account"));
    }
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_existing_user_email() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setScmAccounts(newArrayList("marius@lesbronzes.fr")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.login_or_email_used_as_scm_account"));
    }
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_new_user_email() {
    db.prepareDbUnit(getClass(), "update_user.xml");
    createDefaultGroup();

    try {
      userUpdater.update(UpdateUser.create(DEFAULT_LOGIN)
        .setEmail("marius@newmail.com")
        .setScmAccounts(newArrayList("marius@newmail.com")));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors().messages()).containsOnly(Message.of("user.login_or_email_used_as_scm_account"));
    }
  }

  private void createDefaultGroup() {
    settings.setProperty(CORE_DEFAULT_GROUP, "sonar-users");
    groupDao.insert(session, new GroupDto().setName("sonar-users").setDescription("Sonar Users"));
    session.commit();
  }

  private UserDto addUser(UserDto user) {
    userDao.insert(session, user);
    session.commit();
    return user;
  }
}
