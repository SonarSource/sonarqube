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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateNclocForForProjectsTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateNclocForForProjects.class);

  private final DataChange underTest = new PopulateNclocForForProjects(db.database());

  @Test
  public void migration_populates_ncloc_for_projects() throws SQLException {
    Map<String, Long> expectedNclocByProjectUuid = populateData();
    underTest.execute();
    verifyNclocCorrectlyPopulatedForProjects(expectedNclocByProjectUuid);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    Map<String, Long> expectedNclocByProjectUuid = populateData();
    underTest.execute();
    // re-entrant
    underTest.execute();
    verifyNclocCorrectlyPopulatedForProjects(expectedNclocByProjectUuid);
  }

  private Map<String, Long> populateData() {
    String nclocMetricUuid = insertMetric("ncloc");

    String projectUuid1 = insertProject();
    String project1Branch1 = insertProjectBranch(projectUuid1);
    String project1Branch2 = insertProjectBranch(projectUuid1);

    long project1maxNcloc = 100;
    insertLiveMeasure(nclocMetricUuid, projectUuid1, project1Branch1, 80L);
    insertLiveMeasure(nclocMetricUuid, projectUuid1, project1Branch2, project1maxNcloc);

    String otherMetricUuid = insertMetric("other");
    insertLiveMeasure(otherMetricUuid, projectUuid1, project1Branch1, 5000L);
    insertLiveMeasure(otherMetricUuid, projectUuid1, project1Branch2, 6000L);

    String projectUuid2 = insertProject();
    String project2Branch1 = insertProjectBranch(projectUuid2);
    String project2Branch2 = insertProjectBranch(projectUuid2);
    String project2Branch3 = insertProjectBranch(projectUuid2);

    long project2maxNcloc = 60;
    insertLiveMeasure(nclocMetricUuid, projectUuid2, project2Branch1, 20L);
    insertLiveMeasure(nclocMetricUuid, projectUuid2, project2Branch2, 50L);
    insertLiveMeasure(nclocMetricUuid, projectUuid2, project2Branch3, project2maxNcloc);

    return Map.of(projectUuid1, project1maxNcloc, projectUuid2, project2maxNcloc);
  }

  private void verifyNclocCorrectlyPopulatedForProjects(Map<String, Long> expectedNclocByProjectUuid) {
    for (Map.Entry<String, Long> entry : expectedNclocByProjectUuid.entrySet()) {
      String query = String.format("select ncloc from projects where uuid='%s'", entry.getKey());
      Long nclocFromProject = (Long) db.selectFirst(query).get("NCLOC");
      assertThat(nclocFromProject).isEqualTo(entry.getValue());
    }
  }

  private String insertMetric(String name) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("NAME", name);
    db.executeInsert("metrics", map);
    return uuid;
  }

  private String insertProject() {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("KEE", randomAlphabetic(20));
    map.put("QUALIFIER", "TRK");
    map.put("PRIVATE", true);
    map.put("UPDATED_AT", System.currentTimeMillis());
    db.executeInsert("projects", map);
    return uuid;
  }

  private String insertProjectBranch(String projectUuid) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("PROJECT_UUID", projectUuid);
    map.put("KEE", randomAlphabetic(20));
    map.put("BRANCH_TYPE", "PULL_REQUEST");
    map.put("UPDATED_AT", System.currentTimeMillis());
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("NEED_ISSUE_SYNC", false);
    db.executeInsert("project_branches", map);
    return uuid;
  }

  private void insertLiveMeasure(String metricUuid, String projectUuid, String componentUuid, Long value) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("PROJECT_UUID", projectUuid);
    map.put("COMPONENT_UUID", componentUuid);
    map.put("METRIC_UUID", metricUuid);
    map.put("VALUE", value);
    map.put("UPDATED_AT", System.currentTimeMillis());
    map.put("CREATED_AT", System.currentTimeMillis());
    db.executeInsert("live_measures", map);
  }

}
