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

import java.util.*;

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
  public void lines_covered_a_test_method() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("2", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<Map<String, Object>> result = sut.coveredLines("uuid-1", "name-1");

    assertThat(result).hasSize(3);
    assertThat(result.get(0).get("uuid")).isEqualTo("main-uuid-3");
    assertThat((List<Integer>)result.get(0).get("lines")).containsOnly(25, 33, 82);
  }

  @Test
  public void test_methods() throws Exception {
    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("1", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<TestDoc> result = sut.testMethods("uuid-1");

    assertThat(result).hasSize(2);
  }

  @Test
  public void test_covering() throws Exception {
    List<Map<String, Object>> coverageBlocks = new ArrayList<>();
    coverageBlocks.add(newCoverageBlock("1"));
    coverageBlocks.add(newCoverageBlock("2"));
    coverageBlocks.add(newCoverageBlock("3"));

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      newTestDoc("1", newCoverageBlock("3"), newCoverageBlock("4"), newCoverageBlock("5")),
      newTestDoc("1", newCoverageBlock("5"), newCoverageBlock("6"), newCoverageBlock("7")));

    List<TestDoc> result = sut.testsCovering("main-uuid-5", 82);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("name-1");
    assertThat(result.get(0).coverageBlocks().get(0).get("uuid")).isEqualTo("main-uuid-3");
  }

  private Map<String, Object> newCoverageBlock(String id) {
    Map<String, Object> coverageBlock = new HashMap<>();
    coverageBlock.put("key", "project:file-" + id);
    coverageBlock.put("uuid", "main-uuid-" + id);
    coverageBlock.put("lines", Arrays.asList(25, 33, 82));
    return coverageBlock;
  }

  private TestDoc newTestDoc(String id, Map<String, Object>... coverageBlocks) {
    return new TestDoc()
      .setName("name-" + id)
      .setMessage("message-" + id)
      .setStackTrace("stacktrace-" + id)
      .setStatus("status-" + id)
      .setType("type-" + id)
      .setUuid("uuid-" + id)
      .setCoverageBlocks(Arrays.asList(coverageBlocks));
  }
}
