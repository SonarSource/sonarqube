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
package org.sonar.batch.protocol.output;

import org.sonar.batch.protocol.ProtobufUtil;

import java.io.File;

public class BatchReportWriter {

  private final FileStructure fileStructure;

  public BatchReportWriter(File dir) {
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalStateException("Unable to create directory: " + dir);
    }
    this.fileStructure = new FileStructure(dir);
  }

  FileStructure getFileStructure() {
    return fileStructure;
  }

  public boolean hasComponentData(FileStructure.Domain domain, int componentRef) {
    File file = fileStructure.fileFor(domain, componentRef);
    return file.exists() && file.isFile();
  }

  /**
   * Metadata is mandatory
   */
  public void writeMetadata(BatchReport.Metadata metadata) {
    ProtobufUtil.writeToFile(metadata, fileStructure.metadataFile());
  }

  public void writeComponent(BatchReport.Component component) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, component.getRef());
    ProtobufUtil.writeToFile(component, file);
  }

  public void writeComponentIssues(int componentRef, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
  }

  public void writeComponentMeasures(int componentRef, Iterable<BatchReport.Measure> measures) {
    BatchReport.Measures.Builder measuresBuilder = BatchReport.Measures.newBuilder();
    measuresBuilder.setComponentRef(componentRef);
    measuresBuilder.addAllMeasure(measures);
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    ProtobufUtil.writeToFile(measuresBuilder.build(), file);
  }

  /**
   * Issues on components which have been deleted are stored in another location.
   * Temporary hack, waiting for computation stack
   */
  public void writeDeletedComponentIssues(int componentRef, String componentUuid, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.setComponentUuid(componentUuid);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES_ON_DELETED, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
  }

}
