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
package org.sonar.api.batch;

import java.io.Serializable;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;

/**
 * @since 1.10
 * @deprecated since 5.6 use {@link org.sonar.api.batch.sensor.Sensor}
 */
@Deprecated
public interface SensorContext extends org.sonar.api.batch.sensor.SensorContext {

  /**
   * Search for an indexed resource.
   *
   * @param reference the resource reference
   * @return the indexed resource, null if it's not indexed
   * @since 1.10. Generic types since 2.6.
   */
  @CheckForNull
  <R extends Resource> R getResource(R reference);

  /**
   * @since 2.6
   */
  Resource getParent(Resource reference);

  /**
   * @since 2.6
   */

  Collection<Resource> getChildren(Resource reference);

  // ----------- MEASURES ON PROJECT --------------

  /**
   * @deprecated since 5.1 Sensors should not read but only save data
   */
  @Deprecated
  <G extends Serializable> Measure<G> getMeasure(Metric<G> metric);

  /**
   * @deprecated since 5.1 Sensors should not read but only save data
   */
  @Deprecated
  <M> M getMeasures(MeasuresFilter<M> filter);

  /**
   * Add a measure on project
   */
  Measure saveMeasure(Measure measure);

  /**
   * Add a measure on project
   */
  Measure saveMeasure(Metric metric, Double value);

  // ----------- MEASURES ON RESOURCES --------------

  /**
   * @deprecated since 5.1 Sensors should not read but only save data
   */
  @Deprecated
  <G extends Serializable> Measure<G> getMeasure(Resource resource, Metric<G> metric);

  /**
   * Key is updated when saving the resource.
   *
   * @return the key as saved in database. Null if the resource is set as excluded.
   * @deprecated use the methods index()
   */
  @Deprecated
  String saveResource(Resource resource);

  /**
   * @deprecated since 5.1 Sensors should not read but only save data
   */
  @Deprecated
  <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  /**
   * Add or update a measure.
   * <p>
   * The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as
   * excluded.
   * 
   */
  Measure saveMeasure(Resource resource, Metric metric, Double value);

  /**
   * Add or update a measure.
   * <p>
   * The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as
   * excluded.
   * 
   */
  Measure saveMeasure(Resource resource, Measure measure);

  // ----------- DEPENDENCIES BETWEEN RESOURCES --------------

  /**
   * @deprecated since 5.2 No more design features. No-op
   */
  @Deprecated
  Dependency saveDependency(Dependency dependency);

  /**
   * Save measure on {@link InputFile}
   * @since 4.2
   */
  Measure saveMeasure(InputFile inputFile, Metric metric, Double value);

  /**
   * Save measure on {@link InputFile}
   * @since 4.2
   */
  Measure saveMeasure(InputFile inputFile, Measure measure);

  /**
   * Allow to get {@link Resource} corresponding to provided {@link InputPath}.
   * @since 4.5.2
   */
  Resource getResource(InputPath inputPath);
}
