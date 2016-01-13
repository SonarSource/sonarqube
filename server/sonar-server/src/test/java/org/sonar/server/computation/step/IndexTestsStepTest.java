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
package org.sonar.server.computation.step;

import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.es.EsTester;
import org.sonar.server.test.db.TestTesting;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexTestsStepTest extends BaseStepTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new TestIndexDefinition(new Settings()));

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbClient dbClient = dbTester.getDbClient();

  @Before
  public void setUp() {
    esTester.truncateIndices();
  }

  @Override
  protected ComputationStep step() {
    TestIndexer testIndexer = new TestIndexer(dbClient, esTester.client());
    testIndexer.setEnabled(true);
    return new IndexTestsStep(testIndexer, treeRootHolder);
  }

  @Test
  public void index_test() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index_source.xml");
    TestTesting.updateDataColumn(dbTester.getSession(), "FILE1_UUID", TestTesting.newRandomTests(1));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey("PROJECT_KEY").build());

    step().execute();

    List<SearchHit> docs = esTester.getDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE);
    assertThat(docs).hasSize(1);
    TestDoc doc = new TestDoc(docs.get(0).sourceAsMap());
    assertThat(doc.projectUuid()).isEqualTo("ABCD");
    assertThat(doc.fileUuid()).isEqualTo("FILE1_UUID");
    assertThat(doc.coveredFiles()).isNotEmpty();
  }
}
