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
package org.sonar.server.component.index;

import org.junit.Before;
import org.junit.Test;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

public class ComponentIndexFeaturePartialTest extends ComponentIndexTest {

  @Before
  public void before() {
    features.set(ComponentTextSearchFeatureRepertoire.PARTIAL);
  }

  @Test
  public void search_projects_by_exact_name() {
    ComponentDto struts = indexProject("struts", "Apache Struts");
    indexProject("sonarqube", "SonarQube");

    assertSearchResults("Apache Struts", struts);
    assertSearchResults("APACHE STRUTS", struts);
    assertSearchResults("APACHE struTS", struts);
  }

  @Test
  public void search_file_with_long_name() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/DefaultRubyComponentServiceTestManagerFactory.java", "DefaultRubyComponentServiceTestManagerFactory.java");

    assertSearchResults("DefaultRubyComponentServiceTestManagerFactory", file1);
    assertSearchResults("DefaultRubyComponentServiceTestManagerFactory.java", file1);
    assertSearchResults("RubyComponentServiceTestManager", file1);
    assertSearchResults("te", file1);
  }

  @Test
  public void should_search_by_name_with_two_characters() {
    ComponentDto project = indexProject("struts", "Apache Struts");

    assertSearchResults("st", project);
    assertSearchResults("tr", project);
  }

  @Test
  public void search_projects_by_partial_name() {
    ComponentDto struts = indexProject("struts", "Apache Struts");

    assertSearchResults("truts", struts);
    assertSearchResults("pache", struts);
    assertSearchResults("apach", struts);
    assertSearchResults("che stru", struts);
  }

  @Test
  public void search_projects_and_files_by_partial_name() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");
    indexFile(project, "src/main/java/Foo.java", "Foo.java");

    assertSearchResults("struts", project, file1);
    assertSearchResults("Struts", project, file1);
    assertSearchResults("StrutsManager", file1);
    assertSearchResults("STRUTSMAN", file1);
    assertSearchResults("utsManag", file1);
  }

  @Test
  public void should_find_file_by_file_extension() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");
    ComponentDto file2 = indexFile(project, "src/main/java/Foo.java", "Foo.java");

    assertSearchResults(".java", file1, file2);
    assertSearchResults("manager.java", file1);

    // do not match
    assertNoSearchResults("somethingStrutsManager.java");
  }

  @Test
  public void should_search_for_word_and_suffix() {
    assertFileMatches("plugin java", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_word_and_suffix_in_any_order() {
    assertFileMatches("java plugin", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_two_words() {
    assertFileMatches("abstract factory", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_two_words_in_any_order() {
    assertFileMatches("factory abstract", "AbstractPluginFactory.java");
  }

  @Test
  public void should_require_at_least_one_matching_word() {
    assertNoFileMatches("monitor object", "AbstractPluginFactory.java");
  }
}
