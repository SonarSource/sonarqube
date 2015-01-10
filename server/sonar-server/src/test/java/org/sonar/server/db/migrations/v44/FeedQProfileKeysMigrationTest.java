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
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FeedQProfileKeysMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedQProfileKeysMigrationTest.class, "schema.sql");

  @Test
  public void feed_keys() throws Exception {
    db.prepareDbUnit(getClass(), "feed_keys.xml");

    new FeedQProfileKeysMigration(db.database()).execute();

    QualityProfileDao dao = new QualityProfileDao(db.myBatis(), mock(System2.class));

    QualityProfileDto parentProfile = dao.getById(10);
    assertThat(parentProfile.getKey()).startsWith("java-sonar-way-");
    assertThat(parentProfile.getName()).isEqualTo("Sonar Way");
    assertThat(parentProfile.getLanguage()).isEqualTo("java");
    assertThat(parentProfile.getParentKee()).isNull();

    QualityProfileDto differentCaseProfile = dao.getById(11);
    assertThat(differentCaseProfile.getKey()).startsWith("java-sonar-way-").isNotEqualTo(parentProfile.getKey());
    assertThat(differentCaseProfile.getName()).isEqualTo("Sonar way");
    assertThat(differentCaseProfile.getParentKee()).isNull();

    QualityProfileDto childProfile = dao.getById(12);
    assertThat(childProfile.getKey()).startsWith("java-child-");
    assertThat(childProfile.getName()).isEqualTo("Child");
    assertThat(childProfile.getParentKee()).isEqualTo(parentProfile.getKey());

    QualityProfileDto phpProfile = dao.getById(13);
    assertThat(phpProfile.getKey()).startsWith("php-sonar-way-");
    assertThat(phpProfile.getName()).isEqualTo("Sonar Way");
    assertThat(phpProfile.getLanguage()).isEqualTo("php");
    assertThat(phpProfile.getParentKee()).isNull();
  }

}
