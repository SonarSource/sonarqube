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
 */
public interface SensorContext extends org.sonar.api.batch.sensor.SensorContext {

  /**
   * Indexes a resource as a direct child of project. This method does nothing and returns true if the resource already indexed.
   *
   * @return false if the resource is excluded
   * @deprecated since 4.2 Resource indexing is done by the platform for all physical resources.
   */
  @Deprecated
  boolean index(Resource resource);

  /**
   * Indexes a resource. This method does nothing if the resource is already indexed.
   *
   * @param resource        the resource to index. Not nullable
   * @param parentReference a reference to the parent. If null, the the resource is indexed as a direct child of project.
   * @return false if the parent is not indexed or if the resource is excluded
   * @deprecated since 4.2 Resource indexing is done by the platform for all physical resources.
   */
  @Deprecated
  boolean index(Resource resource, Resource parentReference);

  /**
   * Returns true if the referenced resource is indexed and excluded.
   *
   * @since 2.6
   * @deprecated since 4.2 Excluded resources are not indexed.
   */
  @Deprecated
  boolean isExcluded(Resource reference);

  /**
   * Returns true if the referenced resource is indexed.
   *
   * @since 2.6
   * @deprecated since 4.2 Excluded resources are not indexed.
   */
  @Deprecated
  boolean isIndexed(Resource reference, boolean acceptExcluded);

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
   * </p>
   */
  Measure saveMeasure(Resource resource, Metric metric, Double value);

  /**
   * Add or update a measure.
   * <p>
   * The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as
   * excluded.
   * </p>
   */
  Measure saveMeasure(Resource resource, Measure measure);

  // ----------- DEPENDENCIES BETWEEN RESOURCES --------------

  /**
   * @deprecated since 5.2 No more design features. No-op
   */
  @Deprecated
  Dependency saveDependency(Dependency dependency);

  // ----------- FILE SOURCES --------------

  /**
   * Save the source code of a file. The file must be have been indexed before.
   *
   * @throws org.sonar.api.resources.DuplicatedSourceException if the source has already been set on this resource
   * @since 1.10. Returns a boolean since 2.6.
   * @deprecated since 4.2 Source import is done by the platform
   */
  @Deprecated
  void saveSource(Resource reference, String source);

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
