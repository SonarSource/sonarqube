/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserUpdaterReactivateTest {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final NewUserNotifier newUserNotifier = mock(NewUserNotifier.class);
  private final DbSession session = db.getSession();
  private final UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final UserUpdater underTest = new UserUpdater(newUserNotifier, dbClient, userIndexer,
    new DefaultGroupFinder(dbClient),
    settings.asConfig(), auditPersister, localAuthentication);

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
    assertThat(reloaded.getSalt()).isNotNull();
    assertThat(reloaded.getHashMethod()).isEqualTo(HashMethod.PBKDF2.name());
    assertThat(reloaded.getCryptedPassword()).isNotNull().isNotEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(reloaded.getCreatedAt()).isEqualTo(user.getCreatedAt());
    assertThat(reloaded.getUpdatedAt()).isGreaterThan(user.getCreatedAt());
    verify(auditPersister, times(1)).updateUserPassword(any(), any());
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
    verify(auditPersister, never()).updateUserPassword(any(), any());
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

    assertThatThrownBy(() -> {
      underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
        .setLogin(user.getLogin())
        .setName(user.getName())
        .build(), u -> {
      });
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("An active user with login '%s' already exists", user.getLogin()));
  }

  @Test
  public void associate_default_groups_when_reactivating_user() {
    UserDto userDto = db.users().insertDisabledUser();
    GroupDto groupDto = db.users().insertGroup(GroupTesting.newGroupDto().setName("sonar-devs"));
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
  public void fail_to_reactivate_user_when_login_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setLogin("existing_login"));

    assertThatThrownBy(() -> {
      underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
          .setLogin(existingUser.getLogin())
          .setName("Marius2")
          .setPassword("password2")
          .build(),
        u -> {
        });
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A user with login 'existing_login' already exists");
  }

  @Test
  public void fail_to_reactivate_user_when_external_id_and_external_provider_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setExternalId("existing_external_id").setExternalIdentityProvider("existing_external_provider"));

    assertThatThrownBy(() -> {
      underTest.reactivateAndCommit(db.getSession(), user, NewUser.builder()
          .setLogin(user.getLogin())
          .setName("Marius2")
          .setExternalIdentity(new ExternalIdentity(existingUser.getExternalIdentityProvider(), existingUser.getExternalLogin(), existingUser.getExternalId()))
          .build(),
        u -> {
        });
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A user with provider id 'existing_external_id' and identity provider 'existing_external_provider' already exists");
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup();
  }

}
