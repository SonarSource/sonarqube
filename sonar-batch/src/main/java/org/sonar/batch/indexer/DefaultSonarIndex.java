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
package org.sonar.batch.indexer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.*;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.design.Dependency;
import org.sonar.api.design.DependencyDto;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Violation;

import java.util.*;

public class DefaultSonarIndex extends SonarIndex {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSonarIndex.class);

  private DatabaseSession session;
  private ResourcePersisters resourcePersisters;
  private Bucket<Project> rootProjectBucket;
  private Bucket<Project> selectedProjectBucket;
  private DefaultResourceCreationLock lock;

  private ViolationFilters violationFilters;
  private ResourceFilters resourceFilters;

  // data
  private Map<Resource, Bucket> buckets = Maps.newHashMap();
  private Set<Dependency> dependencies = Sets.newHashSet();
  private Map<Resource, Map<Resource, Dependency>> outgoingDependenciesByResource = new HashMap<Resource, Map<Resource, Dependency>>();
  private Map<Resource, Map<Resource, Dependency>> incomingDependenciesByResource = new HashMap<Resource, Map<Resource, Dependency>>();

  // dao
  private ViolationsDao violationsDao;
  private MeasuresDao measuresDao;
  private ProjectTree projectTree;

  public DefaultSonarIndex(DatabaseSession session, ProjectTree projectTree, DefaultResourceCreationLock lock) {
    this.session = session;
    this.projectTree = projectTree;
    this.resourcePersisters = new ResourcePersisters(session);
    this.lock = lock;
  }

  public void start() {
    Project rootProject = projectTree.getRootProject();

    this.rootProjectBucket = new Bucket<Project>(rootProject);
    persist(rootProjectBucket);
    this.buckets.put(rootProject, rootProjectBucket);
    this.selectedProjectBucket = rootProjectBucket;

    for (Project project : rootProject.getModules()) {
      addProject(project);
    }
    session.commit();
  }

  private void addProject(Project project) {
    addResource(project);
    for (Project module : project.getModules()) {
      addProject(module);
    }
  }


  public void selectProject(Project project, ResourceFilters resourceFilters, ViolationFilters violationFilters, MeasuresDao measuresDao, ViolationsDao violationsDao) {
    this.selectedProjectBucket = buckets.get(project);
    this.resourceFilters = resourceFilters;
    this.violationFilters = violationFilters;
    this.violationsDao = violationsDao;
    this.measuresDao = measuresDao;
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

  /* ------------ RESOURCES */
  public Project getRootProject() {
    return rootProjectBucket.getResource();
  }

  public Project getProject() {
    return selectedProjectBucket.getResource();
  }

  public Bucket getBucket(Resource resource) {
    return buckets.get(resource);
  }

  public Resource getResource(Resource resource) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket.getResource();
    }
    return null;
  }

  public List<Resource> getChildren(Resource resource) {
    List<Resource> children = Lists.newArrayList();
    Bucket<?> bucket = buckets.get(resource);
    if (bucket != null) {
      for (Bucket childBucket : bucket.getChildren()) {
        children.add(childBucket.getResource());
      }
    }
    return children;
  }

  public Resource addResource(Resource resource) {
    return getOrCreateBucket(resource, false).getResource();
  }

  private Bucket<Resource> getOrCreateBucket(Resource resource, boolean mustExist) {
    Bucket bucket = buckets.get(resource);
    if (bucket != null) {
      return bucket;
    }

    if (mustExist && lock.isLocked() && !ResourceUtils.isLibrary(resource)) { 
      LOG.warn("The following resource has not been registered before saving violation/measure/event: " + resource);
    }

    prepareResource(resource);
    bucket = new Bucket<Resource>(resource);
    buckets.put(resource, bucket);

    Bucket parentBucket = null;
    Resource parent = resource.getParent();
    if (parent != null) {
      parentBucket = getOrCreateBucket(parent, mustExist);
    } else if (!ResourceUtils.isLibrary(resource)) {
      parentBucket = selectedProjectBucket;
    }
    bucket.setParent(parentBucket);
    bucket.setProject(selectedProjectBucket);

    persist(bucket);
    return bucket;
  }

  private void prepareResource(Resource resource) {
    resource.setExcluded(resourceFilters != null && resourceFilters.isExcluded(resource));
  }

  private void persist(Bucket bucket) {
    resourcePersisters.get(bucket).persist(bucket);
  }

  /* ------------ MEASURES */
  public Measure getMeasure(Resource resource, Metric metric) {
    return getOrCreateBucket(resource, false).getMeasures(MeasuresFilters.metric(metric));
  }

  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    return getOrCreateBucket(resource, false).getMeasures(filter);
  }


  /* ------------ SOURCE CODE */

  public void setSource(Resource resource, String source) {
    Bucket bucket = getOrCreateBucket(resource, false);

    if (!bucket.isExcluded()) {
      if (bucket.isSourceSaved()) {
        LOG.warn("Trying to save twice the source of " + resource);

      } else {
        session.save(new SnapshotSource(bucket.getSnapshotId(), source));
        bucket.setSourceSaved(true);
      }
    }
  }


  /* ------------ RULE VIOLATIONS */

  public void addViolation(Violation violation) {
    Bucket bucket;
    Resource resource = violation.getResource();
    if (resource == null) {
      bucket = selectedProjectBucket;
    } else {
      bucket = getOrCreateBucket(resource, true);
    }
    if (!bucket.isExcluded()) {
      persistViolation(violation, bucket.getSnapshot());
    }
  }

  private void persistViolation(Violation violation, Snapshot snapshot) {
    boolean isIgnored = violationFilters != null && violationFilters.isIgnored(violation);
    if (!isIgnored) {
      violationsDao.saveViolation(snapshot, violation);
    }
  }


  /* ------------ MEASURES */
  public Measure saveMeasure(Resource resource, Measure measure) {
    if (measure.getId() == null) {
      return addMeasure(resource, measure);
    }
    return updateMeasure(measure);
  }

  public Measure addMeasure(Resource resource, Measure measure) {
    Bucket bucket = getOrCreateBucket(resource, true);
    if (!bucket.isExcluded()) {
      if (bucket.getMeasures(MeasuresFilters.measure(measure))!=null) {
        throw new IllegalArgumentException("This measure has already been saved: " + measure + ",resource: " + resource);
      }
      if (shouldPersistMeasure(resource, measure)) {
        persistMeasure(bucket, measure);
      }

      if (measure.getPersistenceMode().useMemory()) {
        bucket.addMeasure(measure);
      }
    }
    return measure;
  }

  public Measure updateMeasure(Measure measure) {
    if (measure.getId() == null) {
      throw new IllegalStateException("Measure can not be updated because it has never been saved");
    }

    MeasureModel model = session.reattach(MeasureModel.class, measure.getId());
    model = MeasureModel.build(measure, model);
    model.setMetric(measuresDao.getMetric(measure.getMetric()));
    model.save(session);
    return measure;
  }

  private void persistMeasure(Bucket bucket, Measure measure) {
    Metric metric = measuresDao.getMetric(measure.getMetric());
    MeasureModel measureModel = MeasureModel.build(measure);
    measureModel.setMetric(metric); // hibernate synchronized metric
    measureModel.setSnapshotId(bucket.getSnapshotId());
    measureModel.save(session);

    // the id is saved for future updates
    measure.setId(measureModel.getId());
  }

  private boolean shouldPersistMeasure(Resource resource, Measure measure) {
    Metric metric = measure.getMetric();
    return measure.getPersistenceMode().useDatabase() && !(
        ResourceUtils.isEntity(resource) &&
            metric.isOptimizedBestValue() == Boolean.TRUE &&
            metric.getBestValue() != null &&
            metric.getBestValue().equals(measure.getValue()) &&
            !measure.hasOptionalData());
  }


  /* --------------- DEPENDENCIES */
  public Dependency saveDependency(Dependency dependency) {
    Dependency persistedDep = getEdge(dependency.getFrom(), dependency.getTo());
    if (persistedDep != null && persistedDep.getId()!=null) {
      return persistedDep;
    }
    Bucket from = getOrCreateBucket(dependency.getFrom(), true);
    Bucket to = getOrCreateBucket(dependency.getTo(), true);

    DependencyDto dto = new DependencyDto();
    dto.setFromResourceId(from.getResourceId());
    dto.setFromScope(from.getResource().getScope());
    dto.setFromSnapshotId(from.getSnapshotId());
    dto.setToResourceId(to.getResourceId());
    dto.setToSnapshotId(to.getSnapshotId());
    dto.setToScope(to.getResource().getScope());
    dto.setProjectSnapshotId(selectedProjectBucket.getSnapshotId());
    dto.setUsage(dependency.getUsage());
    dto.setWeight(dependency.getWeight());

    Dependency parentDependency = dependency.getParent();
    if (parentDependency != null) {
      saveDependency(parentDependency);
      dto.setParentDependencyId(parentDependency.getId());
    }
    session.save(dto);
    dependency.setId(dto.getId());
    registerDependency(dependency);

    return dependency;
  }

  protected void registerDependency(Dependency dependency) {
    dependencies.add(dependency);
    registerOutgoingDependency(dependency);
    registerIncomingDependency(dependency);
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


  /* ------------ LINKS */

  public void saveLink(ProjectLink link) {
    ResourceModel projectDao = session.reattach(ResourceModel.class, selectedProjectBucket.getResourceId());
    ProjectLink dbLink = projectDao.getProjectLink(link.getKey());
    if (dbLink == null) {
      link.setResource(projectDao);
      projectDao.getProjectLinks().add(link);
      session.save(link);

    } else {
      dbLink.copyFieldsFrom(link);
      session.save(dbLink);
    }
  }

  public void deleteLink(String key) {
    ResourceModel projectDao = session.reattach(ResourceModel.class, selectedProjectBucket.getResourceId());
    ProjectLink dbLink = projectDao.getProjectLink(key);
    if (dbLink != null) {
      session.remove(dbLink);
      projectDao.getProjectLinks().remove(dbLink);
    }
  }


  /* ----------- EVENTS */
  public List<Event> getEvents(Resource resource) {
    Bucket bucket = getOrCreateBucket(resource, true);
    return session.getResults(Event.class, "resourceId", bucket.getResourceId());
  }

  public void deleteEvent(Event event) {
    session.remove(event);
  }

  public Event createEvent(Resource resource, String name, String description, String category, Date date) {
    Bucket bucket = getOrCreateBucket(resource, true);
    Event event;
    if (date == null) {
      event = new Event(name, description, category, bucket.getSnapshot());
    } else {
      event = new Event(name, description, category, date, bucket.getResourceId());
    }
    return session.save(event);
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

  public Set<Dependency> getDependenciesBetweenProjects() {
    Set<Dependency> result = new HashSet<Dependency>();
    for (Project project : projectTree.getProjects()) {
      Collection<Dependency> deps = getOutgoingDependencies(project);
      for (Dependency dep : deps) {
        if (ResourceUtils.isSet(dep.getTo())) {
          result.add(dep);
        }
      }
    }
    return result;
  }
}
