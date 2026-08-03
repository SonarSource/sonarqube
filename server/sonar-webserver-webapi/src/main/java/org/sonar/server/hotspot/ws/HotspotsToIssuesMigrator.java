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

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.HotspotMigrationKeyDto;
import org.sonar.db.issue.HotspotToMigrateDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.user.UserSession;

import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;

/**
 * Migrates existing Security Hotspot findings in place to the type their rule was converted to by Phase 1
 * (Vulnerability, Code Smell, or Bug). No new row is created: the existing {@code issues} row is mutated,
 * preserving its key, history, comments and changelog (MMF-5734, Phase 2).
 *
 * <p>Findings are streamed and processed one bounded batch per transaction (never spanning a branch), so a crash
 * only loses the in-flight batch; committed batches are fully consistent (DB + ES index request via es_queue +
 * live measures + Quality Gate) and the remainder are still {@code SECURITY_HOTSPOT}, so a re-run resumes.</p>
 */
public class HotspotsToIssuesMigrator {

  private static final Logger LOG = LoggerFactory.getLogger(HotspotsToIssuesMigrator.class);

  static final String FORMER_HOTSPOT_TAG = "former-hotspot";
  // Max hotspots committed per transaction. Bounds transaction/memory size; committing per issue would be too slow.
  static final int BATCH_SIZE = 1000;
  // Findings fetched per keyset page. Kept <= 1000 so the "load by keys" IN-list stays within Oracle's limit.
  static final int PAGE_SIZE = 1000;
  private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("[\r\n]");

  private final DbClient dbClient;
  private final IssueFieldsSetter issueFieldsSetter;
  private final MigrationBatchWriter batchWriter;
  private final System2 system2;
  private final UserSession userSession;
  private int pageSize = PAGE_SIZE;

  public HotspotsToIssuesMigrator(DbClient dbClient, IssueFieldsSetter issueFieldsSetter, MigrationBatchWriter batchWriter,
    System2 system2, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueFieldsSetter = issueFieldsSetter;
    this.batchWriter = batchWriter;
    this.system2 = system2;
    this.userSession = userSession;
  }

  @VisibleForTesting
  void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public record ProjectMigrationResult(String projectKey, int migrated, int skipped) {
  }

  public record MigrationResult(boolean dryRun, List<ProjectMigrationResult> projects) {
  }

  /**
   * @param projectKey optional project key to scope the migration; {@code null} migrates all projects.
   * @param dryRun     when true, only counts what would migrate/skip — no writes, no reindex, no recompute.
   */
  public MigrationResult migrate(@Nullable String projectKey, boolean dryRun) {
    Set<String> scopeProjectUuids = resolveScope(projectKey);
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(system2.now()), userSession.getUuid()).build();
    MigrationRun run = new MigrationRun(dryRun, context);
    long startedAt = system2.now();
    // Sanitize the user-provided project key before logging it (strip line breaks) to prevent log injection.
    String scope = projectKey == null ? "all projects" : ("project " + LINE_BREAK_PATTERN.matcher(projectKey).replaceAll("_"));
    LOG.info("Hotspots-to-issues migration started (dryRun={}, scope={})", dryRun, scope);

    // Keyset pagination over (branch uuid, issue key): each page is re-queried, so no cursor is held open while a
    // batch is written and committed. Streaming the findings and mutating them in the same pass would truncate the
    // remaining rows once the first commit ends the read snapshot; keyset paging avoids that entirely.
    String lastBranchUuid = null;
    String lastKee = null;
    // A full page (size == PAGE_SIZE) means there may be more; an empty or partial page ends the loop.
    boolean hasMore = true;
    while (hasMore) {
      List<HotspotMigrationKeyDto> page = selectKeyPage(scopeProjectUuids, lastBranchUuid, lastKee);
      hasMore = page.size() == pageSize;
      if (!page.isEmpty()) {
        processPage(page, run);
        // Advance the keyset past this page. Skipped (still-hotspot) rows are stepped over too, so they are not
        // reprocessed within this run; a later re-run starts fresh and skips them again (idempotent).
        HotspotMigrationKeyDto lastRow = page.getLast();
        lastBranchUuid = lastRow.getBranchUuid();
        lastKee = lastRow.getKee();
      }
    }
    run.flushRemaining();

