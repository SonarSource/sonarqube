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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateProfileKeyOfActivitiesTest {
  private static final String ACTIVITIES_TABLE = "activities";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateProfileKeyOfActivitiesTest.class, "activities.sql");

  PopulateProfileKeyOfActivities underTest = new PopulateProfileKeyOfActivities(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(ACTIVITIES_TABLE)).isEqualTo(0);
  }

  @Test
  public void migration_update_activities_profile_key() throws SQLException {
    insertActivity("first-profile-key");
    insertActivity("first-profile-key");
    insertActivity("first-profile-key");
    insertActivity("second-profile-key");
    insertActivity("third-profile-key");

    underTest.execute();

    assertCountActivitiesWithProfile("first-profile-key", 3);
    assertCountActivitiesWithProfile("second-profile-key", 1);
    assertCountActivitiesWithProfile("third-profile-key", 1);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertActivity("profile-key");
    underTest.execute();
    assertCountActivitiesWithProfile("profile-key", 1);

    underTest.execute();
    assertCountActivitiesWithProfile("profile-key", 1);
  }

  @Test
  public void delete_the_rows_of_ACTIVITIES_that_do_not_have_profileKey() throws SQLException {
    db.executeInsert(ACTIVITIES_TABLE, "data_field", "key=fakeKey");

    underTest.execute();

    assertThat(db.countRowsOfTable(ACTIVITIES_TABLE)).isEqualTo(0);
  }

  @Test
  public void delete_the_rows_of_ACTIVITIES_that_have_empty_profileKey() throws SQLException {
    insertActivity("");

    underTest.execute();

    assertThat(db.countRowsOfTable(ACTIVITIES_TABLE)).isEqualTo(0);
  }

  @Test
  public void delete_the_rows_of_ACTIVITIES_that_have_blank_profileKey() throws SQLException {
    insertActivity("    ");

    underTest.execute();

    assertThat(db.countRowsOfTable(ACTIVITIES_TABLE)).isEqualTo(0);
  }

  private void assertCountActivitiesWithProfile(String profileKey, int expectedNumberOfActivities) {
    assertThat(countActivitiesWithProfile(profileKey)).isEqualTo(expectedNumberOfActivities);
  }

  private int countActivitiesWithProfile(String qualityProfileKey) {
    // profile key is removed from data_field
    return db.countSql(String.format("select count(1) from activities where profile_key='%s' and data_field='key=fakeKey'", qualityProfileKey));
  }

  private void insertActivity(String profileKey) {
    db.executeInsert(ACTIVITIES_TABLE,
      "data_field", "key=fakeKey;profileKey=" + profileKey);
  }
}
