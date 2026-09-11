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
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;

/**
 * Persists one batch of already-transformed migrated issues on a batch session, then indexes.
 *
 * <p>We deliberately do NOT use {@code WebIssueStorage.save}: for existing issues it routes to {@code update()},
 * which commits the row change + changelog in its own session before the index request/measures — leaving a window
 * where a crash yields a migrated-but-unindexed row the {@code issue_type=4} scroll can't re-select. Instead the
 * work is done on one session:</p>
 * <ol>
 *   <li>update the issue rows, then the {@code issue_changes} diffs — one pass per statement, see
 *       {@link #write(java.util.List)} for why the passes must not be interleaved. Impacts are deliberately left
 *       alone ({@code updateWithoutIssueImpacts}): this migration never changes them, so rewriting them would be
 *       pure write amplification (SONAR-32244),</li>
 *   <li>enqueue the {@code es_queue} index request ({@code IssueIndexer.enqueueForIndexing}),</li>
 *   <li>recompute measures + QG ({@code IssueChangePostProcessor.process}, which commits the session to persist
 *       live measures + the branch index request, and broadcasts portfolio refresh),</li>
 *   <li>a final {@code commit()} in case {@code process} short-circuited without committing.</li>
 * </ol>
 * The ES write happens AFTER the commit; if it never runs (crash), the committed {@code es_queue} rows are
 * replayed by the recovery indexer.
 *
 * <p><b>The batch is NOT one atomic transaction.</b> {@code openSession(true)} returns a {@code BatchSession},
 * which commits by itself every {@code BatchSession.MAX_BATCH_SIZE} (250) mutating statements — a read resets that
 * counter, but the passes below issue none. At {@code HotspotsToIssuesMigrator.BATCH_SIZE} = 1000 each batch emits
 * roughly 3000 statements (one update and one changelog per issue, plus the {@code es_queue} rows), so about a
 * dozen implicit commits happen per batch. Batch mode is kept because it raises migration throughput
 * substantially; the cost is that a crash can leave a batch half-written.</p>
 *
 * <p>The reads in step 3 are still correct despite batch mode: MyBatis' {@code BatchExecutor} flushes pending
 * statements before executing a query on the same session.</p>
 *
 * <p><b>A crash mid-batch can lose data, and that is an accepted trade-off — do not "fix" it by reordering the
 * passes without discussing it.</b> The update pass runs first, so an implicit commit can leave up to
 * {@code MAX_BATCH_SIZE} findings whose {@code issue_type} has changed but which have no {@code issue_changes}
 * entry and no {@code es_queue} row. Those are not recoverable: a later run re-selects only findings still typed
 * 4, so it will skip them. Concretely they keep a permanent gap in their issue activity, and their ES document
 * still says {@code SECURITY_HOTSPOT} until something else reindexes them — note {@code RecoveryIndexer} cannot
 * help, since it replays only rows that were actually enqueued in {@code es_queue}. The window is narrow (a crash
 * between an implicit commit and the end of the batch) and the consequence is cosmetic rather than corrupting,
 * which is why batch mode is kept for the throughput it buys.</p>
 */
public class MigrationBatchWriter {

  private final DbClient dbClient;
  private final WebIssueStorage issueStorage;
  private final IssueChangePostProcessor issueChangePostProcessor;
  private final IssueIndexer issueIndexer;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public MigrationBatchWriter(DbClient dbClient, WebIssueStorage issueStorage,
    IssueChangePostProcessor issueChangePostProcessor, IssueIndexer issueIndexer, UuidFactory uuidFactory, System2 system2) {
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.issueChangePostProcessor = issueChangePostProcessor;
    this.issueIndexer = issueIndexer;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  public void write(List<DefaultIssue> batch) {
    long now = system2.now();
    try (DbSession dbSession = dbClient.openSession(true)) {
      IssueChangeMapper changeMapper = dbSession.getMapper(IssueChangeMapper.class);
      IssueDao issueDao = dbClient.issueDao();
      List<IssueDto> updatedDtos = batch.stream().map(issue -> IssueDto.toDtoForUpdate(issue, now)).toList();

      // One contiguous pass per statement, not one pass per issue. MyBatis' BatchExecutor only appends to the
      // current batch while the statement is unchanged, and prepares a fresh PreparedStatement - held open until
      // flush - on every switch. Interleaving the 2 statements per issue would therefore accumulate thousands
      // of single-row statements and batch nothing at all. Same reason IssueDao exposes the *WithoutImpacts
      // variants, and the same grouping PersistIssuesStep uses.
      updatedDtos.forEach(dto -> issueDao.updateWithoutIssueImpacts(dbSession, dto));
      batch.forEach(issue -> issueStorage.insertChanges(changeMapper, issue, uuidFactory));

      Collection<EsQueueDto> esItems = issueIndexer.enqueueForIndexing(dbSession, updatedDtos);
      // Measures + QG for the batch's branch (QG event triggers portfolio/application refresh). Commits the session.
      issueChangePostProcessor.process(dbSession, batch, touchedComponents(dbSession, batch), false);
      dbSession.commit();
      // Post-commit ES write; on failure the committed es_queue rows self-heal via the recovery indexer.
      issueIndexer.index(dbSession, esItems);
    }
  }

  private List<ComponentDto> touchedComponents(DbSession dbSession, List<DefaultIssue> changedIssues) {
    Set<String> componentUuids = changedIssues.stream().map(DefaultIssue::componentUuid).collect(Collectors.toSet());
    return dbClient.componentDao().selectByUuids(dbSession, componentUuids);
  }
}
