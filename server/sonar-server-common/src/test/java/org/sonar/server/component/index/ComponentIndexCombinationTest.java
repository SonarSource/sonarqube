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

import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexCombinationTest extends ComponentIndexTest {

  @Test
  public void return_empty_list_if_no_fields_match_query() {
    indexProject("struts", "Apache Struts");

    assertThat(index.searchSuggestions(SuggestionQuery.builder().setQuery("missing").build()).isEmpty()).isTrue();
  }

  @Test
  public void should_not_return_components_that_do_not_match_at_all() {
    indexProject("banana", "Banana Project 1");

    assertNoSearchResults("Apple");
  }

  @Test
  public void filter_results_by_qualifier() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");

    assertSearchResults(SuggestionQuery.builder().setQuery("struts").setQualifiers(singletonList(Qualifiers.PROJECT)).build(), project);
  }

  @Test
  public void should_limit_the_number_of_results() {
    IntStream.rangeClosed(0, 10).forEach(i -> indexProject("sonarqube" + i, "SonarQube" + i));

    assertSearch(SuggestionQuery.builder().setQuery("sonarqube").setLimit(5).setQualifiers(singletonList(Qualifiers.PROJECT)).build()).hasSize(5);
  }

  @Test
  public void should_not_support_wildcards() {
    indexProject("theKey", "the name");

    assertNoSearchResults("*t*");
    assertNoSearchResults("th?Key");
  }
}
