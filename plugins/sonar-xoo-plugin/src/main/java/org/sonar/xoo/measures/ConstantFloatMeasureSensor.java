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

package org.sonar.xoo.measures;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.xoo.Xoo;

/**
 * Save a constant float measure on each XOO source file
 */
public class ConstantFloatMeasureSensor implements Sensor {

  public static final String SONAR_XOO_ENABLE_FLOAT_SENSOR = "sonar.xoo.enableFloatSensor";
  public static final String SONAR_XOO_FLOAT_PRECISION = "sonar.xoo.floatPrecision";

  public static final double CONSTANT_VALUE = 1.2345678910111213d;

  private final FileSystem fs;
  private final Settings settings;

  public ConstantFloatMeasureSensor(FileSystem fs, Settings settings) {
    this.fs = fs;
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguage(Xoo.KEY)) && settings.getBoolean(
      SONAR_XOO_ENABLE_FLOAT_SENSOR);
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    Measure<?> floatMeasure = settings.hasKey(SONAR_XOO_FLOAT_PRECISION)
      ? new Measure<>(XooMetrics.CONSTANT_FLOAT_MEASURE, CONSTANT_VALUE, settings.getInt(SONAR_XOO_FLOAT_PRECISION))
      : new Measure<>(XooMetrics.CONSTANT_FLOAT_MEASURE, CONSTANT_VALUE);
    for (InputFile inputFile : getSourceFiles()) {
      context.saveMeasure(inputFile, floatMeasure);
    }
  }

  private Iterable<InputFile> getSourceFiles() {
    return fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Xoo.KEY), fs.predicates().hasType(InputFile.Type.MAIN)));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
