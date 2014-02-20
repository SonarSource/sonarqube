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
package org.sonar.batch.index;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.*;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.utils.SonarException;

import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class DefaultResourcePersister implements ResourcePersister {

  private static final String RESOURCE_ID = "resourceId";
  private static final String LAST = "last";
  private static final String VERSION = "version";
  private static final String SCOPE = "scope";
  private static final String QUALIFIER = "qualifier";

  private final DatabaseSession session;
  private final Map<Resource, Snapshot> snapshotsByResource = Maps.newHashMap();
  private final ResourcePermissions permissions;
  private final SnapshotCache snapshotCache;
  private final ResourceCache resourceCache;

  public DefaultResourcePersister(DatabaseSession session, ResourcePermissions permissions, SnapshotCache snapshotCache, ResourceCache resourceCache) {
    this.session = session;
    this.permissions = permissions;
    this.snapshotCache = snapshotCache;
    this.resourceCache = resourceCache;
  }

  public Snapshot saveProject(Project project, Project parent) {
    Snapshot snapshot = snapshotsByResource.get(project);
    if (snapshot == null) {
      snapshot = persistProject(project, parent);
      addToCache(project, snapshot);
    }
    return snapshot;
  }

  private void addToCache(Resource resource, Snapshot snapshot) {
    if (snapshot != null) {
      snapshotsByResource.put(resource, snapshot);
      resourceCache.add(resource);
      snapshotCache.put(resource.getEffectiveKey(), snapshot);
    }
  }

  private Snapshot persistProject(Project project, Project parent) {
    // temporary hack
    project.setEffectiveKey(project.getKey());

    ResourceModel model = findOrCreateModel(project);
    // Used by ResourceKeyMigration in order to know that a project has already being migrated
    model.setDeprecatedKey(project.getKey());
    // language is null for project since multi-language support
    model.setLanguageKey(null);

    // For views
    if (project instanceof ResourceCopy) {
      model.setCopyResourceId(((ResourceCopy) project).getCopyResourceId());
    }

    Snapshot parentSnapshot = null;
    if (parent != null) {
      // assume that the parent project has already been saved
      parentSnapshot = snapshotsByResource.get(project.getParent());
      model.setRootId((Integer) ObjectUtils.defaultIfNull(parentSnapshot.getRootProjectId(), parentSnapshot.getResourceId()));
    } else {
      model.setRootId(null);
    }
    model = session.save(model);
    project.setId(model.getId());

    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot.setVersion(project.getAnalysisVersion());
    snapshot.setCreatedAt(project.getAnalysisDate());
    snapshot.setBuildDate(new Date());
    snapshot = session.save(snapshot);
    session.commit();

    if (!permissions.hasRoles(project)) {
      permissions.grantDefaultRoles(project);
    }

    return snapshot;
  }

  public Snapshot getSnapshot(Resource reference) {
    return snapshotsByResource.get(reference);
  }

  public Snapshot getSnapshotOrFail(Resource resource) {
    Snapshot snapshot = getSnapshot(resource);
    if (snapshot == null) {
      throw new ResourceNotPersistedException(resource);
    }
    return snapshot;
  }

  @Override
  public Snapshot getSnapshotOrFail(InputFile inputFile) {
    return getSnapshotOrFail(fromInputFile(inputFile));
  }

  private Resource fromInputFile(InputFile inputFile) {
    return File.create(inputFile.relativePath());
  }

  /**
   * just for unit tests
   */
  Map<Resource, Snapshot> getSnapshotsByResource() {
    return snapshotsByResource;
  }

  public Snapshot saveResource(Project project, Resource resource) {
    return saveResource(project, resource, null);
  }

  public Snapshot saveResource(Project project, Resource resource, Resource parent) {
    Snapshot snapshot = snapshotsByResource.get(resource);
    if (snapshot == null) {
      snapshot = persist(project, resource, parent);
      addToCache(resource, snapshot);
    }
    return snapshot;
  }

  private Snapshot persist(Project project, Resource resource, Resource parent) {
    Snapshot snapshot;
    if (resource instanceof Project) {
      // should not occur, please use the method saveProject()
      snapshot = persistProject((Project) resource, project);

    } else if (resource instanceof Library) {
      snapshot = persistLibrary(project, (Library) resource);

    } else {
      snapshot = persistFileOrDirectory(project, resource, parent);
    }

    return snapshot;
  }

  private Snapshot persistLibrary(Project project, Library library) {
    ResourceModel model = findOrCreateModel(library);
    model = session.save(model);
    // TODO to be removed
    library.setId(model.getId());
    library.setEffectiveKey(library.getKey());

    Snapshot snapshot = findLibrarySnapshot(model.getId(), library.getVersion());
    if (snapshot == null) {
      snapshot = new Snapshot(model, null);
      snapshot.setCreatedAt(project.getAnalysisDate());
      snapshot.setBuildDate(new Date());
      snapshot.setVersion(library.getVersion());
      snapshot.setStatus(Snapshot.STATUS_PROCESSED);

      // see http://jira.codehaus.org/browse/SONAR-1850
      // The qualifier must be LIB, even if the resource is TRK, because this snapshot has no measures.
      snapshot.setQualifier(Qualifiers.LIBRARY);
      snapshot = session.save(snapshot);
    }
    session.commit();
    return snapshot;
  }

  private Snapshot findLibrarySnapshot(Integer resourceId, String version) {
    Query query = session.createQuery("from " + Snapshot.class.getSimpleName() +
      " s WHERE s.resourceId=:resourceId AND s.version=:version AND s.scope=:scope AND s.qualifier<>:qualifier AND s.last=:last");
    query.setParameter(RESOURCE_ID, resourceId);
    query.setParameter(VERSION, version);
    query.setParameter(SCOPE, Scopes.PROJECT);
    query.setParameter(QUALIFIER, Qualifiers.LIBRARY);
    query.setParameter(LAST, Boolean.TRUE);
    List<Snapshot> snapshots = query.getResultList();
    if (snapshots.isEmpty()) {
      snapshots = session.getResults(Snapshot.class, RESOURCE_ID, resourceId, VERSION, version, SCOPE, Scopes.PROJECT, QUALIFIER, Qualifiers.LIBRARY);
    }
    return snapshots.isEmpty() ? null : snapshots.get(0);
  }

  /**
   * Everything except project and library
   */
  private Snapshot persistFileOrDirectory(Project project, Resource resource, Resource parentReference) {
    Snapshot moduleSnapshot = snapshotsByResource.get(project);
    Integer moduleId = moduleSnapshot.getResourceId();
    ResourceModel model = findOrCreateModel(resource);
    model.setRootId(moduleId);
    model = session.save(model);
    resource.setId(model.getId());

    Snapshot parentSnapshot = (Snapshot) ObjectUtils.defaultIfNull(getSnapshot(parentReference), moduleSnapshot);
    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot.setBuildDate(new Date());
    snapshot = session.save(snapshot);
    session.commit();
    return snapshot;
  }

  public Snapshot getLastSnapshot(Snapshot snapshot, boolean onlyOlder) {
    String hql = "SELECT s FROM " + Snapshot.class.getSimpleName() + " s WHERE s.last=:last AND s.resourceId=:resourceId";
    if (onlyOlder) {
      hql += " AND s.createdAt<:date";
    }
    Query query = session.createQuery(hql);
    query.setParameter(LAST, true);
    query.setParameter(RESOURCE_ID, snapshot.getResourceId());
    if (onlyOlder) {
      query.setParameter("date", snapshot.getCreatedAt());
    }
    return session.getSingleResult(query, null);
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
        if (StringUtils.isBlank(resource.getEffectiveKey())) {
          throw new SonarException("Unable to persist resource " + resource.toString() + ". Resource effective key is blank. This may be caused by an outdated plugin.");
        }
        model = createModel(resource);

      } else {
        mergeModel(model, resource);
      }
      return model;

    } catch (NonUniqueResultException e) {
      throw new SonarException("The resource '" + resource.getEffectiveKey() + "' is duplicated in database.", e);
    }
  }

  static ResourceModel createModel(Resource resource) {
    ResourceModel model = new ResourceModel();
    model.setEnabled(Boolean.TRUE);
    model.setDescription(resource.getDescription());
    model.setKey(resource.getEffectiveKey());
    model.setPath(resource.getPath());
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
    model.setKey(resource.getEffectiveKey());
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    }
    if (StringUtils.isNotBlank(resource.getLongName())) {
      model.setLongName(resource.getLongName());
    }
    if (StringUtils.isNotBlank(resource.getDescription())) {
      model.setDescription(resource.getDescription());
    }
    if (StringUtils.isNotBlank(resource.getPath())) {
      model.setPath(resource.getPath());
    }
    if (!ResourceUtils.isLibrary(resource)) {
      // SONAR-4245
      if (Scopes.PROJECT.equals(resource.getScope()) &&
        Qualifiers.MODULE.equals(resource.getQualifier())
        && Qualifiers.PROJECT.equals(model.getQualifier())) {
        throw new SonarException(
          String.format("The project '%s' is already defined in SonarQube but not as a module of project '%s'. "
            + "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
            resource.getKey(), resource.getParent().getKey(), resource.getKey(), resource.getParent().getKey()));
      }
      model.setScope(resource.getScope());
      model.setQualifier(resource.getQualifier());
    }
    if (resource.getLanguage() != null) {
      model.setLanguageKey(resource.getLanguage().getKey());
    }
  }
}
