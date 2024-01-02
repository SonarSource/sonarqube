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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveReportPropertiesIT {

  private static final String SONAR_GOVERNANCE_REPORT_USER_NOTIFICATION = "sonar.governance.report.userNotification";
  private static final String SONAR_GOVERNANCE_REPORT_PROJECT_BRANCH_USER_NOTIFICATION = "sonar.governance.report.project.branch.userNotification";
  private static final String SONAR_GOVERNANCE_REPORT_LAST_SEND_TIME_IN_MS = "sonar.governance.report.lastSendTimeInMs";
  private static final String SONAR_GOVERNANCE_REPORT_PROJECT_BRANCH_LAST_SEND_TIME_IN_MS = "sonar.governance.report.project.branch.lastSendTimeInMs";
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveReportProperties.class);

  private final DataChange underTest = new RemoveReportProperties(db.database());

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Test
  public void execute_shouldRemoveRelevantPropertiesFromTable() throws SQLException {
    insertProperty( "branch_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_USER_NOTIFICATION, "true");
    insertProperty( "portfolio_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_PROJECT_BRANCH_USER_NOTIFICATION, "true");
    insertProperty( "branch_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_LAST_SEND_TIME_IN_MS, "12");
    insertProperty( "portfolio_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_PROJECT_BRANCH_LAST_SEND_TIME_IN_MS, "123");
    insertProperty( "portfolio_uuid", "user_uuid", "sonar.other.property", "123");

    underTest.execute();

    assertThat(db.select("select * from properties")).hasSize(1)
      .extracting(r->r.get("PROP_KEY")).containsExactly("sonar.other.property");
  }

  @Test
  public void execute_shouldBeIdempotent() throws SQLException {
    insertProperty( "branch_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_USER_NOTIFICATION, "true");
    insertProperty( "portfolio_uuid", "user_uuid", SONAR_GOVERNANCE_REPORT_PROJECT_BRANCH_USER_NOTIFICATION, "true");

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from properties")).isEmpty();
  }

  private void insertProperty(String componentUuid, String userUuid, String propertyKey, String value) {
    db.executeInsert("properties",
      "uuid", uuidFactory.create(),
      "prop_key", propertyKey,
      "is_empty", false,
      "text_value", value,
      "created_at", 1000,
      "entity_uuid", componentUuid,
      "user_uuid", userUuid
    );
  }
}
