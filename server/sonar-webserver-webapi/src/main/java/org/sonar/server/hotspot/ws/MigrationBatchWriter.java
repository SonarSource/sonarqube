/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.IssueUpdatedTelemetryPublisher;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;

/**
 * Persists one batch of already-transformed migrated issues in a SINGLE atomic transaction, then indexes.
 *
 * <p>We deliberately do NOT use {@code WebIssueStorage.save}: for existing issues it routes to {@code update()},
 * which commits the row change + changelog in its own session before the index request/measures — leaving a window
 * where a crash yields a migrated-but-unindexed row the {@code issue_type=4} scroll can't re-select. Instead
 * everything is done on one session and committed once:</p>
 * <ol>
 *   <li>update the issue row ({@code IssueDao.update}),</li>
 *   <li>append the {@code issue_changes} diff ({@code IssueStorage.insertChanges}),</li>
 *   <li>enqueue the {@code es_queue} index request without committing ({@code IssueIndexer.enqueueForIndexing}),</li>
 *   <li>recompute measures + QG ({@code IssueChangePostProcessor.process}, which commits the session to persist
 *       live measures + the branch index request, and broadcasts portfolio refresh),</li>
 *   <li>a final {@code commit()} in case {@code process} short-circuited without committing.</li>
 * </ol>
 * Steps 1–4 land in one commit, so there is no committed state where the type change lacks its index request or
 * measures. The ES write happens AFTER the commit; if it never runs (crash), the committed {@code es_queue} rows
 * are replayed by the recovery indexer.
 */
public class MigrationBatchWriter {

  private final DbClient dbClient;
  private final WebIssueStorage issueStorage;
  private final IssueChangePostProcessor issueChangePostProcessor;
  private final IssueIndexer issueIndexer;
  private final UuidFactory uuidFactory;
  private final System2 system2;
  private final IssueUpdatedTelemetryPublisher issueUpdatedTelemetryPublisher;

  public MigrationBatchWriter(DbClient dbClient, WebIssueStorage issueStorage,
    IssueChangePostProcessor issueChangePostProcessor, IssueIndexer issueIndexer, UuidFactory uuidFactory, System2 system2,
    IssueUpdatedTelemetryPublisher issueUpdatedTelemetryPublisher) {
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.issueChangePostProcessor = issueChangePostProcessor;
    this.issueIndexer = issueIndexer;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
    this.issueUpdatedTelemetryPublisher = issueUpdatedTelemetryPublisher;
  }

  public void write(List<DefaultIssue> batch) {
    long now = system2.now();
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueChangeMapper changeMapper = dbSession.getMapper(IssueChangeMapper.class);
      List<IssueDto> updatedDtos = new ArrayList<>(batch.size());
      for (DefaultIssue issue : batch) {
        IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
        dbClient.issueDao().update(dbSession, dto);
        issueStorage.insertChanges(changeMapper, issue, uuidFactory);
        updatedDtos.add(dto);
      }
      Collection<EsQueueDto> esItems = issueIndexer.enqueueForIndexing(dbSession, updatedDtos);
      // Measures + QG for the batch's branch (QG event triggers portfolio/application refresh). Commits the session.
      issueChangePostProcessor.process(dbSession, batch, touchedComponents(dbSession, batch), false);
      dbSession.commit();
      // Bypasses WebIssueStorage.save (see class javadoc), so the issue-updated telemetry hook is called here too.
      issueUpdatedTelemetryPublisher.publish(dbSession, batch);
      // Post-commit ES write; on failure the committed es_queue rows self-heal via the recovery indexer.
      issueIndexer.index(dbSession, esItems);
    }
  }

  private List<ComponentDto> touchedComponents(DbSession dbSession, List<DefaultIssue> changedIssues) {
    Set<String> componentUuids = changedIssues.stream().map(DefaultIssue::componentUuid).collect(Collectors.toSet());
    return dbClient.componentDao().selectByUuids(dbSession, componentUuids);
  }
}