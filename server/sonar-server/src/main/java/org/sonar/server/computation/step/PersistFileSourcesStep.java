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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
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
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.computation.source.StreamLine;
import org.sonar.server.computation.source.StreamLineCoverage;
import org.sonar.server.computation.source.StreamLineScm;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
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
      LineIterator linesIterator = linesIterator(reportReader.readFileSource(componentRef));
      try {
        persistSource(fileSourcesContext, linesIterator, component, createStreamLines(reportReader, componentRef));
      } finally {
        linesIterator.close();
      }
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(fileSourcesContext, childRef);
    }
  }

  private List<StreamLine> createStreamLines(BatchReportReader reportReader, int componentRef) {
    List<StreamLine> streamLines = newArrayList();
    File coverageFile = reportReader.readFileCoverage(componentRef);
    if (coverageFile != null) {
      streamLines.add(new StreamLineCoverage(new ReportIterator<>(coverageFile, BatchReport.Coverage.PARSER)));
    }
    BatchReport.Scm scmReport = reportReader.readComponentScm(componentRef);
    if (scmReport != null) {
      streamLines.add(new StreamLineScm(scmReport));
    }
    return streamLines;
  }

  private void persistSource(FileSourcesContext fileSourcesContext, Iterator<String> linesIterator, BatchReport.Component component, List<StreamLine> streamLines) {
    StringBuilder lineHashes = new StringBuilder();
    MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
    FileSourceDb.Data fileData = computeData(component.getLines(), linesIterator, streamLines, lineHashes, globalMd5Digest);

    byte[] data = FileSourceDto.encodeData(fileData);
    String dataHash = DigestUtils.md5Hex(data);
    String srcHash = Hex.encodeHexString(globalMd5Digest.digest());
    FileSourceDto previousDto = fileSourcesContext.previousFileSourcesByUuid.get(component.getUuid());

    if (previousDto == null) {
      FileSourceDto dto = new FileSourceDto()
        .setProjectUuid(fileSourcesContext.context.getProject().uuid())
        .setFileUuid(component.getUuid())
        .setBinaryData(data)
        .setSrcHash(srcHash)
        .setDataHash(dataHash)
        .setLineHashes(lineHashes.toString())
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
          .setLineHashes(lineHashes.toString());
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

  private FileSourceDb.Data computeData(int lines, Iterator<String> linesIterator, List<StreamLine> streamLines, StringBuilder lineHashes, MessageDigest globalMd5Digest) {
    FileSourceDb.Data.Builder fileSourceBuilder = FileSourceDb.Data.newBuilder();
    int lineIndex = 0;
    while (linesIterator.hasNext()) {
      lineIndex++;
      processLine(lineIndex, linesIterator.next(), fileSourceBuilder, streamLines, lineHashes, globalMd5Digest);
    }
    // Process last line
    if (lineIndex < lines) {
      processLine(lines, "", fileSourceBuilder, streamLines, lineHashes, globalMd5Digest);
    }
    return fileSourceBuilder.build();
  }

  private void processLine(int line, String sourceLine, FileSourceDb.Data.Builder fileSourceBuilder, List<StreamLine> streamLines,
    StringBuilder lineHashes, MessageDigest globalMd5Digest) {
    lineHashes.append(computeLineChecksum(sourceLine)).append("\n");
    globalMd5Digest.update((sourceLine + "\n").getBytes(UTF_8));

    FileSourceDb.Line.Builder lineBuilder = fileSourceBuilder.addLinesBuilder().setLine(line).setSource(sourceLine);
    for (StreamLine streamLine : streamLines) {
      streamLine.readLine(line, lineBuilder);
    }
  }

  private static String computeLineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, "\t ", "");
    if (reducedLine.isEmpty()) {
      return "";
    }
    return DigestUtils.md5Hex(reducedLine);
  }

  private static LineIterator linesIterator(File file) {
    try {
      return IOUtils.lineIterator(FileUtils.openInputStream(file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to traverse file: " + file, e);
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