    List<ProjectMigrationResult> projects = run.toProjectResults();
    if (dryRun) {
      LOG.info("Hotspots-to-issues migration dry run finished (scope={}): {} project(s), {} to migrate, {} to skip",
        scope, projects.size(), run.totalMigrated(), run.totalSkipped());
    } else {
      LOG.info("Hotspots-to-issues migration finished (scope={}): {} project(s), {} migrated, {} skipped, took {} ms",
        scope, projects.size(), run.totalMigrated(), run.totalSkipped(), system2.now() - startedAt);
    }
    return new MigrationResult(dryRun, projects);
  }

  private List<HotspotMigrationKeyDto> selectKeyPage(@Nullable Set<String> scopeProjectUuids, @Nullable String lastBranchUuid,
    @Nullable String lastKee) {
    try (DbSession keySession = dbClient.openSession(false)) {
      return dbClient.issueDao().selectHotspotKeysForMigration(keySession, scopeProjectUuids, lastBranchUuid, lastKee, pageSize);
    }
  }

  /**
   * Loads the page's findings by key, then feeds them to the run in the page's (branch, key) order — which comes
   * from a single sorted query. Iterating that order (rather than the load's result order) keeps per-branch batching
   * correct regardless of how the by-keys load partitions its IN-list internally (executeLargeInputs concatenates
   * independently-sorted chunks), so it is not coupled to PAGE_SIZE staying within one partition.
   */
  private void processPage(List<HotspotMigrationKeyDto> page, MigrationRun run) {
    Map<String, HotspotToMigrateDto> hotspotsByKey;
    try (DbSession loadSession = dbClient.openSession(false)) {
      hotspotsByKey = dbClient.issueDao().selectHotspotsForMigrationByKeys(loadSession,
          page.stream().map(HotspotMigrationKeyDto::getKee).toList())
        .stream().collect(Collectors.toMap(HotspotToMigrateDto::getKey, h -> h));
    }
    for (HotspotMigrationKeyDto keyRow : page) {
      HotspotToMigrateDto hotspot = hotspotsByKey.get(keyRow.getKee());
      // Skip if the finding vanished between the key page and the load (e.g. project deleted concurrently).
      if (hotspot != null) {
        run.accept(hotspot);
      }
    }
  }

  /** Accumulates a migration run's current per-branch batch and per-project counters across keyset pages. */
  private final class MigrationRun {
    private final boolean dryRun;
    private final IssueChangeContext context;
    // value = {migrated, skipped}, keyed by project key (components.kee is the project key for every branch).
    private final Map<String, int[]> countsByProjectKey = new LinkedHashMap<>();
    private List<DefaultIssue> batch = new ArrayList<>();
    private String currentBranchUuid;
    private int committedHotspots;

    private MigrationRun(boolean dryRun, IssueChangeContext context) {
      this.dryRun = dryRun;
      this.context = context;
    }

    private void accept(HotspotToMigrateDto hotspot) {
      flushIfBranchBoundaryOrFull(hotspot.getProjectUuid());
      currentBranchUuid = hotspot.getProjectUuid();

      int[] counts = countsByProjectKey.computeIfAbsent(hotspot.getProjectKey(), k -> new int[2]);
      RuleType targetType = hotspot.getRuleTypeEnum();
      // Guard: rule not converted yet (still a hotspot) — skip, do not force a type.
      if (targetType == RuleType.SECURITY_HOTSPOT) {
        LOG.debug("Skipping hotspot {}: rule {} not converted to another type yet", hotspot.getKey(), hotspot.getRuleUuid());
        counts[1]++;
        return;
      }
      counts[0]++;
      if (!dryRun) {
        batch.add(toMigratedIssue(hotspot, targetType));
      }
    }

    // Flush the pending batch when the branch changes or it reaches BATCH_SIZE, so every committed transaction stays
    // within a single branch and bounded in size.
    private void flushIfBranchBoundaryOrFull(String branchUuid) {
      if (!dryRun && !batch.isEmpty() && (!branchUuid.equals(currentBranchUuid) || batch.size() >= BATCH_SIZE)) {
        flush(batch);
        batch = new ArrayList<>();
      }
    }

    private void flushRemaining() {
      if (!dryRun && !batch.isEmpty()) {
        flush(batch);
      }
    }

    private List<ProjectMigrationResult> toProjectResults() {
      return countsByProjectKey.entrySet().stream()
        .map(e -> new ProjectMigrationResult(e.getKey(), e.getValue()[0], e.getValue()[1]))
        .toList();
    }

    private int totalMigrated() {
      return countsByProjectKey.values().stream().mapToInt(c -> c[0]).sum();
    }

    private int totalSkipped() {
      return countsByProjectKey.values().stream().mapToInt(c -> c[1]).sum();
    }

    private DefaultIssue toMigratedIssue(HotspotToMigrateDto hotspot, RuleType targetType) {
      DefaultIssue issue = hotspot.toDefaultIssue();
      // Set type, status and tags on the SAME context so all diffs accumulate into one change and are persisted by
      // a SINGLE save (one issue_changes row; no intermediate save between type and status).
      issueFieldsSetter.setType(issue, targetType, context);
      applyStatusMapping(issue);
      Set<String> tags = new HashSet<>(issue.tags());
      tags.add(FORMER_HOTSPOT_TAG);
      issueFieldsSetter.setTags(issue, tags, context);
      return issue;
    }

    /** Persists one bounded batch atomically (single transaction) then indexes — see {@link MigrationBatchWriter}. */
    private void flush(List<DefaultIssue> pending) {
      batchWriter.write(pending);
      committedHotspots += pending.size();
      LOG.info("Committed migration batch of {} hotspots on branch {} ({} migrated so far)",
        pending.size(), currentBranchUuid, committedHotspots);
    }

    /**
     * Maps the hotspot review status/resolution to the issue status/resolution on the run's context, so the change
     * is recorded as diffs and saved together with the type change. Independent of the target type.
     *
     * <pre>
     *   TO_REVIEW               -> OPEN       (resolution null)
     *   REVIEWED / ACKNOWLEDGED -> CONFIRMED  (resolution null)
     *   REVIEWED / FIXED        -> CLOSED     resolution FIXED
     *   REVIEWED / SAFE         -> CLOSED     resolution WONTFIX
     * </pre>
     */
    // Legacy STATUS_*/RESOLUTION_* constants are deprecated on the plugin API but remain the values persisted in
    // the issues.status/resolution columns that IssueFieldsSetter.setStatus/setResolution write.
    @SuppressWarnings("deprecation")
    private void applyStatusMapping(DefaultIssue issue) {
      String status = issue.status();
      String resolution = issue.resolution();

      String newStatus;
      String newResolution;
      if (Issue.STATUS_TO_REVIEW.equals(status)) {
        newStatus = Issue.STATUS_OPEN;
        newResolution = null;
      } else if (Issue.STATUS_REVIEWED.equals(status)) {
        if (Issue.RESOLUTION_FIXED.equals(resolution)) {
          newStatus = Issue.STATUS_CLOSED;
          newResolution = Issue.RESOLUTION_FIXED;
        } else if (Issue.RESOLUTION_SAFE.equals(resolution)) {
          newStatus = Issue.STATUS_CLOSED;
          newResolution = Issue.RESOLUTION_WONT_FIX;
        } else {
          // ACKNOWLEDGED (or any other reviewed resolution) -> Confirmed
          newStatus = Issue.STATUS_CONFIRMED;
          newResolution = null;
        }
      } else if (Issue.STATUS_CLOSED.equals(status)) {
        // A hotspot is CLOSED when it is removed from the code (resolution REMOVED/FIXED). CLOSED is a valid shared
        // status in the issue workflow with the same meaning, so keep status/resolution as-is — no remap needed, and
        // it is not a data inconsistency to warn about.
        return;
      } else {
        LOG.warn("Unexpected hotspot status '{}' for issue {}, leaving status/resolution unchanged", status, issue.key());
        return;
      }

      // Resolution first, then status; both diffs land in the same change on this context.
      issueFieldsSetter.setResolution(issue, newResolution, context);
      issueFieldsSetter.setStatus(issue, newStatus, context);
    }
  }

  @Nullable
  private Set<String> resolveScope(@Nullable String projectKey) {
    if (projectKey == null) {
      return null;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));
      return Set.of(project.getUuid());
    }
  }
}
