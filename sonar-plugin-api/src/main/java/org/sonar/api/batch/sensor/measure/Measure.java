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
package org.sonar.api.batch.sensor.measure;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;

import javax.annotation.CheckForNull;

import java.io.Serializable;

/**
 * Builder to create new Measure.
 * @since 4.4
 */
@Beta
public interface Measure<G extends Serializable> {

  /**
   * The file the measure belongs to.
   */
  Measure<G> onFile(InputFile file);

  /**
   * Tell that the measure is global to the project.
   */
  Measure<G> onProject();

  /**
   * The file the measure belong to.
   * @return null if measure is on project
   */
  @CheckForNull
  InputFile inputFile();

  /**
   * Set the metric this measure belong to.
   */
  Measure<G> forMetric(Metric<G> metric);

  /**
   * The metric this measure belong to.
   */
  Metric<G> metric();

  /**
   * Value of the measure.
   */
  Measure<G> withValue(G value);

  /**
   * Value of the measure.
   */
  G value();

  /**
   * Save the measure. It is not permitted so save several measures of the same metric on the same file/project.
   */
  void save();

}
