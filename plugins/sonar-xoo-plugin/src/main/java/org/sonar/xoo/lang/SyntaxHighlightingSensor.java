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

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

import static java.lang.Integer.parseInt;

/**
 * Parse files *.xoo.highlighting
 */
public class SyntaxHighlightingSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SyntaxHighlightingSensor.class);
  private static final String HIGHLIGHTING_EXTENSION = ".highlighting";

  private void processFileHighlighting(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File highlightingFile = new File(ioFile.getParentFile(), ioFile.getName() + HIGHLIGHTING_EXTENSION);
    if (highlightingFile.exists()) {
      LOG.debug("Processing " + highlightingFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(highlightingFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        NewHighlighting highlighting = context.newHighlighting()
          .onFile(inputFile);
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processLine(highlightingFile, lineNumber, highlighting, line);
        }
        highlighting.save();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static void processLine(File highlightingFile, int lineNumber, NewHighlighting highlighting, String line) {
    try {
      String[] split = line.split(":");
      if (split.length == 3) {
        int startOffset = parseInt(split[0]);
        int endOffset = parseInt(split[1]);
        highlighting.highlight(startOffset, endOffset, TypeOfText.forCssClass(split[2]));
      } else if (split.length == 5) {
        int startLine = parseInt(split[0]);
        int startLineOffset = parseInt(split[1]);
        int endLine = parseInt(split[2]);
        int endLineOffset = parseInt(split[3]);
        highlighting.highlight(startLine, startLineOffset, endLine, endLineOffset, TypeOfText.forCssClass(split[4]));
      } else {
        throw new IllegalStateException("Illegal number of elements separated by ':'. " +
          "Must either be startOffset:endOffset:class (offset in whole file) or startLine:startLineOffset:endLine:endLineOffset:class");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + highlightingFile.getAbsolutePath(), e);
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Highlighting Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileHighlighting(file, context);
    }
  }
}
