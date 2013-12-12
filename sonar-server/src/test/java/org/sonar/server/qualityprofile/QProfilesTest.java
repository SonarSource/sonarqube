/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.qualityprofile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfilesTest {

  @Mock
  QualityProfileDao dao;

  QProfiles qProfiles;

  @Before
  public void setUp() throws Exception {
    qProfiles = new QProfiles(dao);
  }

  @Test
  public void search_profiles() throws Exception {
    when(dao.selectAll()).thenReturn(newArrayList(
      new QualityProfileDto().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    ));

    List<QProfile> result = qProfiles.searchProfiles();
    assertThat(result).hasSize(1);

    QProfile qProfile = result.get(0);
    assertThat(qProfile.id()).isEqualTo(1);
    assertThat(qProfile.name()).isEqualTo("Sonar Way with Findbugs");
    assertThat(qProfile.language()).isEqualTo("java");
    assertThat(qProfile.key()).isEqualTo(QProfileKey.of("Sonar Way with Findbugs", "java"));
    assertThat(qProfile.parent()).isEqualTo("Sonar Way");
    assertThat(qProfile.version()).isEqualTo(1);
    assertThat(qProfile.used()).isFalse();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSearchProfile() throws Exception {
    qProfiles.searchProfile(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testNewProfile() throws Exception {
    qProfiles.newProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDeleteProfile() throws Exception {
    qProfiles.deleteProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRenameProfile() throws Exception {
    qProfiles.renameProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSetDefaultProfile() throws Exception {
    qProfiles.setDefaultProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCopyProfile() throws Exception {
    qProfiles.copyProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testExportProfile() throws Exception {
    qProfiles.exportProfile(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testExportProfileByPlugin() throws Exception {
    qProfiles.exportProfile(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRestoreProfile() throws Exception {
    qProfiles.restoreProfile();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testInheritance() throws Exception {
    qProfiles.inheritance();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testInherit() throws Exception {
    qProfiles.inherit(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testChangelog() throws Exception {
    qProfiles.changelog(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testProjects() throws Exception {
    qProfiles.projects(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAddProject() throws Exception {
    qProfiles.addProject(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveProject() throws Exception {
    qProfiles.removeProject(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveAllProjects() throws Exception {
    qProfiles.removeAllProjects(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSearchActiveRules() throws Exception {
    qProfiles.searchActiveRules(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSearchInactiveRules() throws Exception {
    qProfiles.searchInactiveRules(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testActiveRule() throws Exception {
    qProfiles.activeRule(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDeactiveRule() throws Exception {
    qProfiles.deactiveRule(null, null);
  }
}
