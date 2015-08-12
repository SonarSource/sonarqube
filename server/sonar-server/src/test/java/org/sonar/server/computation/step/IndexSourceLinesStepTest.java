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
package org.sonar.server.computation.step;

import java.sql.Connection;
import java.util.List;
import org.elasticsearch.search.SearchHit;
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
import org.sonar.server.source.index.FileSourceTesting;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexSourceLinesStepTest extends BaseStepTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbClient dbClient = dbTester.getDbClient();

  @Override
  protected ComputationStep step() {
    SourceLineIndexer sourceLineIndexer = new SourceLineIndexer(dbClient, esTester.client());
    sourceLineIndexer.setEnabled(true);
    return new IndexSourceLinesStep(sourceLineIndexer, treeRootHolder);
  }

  @Test
  public void index_source() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index_source.xml");
    try (Connection connection = dbTester.openConnection()) {
      FileSourceTesting.updateDataColumn(connection, "FILE1_UUID", FileSourceTesting.newRandomData(1).build());
    }

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey("PROJECT_KEY").build());

    step().execute();

    List<SearchHit> docs = esTester.getDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE);
    assertThat(docs).hasSize(1);
    SourceLineDoc doc = new SourceLineDoc(docs.get(0).sourceAsMap());
    assertThat(doc.projectUuid()).isEqualTo("ABCD");
    assertThat(doc.fileUuid()).isEqualTo("FILE1_UUID");
  }
}
