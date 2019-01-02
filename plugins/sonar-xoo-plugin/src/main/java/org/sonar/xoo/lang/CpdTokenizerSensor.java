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
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.xoo.Xoo;

/**
 * Tokenize files for CPD
 */
public class CpdTokenizerSensor implements Sensor {

  private void tokenize(InputFile inputFile, SensorContext context) {
    int lineIdx = 1;
    NewCpdTokens newCpdTokens = context.newCpdTokens().onFile(inputFile);
    try {
      StringBuilder sb = new StringBuilder();
      for (String line : FileUtils.readLines(inputFile.file(), inputFile.charset())) {
        int startOffset = 0;
        int endOffset = 0;
        for (int i = 0; i < line.length(); i++) {
          char c = line.charAt(i);
          if (Character.isWhitespace(c)) {
            if (sb.length() > 0) {
              newCpdTokens.addToken(inputFile.newRange(lineIdx, startOffset, lineIdx, endOffset), sb.toString());
              sb.setLength(0);
            }
            startOffset = endOffset;
          } else {
            sb.append(c);
          }
          endOffset++;
        }
        if (sb.length() > 0) {
          newCpdTokens.addToken(inputFile.newRange(lineIdx, startOffset, lineIdx, endOffset), sb.toString());
          sb.setLength(0);
        }
        lineIdx++;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to tokenize", e);
    }
    newCpdTokens.save();
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Cpd Tokenizer Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicates p = context.fileSystem().predicates();
    for (InputFile file : context.fileSystem().inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.MAIN)))) {
      tokenize(file, context);
    }
  }
}
