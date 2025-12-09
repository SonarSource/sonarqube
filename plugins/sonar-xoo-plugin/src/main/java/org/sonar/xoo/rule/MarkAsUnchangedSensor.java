/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;

public class MarkAsUnchangedSensor implements Sensor {
  public static final String ACTIVATE_MARK_AS_UNCHANGED = "sonar.markAsUnchanged";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Mark As Unchanged Sensor")
      .onlyOnLanguages(Xoo.KEY, Xoo2.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ACTIVATE_MARK_AS_UNCHANGED).orElse(false))
      .processesFilesIndependently();
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile f : fs.inputFiles(p.all())) {
      context.markAsUnchanged(f);
    }
  }
}
