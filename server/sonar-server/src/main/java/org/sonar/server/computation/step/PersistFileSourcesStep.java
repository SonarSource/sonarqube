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

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.source.*;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class PersistFileSourcesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;

  public PersistFileSourcesStep(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    // Don't use batch insert for file_sources since keeping all data in memory can produce OOM for big files
    DbSession session = dbClient.openSession(false);
    try {
      final Map<String, FileSourceDto> previousFileSourcesByUuid = new HashMap<>();
      session.select("org.sonar.core.source.db.FileSourceMapper.selectHashesForProject", context.getProject().uuid(), new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          FileSourceDto dto = (FileSourceDto) context.getResultObject();
          previousFileSourcesByUuid.put(dto.getFileUuid(), dto);
        }
      });

      recursivelyProcessComponent(new FileSourcesContext(session, context, previousFileSourcesByUuid), rootComponentRef);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(FileSourcesContext fileSourcesContext, int componentRef) {
    BatchReportReader reportReader = fileSourcesContext.context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      LineIterator linesIterator = sourceLinesIterator(reportReader.readFileSource(componentRef));
      try {
        ComputeFileSourceData computeFileSourceData = new ComputeFileSourceData(linesIterator, dataLineReaders(reportReader, componentRef), component.getLines());
        ComputeFileSourceData.Data fileSourceData = computeFileSourceData.compute();
        persistSource(fileSourcesContext, fileSourceData, component);
      } finally {
        linesIterator.close();
      }
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(fileSourcesContext, childRef);
    }
  }

  private static LineIterator sourceLinesIterator(File file) {
    try {
      return IOUtils.lineIterator(FileUtils.openInputStream(file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to traverse file: " + file, e);
    }
  }

  private List<LineReader> dataLineReaders(BatchReportReader reportReader, int componentRef) {
    List<LineReader> lineReaders = newArrayList();

    File coverageFile = reportReader.readComponentCoverage(componentRef);
    BatchReport.Scm scmReport = reportReader.readComponentScm(componentRef);
    File highlightingFile = reportReader.readComponentSyntaxHighlighting(componentRef);

    lineReaders.add(coverageFile != null ? new CoverageLineReader(new ReportIterator<>(coverageFile, BatchReport.Coverage.PARSER)) : null);
    lineReaders.add(scmReport != null ? new ScmLineReader(scmReport) : null);
    lineReaders.add(highlightingFile != null ? new HighlightingLineReader(new ReportIterator<>(highlightingFile, BatchReport.SyntaxHighlighting.PARSER)) : null);

    Iterables.removeIf(lineReaders, Predicates.isNull());
    return lineReaders;
  }

  private void persistSource(FileSourcesContext fileSourcesContext, ComputeFileSourceData.Data fileSourceData, BatchReport.Component component) {
    FileSourceDb.Data fileData = fileSourceData.getFileSourceData();

    byte[] data = FileSourceDto.encodeData(fileData);
    String dataHash = DigestUtils.md5Hex(data);
    String srcHash = fileSourceData.getSrcHash();
    String lineHashes = fileSourceData.getLineHashes();
    FileSourceDto previousDto = fileSourcesContext.previousFileSourcesByUuid.get(component.getUuid());

    if (previousDto == null) {
      FileSourceDto dto = new FileSourceDto()
        .setProjectUuid(fileSourcesContext.context.getProject().uuid())
        .setFileUuid(component.getUuid())
        .setBinaryData(data)
        .setSrcHash(srcHash)
        .setDataHash(dataHash)
        .setLineHashes(lineHashes)
        .setCreatedAt(system2.now())
        // TODO set current date here when indexing sources in E/S will be done in this class
        .setUpdatedAt(0L);
      dbClient.fileSourceDao().insert(fileSourcesContext.session, dto);
      fileSourcesContext.session.commit();
    } else {
      // Update only if data_hash has changed or if src_hash is missing (progressive migration)
      boolean binaryDataUpdated = !dataHash.equals(previousDto.getDataHash());
      boolean srcHashUpdated = !srcHash.equals(previousDto.getSrcHash());
      if (binaryDataUpdated || srcHashUpdated) {
        previousDto
          .setBinaryData(data)
          .setDataHash(dataHash)
          .setSrcHash(srcHash)
          .setLineHashes(lineHashes);
        // Optimization only change updated at when updating binary data to avoid unecessary indexation by E/S
        if (binaryDataUpdated) {
          // TODO set current date here when indexing sources in E/S will be done in this class
          previousDto.setUpdatedAt(0L);
        }
        dbClient.fileSourceDao().update(previousDto);
        fileSourcesContext.session.commit();
      }
    }
  }

  private static class FileSourcesContext {
    DbSession session;
    ComputationContext context;
    Map<String, FileSourceDto> previousFileSourcesByUuid;

    public FileSourcesContext(DbSession session, ComputationContext context, Map<String, FileSourceDto> previousFileSourcesByUuid) {
      this.context = context;
      this.previousFileSourcesByUuid = previousFileSourcesByUuid;
      this.session = session;
    }
  }

  @Override
  public String getDescription() {
    return "Persist file sources";
  }
}
