/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component.index;

import org.junit.Before;
import org.junit.Test;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

public class ComponentIndexFeatureKeyTest extends ComponentIndexTest {

  @Before
  public void before() {
    features.set(ComponentTextSearchFeatureRepertoire.KEY);
  }

  @Test
  public void should_search_projects_by_exact_case_insensitive_key() {
    ComponentDto project1 = indexProject("keyOne", "Project One");
    indexProject("keyTwo", "Project Two");

    assertSearchResults("keyOne", project1);
    assertSearchResults("keyone", project1);
    assertSearchResults("KEYone", project1);
  }

  @Test
  public void should_search_project_with_dot_in_key() {
    ComponentDto project = indexProject("org.sonarqube", "SonarQube");

    assertSearchResults("org.sonarqube", project);
    assertNoSearchResults("orgsonarqube");
  }

  @Test
  public void should_search_project_with_dash_in_key() {
    ComponentDto project = indexProject("org-sonarqube", "SonarQube");

    assertSearchResults("org-sonarqube", project);
    assertNoSearchResults("orgsonarqube");
  }

  @Test
  public void should_search_project_with_colon_in_key() {
    ComponentDto project = indexProject("org:sonarqube", "Quality Product");

    assertSearchResults("org:sonarqube", project);
    assertNoSearchResults("orgsonarqube");
    assertNoSearchResults("org-sonarqube");
    assertNoSearchResults("org_sonarqube");
  }

  @Test
  public void should_search_project_with_all_special_characters_in_key() {
    ComponentDto project = indexProject("org.sonarqube:sonar-sérvèr_ç", "SonarQube");

    assertSearchResults("org.sonarqube:sonar-sérvèr_ç", project);
  }

  @Test
  public void should_not_return_results_when_searching_by_partial_key() {
    indexProject("theKey", "some name");

    assertNoSearchResults("theke");
    assertNoSearchResults("hekey");
  }
}
