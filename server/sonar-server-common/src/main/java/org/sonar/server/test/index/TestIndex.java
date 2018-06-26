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
package org.sonar.server.test.index;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_LINES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;

public class TestIndex {
  private final EsClient client;
  private final System2 system2;

  public TestIndex(EsClient client, System2 system2) {
    this.client = client;
    this.system2 = system2;
  }

  public List<CoveredFileDoc> coveredFiles(String testUuid) {
    List<CoveredFileDoc> coveredFiles = new ArrayList<>();

    for (SearchHit hit : client.prepareSearch(TestIndexDefinition.INDEX_TYPE_TEST)
      .setSize(1)
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)))
      .get().getHits().getHits()) {
      coveredFiles.addAll(new TestDoc(hit.getSourceAsMap()).coveredFiles());
    }

    return coveredFiles;
  }

  public SearchResult<TestDoc> searchByTestFileUuid(String testFileUuid, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = client.prepareSearch(TestIndexDefinition.INDEX_TYPE_TEST)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_FILE_UUID, testFileUuid)));

    return new SearchResult<>(searchRequest.get(), TestDoc::new, system2.getDefaultTimeZone());
  }

  public SearchResult<TestDoc> searchBySourceFileUuidAndLineNumber(String sourceFileUuid, int lineNumber, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = client.prepareSearch(TestIndexDefinition.INDEX_TYPE_TEST)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(nestedQuery(
        FIELD_COVERED_FILES,
        boolQuery()
          .must(termQuery(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_UUID, sourceFileUuid))
          .must(termQuery(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_LINES, lineNumber)),
        ScoreMode.Avg));

    return new SearchResult<>(searchRequest.get(), TestDoc::new, system2.getDefaultTimeZone());
  }

  public TestDoc getByTestUuid(String testUuid) {
    Optional<TestDoc> testDoc = getNullableByTestUuid(testUuid);
    if (testDoc.isPresent()) {
      return testDoc.get();
    }

    throw new IllegalStateException(String.format("Test id '%s' not found", testUuid));
  }

  public Optional<TestDoc> getNullableByTestUuid(String testUuid) {
    SearchHit[] hits = client.prepareSearch(TestIndexDefinition.INDEX_TYPE_TEST)
      .setSize(1)
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)))
      .get().getHits().getHits();
    if (hits.length > 0) {
      return Optional.of(new TestDoc(hits[0].getSourceAsMap()));
    }
    return Optional.absent();
  }

  public SearchResult<TestDoc> searchByTestUuid(String testUuid, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = client.prepareSearch(TestIndexDefinition.INDEX_TYPE_TEST)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)));

    return new SearchResult<>(searchRequest.get(), TestDoc::new, system2.getDefaultTimeZone());
  }
}
