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
package org.sonar.scanner.deprecated;

import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.config.Configuration;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.index.DefaultIndex;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.sensor.DefaultSensorContext;

public class DeprecatedSensorContext extends DefaultSensorContext implements SensorContext {
  private final DefaultIndex index;
  private final InputModule module;

  public DeprecatedSensorContext(InputModule module, DefaultIndex index, Configuration config, org.sonar.api.config.Settings mutableSettings,
    FileSystem fs, ActiveRules activeRules, AnalysisMode analysisMode, SensorStorage sensorStorage, SonarRuntime sonarRuntime,
    BranchConfiguration branchConfiguration) {
    super(module, config, mutableSettings, fs, activeRules, analysisMode, sensorStorage, sonarRuntime, branchConfiguration);
    this.index = index;
    this.module = module;
  }

  @Override
  public Resource getParent(Resource reference) {
    return index.getParent(getComponentKey(reference));
  }

  @Override
  public Collection<Resource> getChildren(Resource reference) {
    return index.getChildren(getComponentKey(reference));
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Metric<G> metric) {
    return index.getMeasure(module.key(), metric);
  }

  /**
   * Returns effective key of a resource, without branch.
   */
  private String getComponentKey(Resource r) {
    if (ResourceUtils.isProject(r) || /* For technical projects */ResourceUtils.isRootProject(r)) {
      return r.getKey();
    } else {
      return ComponentKeys.createEffectiveKey(module.key(), r);
    }
  }

  @Override
  public <M> M getMeasures(MeasuresFilter<M> filter) {
    return index.getMeasures(module.key(), filter);
  }

  @Override
  public Measure saveMeasure(Measure measure) {
    return index.addMeasure(module.key(), measure);
  }

  @Override
  public Measure saveMeasure(Metric metric, Double value) {
    return index.addMeasure(module.key(), new Measure(metric, value));
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Resource resource, Metric<G> metric) {
    return index.getMeasure(getComponentKey(resource), metric);
  }

  @Override
  public String saveResource(Resource resource) {
    throw new UnsupportedOperationException("No longer possible to save resources");
  }

  @Override
  public Resource getResource(Resource resource) {
    return index.getResource(getComponentKey(resource));
  }

  @Override
  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    return index.getMeasures(getComponentKey(resource), filter);
  }

  @Override
  public Measure saveMeasure(Resource resource, Metric metric, Double value) {
    Measure<?> measure = new Measure(metric, value);
    return saveMeasure(resource, measure);
  }

  @Override
  public Measure saveMeasure(@Nullable Resource resource, Measure measure) {
    Resource resourceOrProject = resourceOrProject(resource);
    return index.addMeasure(getComponentKey(resourceOrProject), measure);
  }

  @Override
  public Dependency saveDependency(Dependency dependency) {
    return null;
  }

  private Resource resourceOrProject(@Nullable Resource resource) {
    if (resource == null) {
      return index.getResource(module.key());
    }
    Resource indexedResource = getResource(resource);
    return indexedResource != null ? indexedResource : resource;
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Metric metric, Double value) {
    Measure<?> measure = new Measure(metric, value);
    return saveMeasure(inputFile, measure);
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Measure measure) {
    return index.addMeasure(inputFile.key(), measure);
  }

  @Override
  public Resource getResource(InputPath inputPath) {
    Resource r = index.toResource(inputPath);
    return getResource(r);
  }
}
