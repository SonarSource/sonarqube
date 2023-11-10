/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PopulateProjectUuidInUserTokensTest {
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateProjectUuidInUserTokens.class);

  private final DataChange underTest = new PopulateProjectUuidInUserTokens(db.database());

  @Test
  public void migration_populates_project_uuid_for_tokens() throws SQLException {
    String project1Uuid = insertProject("project1");
    String project2Uuid = insertProject("project2");

    String token1Uuid = insertUserToken("project1");
    String token2Uuid = insertUserToken("project1");
    String token3Uuid = insertUserToken("project2");
    String token4Uuid = insertUserToken(null);

    underTest.execute();
    assertThat(db.select("select * from user_tokens"))
      .extracting(r -> r.get("UUID"), r -> r.get("PROJECT_UUID"))
      .containsOnly(
        tuple(token1Uuid, project1Uuid),
        tuple(token2Uuid, project1Uuid),
        tuple(token3Uuid, project2Uuid),
        tuple(token4Uuid, null));
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String project1Uuid = insertProject("project1");
    String project2Uuid = insertProject("project2");

    String token1Uuid = insertUserToken("project1");
    String token2Uuid = insertUserToken("project1");
    String token3Uuid = insertUserToken("project2");
    String token4Uuid = insertUserToken(null);

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from user_tokens"))
      .extracting(r -> r.get("UUID"), r -> r.get("PROJECT_UUID"))
      .containsOnly(
        tuple(token1Uuid, project1Uuid),
        tuple(token2Uuid, project1Uuid),
        tuple(token3Uuid, project2Uuid),
        tuple(token4Uuid, null));
  }

  private String insertUserToken(@Nullable String projectKey) {
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
