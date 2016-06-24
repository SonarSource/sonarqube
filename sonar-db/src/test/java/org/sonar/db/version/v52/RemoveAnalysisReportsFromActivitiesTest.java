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
package org.sonar.db.version.v52;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveAnalysisReportsFromActivitiesTest {
  static final String TABLE_ACTIVITIES = "activities";

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, RemoveAnalysisReportsFromActivitiesTest.class, "schema.sql");

  MigrationStep underTest = new RemoveAnalysisReportsFromActivities(db.database());

  @Test
  public void test() throws Exception {
    db.executeInsert(TABLE_ACTIVITIES, "log_type", "ANALYSIS_REPORT", "log_key", "1");
    db.executeInsert(TABLE_ACTIVITIES, "log_type", "ANALYSIS_REPORT", "log_key", "2");
    db.executeInsert(TABLE_ACTIVITIES, "log_type", "PROFILE_CHANGE", "log_key", "3");

    underTest.execute();

    assertThat(db.countRowsOfTable("activities")).isEqualTo(1);
  }
}
