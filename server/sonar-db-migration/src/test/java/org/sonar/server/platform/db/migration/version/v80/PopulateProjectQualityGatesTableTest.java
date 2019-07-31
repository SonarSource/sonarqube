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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;


public class PopulateProjectQualityGatesTableTest {
  private static final String PROJECTS_TABLE_NAME = "projects";
  private static final String QUALITY_GATES_TABLE_NAME = "quality_gates";
  private static final String PROPERTIES_TABLE_NAME = "properties";
  private static final int NUMBER_OF_PROJECTS_TO_INSERT = 5;

  private final Random random = new Random();

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateProjectQualityGatesTableTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateProjectQualityGatesTable underTest = new PopulateProjectQualityGatesTable(dbTester.database());

  @Test
  public void copy_quality_gates_properties_to_project_qgate_table() throws SQLException {
    long firstQualityGateId = insertQualityGate("qg1");
    long secondQualityGateId = insertQualityGate("qg2");

    for (long i = 1; i <= NUMBER_OF_PROJECTS_TO_INSERT; i++) {
      long projectId = insertComponent("p" + i);
      long qualityGateId = random.nextBoolean() ? firstQualityGateId : secondQualityGateId;
      insertQualityGateProperty(projectId, qualityGateId);
    }

    underTest.execute();

    List<ProjectQualityGate> qualityGates = getQualityGates();
    Assert.assertEquals(NUMBER_OF_PROJECTS_TO_INSERT, qualityGates.size());

    //must not delete properties
    int propertiesCount = dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME);
    Assert.assertEquals(5, propertiesCount);

    //should not fail if executed twice
    underTest.execute();
  }

  private long insertQualityGate(String qualityGateUuid) {
    dbTester.executeInsert(
      QUALITY_GATES_TABLE_NAME,
      "UUID", qualityGateUuid,
      "NAME", "name_" + qualityGateUuid,
      "IS_BUILT_IN", valueOf(true)
    );
    return (long) dbTester.selectFirst("select id as \"ID\" from quality_gates where uuid='" + qualityGateUuid + "'").get("ID");
  }

  private long insertComponent(String uuid) {
    dbTester.executeInsert(
      PROJECTS_TABLE_NAME,
      "ORGANIZATION_UUID", "org_" + uuid,
      "SCOPE", "PRJ",
      "QUALIFIER", "TRK",
      "UUID", uuid,
      "UUID_PATH", "path_" + uuid,
      "ROOT_UUID", "root_" + uuid,
      "PROJECT_UUID", uuid,
      "PRIVATE", valueOf(false));
    return (long) dbTester.selectFirst("select id as \"ID\" from projects where uuid='" + uuid + "'").get("ID");
  }

  private void insertQualityGateProperty(Long projectId, Long qualityGateId) {
    dbTester.executeInsert(PROPERTIES_TABLE_NAME,
      "prop_key", "sonar.qualitygate",
      "resource_id", projectId,
      "is_empty", false,
      "text_value", Long.toString(qualityGateId),
      "created_at", Instant.now().toEpochMilli());
  }

  private List<ProjectQualityGate> getQualityGates() {
    return dbTester.select("select pqg.project_uuid, pqg.quality_gate_uuid from project_qgates pqg " +
      "join projects p on pqg.project_uuid = p.uuid " +
      "join quality_gates qg on pqg.quality_gate_uuid = qg.uuid")
      .stream()
      .map(row -> {
        String projectUuid = String.valueOf(row.get("PROJECT_UUID"));
        String qualityGateUuid = String.valueOf(row.get("QUALITY_GATE_UUID"));
        return new ProjectQualityGate(projectUuid, qualityGateUuid);
      })
      .collect(Collectors.toList());
  }

  private static class ProjectQualityGate {
    final String projectUuid;
    final String qualityGateUuid;

    private ProjectQualityGate(String projectUuid, String qualityGateUuid) {
      this.projectUuid = projectUuid;
      this.qualityGateUuid = qualityGateUuid;
    }
  }
}
