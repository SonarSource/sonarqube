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
package org.sonar.batch.source;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.ReportPublisher;

@Phase(name = Phase.Name.POST)
public final class CodeColorizerSensor implements Sensor {

  private final ReportPublisher reportPublisher;
  private final BatchComponentCache resourceCache;
  private final CodeColorizers codeColorizers;

  public CodeColorizerSensor(ReportPublisher reportPublisher, BatchComponentCache resourceCache, CodeColorizers codeColorizers) {
    this.reportPublisher = reportPublisher;
    this.resourceCache = resourceCache;
    this.codeColorizers = codeColorizers;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Code Colorizer Sensor")
      .disabledInIssues();
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().all())) {
      BatchReportReader reader = new BatchReportReader(reportPublisher.getReportDir());
      int batchId = resourceCache.get(f).batchId();
      String language = f.language();
      if (reader.hasSyntaxHighlighting(batchId) || language == null) {
        continue;
      }
      codeColorizers.toSyntaxHighlighting(f.file(), fs.encoding(), language, context.newHighlighting().onFile(f));
    }
  }

}
