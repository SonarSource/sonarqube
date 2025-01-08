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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class RemoveOrphanUserTokensIT {
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveOrphanUserTokens.class);

  private final DataChange underTest = new RemoveOrphanUserTokens(db.database());

  @Test
  void migration_deletes_orphan_tokens() throws SQLException {
    String project1Uuid = insertProject("project1");

    String token1Uuid = insertUserToken("project1");
    String token2Uuid = insertUserToken("orphan");
    String token3Uuid = insertUserToken(null);

    underTest.execute();
    assertThat(db.select("select * from user_tokens"))
      .extracting(r -> r.get("UUID"))
      .containsOnly(token1Uuid, token3Uuid);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    String project1Uuid = insertProject("project1");

    String token1Uuid = insertUserToken("project1");
    String token2Uuid = insertUserToken("orphan");

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from user_tokens"))
      .extracting(r -> r.get("UUID"))
      .containsOnly(token1Uuid);
  }

  private String insertUserToken( @Nullable String projectKey) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("USER_UUID", "user" + uuid);
    map.put("NAME", "name" + uuid);
    map.put("TOKEN_HASH", "token" + uuid);
    map.put("CREATED_AT", 1);
    map.put("PROJECT_KEY", projectKey);
    map.put("TYPE", "PROJECT_ANALYSIS_TOKEN");

    db.executeInsert("user_tokens", map);
    return uuid;
  }

  private String insertProject(String projectKey) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("KEE", projectKey);
    map.put("QUALIFIER", "TRK");
    map.put("PRIVATE", true);
    map.put("UPDATED_AT", System.currentTimeMillis());
    db.executeInsert("projects", map);
    return uuid;
  }
}
