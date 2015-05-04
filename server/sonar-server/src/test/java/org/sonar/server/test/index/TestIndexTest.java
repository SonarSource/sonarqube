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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIndexTest {
  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new TestIndexDefinition(new Settings()));

  TestIndex sut = new TestIndex(es.client());

  @Before
  public void before() throws Exception {
    es.truncateIndices();
  }

  @Test
  public void coveredFiles() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("2", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<CoveredFileDoc> result = sut.coveredFiles("uuid-1");

    assertThat(result).hasSize(3);
    assertThat(result).extractingResultOf("fileUuid").containsOnly("main-uuid-3", "main-uuid-4", "main-uuid-5");
    assertThat(result.get(0).coveredLines()).containsOnly(25, 33, 82);
  }

  @Test
  public void searchByTestFileUuid() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("1", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")),
      newTestDoc("2", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<TestDoc> result = sut.searchByTestFileUuid("file-uuid-1", searchOptions()).getDocs();

    assertThat(result).hasSize(2);
    assertThat(result).extractingResultOf("name").containsOnly("name-1");
  }

  @Test
  public void searchBySourceFileUuidAndLineNumber() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("10"), newCoverageBlock("11"), newCoverageBlock("12")),
      newTestDoc("2", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("3", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<TestDoc> result = sut.searchBySourceFileUuidAndLineNumber("main-uuid-5", 82, searchOptions()).getDocs();

    assertThat(result).hasSize(2);
    assertThat(result).extractingResultOf("name").containsOnly("name-2", "name-3");
  }

  @Test
  public void searchByTestUuid() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("2", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    TestDoc test = sut.searchByTestUuid("uuid-1");

    assertThat(test.testUuid()).isEqualTo("uuid-1");
    assertThat(test.fileUuid()).isEqualTo("file-uuid-1");
    assertThat(test.name()).isEqualTo("name-1");
    assertThat(test.durationInMs()).isEqualTo(1L);
    assertThat(test.status()).isEqualTo("status-1");
    assertThat(test.message()).isEqualTo("message-1");
    assertThat(test.coveredFiles()).hasSize(3);
    assertThat(test.coveredFiles()).extractingResultOf("fileUuid").containsOnly("main-uuid-3", "main-uuid-4", "main-uuid-5");
  }

  @Test
  public void searchByTestUuid_with_SearchOptions() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("2", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<TestDoc> result = sut.searchByTestUuid("uuid-1", searchOptions()).getDocs();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).testUuid()).isEqualTo("uuid-1");
  }

  private CoveredFileDoc newCoverageBlock(String id) {
    return new CoveredFileDoc()
      .setFileUuid("main-uuid-" + id)
      .setCoveredLines(Arrays.asList(25, 33, 82));
  }

  private TestDoc newTestDoc(String id, CoveredFileDoc... coveredFiles) {
    return new TestDoc()
      .setUuid("uuid-" + id)
      .setName("name-" + id)
      .setMessage("message-" + id)
      .setStackTrace("stacktrace-" + id)
      .setStatus("status-" + id)
      .setDurationInMs(Long.valueOf(id))
      .setFileUuid("file-uuid-" + id)
      .setProjectUuid("project-uuid-" + id)
      .setCoveredFiles(Arrays.asList(coveredFiles));
  }

  private SearchOptions searchOptions() {
    return new SearchOptions()
      .setLimit(100)
      .setOffset(0);
  }
}
