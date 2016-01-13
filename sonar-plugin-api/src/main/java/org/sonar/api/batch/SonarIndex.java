/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * @deprecated since 4.5.2 should not be used by plugins. Everything should be accessed using {@link SensorContext}.
 */
@Deprecated
public abstract class SonarIndex {

  /**
   * Indexes a resource as a direct child of project. This method does nothing and returns true if the resource already indexed.
   * If the method resource.getParent() does not return null, then this parent will be indexed too.
   *
   * @return false if the resource is excluded
   * @since 2.6
   */
  public abstract boolean index(Resource resource);

  /**
   * Indexes a resource. This method does nothing if the resource is already indexed.
   *
   * @param resource        the resource to index. Not nullable
   * @param parentReference a reference to the indexed parent. If null, the resource is indexed as a direct child of project.
   * @return false if the parent is not indexed or if the resource is excluded
   * @since 2.6
   */
  public abstract boolean index(Resource resource, Resource parentReference);

  /**
   * Returns true if the referenced resource is excluded. An excluded resource is not indexed.
   * @since 2.6
   */
  public abstract boolean isExcluded(Resource reference);

  /**
   * @since 2.6
   */
  public abstract boolean isIndexed(Resource reference, boolean acceptExcluded);

  /**
   * Search for an indexed resource.
   *
   * @param reference the resource reference
   * @return the indexed resource, null if it's not indexed
   * @since 1.10. Generic types since 2.6.
   */
  public abstract <R extends Resource> R getResource(R reference);

  /**
   * @since 2.6
   */
  public abstract Resource getParent(Resource reference);

  /**
   * @since 2.6
   */

  public abstract Collection<Resource> getChildren(Resource reference);

  /**
   * @return source code associated with a specified resource, <code>null</code> if not available 
   * (for example if resource is not a file)
   * @since 2.9
   * @deprecated since 5.0 sources are no more stored in SQ as a single blob. Use {@link InputFile#file()} to read file content from disk.
   */
  @Deprecated
  @CheckForNull
  public abstract String getSource(Resource resource);

  public abstract Project getProject();

  public abstract Collection<Resource> getResources();

  /**
   * Indexes the resource.
   * @return the indexed resource, even if it's excluded
   * @deprecated since 2.6. Use methods index()
   */
  @Deprecated
  public abstract Resource addResource(Resource resource);

  @CheckForNull
  public abstract Measure getMeasure(Resource resource, org.sonar.api.batch.measure.Metric<?> metric);

  @CheckForNull
  public abstract <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  /**
   * Warning: the resource is automatically indexed for backward-compatibility, but it should be explictly
   * indexed before. Next versions will deactivate this automatic indexation.
   */
  public abstract Measure addMeasure(Resource resource, Measure measure);

  /**
   * @deprecated since 5.2 No more design features. No op.
   */
  @Deprecated
  public abstract Dependency addDependency(Dependency dependency);
}
