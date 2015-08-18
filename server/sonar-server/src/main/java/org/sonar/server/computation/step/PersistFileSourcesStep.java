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
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.source.FileSourceDto.Type;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.source.ComputeFileSourceData;
import org.sonar.server.computation.source.CoverageLineReader;
import org.sonar.server.computation.source.DuplicationLineReader;
import org.sonar.server.computation.source.HighlightingLineReader;
import org.sonar.server.computation.source.LineReader;
import org.sonar.server.computation.source.ScmLineReader;
import org.sonar.server.computation.source.SymbolsLineReader;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistFileSourcesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;

  public PersistFileSourcesStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    // Don't use batch insert for file_sources since keeping all data in memory can produce OOM for big files
    DbSession session = dbClient.openSession(false);
    try {
      new DepthTraversalTypeAwareCrawler(new FileSourceVisitor(session))
        .visit(treeRootHolder.getRoot());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private class FileSourceVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;

    private Map<String, FileSourceDto> previousFileSourcesByUuid = new HashMap<>();
    private String projectUuid;

    private FileSourceVisitor(DbSession session) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitProject(Component project) {
      this.projectUuid = project.getUuid();
      session.select("org.sonar.db.source.FileSourceMapper.selectHashesForProject", ImmutableMap.of("projectUuid", projectUuid, "dataType", Type.SOURCE),
        new ResultHandler() {
          @Override
          public void handleResult(ResultContext context) {
            FileSourceDto dto = (FileSourceDto) context.getResultObject();
            previousFileSourcesByUuid.put(dto.getFileUuid(), dto);
          }
        });
    }

    @Override
    public void visitFile(Component file) {
      int fileRef = file.getReportAttributes().getRef();
      BatchReport.Component component = reportReader.readComponent(fileRef);
      CloseableIterator<String> linesIterator = reportReader.readFileSource(fileRef);
      LineReaders lineReaders = new LineReaders(reportReader, fileRef);
      try {
        ComputeFileSourceData computeFileSourceData = new ComputeFileSourceData(linesIterator, lineReaders.readers(), component.getLines());
        ComputeFileSourceData.Data fileSourceData = computeFileSourceData.compute();
        persistSource(fileSourceData, file.getUuid());
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Cannot persist sources of %s", file.getKey()), e);
      } finally {
        linesIterator.close();
        lineReaders.close();
      }
    }

    private void persistSource(ComputeFileSourceData.Data fileSourceData, String componentUuid) {
      DbFileSources.Data fileData = fileSourceData.getFileSourceData();

      byte[] data = FileSourceDto.encodeSourceData(fileData);
      String dataHash = DigestUtils.md5Hex(data);
      String srcHash = fileSourceData.getSrcHash();
      String lineHashes = fileSourceData.getLineHashes();
      FileSourceDto previousDto = previousFileSourcesByUuid.get(componentUuid);

      if (previousDto == null) {
        FileSourceDto dto = new FileSourceDto()
          .setProjectUuid(projectUuid)
          .setFileUuid(componentUuid)
          .setDataType(Type.SOURCE)
          .setBinaryData(data)
          .setSrcHash(srcHash)
          .setDataHash(dataHash)
          .setLineHashes(lineHashes)
          .setCreatedAt(system2.now())
          .setUpdatedAt(system2.now());
        dbClient.fileSourceDao().insert(session, dto);
        session.commit();
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
          session.commit();
        }
      }
    }
  }

  private static class LineReaders {
    private final List<LineReader> readers = new ArrayList<>();
    private final List<CloseableIterator<?>> closeables = new ArrayList<>();

    LineReaders(BatchReportReader reportReader, int componentRef) {
      CloseableIterator<BatchReport.Coverage> coverageIt = reportReader.readComponentCoverage(componentRef);
      closeables.add(coverageIt);
      readers.add(new CoverageLineReader(coverageIt));

      BatchReport.Changesets scmReport = reportReader.readChangesets(componentRef);
      if (scmReport != null) {
        readers.add(new ScmLineReader(scmReport));
      }

      CloseableIterator<BatchReport.SyntaxHighlighting> highlightingIt = reportReader.readComponentSyntaxHighlighting(componentRef);
      closeables.add(highlightingIt);
      readers.add(new HighlightingLineReader(highlightingIt));

      CloseableIterator<BatchReport.Symbol> symbolsIt = reportReader.readComponentSymbols(componentRef);
      closeables.add(symbolsIt);
      readers.add(new SymbolsLineReader(symbolsIt));

      CloseableIterator<BatchReport.Duplication> duplicationsIt = reportReader.readComponentDuplications(componentRef);
      closeables.add(duplicationsIt);
      readers.add(new DuplicationLineReader(duplicationsIt));
    }

    List<LineReader> readers() {
      return readers;
    }

    void close() {
      for (CloseableIterator<?> reportIterator : closeables) {
        reportIterator.close();
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist file sources";
  }
}
