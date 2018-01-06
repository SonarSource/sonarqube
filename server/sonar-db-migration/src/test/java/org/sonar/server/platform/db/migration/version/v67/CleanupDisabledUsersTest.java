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
package org.sonar.server.platform.db.migration.version.v67;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanupDisabledUsersTest {

  private final static long PAST = 100_000_000_000L;
  private final static long NOW = 500_000_000_000L;
  private final static Random RANDOM = new Random();
  private final static String SELECT_USERS = "select name, scm_accounts, user_local, login, crypted_password, salt, email, external_identity, external_identity_provider, active, is_root, onboarded, created_at, updated_at from users";

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanupDisabledUsersTest.class, "users.sql");

  private CleanupDisabledUsers underTest = new CleanupDisabledUsers(db.database(), system2);

  @Test
  public void do_nothing_when_no_data() throws SQLException {
    assertThat(db.countRowsOfTable("USERS")).isEqualTo(0);
    underTest.execute();
    assertThat(db.countRowsOfTable("USERS")).isEqualTo(0);
  }

  @Test
  public void execute_must_update_database() throws SQLException {
    final List<User> users = generateAndInsertUsers();

    underTest.execute();

    assertDatabaseContainsExactly(applyCleanupOnUsers(users));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    final List<User> users = generateAndInsertUsers();

    underTest.execute();
    underTest.execute();

    assertDatabaseContainsExactly(applyCleanupOnUsers(users));
  }

  private List<User> generateAndInsertUsers() {
    List<User> users = ImmutableList.of(
      new User("user1", null, null, null, null, null, false),
      new User("user2", randomAlphanumeric(10), null, null, null, null, false),
      new User("user3", null, randomAlphanumeric(10), null, null, null, false),
      new User("user4", null, null, randomAlphanumeric(10), null, null, false),
      new User("user5", null, null, null, randomAlphanumeric(10), null, false),
      new User("user6", null, null, null, null, randomAlphanumeric(10), false),
      new User("user7", randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), false),
      new User("user8", randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), true),
      new User("user9", randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), randomAlphanumeric(10), true)
    );

    users.forEach(User::insert);
    return users;
  }

  private List<User> applyCleanupOnUsers(List<User> users) {
    return users.stream().map(
      u -> {
        User cleanedupUser = u.clone();

        // If a user is active => no change
        if (cleanedupUser.active) {
          return cleanedupUser;
        }

        // updated_at field will only be updated if there is a real change so if at least one field is not null
        if (cleanedupUser.cryptedPassword != null || cleanedupUser.salt != null || cleanedupUser.email != null ||
          cleanedupUser.externalIdentityProvider != null || cleanedupUser.externalIdentity != null) {
          cleanedupUser.updatedAt = NOW;
        }

        // Cleanup fields, all those fields must be null
        cleanedupUser.cryptedPassword = null;
        cleanedupUser.salt = null;
        cleanedupUser.email = null;
        cleanedupUser.externalIdentityProvider = null;
        cleanedupUser.externalIdentity = null;

        return cleanedupUser;
      }).collect(Collectors.toList());
  }

  private void assertDatabaseContainsExactly(List<User> expectedUsers) {
    assertThat(db.select(SELECT_USERS)).isEqualTo(
      expectedUsers.stream().map(User::toMap).collect(Collectors.toList())
    );
  }

  private class User {
    private String login;
    private String cryptedPassword;
    private String salt;
    private String email;
    private String externalIdentity;
    private String externalIdentityProvider;
    private String name;
    private String scmAccounts;
    private boolean userLocal;
    private boolean active;
    private boolean isRoot;
    private boolean onBoarded;
    private long updatedAt;
    private long createdAt;

    private User(String login, @Nullable String cryptedPassword, @Nullable String salt, @Nullable String email,
      @Nullable String externalIdentity, @Nullable String externalIdentityProvider, boolean active) {
      this.login = login;
      this.cryptedPassword = cryptedPassword;
      this.salt = salt;
      this.email = email;
      this.externalIdentity = externalIdentity;
      this.externalIdentityProvider = externalIdentityProvider;
      this.active = active;
      this.isRoot = RANDOM.nextBoolean();
      this.onBoarded = RANDOM.nextBoolean();
      this.userLocal = RANDOM.nextBoolean();
      this.scmAccounts = randomAlphanumeric(1500);
      this.name = randomAlphanumeric(200);
      this.updatedAt = PAST;
      this.createdAt = PAST;
    }

    private void insert() {
      db.executeInsert("USERS", toMap());
    }

    private Map<String, Object> toMap() {
      HashMap<String, Object> map = new HashMap<>();
      map.put("LOGIN", login);
      map.put("IS_ROOT", isRoot);
      map.put("ONBOARDED", onBoarded);
      map.put("ACTIVE", active);
      map.put("CREATED_AT", createdAt);
      map.put("UPDATED_AT", updatedAt);
      map.put("CRYPTED_PASSWORD", cryptedPassword);
      map.put("SALT", salt);
      map.put("EMAIL", email);
      map.put("EXTERNAL_IDENTITY", externalIdentity);
      map.put("EXTERNAL_IDENTITY_PROVIDER", externalIdentityProvider);
      map.put("NAME", name);
      map.put("SCM_ACCOUNTS", scmAccounts);
      map.put("USER_LOCAL", userLocal);

      return Collections.unmodifiableMap(map);
    }

    @Override
    public User clone() {
      User user = new User(this.login, this.cryptedPassword, this.salt, this.email, this.externalIdentity, this.externalIdentityProvider, this.active);

      user.name = this.name;
      user.scmAccounts = this.scmAccounts;
      user.userLocal = this.userLocal;
      user.onBoarded = this.onBoarded;
      user.isRoot = this.isRoot;

      return user;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof User)) {
        return false;
      }
      User user = (User) o;
      return active == user.active &&
        isRoot == user.isRoot &&
        onBoarded == user.onBoarded &&
        Objects.equals(login, user.login) &&
        Objects.equals(cryptedPassword, user.cryptedPassword) &&
        Objects.equals(salt, user.salt) &&
        Objects.equals(email, user.email) &&
        Objects.equals(externalIdentity, user.externalIdentity) &&
        Objects.equals(externalIdentityProvider, user.externalIdentityProvider);
    }

    @Override
    public int hashCode() {
      return Objects.hash(login, cryptedPassword, salt, email, externalIdentity, externalIdentityProvider, active, isRoot, onBoarded);
    }
  }
}
