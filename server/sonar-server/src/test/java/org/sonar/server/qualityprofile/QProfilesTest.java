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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class QProfilesTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  QProfileProjectOperations projectOperations;

  @Mock
  QProfileProjectLookup projectLookup;

  @Mock
  QProfileLookup profileLookup;

  QProfiles qProfiles;

  @Before
  public void setUp() {
    qProfiles = new QProfiles(projectOperations, projectLookup, profileLookup, userSessionRule);
  }

  @Test
  public void search_profile_by_id() {
    qProfiles.profile(1);
    verify(profileLookup).profile(1);
  }

  @Test
  public void search_profile_by_name_and_language() {
    qProfiles.profile("Default", "java");
    verify(profileLookup).profile("Default", "java");
  }

  @Test
  public void search_profiles() {
    qProfiles.allProfiles();
    verify(profileLookup).allProfiles();
  }

  @Test
  public void search_profiles_by_language() {
    qProfiles.profilesByLanguage("java");
    verify(profileLookup).profiles("java");
  }

  @Test
  public void search_parent_profile() {
    QProfile profile = new QProfile().setId(1).setParent("Parent").setLanguage("java");
    qProfiles.parent(profile);
    verify(profileLookup).parent(profile);
  }

  @Test
  public void search_children() {
    QProfile profile = new QProfile();
    qProfiles.children(profile);
    verify(profileLookup).children(profile);
  }

  @Test
  public void search_ancestors() {
    QProfile profile = new QProfile();
    qProfiles.ancestors(profile);
    verify(profileLookup).ancestors(profile);
  }

  @Test
  public void projects() {
    qProfiles.projects(1);
    verify(projectLookup).projects(1);
  }

  @Test
  public void count_projects() {
    QProfile profile = new QProfile();
    qProfiles.countProjects(profile);
    verify(projectLookup).countProjects(profile);
  }

  @Test
  public void get_profiles_from_project_and_language() {
    qProfiles.findProfileByProjectAndLanguage(1, "java");
    verify(projectLookup).findProfileByProjectAndLanguage(1, "java");
  }

  @Test
  public void add_project() {
    qProfiles.addProject("sonar-way-java", "ABCD");
    verify(projectOperations).addProject(eq("sonar-way-java"), eq("ABCD"), any(ThreadLocalUserSession.class));
  }

  @Test
  public void remove_project_by_quality_profile_key() {
    qProfiles.removeProject("sonar-way-java", "ABCD");
    verify(projectOperations).removeProject(eq("sonar-way-java"), eq("ABCD"), any(ThreadLocalUserSession.class));
  }

  @Test
  public void remove_project_by_language() {
    qProfiles.removeProjectByLanguage("java", 10L);
    verify(projectOperations).removeProject(eq("java"), eq(10L), any(ThreadLocalUserSession.class));
  }

  @Test
  public void remove_all_projects() {
    qProfiles.removeAllProjects("sonar-way-java");
    verify(projectOperations).removeAllProjects(eq("sonar-way-java"), any(ThreadLocalUserSession.class));
  }
}
