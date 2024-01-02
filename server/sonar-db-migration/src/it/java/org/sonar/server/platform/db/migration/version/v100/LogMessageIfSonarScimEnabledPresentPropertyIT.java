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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v100.LogMessageIfSonarScimEnabledPresentProperty.SONAR_SCIM_ENABLED;

public class LogMessageIfSonarScimEnabledPresentPropertyIT {

  @Rule
  public LogTester logger = new LogTester();

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(LogMessageIfSonarScimEnabledPresentProperty.class);
  private final DataChange underTest = new LogMessageIfSonarScimEnabledPresentProperty(db.database());

  @Before
  public void before() {
    logger.clear();
  }

  @Test
  public void migration_should_log_message_when_scim_property() throws SQLException {
    db.executeInsert("properties ",
      "prop_key", "sonar.scim.enabled",
      "is_empty", false,
      "text_value", "true",
      "created_at", 100_000L,
      "uuid", "some-random-uuid"
    );

    underTest.execute();

    assertThat(logger.logs(Level.WARN))
      .hasSize(1)
      .containsExactly("'" + SONAR_SCIM_ENABLED + "' property is defined but not read anymore. Please read the upgrade notes" +
        " for the instruction to upgrade. User provisioning is deactivated until reactivated from the SonarQube" +
        " Administration Interface (\"General->Authentication\"). "
        + "See documentation: https://docs.sonarsource.com/sonarqube/10.1/instance-administration/authentication/saml/scim/overview/");
  }

  @Test
  public void migration_should_not_log_if_no_scim_property() throws SQLException {

    underTest.execute();

    assertThat(logger.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    db.executeInsert("properties ",
      "prop_key", "sonar.scim.enabled",
      "is_empty", false,
      "text_value", "true",
      "created_at", 100_000L,
      "uuid", "some-random-uuid"
    );

    underTest.execute();
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).hasSize(2);
  }
}
