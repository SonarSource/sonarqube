/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.platform.db.migration.version.v202501.UpdateDefaultAdminPasswordIfInvalidHashMechanism.ADMIN_DEFAULT_PASSWORD;
import static org.sonar.server.platform.db.migration.version.v202501.UpdateDefaultAdminPasswordIfInvalidHashMechanism.ADMIN_SALT;


class UpdateDefaultAdminPasswordIfInvalidHashMechanismIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(LogMessageIfInvalidHashMechanismUsed.class);

  private final DataChange underTest = new UpdateDefaultAdminPasswordIfInvalidHashMechanism(db.database());

  @BeforeEach
  void setUp() {
    deleteUsers();
  }

  @Test
  void execute_whenAdminOnPbkdf2_doesNothing() throws Exception {
    addAdmin("PBKDF2", true);
    underTest.execute();

    assertThat(db.select("select * from users where login = 'admin'"))
      .hasSize(1)
      .extracting(t -> t.get("hash_method"), t -> t.get("salt"), t -> t.get("crypted_password"))
      .containsOnly(tuple("PBKDF2", "", "$2a$12$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi"));
  }

  @Test
  void execute_whenAdminStillOnBcrypt_update() throws Exception {
    addAdmin("BCRYPT", true);

    underTest.execute();

    assertThat(db.select("select * from users where login = 'admin'"))
      .hasSize(1)
      .extracting(t -> t.get("hash_method"), t -> t.get("salt"), t -> t.get("crypted_password"))
      .containsOnly(tuple("PBKDF2", ADMIN_SALT, ADMIN_DEFAULT_PASSWORD));
  }

  private void addAdmin(String hashAlgorithm, boolean active) {
    Map<String, Object> map = new HashMap<>();
    String login = "admin";
    map.put("uuid", login);
    map.put("login", login);
    map.put("name", "name");
    map.put("email", "email");
    map.put("external_id", login);
    map.put("external_login", login);
    map.put("external_identity_provider", "sonarqube");
    map.put("user_local", true);
    map.put("crypted_password", "$2a$12$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi");
    map.put("salt", "");
    map.put("active", active);
    map.put("hash_method", hashAlgorithm);
    map.put("reset_password", true);
    map.put("created_at", 1_000_000_000_000L);
    map.put("updated_at", 1_000_000_000_000L);

    db.executeInsert("users", map);
  }

  private void deleteUsers(){
    db.executeUpdateSql("delete from users");
  }

}
