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

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_LINES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;

public class TestIndex extends BaseIndex {
  public TestIndex(EsClient client) {
    super(client);
  }

  public List<CoveredFileDoc> coveredFiles(String testUuid) {
    List<CoveredFileDoc> coveredFiles = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.termFilter(FIELD_TEST_UUID, testUuid).cache(false)))
      .get().getHits().getHits()) {
      coveredFiles.addAll(new TestDoc(hit.sourceAsMap()).coveredFiles());
    }

    return coveredFiles;
  }

  public List<TestDoc> searchByTestFileUuid(String testFileUuid) {
    List<TestDoc> testDocs = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(10_000)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.termFilter(FIELD_FILE_UUID, testFileUuid)))
      .get().getHits().getHits()) {
      testDocs.add(new TestDoc(hit.sourceAsMap()));
    }

    return testDocs;
  }

  public List<TestDoc> searchBySourceFileUuidAndLineNumber(String sourceFileUuid, int lineNumber) {
    List<TestDoc> testDocs = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(10_000)
      .setQuery(QueryBuilders.nestedQuery(FIELD_COVERED_FILES, FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_UUID, sourceFileUuid).cache(false))
        .must(FilterBuilders.termFilter(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_LINES, lineNumber).cache(false))))
      .get().getHits().getHits()) {
      testDocs.add(new TestDoc(hit.sourceAsMap()));
    }

    return testDocs;
  }

  // TODO to test
  public TestDoc searchByTestUuid(String testUuid) {
    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.termFilter(FIELD_TEST_UUID, testUuid).cache(false)))
      .get().getHits().getHits()) {
      return new TestDoc(hit.sourceAsMap());
    }

    throw new IllegalStateException(String.format("Test uuid '%s' not found", testUuid));
  }
}
