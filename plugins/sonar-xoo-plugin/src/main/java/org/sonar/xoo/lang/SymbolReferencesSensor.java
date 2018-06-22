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
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

/**
 * Parse files *.xoo.symbol
 */
public class SymbolReferencesSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SymbolReferencesSensor.class);
  private static final String SYMBOL_EXTENSION = ".symbol";

  private void processFileSymbol(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File symbolFile = new File(ioFile.getParentFile(), ioFile.getName() + SYMBOL_EXTENSION);
    if (symbolFile.exists()) {
      LOG.debug("Processing " + symbolFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(symbolFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        NewSymbolTable symbolTable = context.newSymbolTable()
          .onFile(inputFile);

        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processLine(symbolFile, lineNumber, symbolTable, line);
        }
        symbolTable.save();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static void processLine(File symbolFile, int lineNumber, NewSymbolTable symbolTable, String line) {
    try {
      Iterator<String> split = Splitter.on(",").split(line).iterator();
      Iterator<String> symbolOffsets = Splitter.on(":").split(split.next()).iterator();

      int startOffset = Integer.parseInt(symbolOffsets.next());
      int endOffset = Integer.parseInt(symbolOffsets.next());
      int defaultLen = endOffset - startOffset;

      NewSymbol s = symbolTable.newSymbol(startOffset, endOffset);

      while (split.hasNext()) {
        addReference(s, split.next(), defaultLen);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + symbolFile.getAbsolutePath(), e);
    }
  }

  private static void addReference(NewSymbol s, String str, int defaultLen) {
    if (str.contains(":")) {
      Iterator<String> split = Splitter.on(":").split(str).iterator();
      int startOffset = Integer.parseInt(split.next());
      int toOffset = Integer.parseInt(split.next());

      s.newReference(startOffset, toOffset);
    } else {
      int startOffset = Integer.parseInt(str);
      s.newReference(startOffset, startOffset + defaultLen);
    }
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
