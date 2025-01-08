/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

import static org.sonar.db.component.ComponentQualifiers.PROJECT;

public class ComponentIndexScoreTest extends ComponentIndexTest {
  @Test
  public void should_prefer_components_without_prefix() {
    assertResultOrder("File.java",
      "File.java",
      "MyFile.java");
  }

  @Test
  public void should_prefer_components_without_suffix() {
    assertResultOrder("File",
      "File",
      "Filex");
  }

  @Test
  public void should_prefer_key_matching_over_name_matching() {
    es.recreateIndexes();
    ProjectDto project1 = indexProject("quality", "SonarQube");
    ProjectDto project2 = indexProject("sonarqube", "Quality Product");

    assertExactResults("sonarqube", project2, project1);
  }

  @Test
  public void should_prefer_prefix_matching_over_partial_matching() {
    assertResultOrder("corem",
      "CoreMetrics.java",
      "ScoreMatrix.java");
  }

  @Test
  public void should_prefer_case_sensitive_prefix() {
    assertResultOrder("caSe",
      "caSeBla.java",
      "CaseBla.java");
  }

  @Test
  public void scoring_prefix_with_multiple_words() {
    assertResultOrder("index java",
      "IndexSomething.java",
      "MyIndex.java");
  }

  @Test
  public void scoring_prefix_with_multiple_words_and_case() {
    assertResultOrder("Index JAVA",
      "IndexSomething.java",
      "index_java.js");
  }

  @Test
  public void scoring_long_items() {
    assertResultOrder("ThisIsAVeryLongNameToSearchForAndItExceeds15Characters.java",
      "ThisIsAVeryLongNameToSearchForAndItExceeds15Characters.java",
      "ThisIsAVeryLongNameToSearchForAndItEndsDifferently.java");
  }

  @Test
  public void scoring_perfect_match() {
    assertResultOrder("SonarQube",
      "SonarQube",
      "SonarQube SCM Git");
  }

  @Test
  public void scoring_perfect_match_dispite_case_changes() {
    assertResultOrder("sonarqube",
      "SonarQube",
      "SonarQube SCM Git");
  }

  @Test
  public void scoring_perfect_match_with_matching_case_higher_than_without_matching_case() {
    assertResultOrder("sonarqube",
      "sonarqube",
      "SonarQube");
  }

  @Test
  public void should_prefer_favorite_over_recently_browsed() {
    ProjectDto project1 = db.components().insertPrivateProject(c -> c.setName("File1")).getProjectDto();
    index(project1);

    ProjectDto project2 = db.components().insertPrivateProject(c -> c.setName("File2")).getProjectDto();
    index(project2);

    assertSearch(SuggestionQuery.builder()
      .setQuery("File")
      .setQualifiers(List.of(PROJECT))
      .setRecentlyBrowsedKeys(Set.of(project1.getKey()))
      .setFavoriteKeys(Set.of(project2.getKey()))
      .build()).containsExactly(uuids(project2, project1));

    assertSearch(SuggestionQuery.builder()
      .setQuery("File")
      .setQualifiers(List.of(PROJECT))
      .setRecentlyBrowsedKeys(Set.of(project2.getKey()))
      .setFavoriteKeys(Set.of(project1.getKey()))
      .build()).containsExactly(uuids(project1, project2));
  }

  @Test
  public void if_relevancy_is_equal_fall_back_to_alphabetical_ordering() {
    assertResultOrder("sonarqube",
      "sonarqubeA",
      "sonarqubeB");
  }

  @Test
  public void scoring_test_DbTester() {
    features.set(ComponentTextSearchFeatureRepertoire.PARTIAL);

    ProjectDto project = indexProject("key-1", "Quality Product");

    assertSearch("dbt").isEmpty();
  }
}
