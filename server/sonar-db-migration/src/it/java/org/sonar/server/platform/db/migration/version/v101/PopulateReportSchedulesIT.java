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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateReportSchedulesIT {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateReportSchedules.class);

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final DataChange underTest = new PopulateReportSchedules(db.database());

  @Test
  public void execute_shouldPopulateFromPortfolioProperties() throws SQLException {
    insertPortfolio("uuid1");
    insertPortfolioProperty("uuid1", "1234");

    underTest.execute();

    assertThat(db.select("select * from report_schedules"))
      .extracting(m -> m.get("PORTFOLIO_UUID"), m -> m.get("BRANCH_UUID"), m -> m.get("LAST_SEND_TIME_IN_MS"))
      .containsOnly(tuple("uuid1", null, 1234L));
  }

  @Test
  public void execute_shouldPopulateFromBranchProperties() throws SQLException {
    insertBranch("uuid1");
    insertProjectBranchProperty("uuid1", "1234");

    underTest.execute();

    assertThat(db.select("select * from report_schedules"))
      .extracting(m -> m.get("PORTFOLIO_UUID"), m -> m.get("BRANCH_UUID"), m -> m.get("LAST_SEND_TIME_IN_MS"))
      .containsOnly(tuple(null, "uuid1", 1234L));
  }

  @Test
  public void execute_whenPropertyMatchesBothBranchAndPortfolio_shouldNotPopulate() throws SQLException {
    insertBranch("uuid1");
    insertPortfolio("uuid1");
    insertProjectBranchProperty("uuid1", "1234");

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from report_schedules")).isEmpty();
  }

  private void insertPortfolio(String uuid) {
    db.executeInsert("portfolios",
      "uuid", uuid,
      "kee", "kee_" + uuid,
      "name", "name_" + uuid,
      "root_uuid", uuid,
      "private", true,
      "selection_mode", "manual",
      "created_at", 1000,
      "updated_at", 1000
    );
  }

  private void insertProjectBranchProperty(String portfolioUuid, String value) {
    db.executeInsert("properties",
      "uuid", uuidFactory.create(),
      "prop_key", "sonar.governance.report.project.branch.lastSendTimeInMs",
      "is_empty", false,
      "text_value", value,
      "created_at", 1000,
      "entity_uuid", portfolioUuid
    );
  }

  private void insertPortfolioProperty(String portfolioUuid, String value) {
    db.executeInsert("properties",
      "uuid", uuidFactory.create(),
      "prop_key", "sonar.governance.report.lastSendTimeInMs",
      "is_empty", false,
      "text_value", value,
      "created_at", 1000,
      "entity_uuid", portfolioUuid
    );
  }

  private void insertBranch(String uuid) {
    db.executeInsert("project_branches",
      "uuid", uuid,
      "project_uuid", "project_" + uuid,
      "kee", "kee_" + uuid,
      "branch_type", "BRANCH",
      "created_at", 1000,
      "updated_at", 1000,
      "need_issue_sync", false,
      "is_main", true
    );
  }
}
