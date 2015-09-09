/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.db.activity.ActivityDto;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveAnalysisReportsFromActivitiesTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  MigrationStep underTest = new RemoveAnalysisReportsFromActivities(db.database());

  @Test
  public void test() throws Exception {
    db.getDbClient().activityDao().insert(new ActivityDto().setType("ANALYSIS_REPORT").setKey("1"));
    db.getDbClient().activityDao().insert(new ActivityDto().setType("ANALYSIS_REPORT").setKey("2"));
    db.getDbClient().activityDao().insert(new ActivityDto().setType("PROFILE_CHANGE").setKey("3"));

    underTest.execute();

    assertThat(db.countRowsOfTable("ACTIVITIES")).isEqualTo(1);
  }
}
