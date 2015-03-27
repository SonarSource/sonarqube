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
package org.sonar.batch.deprecated;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.*;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.sensor.DefaultSensorContext;
import org.sonar.batch.sensor.coverage.CoverageExclusions;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public class DeprecatedSensorContext extends DefaultSensorContext implements SensorContext {

  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedSensorContext.class);

  private final SonarIndex index;
  private final Project project;
  private final CoverageExclusions coverageFilter;

  public DeprecatedSensorContext(SonarIndex index, Project project, Settings settings, FileSystem fs, ActiveRules activeRules,
    AnalysisMode analysisMode, CoverageExclusions coverageFilter,
    SensorStorage sensorStorage) {
    super(settings, fs, activeRules, analysisMode, sensorStorage);
    this.index = index;
    this.project = project;
    this.coverageFilter = coverageFilter;
  }

  public Project getProject() {
    return project;
  }

  @Override
  public boolean index(Resource resource) {
    // SONAR-5006
    if (indexedByCore(resource)) {
      logWarning();
      return true;
    }
    return index.index(resource);
  }

  private boolean indexedByCore(Resource resource) {
    return StringUtils.equals(Qualifiers.DIRECTORY, resource.getQualifier()) ||
      StringUtils.equals(Qualifiers.FILE, resource.getQualifier());
  }

  @Override
  public boolean index(Resource resource, Resource parentReference) {
    // SONAR-5006
    if (indexedByCore(resource)) {
      logWarning();
      return true;
    }
    return index.index(resource, parentReference);
  }

  private void logWarning() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Plugins are no more responsible for indexing physical resources like directories and files. This is now handled by the platform.", new SonarException(
        "Plugin should not index physical resources"));
    }
  }

  @Override
  public boolean isExcluded(Resource reference) {
    return index.isExcluded(reference);
  }

  @Override
  public boolean isIndexed(Resource reference, boolean acceptExcluded) {
    return index.isIndexed(reference, acceptExcluded);
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
    return saveMeasure(resource, new Measure(metric, value));
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
  public void saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resourceOrProject(violation.getResource()));
    }
    index.addViolation(violation, force);
  }

  @Override
  public void saveViolation(Violation violation) {
    saveViolation(violation, false);
  }

  @Override
  public void saveViolations(Collection<Violation> violations) {
    if (violations != null) {
      for (Violation violation : violations) {
        saveViolation(violation);
      }
    }
  }

  @Override
  public Dependency saveDependency(Dependency dependency) {
    return index.addDependency(dependency);
  }

  @Override
  public Set<Dependency> getDependencies() {
    return index.getDependencies();
  }

  @Override
  public Collection<Dependency> getIncomingDependencies(Resource to) {
    return index.getIncomingEdges(resourceOrProject(to));
  }

  @Override
  public Collection<Dependency> getOutgoingDependencies(Resource from) {
    return index.getOutgoingEdges(resourceOrProject(from));
  }

  @Override
  public void saveSource(Resource reference, String source) {
    // useless since 4.2.
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
    return saveMeasure(getResource(inputFile), metric, value);
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Measure measure) {
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
