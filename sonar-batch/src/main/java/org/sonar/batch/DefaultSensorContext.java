/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.utils.SonarException;
import org.sonar.core.measure.MeasurementFilters;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DefaultSensorContext implements SensorContext {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSensorContext.class);

  private SonarIndex index;
  private Project project;
  private MeasurementFilters filters;
  private Languages languages;

  public DefaultSensorContext(SonarIndex index, Project project, MeasurementFilters filters, Languages languages) {
    this.index = index;
    this.project = project;
    this.filters = filters;
    this.languages = languages;
  }

  public Project getProject() {
    return project;
  }

  public boolean index(Resource resource) {
    // SONAR-5006
    if (indexedByCore(resource)) {
      logWarning();
      return true;
    }
    return index.index(resource);
  }

  private boolean indexedByCore(Resource resource) {
    return StringUtils.equals(Scopes.DIRECTORY, resource.getScope()) ||
      StringUtils.equals(Scopes.FILE, resource.getScope());
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
    LOG.debug("Plugins are no more responsible for indexing physical resources like directories and files. This is now handled by the platform.", new SonarException(
      "Plugin should not index physical resources"));
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
  public Measure getMeasure(Metric metric) {
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
  public Measure getMeasure(Resource resource, Metric metric) {
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
    if (filters.accept(resource, measure)) {
      return index.addMeasure(resourceOrProject(resource), measure);
    } else {
      return measure;
    }
  }

  public void saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resourceOrProject(violation.getResource()));
    }
    index.addViolation(violation, force);
  }

  public void saveViolation(Violation violation) {
    saveViolation(violation, false);
  }

  public void saveViolations(Collection<Violation> violations) {
    if (violations != null) {
      for (Violation violation : violations) {
        saveViolation(violation);
      }
    }
  }

  public Dependency saveDependency(Dependency dependency) {
    return index.addDependency(dependency);
  }

  public Set<Dependency> getDependencies() {
    return index.getDependencies();
  }

  public Collection<Dependency> getIncomingDependencies(Resource to) {
    return index.getIncomingEdges(resourceOrProject(to));
  }

  public Collection<Dependency> getOutgoingDependencies(Resource from) {
    return index.getOutgoingEdges(resourceOrProject(from));
  }

  public void saveSource(Resource reference, String source) {
  }

  public void saveLink(ProjectLink link) {
    index.addLink(link);
  }

  public void deleteLink(String key) {
    index.deleteLink(key);
  }

  public List<Event> getEvents(Resource resource) {
    return index.getEvents(resource);
  }

  public Event createEvent(Resource resource, String name, String description, String category, Date date) {
    return index.addEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    index.deleteEvent(event);
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
    return saveMeasure(fromInputFile(inputFile), metric, value);
  }

  @Override
  public Measure saveMeasure(InputFile inputFile, Measure measure) {
    return saveMeasure(fromInputFile(inputFile), measure);
  }

  private Resource fromInputFile(InputFile inputFile) {
    String languageKey = inputFile.attribute(InputFile.ATTRIBUTE_LANGUAGE);
    boolean unitTest = InputFile.TYPE_TEST.equals(inputFile.attribute(InputFile.ATTRIBUTE_TYPE));
    if (Java.KEY.equals(languageKey)) {
      return JavaFile.create(inputFile.path(), inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), unitTest);
    } else {
      return File.create(inputFile.path(), inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), languages.get(languageKey), unitTest);
    }
  }
}
