/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.xoo.rule;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.xoo.Xoo;

@Phase(name = Phase.Name.PRE)
public class NoSonarSensor implements Sensor {

  private NoSonarFilter noSonarFilter;

  public NoSonarSensor(NoSonarFilter noSonarFilter) {
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguage(Xoo.KEY))) {
      processFile(inputFile);
    }
  }

  private void processFile(InputFile inputFile) {
    try {
      Set<Integer> noSonarLines = new HashSet<>();
      int[] lineCounter = {1};
      try (Stream<String> stream = Files.lines(inputFile.path(), inputFile.charset())) {
        stream.forEachOrdered(lineStr -> {
          if (lineStr.contains("//NOSONAR")) {
            noSonarLines.add(lineCounter[0]);
          }
          lineCounter[0]++;
        });
      }
      noSonarFilter.noSonarInFile(inputFile, noSonarLines);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + inputFile, e);
    }
  }
}
