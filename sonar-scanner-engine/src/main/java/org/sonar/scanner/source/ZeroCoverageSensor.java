/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Set;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.report.ReportPublisher;

@Phase(name = Phase.Name.POST)
public final class ZeroCoverageSensor implements ProjectSensor {

  private final ReportPublisher reportPublisher;

  public ZeroCoverageSensor(ReportPublisher reportPublisher) {
    this.reportPublisher = reportPublisher;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Zero Coverage Sensor");
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().hasType(Type.MAIN))) {
      if (((DefaultInputFile) f).isExcludedForCoverage()) {
        continue;
      }
      if (!isCoverageAlreadyDefined(f)) {
        ((DefaultInputFile) f).getExecutableLines().ifPresent(execLines -> {
          storeZeroCoverageForEachExecutableLine(context, f, execLines);
        });
      }
    }
  }

  private static void storeZeroCoverageForEachExecutableLine(final SensorContext context, InputFile f, Set<Integer> executableLines) {
    NewCoverage newCoverage = context.newCoverage().onFile(f);
    for (Integer lineIdx : executableLines) {
      if (lineIdx <= f.lines()) {
        newCoverage.lineHits(lineIdx, 0);
      }
    }
    newCoverage.save();
  }

  private boolean isCoverageAlreadyDefined(InputFile f) {
    return reportPublisher.getReader().hasCoverage(((DefaultInputFile) f).scannerId());
  }

}
