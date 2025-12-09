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

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singletonList;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;

class ComponentIndexFeatureFavoriteTest extends ComponentIndexTest {

  @BeforeEach
  void before() {
    features.set(new ComponentTextSearchFeature() {
      @Override
      public Query getQueryV2(ComponentTextSearchQuery query) {
        return Query.of(q -> q.matchAll(new MatchAllQuery.Builder().build()));
      }

      @Override
      public UseCase getUseCase() {
        return UseCase.GENERATE_RESULTS;
      }
    }, ComponentTextSearchFeatureRepertoire.FAVORITE);
  }

  @Test
  void scoring_cares_about_favorites() {
    ProjectDto project1 = indexProject("sonarqube", "SonarQube");
    ProjectDto project2 = indexProject("recent", "SonarQube Recently");

    SuggestionQuery query1 = SuggestionQuery.builder()
      .setQuery("SonarQube")
      .setQualifiers(singletonList(PROJECT))
      .setFavoriteKeys(of(project1.getKey()))
      .build();
    assertSearch(query1).containsExactly(uuids(project1, project2));

    SuggestionQuery query2 = SuggestionQuery.builder()
      .setQuery("SonarQube")
      .setQualifiers(singletonList(PROJECT))
      .setFavoriteKeys(of(project2.getKey()))
      .build();
    assertSearch(query2).containsExactly(uuids(project2, project1));
  }

  @Test
  void irrelevant_favorites_are_not_returned() {
    features.set(new ComponentTextSearchFeature() {
      @Override
      public Query getQueryV2(ComponentTextSearchQuery query) {
        return Query.of(q -> q.term(t -> t.field(FIELD_KEY).value("non-existing-value")));
      }

      @Override
      public UseCase getUseCase() {
        return UseCase.GENERATE_RESULTS;
      }
    }, ComponentTextSearchFeatureRepertoire.FAVORITE);
    ProjectDto project1 = indexProject("foo", "foo");

    SuggestionQuery query1 = SuggestionQuery.builder()
      .setQuery("bar")
      .setQualifiers(singletonList(PROJECT))
      .setFavoriteKeys(of(project1.getKey()))
      .build();
    assertSearch(query1).isEmpty();
  }
}
