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
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX_TYPE_TEST;

public class TestIndexTest {
  @Rule
  public EsTester es = new EsTester(new TestIndexDefinition(new MapSettings().asConfig()));

  private TestIndex underTest = new TestIndex(es.client(), System2.INSTANCE);

  @Test
  public void coveredFiles() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    List<CoveredFileDoc> result = underTest.coveredFiles("1");

    assertThat(result).hasSize(3);
    assertThat(result).extractingResultOf("fileUuid").containsOnly("main-uuid-3", "main-uuid-4", "main-uuid-5");
    assertThat(result.get(0).coveredLines()).containsOnly(25, 33, 82);
  }

  @Test
  public void searchByTestFileUuid() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")),
      newTestDoc("3", "TESTFILE2", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    List<TestDoc> result = underTest.searchByTestFileUuid("TESTFILE1", searchOptions()).getDocs();

    assertThat(result).hasSize(2);
    assertThat(result).extractingResultOf("name").containsOnly("name-1", "name-2");
  }

  @Test
  public void searchBySourceFileUuidAndLineNumber() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("10"), newCoveredFileDoc("11"), newCoveredFileDoc("12")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("3", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    List<TestDoc> result = underTest.searchBySourceFileUuidAndLineNumber("main-uuid-5", 82, searchOptions()).getDocs();

    assertThat(result).hasSize(2);
    assertThat(result).extractingResultOf("name").containsOnly("name-2", "name-3");
  }

  @Test
  public void searchByTestUuid() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    TestDoc test = underTest.getByTestUuid("1");

    assertThat(test.testUuid()).isEqualTo("1");
    assertThat(test.fileUuid()).isEqualTo("TESTFILE1");
    assertThat(test.name()).isEqualTo("name-1");
    assertThat(test.durationInMs()).isEqualTo(1L);
    assertThat(test.status()).isEqualTo("status-1");
    assertThat(test.message()).isEqualTo("message-1");
    assertThat(test.coveredFiles()).hasSize(3);
    assertThat(test.coveredFiles()).extractingResultOf("fileUuid").containsOnly("main-uuid-3", "main-uuid-4", "main-uuid-5");
  }

  @Test
  public void getNullableByTestUuid() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    Optional<TestDoc> result = underTest.getNullableByTestUuid("1");

    assertThat(result).isPresent();
    TestDoc test = result.get();
    assertThat(test.testUuid()).isEqualTo("1");
    assertThat(test.fileUuid()).isEqualTo("TESTFILE1");
    assertThat(test.name()).isEqualTo("name-1");
    assertThat(test.durationInMs()).isEqualTo(1L);
    assertThat(test.status()).isEqualTo("status-1");
    assertThat(test.message()).isEqualTo("message-1");
    assertThat(test.coveredFiles()).hasSize(3);
    assertThat(test.coveredFiles()).extractingResultOf("fileUuid").containsOnly("main-uuid-3", "main-uuid-4", "main-uuid-5");
  }

  @Test
  public void getNullableByTestUuid_with_absent_value() {
    Optional<TestDoc> result = underTest.getNullableByTestUuid("unknown-uuid");

    assertThat(result).isAbsent();
  }

  @Test
  public void searchByTestUuid_with_SearchOptions() {
    es.putDocuments(INDEX_TYPE_TEST,
      newTestDoc("1", "TESTFILE1", newCoveredFileDoc("3"), newCoveredFileDoc("4"), newCoveredFileDoc("5")),
      newTestDoc("2", "TESTFILE1", newCoveredFileDoc("5"), newCoveredFileDoc("6"), newCoveredFileDoc("7")));

    List<TestDoc> result = underTest.searchByTestUuid("1", searchOptions()).getDocs();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).testUuid()).isEqualTo("1");
  }

  private CoveredFileDoc newCoveredFileDoc(String id) {
    return new CoveredFileDoc()
      .setFileUuid("main-uuid-" + id)
      .setCoveredLines(asList(25, 33, 82));
  }

  private TestDoc newTestDoc(String testUuid, String fileUuid, CoveredFileDoc... coveredFiles) {
    return new TestDoc()
      .setUuid(testUuid)
      .setProjectUuid("P1")
      .setName("name-" + testUuid)
      .setMessage("message-" + testUuid)
      .setStackTrace("stacktrace-" + testUuid)
      .setStatus("status-" + testUuid)
      .setDurationInMs(Long.valueOf(testUuid))
      .setFileUuid(fileUuid)
      .setCoveredFiles(asList(coveredFiles));
  }

  private SearchOptions searchOptions() {
    return new SearchOptions()
      .setLimit(100)
      .setOffset(0);
  }
}
