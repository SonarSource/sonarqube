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

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;

import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class DefaultResourcePersister implements ResourcePersister {

  private DatabaseSession session;

  private Map<Resource, Snapshot> snapshotsByResource = Maps.newHashMap();

  public DefaultResourcePersister(DatabaseSession session) {
    this.session = session;
  }

  public Snapshot saveProject(Project project) {
    Snapshot snapshot = snapshotsByResource.get(project);
    if (snapshot == null) {
      snapshot = doSaveProject(project);
    }
    return snapshot;
  }

  public Snapshot getSnapshot(Resource resource) {
    if (resource != null) {
      return snapshotsByResource.get(resource);
    }
    return null;
  }

  /**
   * just for unit tests
   */
  Map<Resource, Snapshot> getSnapshotsByResource() {
    return snapshotsByResource;
  }

  private Snapshot doSaveProject(Project project) {
    // temporary hack
    project.setEffectiveKey(project.getKey());

    ResourceModel model = findOrCreateModel(project);
    model.setLanguageKey(project.getLanguageKey());// ugly, only for projects

    Snapshot parentSnapshot = null;
    if (project.getParent() != null) {
      // assume that the parent project has already been saved
      parentSnapshot = snapshotsByResource.get(project.getParent());
      model.setRootId((Integer) ObjectUtils.defaultIfNull(parentSnapshot.getRootProjectId(), parentSnapshot.getResourceId()));
    }
    model = session.save(model);
    project.setId(model.getId()); // TODO to be removed

    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot.setVersion(project.getAnalysisVersion());
    snapshot.setCreatedAt(project.getAnalysisDate());
    snapshot = session.save(snapshot);
    session.commit();
    snapshotsByResource.put(project, snapshot);
    return snapshot;
  }

  public Snapshot saveResource(Project project, Resource resource) {
    if (resource == null) {
      return null;
    }
    Snapshot snapshot = snapshotsByResource.get(resource);
    if (snapshot == null) {
      if (resource instanceof Project) {
        snapshot = doSaveProject((Project) resource);

      } else if (resource instanceof Library) {
        snapshot = doSaveLibrary(project, (Library) resource);

      } else {
        snapshot = doSaveResource(project, resource);
      }
    }
    return snapshot;
  }

  private Snapshot doSaveLibrary(Project project, Library library) {
    ResourceModel model = findOrCreateModel(library);
    model = session.save(model);
    library.setId(model.getId()); // TODO to be removed
    library.setEffectiveKey(library.getKey());

    Snapshot snapshot = findLibrarySnapshot(model.getId(), library.getVersion());
    if (snapshot == null) {
      snapshot = new Snapshot(model, null);
      snapshot.setCreatedAt(project.getAnalysisDate());
      snapshot.setVersion(library.getVersion());
      snapshot.setStatus(Snapshot.STATUS_PROCESSED);

      // see http://jira.codehaus.org/browse/SONAR-1850
      // The qualifier must be LIB, even if the resource is TRK, because this snapshot has no measures.
      snapshot.setQualifier(Resource.QUALIFIER_LIB);
      snapshot = session.save(snapshot);
    }
    session.commit();
    return snapshot;
  }

  private Snapshot findLibrarySnapshot(Integer resourceId, String version) {
    Query query = session.createQuery("from " + Snapshot.class.getSimpleName() + " s WHERE s.resourceId=:resourceId AND s.version=:version AND s.scope=:scope AND s.qualifier<>:qualifier AND s.last=:last");
    query.setParameter("resourceId", resourceId);
    query.setParameter("version", version);
    query.setParameter("scope", Resource.SCOPE_SET);
    query.setParameter("qualifier", Resource.QUALIFIER_LIB);
    query.setParameter("last", Boolean.TRUE);
    List<Snapshot> snapshots = query.getResultList();
    if (snapshots.isEmpty()) {
      snapshots = session.getResults(Snapshot.class, "resourceId", resourceId, "version", version, "scope", Resource.SCOPE_SET, "qualifier", Resource.QUALIFIER_LIB);
    }
    return (snapshots.isEmpty() ? null : snapshots.get(0));
  }

  /**
   * Everything except project and library
   */
  private Snapshot doSaveResource(Project project, Resource resource) {
    ResourceModel model = findOrCreateModel(resource);
    Snapshot projectSnapshot = snapshotsByResource.get(project);
    model.setRootId(projectSnapshot.getResourceId());
    model = session.save(model);
    resource.setId(model.getId()); // TODO to be removed

    Snapshot parentSnapshot = (Snapshot)ObjectUtils.defaultIfNull(getSnapshot(resource.getParent()), projectSnapshot);
    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot = session.save(snapshot);
    session.commit();
    snapshotsByResource.put(resource, snapshot);
    return snapshot;
  }

  public void clear() {
    // we keep cache of projects
    for (Iterator<Map.Entry<Resource, Snapshot>> it = snapshotsByResource.entrySet().iterator(); it.hasNext();) {
      Map.Entry<Resource, Snapshot> entry = it.next();
      if (!ResourceUtils.isSet(entry.getKey())) {
        it.remove();
      }
    }
  }


  private ResourceModel findOrCreateModel(Resource resource) {
    ResourceModel model;
    try {
      model = session.getSingleResult(ResourceModel.class, "key", resource.getEffectiveKey());
      if (model == null) {
        model = createModel(resource);

      } else {
        mergeModel(model, resource);
      }
      return model;

    } catch (NonUniqueResultException e) {
      throw new SonarException("The resource '" + resource.getEffectiveKey() + "' is duplicated in database.");
    }
  }

  static ResourceModel createModel(Resource resource) {
    ResourceModel model = new ResourceModel();
    model.setEnabled(Boolean.TRUE);
    model.setDescription(resource.getDescription());
    model.setKey(resource.getEffectiveKey());
    if (resource.getLanguage() != null) {
      model.setLanguageKey(resource.getLanguage().getKey());
    }
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    } else {
      model.setName(resource.getKey());
    }
    model.setLongName(resource.getLongName());
    model.setScope(resource.getScope());
    model.setQualifier(resource.getQualifier());
    return model;
  }

  static void mergeModel(ResourceModel model, Resource resource) {
    model.setEnabled(true);
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    }
    if (StringUtils.isNotBlank(resource.getLongName())) {
      model.setLongName(resource.getLongName());
    }
    if (StringUtils.isNotBlank(resource.getDescription())) {
      model.setDescription(resource.getDescription());
    }
    if (!ResourceUtils.isLibrary(resource)) {
      model.setScope(resource.getScope());
      model.setQualifier(resource.getQualifier());
    }
    if (resource.getLanguage() != null) {
      model.setLanguageKey(resource.getLanguage().getKey());
    }
  }
}
