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

package org.sonar.server.db.migrations.v44;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedQProfileKeysMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedQProfileKeysMigrationTest.class, "schema.sql");

  @Test
  public void feed_keys() throws Exception {
    db.prepareDbUnit(getClass(), "feed_keys.xml");

    new FeedQProfileKeysMigrationStep(db.database()).execute();

    List<Map<String, Object>> profiles = db.select("SELECT kee, name, language, parent_kee FROM rules_profiles ORDER BY id ASC");

    Map<String, Object> parentProfile = profiles.get(0);
    assertThat((String) parentProfile.get("KEE")).startsWith("java-sonar-way-");
    assertThat(parentProfile.get("NAME")).isEqualTo("Sonar Way");
    assertThat(parentProfile.get("LANGUAGE")).isEqualTo("java");
    assertThat(parentProfile.get("PARENT_KEE")).isNull();

    Map<String, Object> differentCaseProfile = profiles.get(1);
    assertThat((String) differentCaseProfile.get("KEE")).startsWith("java-sonar-way-").isNotEqualTo(parentProfile.get("KEE"));
    assertThat(differentCaseProfile.get("NAME")).isEqualTo("Sonar way");
    assertThat(differentCaseProfile.get("PARENT_KEE")).isNull();

    Map<String, Object> childProfile = profiles.get(2);
    assertThat((String) childProfile.get("KEE")).startsWith("java-child-");
    assertThat(childProfile.get("NAME")).isEqualTo("Child");
    assertThat(childProfile.get("PARENT_KEE")).isEqualTo(parentProfile.get("KEE"));

    Map<String, Object> phpProfile = profiles.get(3);
    assertThat((String) phpProfile.get("KEE")).startsWith("php-sonar-way-");
    assertThat(phpProfile.get("NAME")).isEqualTo("Sonar Way");
    assertThat(phpProfile.get("LANGUAGE")).isEqualTo("php");
    assertThat(phpProfile.get("PARENT_KEE")).isNull();
  }

}
