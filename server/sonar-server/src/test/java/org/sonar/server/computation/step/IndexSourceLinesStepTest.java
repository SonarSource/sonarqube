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

import java.io.File;
import java.sql.Connection;
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.FileBatchReportReader;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.source.db.FileSourceDao;
import org.sonar.server.source.db.FileSourceTesting;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IndexSourceLinesStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  DbClient dbClient;

  DbComponentsRefCache dbComponentsRefCache;

  @Before
  public void setUp() {
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(null));
    dbComponentsRefCache = new DbComponentsRefCache();
  }

  @Override
  protected ComputationStep step() {
    SourceLineIndexer sourceLineIndexer = new SourceLineIndexer(dbClient, esTester.client());
    sourceLineIndexer.setEnabled(true);
    return new IndexSourceLinesStep(sourceLineIndexer, dbComponentsRefCache);
  }

  @Test
  public void index_source() throws Exception {
    dbComponentsRefCache.addComponent(1, new DbComponentsRefCache.DbComponent(1L, PROJECT_KEY, "ABCD"));

    dbTester.prepareDbUnit(getClass(), "index_source.xml");
    Connection connection = dbTester.openConnection();
    FileSourceTesting.updateDataColumn(connection, "FILE1_UUID", FileSourceTesting.newRandomData(1).build());
    connection.close();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    step().execute(new ComputationContext(new FileBatchReportReader(new BatchReportReader(reportDir)), PROJECT_KEY, new Settings(), dbClient,
        ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT), mock(LanguageRepository.class)));

    List<SearchHit> docs = esTester.getDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE);
    assertThat(docs).hasSize(1);
    SourceLineDoc doc = new SourceLineDoc(docs.get(0).sourceAsMap());
    assertThat(doc.projectUuid()).isEqualTo("ABCD");
    assertThat(doc.fileUuid()).isEqualTo("FILE1_UUID");
  }
}
