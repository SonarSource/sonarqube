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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.protocol.output.FileStructure;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistFileSourcesStepTest extends BaseStepTest {

  private static final int FILE_REF = 3;

  private static final String PROJECT_UUID = "PROJECT";
  private static final String FILE_UUID = "FILE";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  DbClient dbClient;

  System2 system2;

  PersistFileSourcesStep sut;

  long now = 123456789L;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(dbTester.myBatis()));

    reportDir = temp.newFolder();

    system2 = mock(System2.class);
    when(system2.now()).thenReturn(now);
    sut = new PersistFileSourcesStep(dbClient, system2);
  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void persist_sources() throws Exception {
    initBasicReport(2);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(fileSourceDto.getFileUuid()).isEqualTo(FILE_UUID);
    assertThat(fileSourceDto.getBinaryData()).isNotEmpty();
    assertThat(fileSourceDto.getDataHash()).isNotEmpty();
    assertThat(fileSourceDto.getLineHashes()).isNotEmpty();
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(now);
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(0L);

    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());
    assertThat(data.getLinesCount()).isEqualTo(2);
    assertThat(data.getLines(0).getLine()).isEqualTo(1);
    assertThat(data.getLines(0).getSource()).isEqualTo("line1");
    assertThat(data.getLines(1).getLine()).isEqualTo(2);
    assertThat(data.getLines(1).getSource()).isEqualTo("line2");
  }

  @Test
  public void persist_last_line() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    FileUtils.writeLines(writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, FILE_REF), Lists.newArrayList("line1", "line2"));
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(FILE_REF)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setType(Constants.ComponentType.FILE)
      .setUuid(FILE_UUID)
      // Lines is set to 3 but only 2 lines are read from the file -> the last lines should be added
      .setLines(3)
      .build());

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());
    assertThat(data.getLinesCount()).isEqualTo(3);
    assertThat(data.getLines(2).getLine()).isEqualTo(3);
    assertThat(data.getLines(2).getSource()).isEmpty();
  }

  @Test
  public void persist_source_hashes() throws Exception {
    initBasicReport(2);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select("FILE");
    assertThat(fileSourceDto.getLineHashes()).isEqualTo("137f72c3708c6bd0de00a0e5a69c699b\ne6251bcf1a7dc3ba5e7933e325bbe605\n");
    assertThat(fileSourceDto.getSrcHash()).isEqualTo("4fcc82a88ee38e0aa16c17f512c685c9");
  }

  @Test
  public void persist_coverage() throws Exception {
    BatchReportWriter writer = initBasicReport(1);

    writer.writeComponentCoverage(FILE_REF, newArrayList(BatchReport.Coverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setUtHits(true)
      .setUtCoveredConditions(2)
      .setItHits(true)
      .setItCoveredConditions(3)
      .setOverallCoveredConditions(4)
      .build()));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());

    assertThat(data.getLinesList()).hasSize(1);

    assertThat(data.getLines(0).getUtLineHits()).isEqualTo(1);
    assertThat(data.getLines(0).getUtConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getUtCoveredConditions()).isEqualTo(2);
    assertThat(data.getLines(0).hasItLineHits()).isTrue();
    assertThat(data.getLines(0).getItConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getItCoveredConditions()).isEqualTo(3);
    assertThat(data.getLines(0).getOverallLineHits()).isEqualTo(1);
    assertThat(data.getLines(0).getOverallConditions()).isEqualTo(10);
    assertThat(data.getLines(0).getOverallCoveredConditions()).isEqualTo(4);
  }

  @Test
  public void persist_scm() throws Exception {
    BatchReportWriter writer = initBasicReport(1);

    writer.writeComponentScm(BatchReport.Scm.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build());

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());

    assertThat(data.getLinesList()).hasSize(1);

    assertThat(data.getLines(0).getScmAuthor()).isEqualTo("john");
    assertThat(data.getLines(0).getScmDate()).isEqualTo(123456789L);
    assertThat(data.getLines(0).getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void persist_highlighting() throws Exception {
    BatchReportWriter writer = initBasicReport(1);

    writer.writeComponentSyntaxHighlighting(FILE_REF, newArrayList(BatchReport.SyntaxHighlighting.newBuilder()
      .setRange(BatchReport.Range.newBuilder()
        .setStartLine(1).setEndLine(1)
        .setStartOffset(2).setEndOffset(4)
        .build())
      .setType(Constants.HighlightingType.ANNOTATION)
      .build()
      ));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());

    assertThat(data.getLinesList()).hasSize(1);

    assertThat(data.getLines(0).getHighlighting()).isEqualTo("2,4,a");
  }

  @Test
  public void persist_symbols() throws Exception {
    BatchReportWriter writer = initBasicReport(3);

    writer.writeComponentSymbols(FILE_REF, newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(1).setEndOffset(3)
          .build()
        ).build()
      ));

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    FileSourceDb.Data data = FileSourceDto.decodeData(fileSourceDto.getBinaryData());

    assertThat(data.getLinesList()).hasSize(3);

    assertThat(data.getLines(0).getSymbols()).isEqualTo("2,4,1");
    assertThat(data.getLines(1).getSymbols()).isEmpty();
    assertThat(data.getLines(2).getSymbols()).isEqualTo("1,3,1");
  }

  @Test
  public void not_update_sources_when_nothing_has_changed() throws Exception {
    // Existing sources
    long past = 150000L;
    String srcHash = "1ddab9058a07abc0db2605ab02a61a00";
    String lineHashes = "137f72c3708c6bd0de00a0e5a69c699b\n";
    String dataHash = "29f25900140c94db38035128cb6de6a2";

    dbClient.fileSourceDao().insert(session, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSrcHash(srcHash)
      .setLineHashes(lineHashes)
      .setDataHash(dataHash)
      .setBinaryData(FileSourceDto.encodeData(FileSourceDb.Data.newBuilder()
        .addLines(FileSourceDb.Line.newBuilder()
          .setLine(1)
          .setSource("line1")
          .build())
        .build()))
      .setCreatedAt(past)
      .setUpdatedAt(past));
    session.commit();

    // Sources from the report
    initBasicReport(1);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    assertThat(fileSourceDto.getSrcHash()).isEqualTo(srcHash);
    assertThat(fileSourceDto.getLineHashes()).isEqualTo(lineHashes);
    assertThat(fileSourceDto.getDataHash()).isEqualTo(dataHash);
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(past);
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(past);
  }

  @Test
  public void update_sources_when_source_updated() throws Exception {
    // Existing sources
    long past = 150000L;
    dbClient.fileSourceDao().insert(session, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSrcHash("5b4bd9815cdb17b8ceae19eb1810c34c")
      .setLineHashes("6438c669e0d0de98e6929c2cc0fac474\n")
      .setDataHash("6cad150e3d065976c230cddc5a09efaa")
      .setBinaryData(FileSourceDto.encodeData(FileSourceDb.Data.newBuilder()
        .addLines(FileSourceDb.Line.newBuilder()
          .setLine(1)
          .setSource("old line")
          .build())
        .build()))
      .setCreatedAt(past)
      .setUpdatedAt(past));
    session.commit();

    initBasicReport(1);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(past);
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(0L);
  }

  @Test
  public void update_sources_when_src_hash_is_missing() throws Exception {
    // Existing sources
    long past = 150000L;
    dbClient.fileSourceDao().insert(session, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      // Source hash is missing, update will be made
      .setLineHashes("137f72c3708c6bd0de00a0e5a69c699b\n")
      .setDataHash("29f25900140c94db38035128cb6de6a2")
      .setBinaryData(FileSourceDto.encodeData(FileSourceDb.Data.newBuilder()
        .addLines(FileSourceDb.Line.newBuilder()
          .setLine(1)
          .setSource("line")
          .build())
        .build()))
      .setCreatedAt(past)
      .setUpdatedAt(past));
    session.commit();

    initBasicReport(1);

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID)));

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(1);
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().select(FILE_UUID);
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(past);
    // Updated at is not updated to not reindex the file source in E/S as the src hash is not indexed
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(past);
    assertThat(fileSourceDto.getSrcHash()).isEqualTo("1ddab9058a07abc0db2605ab02a61a00");
  }

  private BatchReportWriter initBasicReport(int numberOfLines) throws IOException {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("MODULE")
      .addChildRef(FILE_REF)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setType(Constants.ComponentType.FILE)
      .setUuid(FILE_UUID)
      .setLines(numberOfLines)
      .build());

    List<String> lines = newArrayList();
    for (int i = 1; i <= numberOfLines; i++) {
      lines.add("line" + i);
    }
    FileUtils.writeLines(writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, FILE_REF), lines);

    return writer;
  }

}
