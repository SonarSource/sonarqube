/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

class ComponentIndexFeaturePartialTest extends ComponentIndexTest {

  @BeforeEach
  void before() {
    features.set(ComponentTextSearchFeatureRepertoire.PARTIAL);
  }

  @Test
  void search_projects_by_exact_name() {
    ProjectDto struts = indexProject("struts", "Apache Struts");
    indexProject("sonarqube", "SonarQube");

    assertSearchResults("Apache Struts", struts);
    assertSearchResults("APACHE STRUTS", struts);
    assertSearchResults("APACHE struTS", struts);
  }

  @Test
  void should_search_by_name_with_two_characters() {
    ProjectDto project = indexProject("struts", "Apache Struts");

    assertSearchResults("st", project);
    assertSearchResults("tr", project);
  }

  @Test
  void search_projects_by_partial_name() {
    ProjectDto struts = indexProject("struts", "Apache Struts");

    assertSearchResults("truts", struts);
    assertSearchResults("pache", struts);
    assertSearchResults("apach", struts);
    assertSearchResults("che stru", struts);
  }

  @Test
  void search_projects_and_files_by_partial_name() {
    ProjectDto project = indexProject("struts", "Apache Struts");

    assertSearchResults("struts", project);
    assertSearchResults("Struts", project);
  }

  @Test
  void should_search_for_word_and_suffix() {
    assertResultOrder("plugin java", "AbstractPluginFactory.java");
  }

  @Test
  void should_search_for_word_and_suffix_in_any_order() {
    assertResultOrder("java plugin", "AbstractPluginFactory.java");
  }

  @Test
  void should_search_for_two_words() {
    assertResultOrder("abstract factory", "AbstractPluginFactory.java");
  }

  @Test
  void should_search_for_two_words_in_any_order() {
    assertResultOrder("factory abstract", "AbstractPluginFactory.java");
  }

  @Test
  void should_require_at_least_one_matching_word() {
    indexProject("AbstractPluginFactory");

    assertNoSearchResults("monitor object");
  }
}
