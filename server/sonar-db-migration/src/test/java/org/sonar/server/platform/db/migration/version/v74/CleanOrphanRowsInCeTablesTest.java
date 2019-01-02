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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanOrphanRowsInCeTablesTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CleanOrphanRowsInCeTablesTest.class, "ce_tables.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();
  private MapSettings settings = new MapSettings();
  private CleanOrphanRowsInCeTables underTest = new CleanOrphanRowsInCeTables(db.database(), settings.asConfig());

  @Test
  public void execute_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_deletes_rows_of_ce_activity_and_child_tables_which_have_main_component_equals_component_and_at_least_one_characteristic() throws SQLException {
    String projectUuid = randomAlphabetic(10);
    String branchUuid = randomAlphanumeric(11);
    String mainBranchTask = insertCeActivity(projectUuid, projectUuid, 0);
    String existingBranchTask = insertCeActivity(projectUuid, branchUuid, 1 + new Random().nextInt(10));
    String nonExistingBranchTask = insertCeActivity(projectUuid, projectUuid, 1 + new Random().nextInt(10));
    String missingTask = "missing_task";
    Stream.of(mainBranchTask, existingBranchTask, nonExistingBranchTask, missingTask)
      .forEach(taskUuid ->  {
        insertCeTaskInput(taskUuid);
        insertCeScannerContext(taskUuid);
      });
    insertCeTaskCharacteristics(missingTask);

    underTest.execute();

    assertThat(uuidsOf("ce_activity")).containsOnly(mainBranchTask, existingBranchTask);
    assertThat(taskUuidsOf("ce_task_input")).containsOnly(mainBranchTask, existingBranchTask, missingTask);
    assertThat(taskUuidsOf("ce_scanner_context")).containsOnly(mainBranchTask, existingBranchTask, missingTask);
    assertThat(taskUuidsOf("ce_task_characteristics")).containsOnly(existingBranchTask, missingTask);
  }

  @Test
  public void execute_has_no_effect_on_SonarCloud() throws SQLException {
    String projectUuid = randomAlphabetic(10);
    String branchUuid = randomAlphanumeric(11);
    String mainBranchTask = insertCeActivity(projectUuid, projectUuid, 0);
    String existingBranchTask = insertCeActivity(projectUuid, branchUuid, 1 + new Random().nextInt(10));
    String nonExistingBranchTask = insertCeActivity(projectUuid, projectUuid, 1 + new Random().nextInt(10));
    String missingTask = "missing_task";
    Stream.of(mainBranchTask, existingBranchTask, nonExistingBranchTask, missingTask)
      .forEach(taskUuid ->  {
        insertCeTaskInput(taskUuid);
        insertCeScannerContext(taskUuid);
      });
    insertCeTaskCharacteristics(missingTask);
    settings.setProperty("sonar.sonarcloud.enabled", true);

    underTest.execute();

    assertThat(uuidsOf("ce_activity")).containsOnly(mainBranchTask, existingBranchTask, nonExistingBranchTask);
    assertThat(taskUuidsOf("ce_task_input")).containsOnly(mainBranchTask, existingBranchTask, nonExistingBranchTask, missingTask);
    assertThat(taskUuidsOf("ce_scanner_context")).containsOnly(mainBranchTask, existingBranchTask, nonExistingBranchTask, missingTask);
    assertThat(taskUuidsOf("ce_task_characteristics")).containsOnly(existingBranchTask, nonExistingBranchTask, missingTask);
  }

  @Test
  public void execute_is_reentrant() throws SQLException {
    String projectUuid = randomAlphabetic(10);
    String branchUuid = randomAlphanumeric(11);
    String mainBranchTask = insertCeActivity(projectUuid, projectUuid, 0);
    String existingBranchTask = insertCeActivity(projectUuid, branchUuid, 1 + new Random().nextInt(10));
    String nonExistingBranchTask = insertCeActivity(projectUuid, projectUuid, 1 + new Random().nextInt(10));
    Stream.of(mainBranchTask, existingBranchTask, nonExistingBranchTask)
      .forEach(taskUuid ->  {
        insertCeTaskInput(taskUuid);
        insertCeScannerContext(taskUuid);
      });
    underTest.execute();

    underTest.execute();
  }

  private Stream<String> taskUuidsOf(String tableName) {
    return db.select("select distinct task_uuid as \"TASK_UUID\" from " + tableName)
      .stream()
      .map(r -> (String) r.get("TASK_UUID"));
  }

  private Stream<String> uuidsOf(String tableName) {
    return db.select("select uuid as \"UUID\" from " + tableName)
      .stream()
      .map(r -> (String) r.get("UUID"));
  }

  private String insertCeActivity(@Nullable String componentUuid, @Nullable String mainComponentUuid, int numberOfCharacteristics) {
    String uuid = UuidFactoryFast.getInstance().create();
    db.executeInsert(
      "ce_activity",
      "UUID", uuid,
      "COMPONENT_UUID", componentUuid,
      "MAIN_COMPONENT_UUID", mainComponentUuid,
      "TASK_TYPE", randomAlphanumeric(10),
      "STATUS", randomAlphanumeric(10),
      "IS_LAST", random.nextBoolean(),
      "IS_LAST_KEY", randomAlphanumeric(15),
      "EXECUTION_COUNT", random.nextInt(99),
      "SUBMITTED_AT", random.nextInt(95654354),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));

    IntStream.range(0, numberOfCharacteristics)
      .forEach(i -> insertCeTaskCharacteristic(uuid, i));

    return uuid;
  }

  private void insertCeTaskInput(String taskUuid) {
    db.executeInsert(
      "ce_task_input",
      "TASK_UUID", taskUuid,
      "INPUT_DATA", randomAlphanumeric(123).getBytes(),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
  }

  private void insertCeScannerContext(String taskUuid) {
    db.executeInsert(
      "ce_scanner_context",
      "TASK_UUID", taskUuid,
      "CONTEXT_DATA", randomAlphanumeric(123).getBytes(),
      "CREATED_AT", random.nextInt(95654354),
      "UPDATED_AT", random.nextInt(95654354));
  }

  private void insertCeTaskCharacteristics(String taskUuid) {
    for (int i = 0; i < 1 + random.nextInt(3); i++) {
      insertCeTaskCharacteristic(taskUuid, i);
    }
  }

  private void insertCeTaskCharacteristic(String taskUuid, int i) {
    String uuid = UuidFactoryFast.getInstance().create();
    db.executeInsert(
      "ce_task_characteristics",
      "UUID", uuid,
      "TASK_UUID", taskUuid,
      "KEE", "kee_" + uuid + i,
      "TEXT_VALUE", randomAlphanumeric(18));
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    // FIXME
    underTest.execute();

    underTest.execute();
  }

}
