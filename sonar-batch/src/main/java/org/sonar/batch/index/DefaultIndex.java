/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;
import org.sonar.core.component.ScanGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultIndex extends SonarIndex {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIndex.class);

  private RulesProfile profile;
  private PersistenceManager persistence;
  private DefaultResourceCreationLock lock;
  private MetricFinder metricFinder;
  private ScanGraph graph;

  // filters
  private ViolationFilters violationFilters;
  private ResourceFilters resourceFilters;

  // caches
  private Project currentProject;
  private Map<Resource, Bucket> buckets = Maps.newHashMap();
  private Set<Dependency> dependencies = Sets.newHashSet();
  private Map<Resource, Map<Resource, Dependency>> outgoingDependenciesByResource = Maps.newHashMap();
  private Map<Resource, Map<Resource, Dependency>> incomingDependenciesByResource = Maps.newHashMap();
  private ProjectTree projectTree;

  public DefaultIndex(PersistenceManager persistence, DefaultResourceCreationLock lock, ProjectTree projectTree, MetricFinder metricFinder, ScanGraph graph) {
    this.persistence = persistence;
    this.lock = lock;
    this.projectTree = projectTree;
    this.metricFinder = metricFinder;
    this.graph = graph;
  }

  public void start() {
    Project rootProject = projectTree.getRootProject();
    if (StringUtils.isNotBlank(rootProject.getKey())) {
      doStart(rootProject);
    }
  }

  void doStart(Project rootProject) {
    Bucket bucket = new Bucket(rootProject);
    buckets.put(rootProject, bucket);
    persistence.saveProject(rootProject, null);
    currentProject = rootProject;

    for (Project project : rootProject.getModules()) {
      addProject(project);
    }
  }

  private void addProject(Project project) {
    addResource(project);
    for (Project module : project.getModules()) {
      addProject(module);
    }
  }

  @Override
  public Project getProject() {
    return currentProject;
  }

  public void setCurrentProject(Project project, ResourceFilters resourceFilters, ViolationFilters violationFilters, RulesProfile profile) {
    this.currentProject = project;

    // the following components depend on the current project, so they need to be reloaded.
    this.resourceFilters = resourceFilters;
    this.violationFilters = violationFilters;
    this.profile = profile;
  }

  /**
   * Keep only project stuff
   */
  public void clear() {
    Iterator<Map.Entry<Resource, Bucket>> it = buckets.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Resource, Bucket> entry = it.next();
      Resource resource = entry.getKey();
      if (!ResourceUtils.isSet(resource)) {
        entry.getValue().clear();
        it.remove();
      }
    }

    Set<Dependency> projectDependencies = getDependenciesBetweenProjects();
    dependencies.clear();
    incomingDependenciesByResource.clear();
    outgoingDependenciesByResource.clear();
    for (Dependency projectDependency : projectDependencies) {
      projectDependency.setId(null);
      registerDependency(projectDependency);
    }

    lock.unlock();
  }

  @Override
  public Measure getMeasure(Resource resource, Metric metric) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      Measure measure = bucket.getMeasures(MeasuresFilters.metric(metric));
      if (measure != null) {
        return persistence.reloadMeasure(measure);
      }
    }
    return null;
  }

  @Override
  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      // TODO the data measures which are not kept in memory are not reloaded yet. Use getMeasure().
      return bucket.getMeasures(filter);
    }
    return null;
  }

  /**
   * the measure is updated if it's already registered.
   */
  @Override
  public Measure addMeasure(Resource resource, Measure measure) {
    Bucket bucket = checkIndexed(resource);
    if (bucket != null && !bucket.isExcluded()) {
      Metric metric = metricFinder.findByKey(measure.getMetricKey());
      if (metric == null) {
        throw new SonarException("Unknown metric: " + measure.getMetricKey());
      }
      measure.setMetric(metric);
      bucket.addMeasure(measure);

      if (measure.getPersistenceMode().useDatabase()) {
        persistence.saveMeasure(resource, measure);
      }
    }
    return measure;
  }

  //
  //
  //
  // DEPENDENCIES
  //
  //
  //

  @Override
  public Dependency addDependency(Dependency dependency) {
    Dependency existingDep = getEdge(dependency.getFrom(), dependency.getTo());
    if (existingDep != null) {
      return existingDep;
    }

    Dependency parentDependency = dependency.getParent();
    if (parentDependency != null) {
      addDependency(parentDependency);
    }

    if (registerDependency(dependency)) {
      persistence.saveDependency(currentProject, dependency, parentDependency);
    }
    return dependency;
  }

  boolean registerDependency(Dependency dependency) {
    Bucket fromBucket = doIndex(dependency.getFrom());
    Bucket toBucket = doIndex(dependency.getTo());

    if (fromBucket != null && !fromBucket.isExcluded() && toBucket != null && !toBucket.isExcluded()) {
      dependencies.add(dependency);
      registerOutgoingDependency(dependency);
      registerIncomingDependency(dependency);
      return true;
    }
    return false;
  }

  private void registerOutgoingDependency(Dependency dependency) {
    Map<Resource, Dependency> outgoingDeps = outgoingDependenciesByResource.get(dependency.getFrom());
    if (outgoingDeps == null) {
      outgoingDeps = new HashMap<Resource, Dependency>();
      outgoingDependenciesByResource.put(dependency.getFrom(), outgoingDeps);
    }
    outgoingDeps.put(dependency.getTo(), dependency);
  }

  private void registerIncomingDependency(Dependency dependency) {
    Map<Resource, Dependency> incomingDeps = incomingDependenciesByResource.get(dependency.getTo());
    if (incomingDeps == null) {
      incomingDeps = new HashMap<Resource, Dependency>();
      incomingDependenciesByResource.put(dependency.getTo(), incomingDeps);
    }
    incomingDeps.put(dependency.getFrom(), dependency);
  }

  @Override
  public Set<Dependency> getDependencies() {
    return dependencies;
  }

  public Dependency getEdge(Resource from, Resource to) {
    Map<Resource, Dependency> map = outgoingDependenciesByResource.get(from);
    if (map != null) {
      return map.get(to);
    }
    return null;
  }

  public boolean hasEdge(Resource from, Resource to) {
    return getEdge(from, to) != null;
  }

  public Set<Resource> getVertices() {
    return buckets.keySet();
  }

  public Collection<Dependency> getOutgoingEdges(Resource from) {
    Map<Resource, Dependency> deps = outgoingDependenciesByResource.get(from);
    if (deps != null) {
      return deps.values();
    }
    return Collections.emptyList();
  }

  public Collection<Dependency> getIncomingEdges(Resource to) {
    Map<Resource, Dependency> deps = incomingDependenciesByResource.get(to);
    if (deps != null) {
      return deps.values();
    }
    return Collections.emptyList();
  }

  Set<Dependency> getDependenciesBetweenProjects() {
    Set<Dependency> result = Sets.newLinkedHashSet();
    for (Dependency dependency : dependencies) {
      if (ResourceUtils.isSet(dependency.getFrom()) || ResourceUtils.isSet(dependency.getTo())) {
        result.add(dependency);
      }
    }
    return result;
  }

  //
  //
  //
  // VIOLATIONS
  //
  //
  //

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Violation> getViolations(ViolationQuery violationQuery) {
    Resource resource = violationQuery.getResource();
    if (resource == null) {
      throw new IllegalArgumentException("A resource must be set on the ViolationQuery in order to search for violations.");
    }

    Bucket bucket = buckets.get(resource);
    if (bucket == null) {
      return Collections.emptyList();
    }

    List<Violation> filteredViolations = Lists.newArrayList();
    ViolationQuery.SwitchMode mode = violationQuery.getSwitchMode();
    for (Violation violation : bucket.getViolations()) {
      if (isFiltered(violation, mode)) {
        filteredViolations.add(violation);
      }
    }
    return filteredViolations;
  }

  private static boolean isFiltered(Violation violation, ViolationQuery.SwitchMode mode) {
    return (mode == ViolationQuery.SwitchMode.BOTH
      || (mode == ViolationQuery.SwitchMode.OFF && violation.isSwitchedOff())
      || (mode == ViolationQuery.SwitchMode.ON && !violation.isSwitchedOff()));
  }

  @Override
  public void addViolation(Violation violation, boolean force) {
    Resource resource = violation.getResource();
    if (resource == null) {
      violation.setResource(currentProject);
    } else if (!Scopes.isHigherThanOrEquals(resource, Scopes.FILE)) {
      throw new IllegalArgumentException("Violations are only supported on files, directories and project");
    }

    if (violation.getRule() == null || violation.getRule().getId() == null) {
      LOG.warn("Rule does not exist (it is null or its ID is null): ignoring violation {}", violation);
      return;
    }

    Bucket bucket = checkIndexed(resource);
    if (bucket == null || bucket.isExcluded()) {
      return;
    }

    addViolation(violation, bucket, force);
  }

  private void addViolation(Violation violation, Bucket bucket, boolean force) {
    boolean isIgnored = !force && violationFilters != null && violationFilters.isIgnored(violation);
    if (isIgnored) {
      return;
    }

    // TODO this code is not the responsibility of this index. It should be moved somewhere else.
    if (!violation.isManual()) {
      ActiveRule activeRule = profile.getActiveRule(violation.getRule());
      if (activeRule != null) {
        violation.setSeverity(activeRule.getSeverity());
      } else if (currentProject.getReuseExistingRulesConfig()) {
        violation.setSeverity(violation.getRule().getSeverity());
      } else {
        LoggerFactory.getLogger(getClass()).debug("Rule is not activated, ignoring violation {}", violation);
        return;
      }
    }

    bucket.addViolation(violation);
  }

  //
  //
  //
  // LINKS
  //
  //
  //

  @Override
  public void addLink(ProjectLink link) {
    persistence.saveLink(currentProject, link);
  }

  @Override
  public void deleteLink(String key) {
    persistence.deleteLink(currentProject, key);
  }

  //
  //
  //
  // EVENTS
  //
  //
  //

  @Override
  public List<Event> getEvents(Resource resource) {
    // currently events are not cached in memory
    return persistence.getEvents(resource);
  }

  @Override
  public void deleteEvent(Event event) {
    persistence.deleteEvent(event);
  }

  @Override
  public Event addEvent(Resource resource, String name, String description, String category, Date date) {
    Event event = new Event(name, description, category);
    event.setDate(date);
    event.setCreatedAt(new Date());

    persistence.saveEvent(resource, event);
    return null;
  }

  @Override
  public void setSource(Resource reference, String source) {
    Bucket bucket = checkIndexed(reference);
    if (bucket != null && !bucket.isExcluded()) {
      persistence.setSource(reference, source);
    }
  }

  @Override
  public String getSource(Resource resource) {
    return persistence.getSource(resource);
  }

  /**
   * Does nothing if the resource is already registered.
   */
  @Override
  public Resource addResource(Resource resource) {
    Bucket bucket = doIndex(resource);
    return bucket != null ? bucket.getResource() : null;
  }

  @Override
  public <R extends Resource> R getResource(R reference) {
    Bucket bucket = buckets.get(reference);
    if (bucket != null) {
      return (R) bucket.getResource();
    }
    return null;
  }

  static String createUID(Project project, Resource resource) {
    String uid = resource.getKey();
    if (!StringUtils.equals(Scopes.PROJECT, resource.getScope())) {
      // not a project nor a library
      uid = new StringBuilder(ResourceModel.KEY_SIZE)
          .append(project.getKey())
          .append(':')
          .append(resource.getKey())
          .toString();
    }
    return uid;
  }

  private boolean checkExclusion(Resource resource, Bucket parent) {
    boolean excluded = (parent != null && parent.isExcluded()) || (resourceFilters != null && resourceFilters.isExcluded(resource));
    resource.setExcluded(excluded);
    return excluded;
  }

  @Override
  public List<Resource> getChildren(Resource resource) {
    return getChildren(resource, false);
  }

  public List<Resource> getChildren(Resource resource, boolean acceptExcluded) {
    List<Resource> children = Lists.newLinkedList();
    Bucket bucket = getBucket(resource, acceptExcluded);
    if (bucket != null) {
      for (Bucket childBucket : bucket.getChildren()) {
        if (acceptExcluded || !childBucket.isExcluded()) {
          children.add(childBucket.getResource());
        }
      }
    }
    return children;
  }

  @Override
  public Resource getParent(Resource resource) {
    Bucket bucket = getBucket(resource, false);
    if (bucket != null && bucket.getParent() != null) {
      return bucket.getParent().getResource();
    }
    return null;
  }

  @Override
  public boolean index(Resource resource) {
    Bucket bucket = doIndex(resource);
    return bucket != null && !bucket.isExcluded();
  }

  private Bucket doIndex(Resource resource) {
    if (resource.getParent() != null) {
      doIndex(resource.getParent());
    }
    return doIndex(resource, resource.getParent());
  }

  @Override
  public boolean index(Resource resource, Resource parentReference) {
    Bucket bucket = doIndex(resource, parentReference);
    return bucket != null && !bucket.isExcluded();
  }

  private Bucket doIndex(Resource resource, Resource parentReference) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket;
    }

    checkLock(resource);

    Resource parent = null;
    if (!ResourceUtils.isLibrary(resource)) {
      // a library has no parent
      parent = (Resource) ObjectUtils.defaultIfNull(parentReference, currentProject);
    }

    Bucket parentBucket = getBucket(parent, true);
    if (parentBucket == null && parent != null) {
      LOG.warn("Resource ignored, parent is not indexed: " + resource);
      return null;
    }

    resource.setEffectiveKey(createUID(currentProject, resource));
    bucket = new Bucket(resource).setParent(parentBucket);
    buckets.put(resource, bucket);

    boolean excluded = checkExclusion(resource, parentBucket);
    if (!excluded) {
      Snapshot snapshot = persistence.saveResource(currentProject, resource, (parentBucket != null ? parentBucket.getResource() : null));
      if (ResourceUtils.isPersistable(resource) && !Qualifiers.LIBRARY.equals(resource.getQualifier())) {
        graph.addComponent(resource, snapshot);
      }
    }

    return bucket;
  }

  private void checkLock(Resource resource) {
    if (lock.isLocked() && !ResourceUtils.isLibrary(resource) && lock.isFailWhenLocked()) {
      throw new SonarException("Index is locked, resource can not be indexed: " + resource);
    }
  }

  private Bucket checkIndexed(Resource resource) {
    Bucket bucket = getBucket(resource, true);
    if (bucket == null) {
      if (lock.isLocked()) {
        if (lock.isFailWhenLocked()) {
          throw new ResourceNotIndexedException(resource);
        }
        LOG.warn("Resource will be ignored in next Sonar versions, index is locked: " + resource);
      }
      if (Scopes.isDirectory(resource) || Scopes.isFile(resource)) {
        bucket = doIndex(resource);
      } else if (!lock.isLocked()) {
        LOG.warn("Resource will be ignored in next Sonar versions, it must be indexed before adding data: " + resource);
      }
    }
    return bucket;
  }

  @Override
  public boolean isExcluded(Resource reference) {
    Bucket bucket = getBucket(reference, true);
    return bucket != null && bucket.isExcluded();
  }

  @Override
  public boolean isIndexed(Resource reference, boolean acceptExcluded) {
    return getBucket(reference, acceptExcluded) != null;
  }

  private Bucket getBucket(Resource resource, boolean acceptExcluded) {
    Bucket bucket = null;
    if (resource != null) {
      bucket = buckets.get(resource);
      if (!acceptExcluded && bucket != null && bucket.isExcluded()) {
        bucket = null;
      }
    }
    return bucket;
  }
}
