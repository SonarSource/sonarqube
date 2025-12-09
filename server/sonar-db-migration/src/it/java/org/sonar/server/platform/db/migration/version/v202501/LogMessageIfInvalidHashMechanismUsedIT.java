/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202501;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;


class LogMessageIfInvalidHashMechanismUsedIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(LogMessageIfInvalidHashMechanismUsed.class);

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final DataChange underTest = new LogMessageIfInvalidHashMechanismUsed(db.database());

  @Test
  void execute_whenNoActiveUsersOnBcrypt_doesNothing() throws Exception {
    addUser("user1", "PBKDF2", true);
    addUser("user2", "BCRYPT", false);
    underTest.execute();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void execute_whenActiveUsersAreStillOnBcrypt_warns() throws Exception {
    addUser("user1", "BCRYPT", true);
    addUser("user2", "BCRYPT", false);
    addUser("user3", "BCRYPT", true);
    addUser("user4", "PBKDF2", true);

    underTest.execute();
    assertThat(logTester.logs()).containsExactly("The following active users are still relying on passwords using the unsupported hash mechanism (BCRYPT). Their passwords should" +
      " be manually updated by an administrator: user1, user3");
  }

  private void addUser(String login, String hashAlgorithm, boolean active) {
    Map<String, Object> map = new HashMap<>();
    map.put("uuid", login);
    map.put("login", login);
    map.put("name", "name");
    map.put("email", "email");
    map.put("external_id", login);
    map.put("external_login", login);
    map.put("external_identity_provider", "sonarqube");
    map.put("user_local", true);
    map.put("crypted_password", "password");
    map.put("salt", "");
    map.put("active", active);
    map.put("hash_method", hashAlgorithm);
    map.put("reset_password", false);
    map.put("created_at", 1_000_000_000_000L);
    map.put("updated_at", 1_000_000_000_000L);

    db.executeInsert("users", map);
  }
}
