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

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceDto.Type;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.source.ComputeFileSourceData;
import org.sonar.server.computation.source.CoverageLineReader;
import org.sonar.server.computation.source.DuplicationLineReader;
import org.sonar.server.computation.source.HighlightingLineReader;
import org.sonar.server.computation.source.LineReader;
import org.sonar.server.computation.source.ScmLineReader;
import org.sonar.server.computation.source.SymbolsLineReader;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.util.CloseableIterator;

public class PersistFileSourcesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final DbComponentsRefCache dbComponentsRefCache;
  private final BatchReportReader reportReader;

  public PersistFileSourcesStep(DbClient dbClient, System2 system2, DbComponentsRefCache dbComponentsRefCache, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.dbComponentsRefCache = dbComponentsRefCache;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    int rootComponentRef = reportReader.readMetadata().getRootComponentRef();
    // Don't use batch insert for file_sources since keeping all data in memory can produce OOM for big files
    DbSession session = dbClient.openSession(false);
    try {
      final Map<String, FileSourceDto> previousFileSourcesByUuid = new HashMap<>();
      String projectUuid = dbComponentsRefCache.getByRef(rootComponentRef).getUuid();
      session.select("org.sonar.core.source.db.FileSourceMapper.selectHashesForProject", ImmutableMap.of("projectUuid", projectUuid, "dataType", Type.SOURCE),
        new ResultHandler() {
          @Override
          public void handleResult(ResultContext context) {
            FileSourceDto dto = (FileSourceDto) context.getResultObject();
            previousFileSourcesByUuid.put(dto.getFileUuid(), dto);
          }
        });

      recursivelyProcessComponent(new FileSourcesContext(session, previousFileSourcesByUuid, projectUuid), rootComponentRef);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(FileSourcesContext fileSourcesContext, int componentRef) {
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      CloseableIterator<String> linesIterator = reportReader.readFileSource(componentRef);
      LineReaders lineReaders = new LineReaders(reportReader, componentRef);
      try {
        ComputeFileSourceData computeFileSourceData = new ComputeFileSourceData(linesIterator, lineReaders.readers(), component.getLines());
        ComputeFileSourceData.Data fileSourceData = computeFileSourceData.compute();
        persistSource(fileSourcesContext, fileSourceData, component);
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Cannot persist sources of %s", component.getPath()), e);
      } finally {
        linesIterator.close();
        lineReaders.close();
      }
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(fileSourcesContext, childRef);
    }
  }

  private void persistSource(FileSourcesContext fileSourcesContext, ComputeFileSourceData.Data fileSourceData, BatchReport.Component component) {
    FileSourceDb.Data fileData = fileSourceData.getFileSourceData();

    byte[] data = FileSourceDto.encodeSourceData(fileData);
    String dataHash = DigestUtils.md5Hex(data);
    String srcHash = fileSourceData.getSrcHash();
    String lineHashes = fileSourceData.getLineHashes();
    String componentUuid = dbComponentsRefCache.getByRef(component.getRef()).getUuid();
    FileSourceDto previousDto = fileSourcesContext.previousFileSourcesByUuid.get(componentUuid);

    if (previousDto == null) {
      FileSourceDto dto = new FileSourceDto()
        .setProjectUuid(fileSourcesContext.projectUuid)
        .setFileUuid(componentUuid)
        .setDataType(Type.SOURCE)
        .setBinaryData(data)
        .setSrcHash(srcHash)
        .setDataHash(dataHash)
        .setLineHashes(lineHashes)
        .setCreatedAt(system2.now())
        .setUpdatedAt(system2.now());
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
        // Optimization only change updated at when updating binary data to avoid unnecessary indexation by E/S
        if (binaryDataUpdated) {
          previousDto.setUpdatedAt(system2.now());
        }
        dbClient.fileSourceDao().update(previousDto);
        fileSourcesContext.session.commit();
      }
    }
  }

  private static class FileSourcesContext {
    DbSession session;
    Map<String, FileSourceDto> previousFileSourcesByUuid;
    String projectUuid;

    public FileSourcesContext(DbSession session, Map<String, FileSourceDto> previousFileSourcesByUuid, String projectUuid) {
      this.previousFileSourcesByUuid = previousFileSourcesByUuid;
      this.session = session;
      this.projectUuid = projectUuid;
    }
  }

  private static class LineReaders {
    private final List<LineReader> readers = new ArrayList<>();
    private final List<CloseableIterator<?>> iterators = new ArrayList<>();

    LineReaders(BatchReportReader reportReader, int componentRef) {
      CloseableIterator<BatchReport.Coverage> coverageReportIterator = reportReader.readComponentCoverage(componentRef);
      BatchReport.Changesets scmReport = reportReader.readChangesets(componentRef);
      CloseableIterator<BatchReport.SyntaxHighlighting> highlightingIterator = reportReader.readComponentSyntaxHighlighting(componentRef);
      List<BatchReport.Symbols.Symbol> symbols = reportReader.readComponentSymbols(componentRef);
      List<BatchReport.Duplication> duplications = reportReader.readComponentDuplications(componentRef);

      if (coverageReportIterator != null) {
        iterators.add(coverageReportIterator);
        readers.add(new CoverageLineReader(coverageReportIterator));
      }
      if (scmReport != null) {
        readers.add(new ScmLineReader(scmReport));
      }
      if (highlightingIterator != null) {
        iterators.add(highlightingIterator);
        readers.add(new HighlightingLineReader(highlightingIterator));
      }
      if (!duplications.isEmpty()) {
        readers.add(new DuplicationLineReader(duplications));
      }
      if (!symbols.isEmpty()) {
        readers.add(new SymbolsLineReader(symbols));
      }
    }

    List<LineReader> readers() {
      return readers;
    }

    void close() {
      for (CloseableIterator<?> reportIterator : iterators) {
        reportIterator.close();
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist file sources";
  }
}
