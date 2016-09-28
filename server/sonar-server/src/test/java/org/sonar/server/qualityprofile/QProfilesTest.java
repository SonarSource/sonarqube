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
package org.sonar.server.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class QProfilesTest {

  private static final String PROJECT_UUID = "P1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private QProfileLookup profileLookup = mock(QProfileLookup.class);
  private QProfiles underTest = new QProfiles(profileLookup, dbTester.getDbClient());

  @Test
  public void search_profiles() {
    underTest.allProfiles();
    verify(profileLookup).allProfiles();
  }

  @Test
  public void search_profiles_by_language() {
    underTest.profilesByLanguage("java");
    verify(profileLookup).profiles("java");
  }

  @Test
  public void findProfileByProjectAndLanguage() {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project);
    QualityProfileDao qProfileDao = dbTester.getDbClient().qualityProfileDao();
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto().setLanguage("js");
    qProfileDao.insert(dbTester.getSession(), profile);
    qProfileDao.insertProjectProfileAssociation(PROJECT_UUID, profile.getKey(), dbTester.getSession());
    dbTester.commit();

    assertThat(underTest.findProfileByProjectAndLanguage(1, "java")).isNull();
    assertThat(underTest.findProfileByProjectAndLanguage(project.getId(), "java")).isNull();
    assertThat(underTest.findProfileByProjectAndLanguage(project.getId(), "js").key()).isEqualTo(profile.getKey());
  }
}
