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
package org.sonar.xoo.rule;

import java.util.Iterator;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

/**
 * This sensor will create and save highlighting twice on the first file that it finds in the index.
 * It requires the property sonar.it.savedatatwice
 */
public class SaveDataTwiceSensor implements Sensor {

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("SaveDataTwice IT Sensor ")
      .requireProperty("sonar.it.savedatatwice");
  }

  @Override
  public void execute(SensorContext context) {
    Iterator<InputFile> inputFiles = context.fileSystem().inputFiles(context.fileSystem().predicates().all()).iterator();

    if (!inputFiles.hasNext()) {
      throw new IllegalStateException("No files indexed");
    }

    InputFile file = inputFiles.next();
    context.newHighlighting()
      .onFile(file)
      .highlight(file.selectLine(1), TypeOfText.CONSTANT)
      .save();

    context.newHighlighting()
      .onFile(file)
      .highlight(file.selectLine(file.lines()), TypeOfText.COMMENT)
      .save();
  }
}
