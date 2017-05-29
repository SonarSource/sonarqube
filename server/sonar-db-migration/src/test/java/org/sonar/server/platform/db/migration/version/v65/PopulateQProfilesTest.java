/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class PopulateQProfilesTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQProfilesTest.class, "initial.sql");

  private System2 system2 = new AlwaysIncreasingSystem2();
  private PopulateQProfiles underTest = new PopulateQProfiles(db.database(), system2);

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertRulesProfile("ORG_1", "java", "u1", null,true);
    insertRulesProfile("ORG_2", "js", "u2", "u1", true);

    // org1 is already processed
    insertQProfile("u1", "ORG_1", "RPU1");

    underTest.execute();

    assertThat(countRows()).isEqualTo(2);
    Map<String, Object> qprofile1 = selectQProfile("u1", "ORG_1");
    Map<String, Object> qprofile2 = selectQProfile("u2", "ORG_2");

    assertThat(qprofile1.get("UUID")).isEqualTo("u1");
    assertThat(qprofile1.get("ORGANIZATION_UUID")).isEqualTo("ORG_1");
    assertThat(qprofile1.get("RULES_PROFILE_UUID")).isEqualTo("RPU1"); // Ok if not overridden ?
    assertThat(qprofile1.get("PARENT_UUID")).isNull();

    assertThat(qprofile2.get("UUID")).isEqualTo("u2");
    assertThat(qprofile2.get("ORGANIZATION_UUID")).isEqualTo("ORG_2");
    assertThat(qprofile2.get("RULES_PROFILE_UUID")).isEqualTo("u2");
    assertThat(qprofile2.get("PARENT_UUID")).isEqualTo("u1");
  }

  @Test
  public void migration_must_create_as_much_as_rules_profile() throws SQLException {
    Random random = new Random();
    int nbRulesProfile = 100 + random.nextInt(100);
    IntStream.range(0, nbRulesProfile).forEach(
      i -> insertRulesProfile("ORG_" + i, "java", "uuid" + i, random.nextBoolean() ? "ORG_" + random.nextInt(i + 1) : null,random.nextBoolean())
    );

    underTest.execute();

    assertThat(countRows()).isEqualTo(nbRulesProfile);
  }


  private int countRows() {
    return db.countRowsOfTable("qprofiles");
  }

  private void insertRulesProfile(String orgUuid, String language, String uuid, String parentKee, boolean isDefault) {
    db.executeInsert("RULES_PROFILES",
      "NAME", "name_" + uuid,
      "KEE", uuid,
      "ORGANIZATION_UUID", orgUuid,
      "PARENT_KEE", parentKee,
      "LANGUAGE", language,
      "IS_DEFAULT", isDefault,
      "IS_BUILT_IN", true);
  }

  private void insertQProfile(String uuid, String orgUuid, String rulesProfileUuid) {
    db.executeInsert("QPROFILES",
      "ORGANIZATION_UUID", orgUuid,
      "RULES_PROFILE_UUID", rulesProfileUuid,
      "UUID", uuid,
      "CREATED_AT", system2.now(),
      "UPDATED_AT", system2.now()
      );
  }

  private Map<String, Object> selectQProfile(String uuid, String orgUuid) {
    return db.selectFirst(format("select * from qprofiles where uuid='%s' and organization_uuid='%s'", uuid, orgUuid));
  }
}
