/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.test.index;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sonar.server.test.index.TestIndexDefinition.*;

public class TestIndex extends BaseIndex {
  public TestIndex(EsClient client) {
    super(client);
  }

  public List<Map<String, Object>> coveredLines(String testFileUuid, String methodName) {
    List<Map<String, Object>> coverageBlocks = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter(FIELD_UUID, testFileUuid).cache(false))
        .must(FilterBuilders.termFilter(TestIndexDefinition.FIELD_NAME, methodName).cache(false))))
      .get().getHits().getHits()) {
      coverageBlocks.addAll(new TestDoc(hit.sourceAsMap()).coverageBlocks());
    }

    return coverageBlocks;
  }

  public List<TestDoc> testMethods(String testFileUuid) {
    List<TestDoc> testDocs = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(10_000)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.termFilter(FIELD_UUID, testFileUuid)))
      .get().getHits().getHits()) {
      testDocs.add(new TestDoc(hit.sourceAsMap()));
    }

    return testDocs;
  }

  public List<TestDoc> testsCovering(String mainFileUuid, int line) {
    List<TestDoc> testDocs = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(10_000)
      .setQuery(QueryBuilders.nestedQuery(FIELD_COVERAGE_BLOCKS, FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter(FIELD_COVERAGE_BLOCKS + "." + FIELD_COVERAGE_BLOCK_UUID, mainFileUuid).cache(false))
        .must(FilterBuilders.termFilter(FIELD_COVERAGE_BLOCKS + "." + FIELD_COVERAGE_BLOCK_LINES, line).cache(false))))
      .get().getHits().getHits()) {
      testDocs.add(new TestDoc(hit.sourceAsMap()));
    }

    return testDocs;
  }
}
