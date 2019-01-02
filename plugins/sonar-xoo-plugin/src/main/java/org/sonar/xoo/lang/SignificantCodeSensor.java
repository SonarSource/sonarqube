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
package org.sonar.xoo.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

public class SignificantCodeSensor implements Sensor {
  private static final Logger LOG = Loggers.get(SignificantCodeSensor.class);
  private static final String FILE_EXTENSION = ".significantCode";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Significant Code Ranges Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processSignificantCodeFile(file, context);
    }
  }

  private void processSignificantCodeFile(InputFile inputFile, SensorContext context) {
    Path ioFile = inputFile.path();
    Path significantCodeFile = ioFile.resolveSibling(ioFile.getFileName() + FILE_EXTENSION).toAbsolutePath();
    if (Files.exists(significantCodeFile) && Files.isRegularFile(significantCodeFile)) {
      LOG.debug("Processing " + significantCodeFile.toString());
      try {
        List<String> lines = Files.readAllLines(significantCodeFile, context.fileSystem().encoding());
        NewSignificantCode significantCode = context.newSignificantCode()
          .onFile(inputFile);
        for (String line : lines) {
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processLine(line, inputFile, significantCode);
        }
        significantCode.save();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void processLine(String fileLine, InputFile inputFile, NewSignificantCode significantCode) {
    String[] textPointer = fileLine.split(",");
    if (textPointer.length != 3) {
      throw new IllegalStateException("Invalid format in error file");
    }

    try {
      int line = Integer.parseInt(textPointer[0]);
      int startLineOffset = Integer.parseInt(textPointer[1]);
      int endLineOffset = Integer.parseInt(textPointer[2]);

      significantCode.addRange(inputFile.newRange(line, startLineOffset, line, endLineOffset));

    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid format in error file", e);
    }
  }

}
