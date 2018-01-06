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
package org.sonar.xoo.lang;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Highlightable.HighlightingBuilder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

/**
 * Parse files *.xoo.highlighting
 */
public class SyntaxHighlightingSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SyntaxHighlightingSensor.class);

  private static final String HIGHLIGHTING_EXTENSION = ".highlighting";

  private final ResourcePerspectives perspectives;

  public SyntaxHighlightingSensor(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  private void processFileHighlighting(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File highlightingFile = new File(ioFile.getParentFile(), ioFile.getName() + HIGHLIGHTING_EXTENSION);
    if (highlightingFile.exists()) {
      LOG.debug("Processing " + highlightingFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(highlightingFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        Highlightable highlightable = perspectives.as(Highlightable.class, inputFile);
        if (highlightable != null) {
          HighlightingBuilder highlightingBuilder = highlightable.newHighlighting();
          for (String line : lines) {
            lineNumber++;
            if (StringUtils.isBlank(line) || line.startsWith("#")) {
              continue;
            }
            processLine(highlightingFile, lineNumber, highlightingBuilder, line);
          }
          highlightingBuilder.done();
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static void processLine(File highlightingFile, int lineNumber, HighlightingBuilder highlightingBuilder, String line) {
    try {
      Iterator<String> split = Splitter.on(":").split(line).iterator();
      int startOffset = Integer.parseInt(split.next());
      int endOffset = Integer.parseInt(split.next());
      highlightingBuilder.highlight(startOffset, endOffset, split.next());
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
