/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.db.version.v54;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrateQualityGatesConditionsTest {
  static final String NOW = "1919-12-24";
  private static final String MSG_WARNING_QG_CONDITIONS_UPDATED = "The following Quality Gates have been updated to compare with the leak period: qg-1, qg-2.";
  final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, MigrateQualityGatesConditionsTest.class, "schema.sql");
  @Rule
  public LogTester log = new LogTester();

  MigrationStep migration;

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(DateUtils.parseDate(NOW).getTime());
    migration = new MigrateQualityGatesConditions(db.database(), system2);
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate-result.xml", "quality_gates", "quality_gate_conditions");
    assertThat(log.logs(LoggerLevel.WARN)).contains(MSG_WARNING_QG_CONDITIONS_UPDATED);
  }

  @Test
  public void nothing_to_do_on_already_migrated_data() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate-result.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate-result.xml", "quality_gates", "quality_gate_conditions");
    assertThat(log.logs(LoggerLevel.WARN)).doesNotContain(MSG_WARNING_QG_CONDITIONS_UPDATED);
  }
}
