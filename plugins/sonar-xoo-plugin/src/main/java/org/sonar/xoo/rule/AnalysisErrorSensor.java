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
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

public class AnalysisErrorSensor implements Sensor {
  private static final Logger LOG = Loggers.get(AnalysisErrorSensor.class);
  private static final String ERROR_EXTENSION = ".error";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Analysis Error Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileError(file, context);
    }
  }

  private void processFileError(InputFile inputFile, SensorContext context) {
    Path ioFile = inputFile.file().toPath();
    Path errorFile = ioFile.resolveSibling(ioFile.getFileName() + ERROR_EXTENSION).toAbsolutePath();
    if (Files.exists(errorFile) && Files.isRegularFile(errorFile)) {
      LOG.debug("Processing " + errorFile.toString());
      try {
        List<String> lines = Files.readAllLines(errorFile, context.fileSystem().encoding());
        for (String line : lines) {
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processLine(line, inputFile, context);
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void processLine(String fileLine, InputFile inputFile, SensorContext context) {
    String[] textPointer = fileLine.split(",");
    if (textPointer.length != 3) {
      throw new IllegalStateException("Invalid format in error file");
    }

    try {
      int line = Integer.parseInt(textPointer[0]);
      int lineOffset = Integer.parseInt(textPointer[1]);
      String msg = textPointer[2];

      context.newAnalysisError()
        .onFile(inputFile)
        .at(inputFile.newPointer(line, lineOffset))
        .message(msg)
        .save();

    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid format in error file", e);
    }
  }

}
