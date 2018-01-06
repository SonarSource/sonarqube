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
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

/**
 * Parse files *.xoo.symbol
 */
public class SymbolReferencesSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SymbolReferencesSensor.class);

  private static final String SYMBOL_EXTENSION = ".symbol";

  private ResourcePerspectives perspectives;

  public SymbolReferencesSensor(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  private void processFileSymbol(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File symbolFile = new File(ioFile.getParentFile(), ioFile.getName() + SYMBOL_EXTENSION);
    if (symbolFile.exists()) {
      LOG.debug("Processing " + symbolFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(symbolFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        Symbolizable symbolizable = perspectives.as(Symbolizable.class, inputFile);
        if (symbolizable != null) {
          Symbolizable.SymbolTableBuilder symbolTableBuilder = symbolizable.newSymbolTableBuilder();
          for (String line : lines) {
            lineNumber++;
            if (StringUtils.isBlank(line) || line.startsWith("#")) {
              continue;
            }
            processLine(symbolFile, lineNumber, symbolTableBuilder, line);
          }
          symbolizable.setSymbolTable(symbolTableBuilder.build());
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static void processLine(File symbolFile, int lineNumber, Symbolizable.SymbolTableBuilder symbolTableBuilder, String line) {
    try {
      Iterator<String> split = Splitter.on(",").split(line).iterator();

      Symbol s = addSymbol(symbolTableBuilder, split.next());
      while (split.hasNext()) {
        addReference(symbolTableBuilder, s, split.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + symbolFile.getAbsolutePath(), e);
    }
  }

  private static void addReference(Symbolizable.SymbolTableBuilder symbolTableBuilder, Symbol s, String str) {
    if (str.contains(":")) {
      Iterator<String> split = Splitter.on(":").split(str).iterator();
      int startOffset = Integer.parseInt(split.next());
      int toOffset = Integer.parseInt(split.next());
      symbolTableBuilder.newReference(s, startOffset, toOffset);
    } else {
      symbolTableBuilder.newReference(s, Integer.parseInt(str));
    }
  }

  private static Symbol addSymbol(Symbolizable.SymbolTableBuilder symbolTableBuilder, String str) {
    Iterator<String> split = Splitter.on(":").split(str).iterator();

    int startOffset = Integer.parseInt(split.next());
    int endOffset = Integer.parseInt(split.next());

    return symbolTableBuilder.newSymbol(startOffset, endOffset);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Symbol Reference Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileSymbol(file, context);
    }
  }
}
