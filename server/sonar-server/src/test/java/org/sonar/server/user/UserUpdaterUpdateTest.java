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
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.user.UserTesting.newExternalUser;
import static org.sonar.db.user.UserTesting.newLocalUser;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserUpdaterUpdateTest {

  private static final String DEFAULT_LOGIN = "marius";

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
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private MapSettings settings = new MapSettings();
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());
  private UserUpdater underTest = new UserUpdater(system2, newUserNotifier, dbClient, userIndexer, organizationFlags, defaultOrganizationProvider,
    new DefaultGroupFinder(dbClient), settings.asConfig(), localAuthentication);

  @Test
  public void update_user() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setScmAccounts(asList("ma", "marius33")));
    createDefaultGroup();
    userIndexer.indexOnStartup(null);

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setScmAccounts(singletonList("ma2")), u -> {
      });

    UserDto updatedUser = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(updatedUser.isActive()).isTrue();
    assertThat(updatedUser.getName()).isEqualTo("Marius2");
    assertThat(updatedUser.getEmail()).isEqualTo("marius2@mail.com");
    assertThat(updatedUser.getScmAccountsAsList()).containsOnly("ma2");
    assertThat(updatedUser.getCreatedAt()).isEqualTo(user.getCreatedAt());
    assertThat(updatedUser.getUpdatedAt()).isGreaterThan(user.getCreatedAt());

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSourceAsMap())
      .contains(
        entry("login", DEFAULT_LOGIN),
        entry("name", "Marius2"),
        entry("email", "marius2@mail.com"));
  }

  @Test
  public void update_user_external_identity_when_user_was_not_local() {
    UserDto user = db.users().insertUser(newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setExternalIdentity(new ExternalIdentity("github", "john", "ABCD")), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalId()).isEqualTo("ABCD");
    assertThat(dto.getExternalLogin()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(dto.getUpdatedAt()).isGreaterThan(user.getCreatedAt());
  }

  @Test
  public void update_user_external_identity_when_user_was_local() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@email.com")
      .setExternalIdentity(new ExternalIdentity("github", "john", "ABCD")), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getExternalId()).isEqualTo("ABCD");
    assertThat(dto.getExternalLogin()).isEqualTo("john");
    assertThat(dto.getExternalIdentityProvider()).isEqualTo("github");
    // Password must be removed
    assertThat(dto.getCryptedPassword()).isNull();
    assertThat(dto.getSalt()).isNull();
    assertThat(dto.getUpdatedAt()).isGreaterThan(user.getCreatedAt());
  }

  @Test
  public void update_user_with_scm_accounts_containing_blank_entry() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33")));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("ma2", "", null)), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma2");
  }

  @Test
  public void update_only_login_of_local_account() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setLogin("new_login"), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN)).isNull();
    UserDto userReloaded = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(userReloaded.getLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalIdentityProvider()).isEqualTo("sonarqube");
    assertThat(userReloaded.getExternalLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalId()).isEqualTo("new_login");
    // Following fields has not changed
    assertThat(userReloaded.isLocal()).isTrue();
    assertThat(userReloaded.getName()).isEqualTo(user.getName());
    assertThat(userReloaded.getEmail()).isEqualTo(user.getEmail());
    assertThat(userReloaded.getScmAccountsAsList()).containsAll(user.getScmAccountsAsList());
    assertThat(userReloaded.getSalt()).isEqualTo(user.getSalt());
    assertThat(userReloaded.getCryptedPassword()).isEqualTo(user.getCryptedPassword());
  }

  @Test
  public void update_only_login_of_external_account() {
    UserDto user = db.users().insertUser(newExternalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setLogin("new_login"), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN)).isNull();
    UserDto userReloaded = dbClient.userDao().selectByUuid(session, user.getUuid());
    assertThat(userReloaded.getLogin()).isEqualTo("new_login");
    // Following fields has not changed
    assertThat(userReloaded.isLocal()).isFalse();
    assertThat(userReloaded.getExternalLogin()).isEqualTo(user.getExternalLogin());
    assertThat(userReloaded.getExternalId()).isEqualTo(user.getExternalId());
    assertThat(userReloaded.getName()).isEqualTo(user.getName());
    assertThat(userReloaded.getEmail()).isEqualTo(user.getEmail());
    assertThat(userReloaded.getScmAccountsAsList()).containsAll(user.getScmAccountsAsList());
    assertThat(userReloaded.getSalt()).isEqualTo(user.getSalt());
    assertThat(userReloaded.getCryptedPassword()).isEqualTo(user.getCryptedPassword());
  }

  @Test
  public void update_index_when_updating_user_login() {
    UserDto oldUser = db.users().insertUser();
    createDefaultGroup();
    userIndexer.indexOnStartup(null);

    underTest.updateAndCommit(session, oldUser, new UpdateUser()
      .setLogin("new_login"), u -> {
      });

    List<SearchHit> indexUsers = es.getDocuments(UserIndexDefinition.TYPE_USER);
    assertThat(indexUsers).hasSize(1);
    assertThat(indexUsers.get(0).getSourceAsMap())
      .contains(entry("login", "new_login"));
  }

  @Test
  public void update_default_assignee_when_updating_login() {
    createDefaultGroup();
    UserDto oldUser = db.users().insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    db.properties().insertProperties(
      new PropertyDto().setKey(DEFAULT_ISSUE_ASSIGNEE).setValue(oldUser.getLogin()),
      new PropertyDto().setKey(DEFAULT_ISSUE_ASSIGNEE).setValue(oldUser.getLogin()).setResourceId(project1.getId()),
      new PropertyDto().setKey(DEFAULT_ISSUE_ASSIGNEE).setValue(oldUser.getLogin()).setResourceId(project2.getId()),
      new PropertyDto().setKey(DEFAULT_ISSUE_ASSIGNEE).setValue("another login").setResourceId(anotherProject.getId()));
    userIndexer.indexOnStartup(null);

    underTest.updateAndCommit(session, oldUser, new UpdateUser()
      .setLogin("new_login"), u -> {
      });

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setKey(DEFAULT_ISSUE_ASSIGNEE).build(), db.getSession()))
      .extracting(PropertyDto::getValue, PropertyDto::getResourceId)
      .containsOnly(
        tuple("new_login", null),
        tuple("new_login", project1.getId()),
        tuple("new_login", project2.getId()),
        tuple("another login", anotherProject.getId()));
  }

  @Test
  public void update_only_user_name() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2"), u -> {
      });

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
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setEmail("marius2@mail.com"), u -> {
      });

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
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setScmAccounts(asList("ma2")), u -> {
      });

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
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33")));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setScmAccounts(asList("ma", "marius33")), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
  }

  @Test
  public void remove_scm_accounts() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33")));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setScmAccounts(null), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getScmAccounts()).isNull();
  }

  @Test
  public void update_only_user_password() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr")
      .setScmAccounts(asList("ma", "marius33"))
      .setSalt("salt")
      .setCryptedPassword("crypted password"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setPassword("password2"), u -> {
      });

    UserDto dto = dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN);
    assertThat(dto.getSalt()).isNotEqualTo("salt");
    assertThat(dto.getCryptedPassword()).isNotEqualTo("crypted password");

    // Following fields has not changed
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
  }

  @Test
  public void update_only_external_id() {
    UserDto user = db.users().insertUser(newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalId("1234")
      .setExternalLogin("john.smith")
      .setExternalIdentityProvider("github"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser().setExternalIdentity(new ExternalIdentity("github", "john.smith", "ABCD")), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN))
      .extracting(UserDto::getExternalId)
      .isEqualTo("ABCD");
  }

  @Test
  public void update_only_external_login() {
    UserDto user = db.users().insertUser(newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalId("ABCD")
      .setExternalLogin("john")
      .setExternalIdentityProvider("github"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser().setExternalIdentity(new ExternalIdentity("github", "john.smith", "ABCD")), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN))
      .extracting(UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .containsOnly("john.smith", "github");
  }

  @Test
  public void update_only_external_identity_provider() {
    UserDto user = db.users().insertUser(newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setExternalId("ABCD")
      .setExternalLogin("john")
      .setExternalIdentityProvider("github"));
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser().setExternalIdentity(new ExternalIdentity("bitbucket", "john", "ABCD")), u -> {
    });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN))
      .extracting(UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .containsOnly("john", "bitbucket");
  }

  @Test
  public void does_not_update_user_when_no_change() {
    UserDto user = newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setScmAccounts(asList("ma1", "ma2"));
    db.users().insertUser(user);
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName(user.getName())
      .setEmail(user.getEmail())
      .setScmAccounts(user.getScmAccountsAsList())
      .setExternalIdentity(new ExternalIdentity(user.getExternalIdentityProvider(), user.getExternalLogin(), user.getExternalId())), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN).getUpdatedAt()).isEqualTo(user.getUpdatedAt());
  }

  @Test
  public void does_not_update_user_when_no_change_and_scm_account_reordered() {
    UserDto user = newExternalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setScmAccounts(asList("ma1", "ma2"));
    db.users().insertUser(user);
    createDefaultGroup();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName(user.getName())
      .setEmail(user.getEmail())
      .setScmAccounts(asList("ma2", "ma1"))
      .setExternalIdentity(new ExternalIdentity(user.getExternalIdentityProvider(), user.getExternalLogin(), user.getExternalId())), u -> {
      });

    assertThat(dbClient.userDao().selectByLogin(session, DEFAULT_LOGIN).getUpdatedAt()).isEqualTo(user.getUpdatedAt());
  }

  @Test
  public void update_user_and_index_other_user() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com")
      .setScmAccounts(asList("ma", "marius33")));
    UserDto otherUser = db.users().insertUser();

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("ma2")), u -> {
      }, otherUser);

    assertThat(es.getIds(UserIndexDefinition.TYPE_USER)).containsExactlyInAnyOrder(user.getUuid(), otherUser.getUuid());
  }

  @Test
  public void fail_to_set_null_password_when_local_user() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Password can't be empty");

    underTest.updateAndCommit(session, user, new UpdateUser().setPassword(null), u -> {
    });
  }

  @Test
  public void fail_to_update_password_when_user_is_not_local() {
    UserDto user = db.users().insertUser(newUserDto()
      .setLogin(DEFAULT_LOGIN)
      .setLocal(false));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Password cannot be changed when external authentication is used");

    underTest.updateAndCommit(session, user, new UpdateUser().setPassword("password2"), u -> {
    });
  }

  @Test
  public void not_associate_default_group_when_updating_user() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com"));
    GroupDto defaultGroup = createDefaultGroup();

    // Existing user, he has no group, and should not be associated to the default one
    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("ma2")), u -> {
      });

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

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("ma2")), u -> {
      });

    // Nothing as changed
    groups = dbClient.groupMembershipDao().selectGroupsByLogins(session, asList(DEFAULT_LOGIN));
    assertThat(groups.get(DEFAULT_LOGIN).stream().anyMatch(g -> g.equals(defaultGroup.getName()))).isTrue();
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_already_used() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@email.com").setScmAccounts(singletonList("ma")));
    db.users().insertUser(newLocalUser("john", "John", "john@email.com").setScmAccounts(singletonList("jo")));
    createDefaultGroup();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The scm account 'jo' is already used by user(s) : 'John (john)'");

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setName("Marius2")
      .setEmail("marius2@mail.com")
      .setPassword("password2")
      .setScmAccounts(asList("jo")), u -> {
      });
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_user_login() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.updateAndCommit(session, user, new UpdateUser().setScmAccounts(asList(DEFAULT_LOGIN)), u -> {
    });
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_existing_user_email() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.updateAndCommit(session, user, new UpdateUser().setScmAccounts(asList("marius@lesbronzes.fr")), u -> {
    });
  }

  @Test
  public void fail_to_update_user_when_scm_account_is_new_user_email() {
    UserDto user = db.users().insertUser(newLocalUser(DEFAULT_LOGIN, "Marius", "marius@lesbronzes.fr"));
    createDefaultGroup();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Login and email are automatically considered as SCM accounts");

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setEmail("marius@newmail.com")
      .setScmAccounts(asList("marius@newmail.com")), u -> {
      });
  }

  @Test
  public void fail_to_update_login_when_format_is_invalid() {
    UserDto user = db.users().insertUser();
    createDefaultGroup();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Use only letters, numbers, and .-_@ please.");

    underTest.updateAndCommit(session, user, new UpdateUser().setLogin("With space"), u -> {
    });
  }

  @Test
  public void fail_to_update_user_when_login_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setLogin("existing_login"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with login 'existing_login' already exists");

    underTest.updateAndCommit(session, user, new UpdateUser().setLogin(existingUser.getLogin()), u -> {
    });
  }

  @Test
  public void fail_to_update_user_when_external_id_and_external_provider_already_exists() {
    createDefaultGroup();
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    UserDto existingUser = db.users().insertUser(u -> u.setExternalId("existing_external_id").setExternalIdentityProvider("existing_external_provider"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A user with provider id 'existing_external_id' and identity provider 'existing_external_provider' already exists");

    underTest.updateAndCommit(session, user, new UpdateUser()
      .setExternalIdentity(new ExternalIdentity(existingUser.getExternalIdentityProvider(), existingUser.getExternalLogin(), existingUser.getExternalId())), u -> {
      });
  }

  private GroupDto createDefaultGroup() {
    return db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

}
