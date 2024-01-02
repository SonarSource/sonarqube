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
package org.sonar.server.platform.db.migration.version.v97;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RemoveBranchInformationFromComponentsKeyTest {

  private static final String FILE_KEY_PREFIX = "org.sonarsource.sonarqube:sonarqube-private:server/sonar-ce-task-projectanalysis/src/test/java/org/sonar/ce/task/projectanalysis/step/PersistAdHocRulesStepTest.java";
  private static final String DIR_KEY_PREFIX = "org.sonarsource.sonarqube:sonarqube-private:server/sonar-ce-task-projectanalysis/src/test/java/org";
  private static final String PROJ_KEY_PREFIX = "org.sonarsource.sonarqube:sonarqube-private";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(RemoveBranchInformationFromComponentsKeyTest.class, "schema.sql");

  private DataChange underTest;
  private MigrationEsClient migrationEsClient;

  @Before
  public void before() {
    migrationEsClient = mock(MigrationEsClient.class);
    underTest = new RemoveBranchInformationFromComponentsKey(db.database(), migrationEsClient);
  }

  @Test
  public void migration_should_update_component_keys_from_branch_or_pull_request() throws SQLException {
    insertComponents(db, "uuid1", FILE_KEY_PREFIX, null);
    insertComponents(db, "uuid2", FILE_KEY_PREFIX + ":BRANCH:branch-9.4", "main_branch");
    insertComponents(db, "uuid3", FILE_KEY_PREFIX + ":PULL_REQUEST:143", "main_branch");

    underTest.execute();

    Map<String, String> componentKeyByUuid = retrieveComponentKeyByUuid(db);
    assertThat(componentKeyByUuid)
      .containsEntry("uuid1", FILE_KEY_PREFIX)
      .containsEntry("uuid2", FILE_KEY_PREFIX)
      .containsEntry("uuid3", FILE_KEY_PREFIX);

    verify(migrationEsClient).deleteIndexes("components");

  }

  @Test
  public void migration_should_update_component_keys_from_file_proj_and_dir() throws SQLException {
    insertComponents(db, "uuid1", FILE_KEY_PREFIX + ":PULL_REQUEST:6637", "main_branch");
    insertComponents(db, "uuid2", DIR_KEY_PREFIX + ":BRANCH:this:is:a:branch", "main_branch");
    insertComponents(db, "uuid3", PROJ_KEY_PREFIX + ":PULL_REQUEST:143", "main_branch");

    underTest.execute();

    Map<String, String> componentKeyByUuid = retrieveComponentKeyByUuid(db);
    assertThat(componentKeyByUuid)
      .containsEntry("uuid1", FILE_KEY_PREFIX)
      .containsEntry("uuid2", DIR_KEY_PREFIX)
      .containsEntry("uuid3", PROJ_KEY_PREFIX);

    verify(migrationEsClient).deleteIndexes("components");

  }

  @Test
  public void migration_should_reentrant() throws SQLException {

    insertComponents(db, "uuid1", FILE_KEY_PREFIX + ":BRANCH:branch-9.4", "main_branch");

    underTest.execute();

    underTest.execute();

    Map<String, String> componentKeyByUuid = retrieveComponentKeyByUuid(db);
    assertThat(componentKeyByUuid).containsEntry("uuid1", FILE_KEY_PREFIX);
  }

  private static Map<String, String> retrieveComponentKeyByUuid(CoreDbTester db) {
    return db.select("select uuid, kee from components").stream()
      .collect(Collectors.toMap(e -> (String) e.get("UUID"), e -> (String) e.get("KEE")));
  }

  private static void insertComponents(CoreDbTester db, String uuid, String key, @Nullable String mainBranchUuid) {
    db.executeInsert("components",
      "enabled", true,
      "uuid", uuid,
      "kee", key,
      "branch_uuid", "branch_uuid",
      "root_uuid", "root_uuid",
      "uuid_path", "uuid_path",
      "private", false,
      "main_branch_project_uuid", mainBranchUuid);
  }
}
