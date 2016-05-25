/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.es.BaseIndex;
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

public class TestIndex extends BaseIndex {
  private static final Function<Map<String, Object>, TestDoc> CONVERTER = new NonNullInputFunction<Map<String, Object>, TestDoc>() {
    @Override
    protected TestDoc doApply(Map<String, Object> fields) {
      return new TestDoc(fields);
    }
  };

  public TestIndex(EsClient client) {
    super(client);
  }

  public List<CoveredFileDoc> coveredFiles(String testUuid) {
    List<CoveredFileDoc> coveredFiles = new ArrayList<>();

    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)))
      .get().getHits().getHits()) {
      coveredFiles.addAll(new TestDoc(hit.sourceAsMap()).coveredFiles());
    }

    return coveredFiles;
  }

  public SearchResult<TestDoc> searchByTestFileUuid(String testFileUuid, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_FILE_UUID, testFileUuid)));

    return new SearchResult<>(searchRequest.get(), CONVERTER);
  }

  public SearchResult<TestDoc> searchBySourceFileUuidAndLineNumber(String sourceFileUuid, int lineNumber, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(nestedQuery(FIELD_COVERED_FILES, boolQuery()
        .must(termQuery(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_UUID, sourceFileUuid))
        .must(termQuery(FIELD_COVERED_FILES + "." + FIELD_COVERED_FILE_LINES, lineNumber))));

    return new SearchResult<>(searchRequest.get(), CONVERTER);
  }

  public TestDoc getByTestUuid(String testUuid) {
    Optional<TestDoc> testDoc = getNullableByTestUuid(testUuid);
    if (testDoc.isPresent()) {
      return testDoc.get();
    }

    throw new IllegalStateException(String.format("Test id '%s' not found", testUuid));
  }

  public Optional<TestDoc> getNullableByTestUuid(String testUuid) {
    for (SearchHit hit : getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)))
      .get().getHits().getHits()) {
      return Optional.of(new TestDoc(hit.sourceAsMap()));
    }

    return Optional.absent();
  }

  public SearchResult<TestDoc> searchByTestUuid(String testUuid, SearchOptions searchOptions) {
    SearchRequestBuilder searchRequest = getClient().prepareSearch(TestIndexDefinition.INDEX)
      .setTypes(TestIndexDefinition.TYPE)
      .setSize(searchOptions.getLimit())
      .setFrom(searchOptions.getOffset())
      .setQuery(boolQuery().must(matchAllQuery()).filter(termQuery(FIELD_TEST_UUID, testUuid)));

    return new SearchResult<>(searchRequest.get(), CONVERTER);
  }
}
