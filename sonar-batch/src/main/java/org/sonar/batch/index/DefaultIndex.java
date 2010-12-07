/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.*;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;

import java.util.*;

public final class DefaultIndex extends SonarIndex {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIndex.class);

  private RulesProfile profile;
  private PersistenceManager persistence;
  private DefaultResourceCreationLock lock;
  private MetricFinder metricFinder;

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

  public DefaultIndex(PersistenceManager persistence, DefaultResourceCreationLock lock, ProjectTree projectTree, MetricFinder metricFinder) {
    this.persistence = persistence;
    this.lock = lock;
    this.projectTree = projectTree;
    this.metricFinder = metricFinder;
  }

  public void start() {
    Project rootProject = projectTree.getRootProject();
    Bucket bucket = new Bucket(rootProject);
    buckets.put(rootProject, bucket);
    persistence.saveProject(rootProject);
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

  /**
   * Does nothing if the resource is already registered.
   */
  public Resource addResource(Resource resource) {
    Bucket bucket = getOrAddBucket(resource);
    return bucket != null ? bucket.getResource() : null;
  }

  public Resource getResource(Resource resource) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket.getResource();
    }
    return null;
  }

  private Bucket getOrAddBucket(Resource resource) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket;
    }

    if (lock.isLocked() && !ResourceUtils.isLibrary(resource)) {
      LOG.warn("The following resource has not been registered before saving data: " + resource);
    }

    resource.setEffectiveKey(calculateResourceEffectiveKey(currentProject, resource));
    bucket = new Bucket(resource);
    Bucket parentBucket = null;
    Resource parent = resource.getParent();
    if (parent != null) {
      parentBucket = getOrAddBucket(parent);
    } else if (!ResourceUtils.isLibrary(resource)) {
      parentBucket = buckets.get(currentProject);
    }
    bucket.setParent(parentBucket);
    buckets.put(resource, bucket);

    boolean excluded = checkExclusion(resource, parentBucket);
    if (!excluded) {
      persistence.saveResource(currentProject, resource);
    }
    return bucket;
  }

  static String calculateResourceEffectiveKey(Project project, Resource resource) {
    String effectiveKey = resource.getKey();
    if (!StringUtils.equals(Resource.SCOPE_SET, resource.getScope())) {
      // not a project nor a library
      effectiveKey = new StringBuilder(ResourceModel.KEY_SIZE)
          .append(project.getKey())
          .append(':')
          .append(resource.getKey())
          .toString();
    }
    return effectiveKey;
  }

  private boolean checkExclusion(Resource resource, Bucket parent) {
    boolean excluded = (parent != null && parent.isExcluded()) || (resourceFilters != null && resourceFilters.isExcluded(resource));
    resource.setExcluded(excluded);
    return excluded;
  }

  public List<Resource> getChildren(Resource resource) {
    return getChildren(resource, false);
  }

  public List<Resource> getChildren(Resource resource, boolean includeExcludedResources) {
    List<Resource> children = Lists.newArrayList();
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      for (Bucket childBucket : bucket.getChildren()) {
        if (includeExcludedResources || !childBucket.isExcluded())
          children.add(childBucket.getResource());
      }
    }
    return children;
  }

  public Resource getParent(Resource resource) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null && bucket.getParent() != null) {
      return bucket.getParent().getResource();
    }
    return null;
  }

  public Measure getMeasure(Resource resource, Metric metric) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket.getMeasures(MeasuresFilters.metric(metric));
    }
    return null;
  }

  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket.getMeasures(filter);
    }
    return null;
  }

  /**
   * the measure is updated if it's already registered.
   */
  public Measure addMeasure(Resource resource, Measure measure) {
    Bucket bucket = getOrAddBucket(resource);
    if (!bucket.isExcluded()) {
      Metric metric = metricFinder.findByKey(measure.getMetricKey());
      if (metric == null) {
        throw new SonarException("Unknown metric: " + measure.getMetricKey());
      }
      measure.setMetric(metric);
      if (measure.getPersistenceMode().useMemory()) {
        bucket.addMeasure(measure);
      }
      if (measure.getPersistenceMode().useDatabase()) {
        persistence.saveMeasure(currentProject, resource, measure);
      }

      // TODO keep database measures in cache but remove data
    }
    return measure;
  }

  public void setSource(Resource resource, String source) {
    Bucket bucket = getOrAddBucket(resource);
    if (!bucket.isExcluded()) {
      persistence.setSource(currentProject, resource, source);
    }
  }

  //
  //
  //
  // DEPENDENCIES
  //
  //
  //

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
    Bucket fromBucket = getOrAddBucket(dependency.getFrom());
    Bucket toBucket = getOrAddBucket(dependency.getTo());

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

  public List<Violation> getViolations(Resource resource) {
    Bucket bucket = buckets.get(resource);
    if (bucket == null) {
      return Collections.emptyList();
    }
    return bucket.getViolations();
  }

  public void addViolation(Violation violation, boolean force) {
    Bucket bucket;
    Resource resource = violation.getResource();
    if (resource == null) {
      violation.setResource(currentProject);
    }
    bucket = getOrAddBucket(violation.getResource());
    if (!bucket.isExcluded()) {
      boolean isIgnored = !force && violationFilters != null && violationFilters.isIgnored(violation);
      if (!isIgnored) {
        ActiveRule activeRule = profile.getActiveRule(violation.getRule());
        if (activeRule == null) {
          if (currentProject.getReuseExistingRulesConfig()) {
            violation.setSeverity(violation.getRule().getSeverity());
            doAddViolation(violation, bucket);

          } else {
            LoggerFactory.getLogger(getClass()).debug("Rule is not activated, ignoring violation {}", violation);
          }

        } else {
          violation.setPriority(activeRule.getSeverity());
          doAddViolation(violation, bucket);
        }
      }
    }
  }

  private void doAddViolation(Violation violation, Bucket bucket) {
    bucket.addViolation(violation);
  }

  //
  //
  //
  // LINKS
  //
  //
  //

  public void addLink(ProjectLink link) {
    persistence.saveLink(currentProject, link);
  }

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

  public List<Event> getEvents(Resource resource) {
    // currently events are not cached in memory
    return persistence.getEvents(resource);
  }

  public void deleteEvent(Event event) {
    persistence.deleteEvent(event);
  }

  public Event addEvent(Resource resource, String name, String description, String category, Date date) {
    Event event = new Event(name, description, category);
    event.setDate(date);
    persistence.saveEvent(currentProject, resource, event);
    return null;
  }
}
