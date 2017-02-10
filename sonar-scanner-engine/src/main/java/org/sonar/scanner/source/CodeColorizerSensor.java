/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.source;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.report.ReportPublisher;

@Phase(name = Phase.Name.POST)
public final class CodeColorizerSensor implements Sensor {

  private final ReportPublisher reportPublisher;
  private final CodeColorizers codeColorizers;

  public CodeColorizerSensor(ReportPublisher reportPublisher, CodeColorizers codeColorizers) {
    this.reportPublisher = reportPublisher;
    this.codeColorizers = codeColorizers;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Code Colorizer Sensor");
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().all())) {
      ScannerReportReader reader = new ScannerReportReader(reportPublisher.getReportDir());
      DefaultInputFile inputFile = (DefaultInputFile) f;
      String language = f.language();
      if (reader.hasSyntaxHighlighting(inputFile.batchId()) || language == null) {
        continue;
      }
      codeColorizers.toSyntaxHighlighting(f.file(), fs.encoding(), language, context.newHighlighting().onFile(f));
    }
  }

}
