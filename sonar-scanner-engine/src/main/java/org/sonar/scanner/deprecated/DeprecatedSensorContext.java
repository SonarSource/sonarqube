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
package org.sonar.scanner.deprecated;

import java.io.Serializable;
import java.util.Collection;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.scanner.index.DefaultIndex;
import org.sonar.scanner.sensor.DefaultSensorContext;
import org.sonar.scanner.sensor.coverage.CoverageExclusions;

public class DeprecatedSensorContext extends DefaultSensorContext implements SensorContext {

  private final DefaultIndex index;
  private final Project project;
  private final CoverageExclusions coverageFilter;

  public DeprecatedSensorContext(InputModule module, DefaultIndex index, Project project, Settings settings, FileSystem fs, ActiveRules activeRules,
    AnalysisMode analysisMode, CoverageExclusions coverageFilter, SensorStorage sensorStorage, SonarRuntime sonarRuntime) {
    super(module, settings, fs, activeRules, analysisMode, sensorStorage, sonarRuntime);
    this.index = index;
    this.project = project;
    this.coverageFilter = coverageFilter;

  }

  public Project getProject() {
    return project;
  }

  @Override
  public Resource getParent(Resource reference) {
    return index.getParent(reference);
  }

  @Override
  public Collection<Resource> getChildren(Resource reference) {
    return index.getChildren(reference);
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Metric<G> metric) {
    return index.getMeasure(project, metric);
  }

  @Override
  public <M> M getMeasures(MeasuresFilter<M> filter) {
    return index.getMeasures(project, filter);
  }

  @Override
  public Measure saveMeasure(Measure measure) {
    return index.addMeasure(project, measure);
  }

  @Override
  public Measure saveMeasure(Metric metric, Double value) {
    return index.addMeasure(project, new Measure(metric, value));
  }

  @Override
  public <G extends Serializable> Measure<G> getMeasure(Resource resource, Metric<G> metric) {
    return index.getMeasure(resource, metric);
  }

  @Override
  public String saveResource(Resource resource) {
    Resource persistedResource = index.addResource(resource);
    if (persistedResource != null) {
      return persistedResource.getEffectiveKey();
    }
    return null;
  }

  public boolean saveResource(Resource resource, Resource parentReference) {
    return index.index(resource, parentReference);
  }

  @Override
  public Resource getResource(Resource resource) {
    return index.getResource(resource);
  }

  @Override
  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    return index.getMeasures(resource, filter);
  }

  @Override
  public Measure saveMeasure(Resource resource, Metric metric, Double value) {
    Measure<?> measure = new Measure(metric, value);
    coverageFilter.validate(measure, resource.getPath());
    return saveMeasure(resource, measure);
  }

  @Override
  public Measure saveMeasure(Resource resource, Measure measure) {
    Resource resourceOrProject = resourceOrProject(resource);

    if (coverageFilter.accept(resourceOrProject, measure)) {
      return index.addMeasure(resourceOrProject, measure);
    } else {
      return measure;
    }
  }

  @Override
  public Dependency saveDependency(Dependency dependency) {
    return null;
  }

  private Resource resourceOrProject(Resource resource) {
    if (resource == null) {
      return project;
    }
    Resource indexedResource = getResource(resource);
    return indexedResource != null ? indexedResource : resource;
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Metric metric, Double value) {
    Measure<?> measure = new Measure(metric, value);
    coverageFilter.validate(measure, inputFile);
    return saveMeasure(getResource(inputFile), measure);
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Measure measure) {
    coverageFilter.validate(measure, inputFile);
    return saveMeasure(getResource(inputFile), measure);
  }

  @Override
  public Resource getResource(InputPath inputPath) {
    Resource r;
    if (inputPath instanceof InputDir) {
      r = Directory.create(((InputDir) inputPath).relativePath());
    } else if (inputPath instanceof InputFile) {
      r = File.create(((InputFile) inputPath).relativePath());
    } else {
      throw new IllegalArgumentException("Unknow input path type: " + inputPath);
    }
    return getResource(r);
  }
}
