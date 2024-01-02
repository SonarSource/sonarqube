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
package org.sonar.db.purge;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchMapper;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PurgeDao implements Dao {
  private static final Logger LOG = Loggers.get(PurgeDao.class);
  private static final Set<String> QUALIFIERS_PROJECT_VIEW = ImmutableSet.of("TRK", "VW");
  private static final Set<String> QUALIFIERS_MODULE_SUBVIEW = ImmutableSet.of("BRC", "SVW");
  private static final String SCOPE_PROJECT = "PRJ";

  private final System2 system2;
  private final AuditPersister auditPersister;

  public PurgeDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public void purge(DbSession session, PurgeConfiguration conf, PurgeListener listener, PurgeProfiler profiler) {
    PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    PurgeCommands commands = new PurgeCommands(session, mapper, profiler, system2);
    String rootUuid = conf.rootUuid();
    deleteAbortedAnalyses(rootUuid, commands);
    purgeAnalyses(commands, rootUuid);
    purgeDisabledComponents(commands, conf, listener);
    deleteOldClosedIssues(conf, mapper, listener);
    purgeOldCeActivities(rootUuid, commands);
    purgeOldCeScannerContexts(rootUuid, commands);

    deleteOldDisabledComponents(commands, mapper, rootUuid);
    purgeStaleBranches(commands, conf, mapper, rootUuid);
  }

  private static void purgeStaleBranches(PurgeCommands commands, PurgeConfiguration conf, PurgeMapper mapper, String rootUuid) {
    Optional<Date> maxDate = conf.maxLiveDateOfInactiveBranches();
    if (maxDate.isEmpty()) {
      // not available if branch plugin is not installed
      return;
    }
    LOG.debug("<- Purge stale branches");

    Long maxDateValue = ofNullable(dateToLong(maxDate.get())).orElseThrow(IllegalStateException::new);
    List<String> branchUuids = mapper.selectStaleBranchesAndPullRequests(conf.projectUuid(), maxDateValue);

    for (String branchUuid : branchUuids) {
      if (!rootUuid.equals(branchUuid)) {
        deleteRootComponent(branchUuid, mapper, commands);
      }
    }
  }

  private static void purgeAnalyses(PurgeCommands commands, String rootUuid) {
    List<String> analysisUuids = commands.selectSnapshotUuids(
      new PurgeSnapshotQuery(rootUuid)
        .setIslast(false)
        .setNotPurged(true));
    commands.purgeAnalyses(analysisUuids);
  }

  private static void purgeDisabledComponents(PurgeCommands commands, PurgeConfiguration conf, PurgeListener listener) {
    String rootUuid = conf.rootUuid();
    listener.onComponentsDisabling(rootUuid, conf.getDisabledComponentUuids());
    commands.purgeDisabledComponents(rootUuid, conf.getDisabledComponentUuids(), listener);
  }

  private static void deleteOldClosedIssues(PurgeConfiguration conf, PurgeMapper mapper, PurgeListener listener) {
    Date toDate = conf.maxLiveDateOfClosedIssues();
    String rootUuid = conf.rootUuid();
    List<String> issueKeys = mapper.selectOldClosedIssueKeys(rootUuid, dateToLong(toDate));
    executeLargeInputs(issueKeys, input -> {
      mapper.deleteIssueChangesFromIssueKeys(input);
      return emptyList();
    });

    executeLargeInputs(issueKeys, input -> {
      mapper.deleteNewCodeReferenceIssuesFromKeys(input);
      return emptyList();
    });

    executeLargeInputs(issueKeys, input -> {
      mapper.deleteIssuesFromKeys(input);
      return emptyList();
    });
    listener.onIssuesRemoval(rootUuid, issueKeys);
  }

  private static void deleteAbortedAnalyses(String rootUuid, PurgeCommands commands) {
    LOG.debug("<- Delete aborted builds");
    commands.deleteAbortedAnalyses(rootUuid);
  }

  private static void deleteOldDisabledComponents(PurgeCommands commands, PurgeMapper mapper, String rootUuid) {
    List<String> disabledComponentsWithoutIssue = mapper.selectDisabledComponentsWithoutIssues(rootUuid);
    commands.deleteDisabledComponentsWithoutIssues(disabledComponentsWithoutIssue);
  }

  public List<PurgeableAnalysisDto> selectPurgeableAnalyses(String componentUuid, DbSession session) {
    PurgeMapper mapper = mapper(session);
    return mapper.selectPurgeableAnalyses(componentUuid).stream()
      .filter(new NewCodePeriodAnalysisFilter(mapper, componentUuid))
      .sorted()
      .collect(MoreCollectors.toList());
  }

  public void purgeCeActivities(DbSession session, PurgeProfiler profiler) {
    PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    PurgeCommands commands = new PurgeCommands(session, mapper, profiler, system2);
    purgeOldCeActivities(null, commands);
  }

  private void purgeOldCeActivities(@Nullable String rootUuid, PurgeCommands commands) {
    Date sixMonthsAgo = DateUtils.addDays(new Date(system2.now()), -180);
    commands.deleteCeActivityBefore(rootUuid, sixMonthsAgo.getTime());
  }

  public void purgeCeScannerContexts(DbSession session, PurgeProfiler profiler) {
    PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    PurgeCommands commands = new PurgeCommands(session, mapper, profiler, system2);
    purgeOldCeScannerContexts(null, commands);
  }

  private void purgeOldCeScannerContexts(@Nullable String rootUuid, PurgeCommands commands) {
    Date fourWeeksAgo = DateUtils.addDays(new Date(system2.now()), -28);
    commands.deleteCeScannerContextBefore(rootUuid, fourWeeksAgo.getTime());
  }

  private static final class NewCodePeriodAnalysisFilter implements Predicate<PurgeableAnalysisDto> {
    @Nullable
    private final String analysisUuid;

    private NewCodePeriodAnalysisFilter(PurgeMapper mapper, String componentUuid) {
      this.analysisUuid = mapper.selectSpecificAnalysisNewCodePeriod(componentUuid);
    }

    @Override
    public boolean test(PurgeableAnalysisDto purgeableAnalysisDto) {
      return analysisUuid == null || !analysisUuid.equals(purgeableAnalysisDto.getAnalysisUuid());
    }
  }

  public void deleteBranch(DbSession session, String uuid) {
    PurgeProfiler profiler = new PurgeProfiler();
    PurgeMapper purgeMapper = mapper(session);
    PurgeCommands purgeCommands = new PurgeCommands(session, profiler, system2);
    deleteRootComponent(uuid, purgeMapper, purgeCommands);
  }

  public void deleteProject(DbSession session, String uuid, String qualifier, String name, String key) {
    PurgeProfiler profiler = new PurgeProfiler();
    PurgeMapper purgeMapper = mapper(session);
    PurgeCommands purgeCommands = new PurgeCommands(session, profiler, system2);
    long start = System2.INSTANCE.now();

    List<String> branchUuids = session.getMapper(BranchMapper.class).selectByProjectUuid(uuid).stream()
      .map(BranchDto::getUuid)
      .filter(branchUuid -> !uuid.equals(branchUuid))
      .toList();

    branchUuids.forEach(id -> deleteRootComponent(id, purgeMapper, purgeCommands));

    deleteRootComponent(uuid, purgeMapper, purgeCommands);
    auditPersister.deleteComponent(session, new ComponentNewValue(uuid, name, key, qualifier));
    logProfiling(profiler, start);
  }

  private static void logProfiling(PurgeProfiler profiler, long start) {
    long duration = System.currentTimeMillis() - start;
    LOG.info("");
    LOG.info(" -------- Profiling for project deletion: " + TimeUtils.formatDuration(duration) + " --------");
    LOG.info("");
    profiler.dump(duration, LOG);
    LOG.info("");
    LOG.info(" -------- End of profiling for project deletion--------");
    LOG.info("");
  }

  private static void deleteRootComponent(String rootUuid, PurgeMapper mapper, PurgeCommands commands) {
    List<String> rootAndModulesOrSubviews = mapper.selectRootAndModulesOrSubviewsByProjectUuid(rootUuid);
    commands.deleteLinks(rootUuid);
    commands.deleteScannerCache(rootUuid);
    commands.deleteAnalyses(rootUuid);
    commands.deleteByRootAndModulesOrSubviews(rootAndModulesOrSubviews);
    commands.deleteIssues(rootUuid);
    commands.deleteFileSources(rootUuid);
    commands.deleteCeActivity(rootUuid);
    commands.deleteCeQueue(rootUuid);
    commands.deleteWebhooks(rootUuid);
    commands.deleteWebhookDeliveries(rootUuid);
    commands.deleteLiveMeasures(rootUuid);
    commands.deleteProjectMappings(rootUuid);
    commands.deleteProjectAlmSettings(rootUuid);
    commands.deletePermissions(rootUuid);
    commands.deleteNewCodePeriods(rootUuid);
    commands.deleteBranch(rootUuid);
    commands.deleteApplicationBranchProjects(rootUuid);
    commands.deleteApplicationProjects(rootUuid);
    commands.deleteApplicationProjectsByProject(rootUuid);
    commands.deleteProjectInPortfolios(rootUuid);
    commands.deleteComponents(rootUuid);
    commands.deleteComponentsByMainBranchProjectUuid(rootUuid);
    commands.deleteProjectBadgeToken(rootUuid);
    commands.deleteProject(rootUuid);
    commands.deleteUserDismissedMessages(rootUuid);
    commands.deleteOutdatedProperties(rootUuid);
  }

  /**
   * Delete the non root components (ie. sub-view, application or project copy) from the specified collection of {@link ComponentDto}
   * and data from their child tables.
   * <p>
   * This method has no effect when passed an empty collection or only root components.
   * </p>
   */
  public void deleteNonRootComponentsInView(DbSession dbSession, Collection<ComponentDto> components) {
    Set<ComponentDto> nonRootComponents = components.stream().filter(PurgeDao::isNotRoot).collect(MoreCollectors.toSet());
    if (nonRootComponents.isEmpty()) {
      return;
    }

    PurgeProfiler profiler = new PurgeProfiler();
    PurgeCommands purgeCommands = new PurgeCommands(dbSession, profiler, system2);
    deleteNonRootComponentsInView(nonRootComponents, purgeCommands);
  }

  private static void deleteNonRootComponentsInView(Set<ComponentDto> nonRootComponents, PurgeCommands purgeCommands) {
    List<String> subviewsOrProjectCopies = nonRootComponents.stream()
      .filter(PurgeDao::isModuleOrSubview)
      .map(ComponentDto::uuid)
      .collect(MoreCollectors.toList());
    purgeCommands.deleteByRootAndModulesOrSubviews(subviewsOrProjectCopies);
    List<String> nonRootComponentUuids = nonRootComponents.stream().map(ComponentDto::uuid).collect(MoreCollectors.toList(nonRootComponents.size()));
    purgeCommands.deleteComponentMeasures(nonRootComponentUuids);
    purgeCommands.deleteComponents(nonRootComponentUuids);
  }

  private static boolean isNotRoot(ComponentDto dto) {
    return !(SCOPE_PROJECT.equals(dto.scope()) && QUALIFIERS_PROJECT_VIEW.contains(dto.qualifier()));
  }

  private static boolean isModuleOrSubview(ComponentDto dto) {
    return SCOPE_PROJECT.equals(dto.scope()) && QUALIFIERS_MODULE_SUBVIEW.contains(dto.qualifier());
  }

  public void deleteAnalyses(DbSession session, PurgeProfiler profiler, List<String> analysisUuids) {
    new PurgeCommands(session, profiler, system2).deleteAnalyses(analysisUuids);
  }

  private static PurgeMapper mapper(DbSession session) {
    return session.getMapper(PurgeMapper.class);
  }

}
