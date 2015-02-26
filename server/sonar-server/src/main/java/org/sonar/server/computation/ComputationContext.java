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

package org.sonar.server.computation;

import org.sonar.batch.protocol.output.BatchOutputReader;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;

public class ComputationContext {

  private final BatchOutputReader reportReader;
  private final ComponentDto project;

  // cache of metadata as it's frequently accessed
  private final BatchReport.Metadata reportMetadata;

  public ComputationContext(BatchOutputReader reportReader, ComponentDto project) {
    this.reportReader = reportReader;
    this.project = project;
    this.reportMetadata = reportReader.readMetadata();
  }

  public BatchReport.Metadata getReportMetadata() {
    return reportMetadata;
  }

  public ComponentDto getProject() {
    return project;
  }

  public BatchOutputReader getReportReader() {
    return reportReader;
  }
}
