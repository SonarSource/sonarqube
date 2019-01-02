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

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUserUpdatedAtOfRulesProfilesTest {
  private static final String TABLE_QUALITY_PROFILES = "rules_profiles";
  private static final String TABLE_ACTIVITIES = "activities";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUserUpdatedAtOfRulesProfilesTest.class, "schema.sql");

  PopulateUserUpdatedAtOfRulesProfiles underTest = new PopulateUserUpdatedAtOfRulesProfiles(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_QUALITY_PROFILES)).isEqualTo(0);
    assertThat(db.countRowsOfTable(TABLE_ACTIVITIES)).isEqualTo(0);
  }

  @Test
  public void migration_update_quality_profiles_user_updated_at() throws SQLException {

    insertQualityProfile(1, "first-quality-profile");
    insertActivity("first-quality-profile", "my-login", 1_000_000_00L);
    insertActivity("first-quality-profile", null, 2_000_000_000L);
    insertActivity("first-quality-profile", "my-login", 1_100_000_000L);
    insertQualityProfile(2, "second-quality-profile");
    insertActivity("second-quality-profile", null, 1_000_000_00L);
    insertQualityProfile(3, "third-quality-profile");
    insertQualityProfile(4, "fourth-quality-profile");
    insertActivity("fourth-quality-profile", "my-login", 1_000_000_00L);

    underTest.execute();

    assertUserUpdatedAt("first-quality-profile", 1_100_000_000L);
    assertNoUserUpdatedAtDate("second-quality-profile");
    assertNoUserUpdatedAtDate("third-quality-profile");
    assertUserUpdatedAt("fourth-quality-profile", 1_000_000_00L);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertQualityProfile(1, "first-quality-profile");
    insertActivity("first-quality-profile", "my-login", 1_000_000_000L);

    underTest.execute();
    assertUserUpdatedAt("first-quality-profile", 1_000_000_000L);

    underTest.execute();
    assertUserUpdatedAt("first-quality-profile", 1_000_000_000L);
  }

  private void assertUserUpdatedAt(String qualityProfileKey, long expectedLastUsed) {
    assertThat(selectUserUpdatedAt(qualityProfileKey)).isEqualTo(expectedLastUsed);
  }

  private void assertNoUserUpdatedAtDate(String qualityProfileKey) {
    assertThat(selectUserUpdatedAt(qualityProfileKey)).isNull();
  }

  @CheckForNull
  private Long selectUserUpdatedAt(String qualityProfileKey) {
    return (Long) db.selectFirst(String.format("select user_updated_at as \"userUpdatedAt\" from rules_profiles where kee ='%s'", qualityProfileKey)).get("userUpdatedAt");
  }

  private void insertActivity(String profileKey, @Nullable String login, @Nullable Long createdAt) {
    final String sqlInsertActivity = "insert into activities (profile_key, user_login, created_at) values (?, ?, ?) ";

    try (Connection connection = db.openConnection();
      PreparedStatement ps = connection.prepareStatement(sqlInsertActivity)) {
      ps.setString(1, profileKey);
      ps.setString(2, login);
      ps.setTimestamp(3, createdAt == null ? null : new Timestamp(createdAt));
      ps.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
  }

  private void insertQualityProfile(long id, String key) {
    db.executeInsert(TABLE_QUALITY_PROFILES,
      "id", valueOf(id),
      "name", key,
      "kee", key);
  }
}
