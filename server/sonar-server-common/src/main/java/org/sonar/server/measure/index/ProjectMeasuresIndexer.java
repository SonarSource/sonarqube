/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.measure.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EventIndexer;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

/**
 * Indexes data related to projects and applications.
 * The name is a bit misleading - it indexes a lot more data than just measures.
 * We index by project/app UUID, but we get a lot of the data from their main branches.
 */
public class ProjectMeasuresIndexer implements EventIndexer, AnalysisIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_PROJECT_MEASURES,
    entity -> ComponentQualifiers.PROJECT.equals(entity.getQualifier()) || ComponentQualifiers.APP.equals(entity.getQualifier()));
  private static final Set<IndexType> INDEX_TYPES = Set.of(TYPE_PROJECT_MEASURES);

  private final DbClient dbClient;
  private final EsClient esClient;

  public ProjectMeasuresIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return INDEX_TYPES;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndex(Size.LARGE, null);
  }

  public void indexAll() {
    doIndex(Size.REGULAR, null);
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    doIndex(Size.REGULAR, branchUuid);
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, Indexers.EntityEvent cause) {
    return switch (cause) {
      case PERMISSION_CHANGE ->
        // nothing to do, permissions are not used in index type projectmeasures/projectmeasure
        Collections.emptyList();
      case PROJECT_KEY_UPDATE, CREATION, PROJECT_TAGS_UPDATE, DELETION ->
        // when CREATION provisioned projects are supported by WS api/components/search_projects
        prepareForRecovery(dbSession, entityUuids);
    };
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, Indexers.BranchEvent cause) {
    return switch (cause) {
      case DELETION -> Collections.emptyList();
      case MEASURE_CHANGE, SWITCH_OF_MAIN_BRANCH -> {
        Set<String> projectUuids = retrieveProjectUuidsFromBranchUuids(dbSession, branchUuids);
        yield prepareForRecovery(dbSession, projectUuids);
      }
    };
  }

  @NotNull
  private Set<String> retrieveProjectUuidsFromBranchUuids(DbSession dbSession, Collection<String> branchUuids) {
    return dbClient.branchDao().selectByUuids(dbSession, branchUuids)
      .stream().map(BranchDto::getProjectUuid)
      .collect(Collectors.toSet());
  }

  private Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> entityUuids) {
    List<EsQueueDto> items = entityUuids.stream()
      .map(entityUuid -> EsQueueDto.create(TYPE_PROJECT_MEASURES.format(), entityUuid, null, entityUuid))
      .toList();
    return dbClient.esQueueDao().insert(dbSession, items);
  }

  public IndexingResult commitAndIndex(DbSession dbSession, Collection<String> projectUuids) {
    List<EsQueueDto> items = projectUuids.stream()
      .map(projectUuid -> EsQueueDto.create(TYPE_PROJECT_MEASURES.format(), projectUuid, null, projectUuid))
      .toList();
    dbClient.esQueueDao().insert(dbSession, items);

    dbSession.commit();

    return index(dbSession, items);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }
    OneToOneResilientIndexingListener listener = new OneToOneResilientIndexingListener(dbClient, dbSession, items);
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, listener);
    bulkIndexer.start();

    List<String> projectUuids = items.stream().map(EsQueueDto::getDocId).toList();
    List<String> projectToDelete = new ArrayList<>(projectUuids);

    for (String projectUuid : projectUuids) {
      try (ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, projectUuid)) {
        while (rowIt.hasNext()) {
          bulkIndexer.add(toProjectMeasuresDoc(rowIt.next()).toIndexRequest());
          projectToDelete.remove(projectUuid);
        }
      }
    }

    // the remaining uuids reference projects that don't exist in db. They must be deleted from index.
    projectToDelete.forEach(projectUuid -> bulkIndexer.addDeletion(TYPE_PROJECT_MEASURES, projectUuid, AuthorizationDoc.idOf(projectUuid)));

    return bulkIndexer.stop();
  }

  private void doIndex(Size size, @Nullable String branchUuid) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectUuid = null;
      if (branchUuid != null) {
        Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, branchUuid);
        if (branchDto.isEmpty() || !branchDto.get().isMain()) {
          return;
        } else {
          projectUuid = branchDto.get().getProjectUuid();
        }
      }

      try (ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, projectUuid)) {
        BulkIndexer bulkIndexer = createBulkIndexer(size, IndexingListener.FAIL_ON_ERROR);
        bulkIndexer.start();
        while (rowIt.hasNext()) {
          ProjectMeasures doc = rowIt.next();
          bulkIndexer.add(toProjectMeasuresDoc(doc).toIndexRequest());
        }
        bulkIndexer.stop();
      }
    }
  }

  private BulkIndexer createBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_PROJECT_MEASURES, bulkSize, listener);
  }

  private static ProjectMeasuresDoc toProjectMeasuresDoc(ProjectMeasures projectMeasures) {
    ProjectMeasuresIndexerIterator.Project project = projectMeasures.getProject();
    Long analysisDate = project.getAnalysisDate();
    return new ProjectMeasuresDoc()
      .setId(project.getUuid())
      .setKey(project.getKey())
      .setName(project.getName())
      .setQualifier(project.getQualifier())
      .setQualityGateStatus(projectMeasures.getMeasures().getQualityGateStatus())
      .setTags(project.getTags())
      .setAnalysedAt(analysisDate == null ? null : new Date(analysisDate))
      .setCreatedAt(new Date(project.getCreationDate()))
      .setMeasuresFromMap(ProjectMeasuresSoftwareQualityRatingsInitializer.initializeSoftwareQualityRatings(projectMeasures.getMeasures().getNumericMeasures()))
      .setLanguages(new ArrayList<>(projectMeasures.getMeasures().getNclocByLanguages().keySet()))
      .setNclocLanguageDistributionFromMap(projectMeasures.getMeasures().getNclocByLanguages());
  }
}
