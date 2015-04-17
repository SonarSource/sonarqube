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

import org.sonar.server.source.db.FileSourceDao;

import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.source.db.FileSourceTesting;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IndexSourceLinesStepTest extends BaseStepTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  DbClient dbClient;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(null));
  }

  @Override
  protected ComputationStep step() throws IOException {
    SourceLineIndexer sourceLineIndexer = new SourceLineIndexer(dbClient, esTester.client());
    sourceLineIndexer.setEnabled(true);
    return new IndexSourceLinesStep(sourceLineIndexer);
  }

  @Test
  public void supported_project_qualifiers() throws Exception {
    assertThat(step().supportedProjectQualifiers()).containsOnly(Qualifiers.PROJECT);
  }

  @Test
  public void index_source() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index_source.xml");
    Connection connection = dbTester.openConnection();
    FileSourceTesting.updateDataColumn(connection, "FILE1_UUID", FileSourceTesting.newRandomData(1).build());
    connection.close();

    step().execute(new ComputationContext(mock(BatchReportReader.class), ComponentTesting.newProjectDto("ABCD")));

    List<SearchHit> docs = esTester.getDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE);
    assertThat(docs).hasSize(1);
    SourceLineDoc doc = new SourceLineDoc(docs.get(0).sourceAsMap());
    assertThat(doc.projectUuid()).isEqualTo("ABCD");
    assertThat(doc.fileUuid()).isEqualTo("FILE1_UUID");
  }
}
