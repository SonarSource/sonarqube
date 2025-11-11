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

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collections;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static org.sonar.db.component.ComponentQualifiers.PROJECT;

class ComponentIndexFeatureExactTest extends ComponentIndexTest {

  @BeforeEach
  void before() {
    features.set(new ComponentTextSearchFeature() {
      @Override
      public Query getQueryV2(ComponentTextSearchQuery query) {
        return Query.of(q -> q.matchAll(new MatchAllQuery.Builder().build()));
      }
    }, ComponentTextSearchFeatureRepertoire.EXACT_IGNORE_CASE);
  }

  @Test
  void scoring_cares_about_exact_matches() {
    ProjectDto project1 = indexProject("project1", "LongNameLongNameLongNameLongNameSonarQube");
    ProjectDto project2 = indexProject("project2", "LongNameLongNameLongNameLongNameSonarQubeX");

    SuggestionQuery query1 = SuggestionQuery.builder()
      .setQuery("LongNameLongNameLongNameLongNameSonarQube")
      .setQualifiers(Collections.singletonList(PROJECT))
      .build();
    assertSearch(query1).containsExactly(uuids(project1, project2));
  }
}
