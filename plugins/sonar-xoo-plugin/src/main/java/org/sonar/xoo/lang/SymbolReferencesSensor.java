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

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
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

import static java.lang.Integer.parseInt;

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
      String[] symbolOffsets = split.next().split(":");

      if (symbolOffsets.length == 2) {
        int startOffset = parseInt(symbolOffsets[0]);
        int endOffset = parseInt(symbolOffsets[1]);
        NewSymbol s = symbolTable.newSymbol(startOffset, endOffset);
        parseReferences(s, split, endOffset - startOffset);
      } else if (symbolOffsets.length == 4) {
        int startLine = parseInt(symbolOffsets[0]);
        int startLineOffset = parseInt(symbolOffsets[1]);
        int endLine = parseInt(symbolOffsets[2]);
        int endLineOffset = parseInt(symbolOffsets[3]);
        NewSymbol s = symbolTable.newSymbol(startLine, startLineOffset, endLine, endLineOffset);
        parseReferences(s, split, null);
      } else {
        throw new IllegalStateException("Illegal number of elements separated by ':'. " +
          "Must either be startOffset:endOffset (offset in whole file) or startLine:startLineOffset:endLine:endLineOffset");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + symbolFile.getAbsolutePath(), e);
    }
  }

  private static void parseReferences(NewSymbol s, Iterator<String> split, @Nullable Integer defaultLen) {
    while (split.hasNext()) {
      addReference(s, split.next(), defaultLen);
    }
  }

  private static void addReference(NewSymbol s, String str, @Nullable Integer defaultLen) {
    if (str.contains(":")) {
      String[] split = str.split(":");
      if (split.length == 2) {
        int startOffset = parseInt(split[0]);
        int endOffset = parseInt(split[1]);
        s.newReference(startOffset, endOffset);
      } else if (split.length == 4) {
        int startLine = parseInt(split[0]);
        int startLineOffset = parseInt(split[1]);
        int endLine = parseInt(split[2]);
        int endLineOffset = parseInt(split[3]);
        s.newReference(startLine, startLineOffset, endLine, endLineOffset);
      } else {
        throw new IllegalStateException("Illegal number of elements separated by ':'");
      }
    } else if (defaultLen == null) {
      throw new IllegalStateException("Mix of new and old format. Use startLine:startLineOffset:endLine:endLineOffset for references too");
    } else {
      int startOffset = parseInt(str);
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
