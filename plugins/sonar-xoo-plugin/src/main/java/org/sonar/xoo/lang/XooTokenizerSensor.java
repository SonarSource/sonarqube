/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.xoo.Xoo;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tokenize xoo files (separator is whitespace) for duplication detection
 */
public class XooTokenizerSensor implements Sensor {

  private void computeTokens(InputFile inputFile, SensorContext context) {
    DuplicationTokenBuilder tokenBuilder = context.duplicationTokenBuilder(inputFile);
    File ioFile = inputFile.file();
    int lineId = 0;
    try {
      for (String line : FileUtils.readLines(ioFile)) {
        lineId++;
        for (String token : Splitter.on(" ").split(line)) {
          tokenBuilder.addToken(lineId, token);
        }
      }
      tokenBuilder.done();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file " + ioFile, e);
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Tokenizer Sensor")
      .onlyOnLanguages(Xoo.KEY)
      .onlyOnFileType(InputFile.Type.MAIN);
  }

  @Override
  public void execute(SensorContext context) {
    String[] cpdExclusions = context.settings().getStringArray(CoreProperties.CPD_EXCLUSIONS);
    FilePredicates p = context.fileSystem().predicates();
    List<InputFile> sourceFiles = Lists.newArrayList(context.fileSystem().inputFiles(p.and(
      p.hasType(InputFile.Type.MAIN),
      p.hasLanguage(Xoo.KEY),
      p.doesNotMatchPathPatterns(cpdExclusions)
      )));
    if (sourceFiles.isEmpty()) {
      return;
    }
    for (InputFile file : sourceFiles) {
      computeTokens(file, context);
    }
  }
}
