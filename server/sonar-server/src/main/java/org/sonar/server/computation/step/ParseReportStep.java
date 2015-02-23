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

import org.apache.commons.io.FileUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchOutputReader;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.IssueComputation;
import org.sonar.server.db.DbClient;

import java.io.File;

public class ParseReportStep implements ComputationStep {

  private static final Logger LOG = Loggers.get(ParseReportStep.class);

  private final IssueComputation issueComputation;
  private final DbClient dbClient;
  private final TempFolder tempFolder;

  public ParseReportStep(IssueComputation issueComputation, DbClient dbClient, TempFolder tempFolder) {
    this.issueComputation = issueComputation;
    this.dbClient = dbClient;
    this.tempFolder = tempFolder;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT, Qualifiers.VIEW};
  }

  @Override
  public void execute(ComputationContext context) {
    File reportDir = tempFolder.newDir();
    try {
      // extract compressed report from database and uncompress it in temporary directory
      extractReport(context.getReportDto(), reportDir);

      // prepare parsing of report
      BatchOutputReader reader = new BatchOutputReader(reportDir);
      BatchReport.Metadata reportMetadata = reader.readMetadata();
      context.setReportMetadata(reportMetadata);

      // and parse!
      int rootComponentRef = reportMetadata.getRootComponentRef();
      recursivelyProcessComponent(reader, context, rootComponentRef);
      issueComputation.afterReportProcessing();

    } finally {
      FileUtils.deleteQuietly(reportDir);
    }
  }

  private void extractReport(AnalysisReportDto report, File toDir) {
    long startTime = System.currentTimeMillis();
    DbSession session = dbClient.openSession(false);
    try {
      dbClient.analysisReportDao().selectAndDecompressToDir(session, report.getId(), toDir);
    } finally {
      MyBatis.closeQuietly(session);
    }
    long stopTime = System.currentTimeMillis();
    LOG.info(String.format("Report extracted in %dms | uncompressed size=%s | project=%s",
      stopTime - startTime, FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(toDir)), report.getProjectKey()));
  }

  private void recursivelyProcessComponent(BatchOutputReader reportReader, ComputationContext context, int componentRef) {
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component != null) {
      issueComputation.processComponentIssues(context, component.getUuid(), reportReader.readComponentIssues(componentRef));
      for (Integer childRef : component.getChildRefsList()) {
        recursivelyProcessComponent(reportReader, context, childRef);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Digest analysis report";
  }
}
