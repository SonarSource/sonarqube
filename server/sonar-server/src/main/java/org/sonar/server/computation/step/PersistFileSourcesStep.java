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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
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
import java.util.Iterator;
import java.util.List;

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
      recursivelyProcessComponent(session, context, rootComponentRef);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(DbSession session, ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      LineIterator linesIterator = linesIterator(reportReader.readFileSource(componentRef));
      try {
        processFileSources(session, linesIterator, component, context.getProject().uuid(), loadStreamLines(reportReader, componentRef));
      } finally {
        linesIterator.close();
      }
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(session, context, childRef);
    }
  }

  private List<StreamLine> loadStreamLines(BatchReportReader reportReader, int componentRef) {
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

  private void processFileSources(DbSession session, Iterator<String> linesIterator, BatchReport.Component component, String projectUuid, List<StreamLine> streamLines) {
    Object[] dataWithLineHashes = processLines(component.getLines(), linesIterator, streamLines);
    byte[] data = (byte[]) dataWithLineHashes[0];
    String dataHash = DigestUtils.md5Hex(data);
    String lineHashes = (String) dataWithLineHashes[1];

    // TODO load previous source (from db or ES ?) in order to not update source each time
    FileSourceDto dto = new FileSourceDto()
      .setProjectUuid(projectUuid)
      .setFileUuid(component.getUuid())
      .setBinaryData(data)
      .setDataHash(dataHash)
      .setLineHashes(lineHashes)
      .setCreatedAt(system2.now())
      // TODO set current date here when indexing sources in E/S will be done in this class
      .setUpdatedAt(0L);
    dbClient.fileSourceDao().insert(session, dto);
    session.commit();
  }

  private Object[] processLines(int lines, Iterator<String> linesIterator, List<StreamLine> streamLines) {
    StringBuilder lineHashes = new StringBuilder();
    FileSourceDb.Data.Builder fileSourceBuilder = FileSourceDb.Data.newBuilder();
    int lineIndex = 0;
    while (linesIterator.hasNext()) {
      lineIndex++;
      processLine(lineIndex, linesIterator.next(), fileSourceBuilder, lineHashes, streamLines);
    }
    // Process last line
    if (lineIndex < lines) {
      processLine(lines, "", fileSourceBuilder, lineHashes, streamLines);
    }
    return new Object[] {FileSourceDto.encodeData(fileSourceBuilder.build()), lineHashes.toString()};
  }

  private void processLine(int line, String sourceLine, FileSourceDb.Data.Builder fileSourceBuilder, StringBuilder lineHashes, List<StreamLine> streamLines){
    FileSourceDb.Line.Builder lineBuilder = fileSourceBuilder.addLinesBuilder().setLine(line).setSource(sourceLine);
    lineHashes.append(lineChecksum(sourceLine)).append("\n");
    for (StreamLine streamLine : streamLines) {
      streamLine.readLine(line, lineBuilder);
    }
  }

  private static String lineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, "\t\n\r ", "");
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

  @Override
  public String getDescription() {
    return "Persist file sources";
  }
}
