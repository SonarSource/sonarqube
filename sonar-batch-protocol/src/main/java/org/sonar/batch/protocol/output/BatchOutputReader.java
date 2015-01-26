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

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.Collections;

public class BatchOutputReader {

  private final FileStructure fileStructure;

  public BatchOutputReader(File dir) {
    this.fileStructure = new FileStructure(dir);
  }

  public BatchOutput.ReportMetadata readMetadata() {
    File file = fileStructure.metadataFile();
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("Metadata file is missing in analysis report: " + file);
    }
    return ProtobufUtil.readFile(file, BatchOutput.ReportMetadata.PARSER);
  }

  @CheckForNull
  public BatchOutput.ReportComponent readComponent(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, componentRef);
    if (file.exists() && file.isFile()) {
      return ProtobufUtil.readFile(file, BatchOutput.ReportComponent.PARSER);
    }
    return null;
  }

  public Iterable<BatchOutput.ReportIssue> readComponentIssues(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    if (file.exists() && file.isFile()) {
      // all the issues are loaded in memory
      BatchOutput.ReportIssues issues = ProtobufUtil.readFile(file, BatchOutput.ReportIssues.PARSER);
      return issues.getListList();
    }
    return Collections.emptyList();
  }
}
