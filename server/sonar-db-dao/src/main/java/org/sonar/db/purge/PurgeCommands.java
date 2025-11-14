/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

class PurgeCommands {

  private static final int MAX_SNAPSHOTS_PER_QUERY = 1000;
  private static final int MAX_RESOURCES_PER_QUERY = 1000;
  private static final String[] UNPROCESSED_STATUS = new String[]{"U"};

  private final DbSession session;
  private final PurgeMapper purgeMapper;
  private final PurgeProfiler profiler;
  private final System2 system2;

  PurgeCommands(DbSession session, PurgeMapper purgeMapper, PurgeProfiler profiler, System2 system2) {
    this.session = session;
    this.purgeMapper = purgeMapper;
    this.profiler = profiler;
    this.system2 = system2;
  }

  PurgeCommands(DbSession session, PurgeProfiler profiler, System2 system2) {
    this(session, session.getMapper(PurgeMapper.class), profiler, system2);
  }

  List<String> selectSnapshotUuids(PurgeSnapshotQuery query) {
    return purgeMapper.selectAnalysisUuids(query);
  }

  void deleteEventComponentChanges(String rootComponentUuid) {
    profiler.start("deleteAnalyses (event_component_changes)");
    purgeMapper.deleteEventComponentChangesByComponentUuid(rootComponentUuid);
    session.commit();
    profiler.stop();
  }

  void deleteAnalyses(String rootComponentUuid) {
    profiler.start("deleteAnalyses (events)");
    purgeMapper.deleteEventsByComponentUuid(rootComponentUuid);
    session.commit();
    profiler.stop();

    List<List<String>> analysisUuidsPartitions = Lists.partition(purgeMapper.selectAnalysisUuids(new PurgeSnapshotQuery(rootComponentUuid)), MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteAnalyses (project_measures)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisMeasures);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (analysis_properties)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisProperties);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalyses);
    session.commit();
    profiler.stop();
  }

  void deleteAbortedAnalyses(String rootUuid) {
    PurgeSnapshotQuery query = new PurgeSnapshotQuery(rootUuid)
      .setIslast(false)
      .setStatus(UNPROCESSED_STATUS);
    deleteAnalyses(purgeMapper.selectAnalysisUuids(query));
  }

  @VisibleForTesting
  void deleteAnalyses(List<String> analysisIdUuids) {
    List<List<String>> analysisUuidsPartitions = Lists.partition(analysisIdUuids, MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteAnalyses (event_component_changes)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisEventComponentChanges);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (events)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisEvents);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (project_measures)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisMeasures);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (analysis_properties)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisProperties);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalyses);
    session.commit();
    profiler.stop();
  }

  void purgeAnalyses(List<String> analysisUuids) {
    List<List<String>> analysisUuidsPartitions = Lists.partition(analysisUuids, MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("updatePurgeStatusToOne (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::updatePurgeStatusToOne);
    session.commit();
    profiler.stop();
  }

  void purgeDisabledComponents(String rootComponentUuid, Collection<String> disabledComponentUuids, PurgeListener listener) {

    profiler.start("purgeDisabledComponents (file_sources)");
      executeLargeInputs(
        purgeMapper.selectDisabledComponentsWithFileSource(rootComponentUuid),
        input -> {
          purgeMapper.deleteFileSourcesByFileUuid(input);
          return input;
        });
    profiler.stop();

    profiler.start("purgeDisabledComponents (unresolved_issues)");
      executeLargeInputs(
        purgeMapper.selectDisabledComponentsWithUnresolvedIssues(rootComponentUuid),
        input -> {
          purgeMapper.resolveComponentIssuesNotAlreadyResolved(input, system2.now());
          return input;
        });
    profiler.stop();

    profiler.start("purgeDisabledComponents (measures)");
    executeLargeInputs(
      purgeMapper.selectDisabledComponentsWithMeasures(rootComponentUuid),
      input -> {
        purgeMapper.deleteMeasuresByComponentUuids(input);
        return input;
      });
    profiler.stop();

    session.commit();
  }

  private void deleteAnalysisDuplications(List<List<String>> snapshotUuidsPartitions) {
    profiler.start("deleteAnalysisDuplications (duplications_index)");
    snapshotUuidsPartitions.forEach(purgeMapper::deleteAnalysisDuplications);
    session.commit();
    profiler.stop();
  }

  void deletePermissions(String entityUuid) {
    profiler.start("deletePermissions (group_roles)");
    purgeMapper.deleteGroupRolesByEntityUuid(entityUuid);
    session.commit();
    profiler.stop();

    profiler.start("deletePermissions (user_roles)");
    purgeMapper.deleteUserRolesByEntityUuid(entityUuid);
    session.commit();
    profiler.stop();
  }

  void deleteIssues(String rootUuid) {
    profiler.start("deleteIssues (issue_changes)");
    purgeMapper.deleteIssueChangesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteIssues (new_code_reference_issues)");
    purgeMapper.deleteNewCodeReferenceIssuesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteIssues (issues_impacts)");
    purgeMapper.deleteIssuesImpactsByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteIssues (issues)");
    purgeMapper.deleteIssuesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteLinks(String rootUuid) {
    profiler.start("deleteLinks (project_links)");
    purgeMapper.deleteProjectLinksByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteByRootAndSubviews(List<String> rootAndSubviewsUuids) {
    if (rootAndSubviewsUuids.isEmpty()) {
      return;
    }
    List<List<String>> uuidsPartitions = Lists.partition(rootAndSubviewsUuids, MAX_RESOURCES_PER_QUERY);

    profiler.start("deleteByRootAndSubviews (properties)");
    uuidsPartitions.forEach(purgeMapper::deletePropertiesByEntityUuids);
    session.commit();
    profiler.stop();

    profiler.start("deleteByRootAndSubviews (report_schedules)");
    uuidsPartitions.forEach(purgeMapper::deleteReportSchedulesByPortfolioUuids);
    session.commit();
    profiler.stop();

    profiler.start("deleteByRootAndSubviews (report_subscriptions)");
    uuidsPartitions.forEach(purgeMapper::deleteReportSubscriptionsByPortfolioUuids);
    session.commit();
    profiler.stop();
  }

  void deleteDisabledComponentsWithoutIssues(List<String> disabledComponentsWithoutIssue) {
    if (disabledComponentsWithoutIssue.isEmpty()) {
      return;
    }
    List<List<String>> uuidsPartitions = Lists.partition(disabledComponentsWithoutIssue, MAX_RESOURCES_PER_QUERY);

    profiler.start("deleteDisabledComponentsWithoutIssues (properties)");
    uuidsPartitions.forEach(purgeMapper::deletePropertiesByEntityUuids);
    session.commit();
    profiler.stop();

    profiler.start("deleteDisabledComponentsWithoutIssues (projects)");
    uuidsPartitions.forEach(purgeMapper::deleteComponentsByUuids);
    session.commit();
    profiler.stop();
  }

  void deleteOutdatedProperties(String entityUuid) {
    profiler.start("deleteOutdatedProperties (properties)");
    purgeMapper.deletePropertiesByEntityUuids(List.of(entityUuid));
    session.commit();
    profiler.stop();
  }

  void deleteComponents(String rootUuid) {
    profiler.start("deleteComponents (projects)");
    purgeMapper.deleteComponentsByBranchUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteNonMainBranchComponentsByProjectUuid(String uuid) {
    profiler.start("deleteNonMainBranchComponentsByProjectUuid (projects)");
    purgeMapper.deleteNonMainBranchComponentsByProjectUuid(uuid);
    session.commit();
    profiler.stop();
  }

  void deleteProject(String projectUuid) {
    profiler.start("deleteProject (projects)");
    purgeMapper.deleteProjectsByProjectUuid(projectUuid);
    session.commit();
    profiler.stop();
  }

  void deleteComponents(List<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      return;
    }

    profiler.start("deleteComponents (projects)");
    Lists.partition(componentUuids, MAX_RESOURCES_PER_QUERY).forEach(purgeMapper::deleteComponentsByUuids);
    session.commit();
    profiler.stop();
  }

  void deleteComponentMeasures(List<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      return;
    }

    profiler.start("deleteComponentMeasures (project_measures)");
    Lists.partition(componentUuids, MAX_RESOURCES_PER_QUERY).forEach(purgeMapper::fullDeleteComponentMeasures);
    session.commit();
    profiler.stop();
  }

  void deleteFileSources(String rootUuid) {
    profiler.start("deleteFileSources (file_sources)");
    purgeMapper.deleteFileSourcesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteCeActivity(String rootUuid) {
    profiler.start("deleteCeActivity (ce_scanner_context)");
    purgeMapper.deleteCeScannerContextOfCeActivityByRootUuidOrBefore(rootUuid, null, null);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_task_characteristics)");
    purgeMapper.deleteCeTaskCharacteristicsOfCeActivityByRootUuidOrBefore(rootUuid, null, null);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_task_input)");
    purgeMapper.deleteCeTaskInputOfCeActivityByRootUuidOrBefore(rootUuid, null, null);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_task_message)");
    purgeMapper.deleteCeTaskMessageOfCeActivityByRootUuidOrBefore(rootUuid, null, null);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_activity)");
    purgeMapper.deleteCeActivityByRootUuidOrBefore(rootUuid, null, null);
    session.commit();
    profiler.stop();
  }

  void deleteCeActivityBefore(@Nullable String rootUuid, @Nullable String entityUuidToPurge, long createdAt) {
    profiler.start("deleteCeActivityBefore (ce_scanner_context)");
    purgeMapper.deleteCeScannerContextOfCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivityBefore (ce_task_characteristics)");
    purgeMapper.deleteCeTaskCharacteristicsOfCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivityBefore (ce_task_input)");
    purgeMapper.deleteCeTaskInputOfCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivityBefore (ce_task_message)");
    purgeMapper.deleteCeTaskMessageOfCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivityBefore (ce_activity)");
    purgeMapper.deleteCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
    profiler.stop();
  }

  void deleteCeScannerContextBefore(@Nullable String rootUuid, @Nullable String entityUuidToPurge, long createdAt) {
    // assuming CeScannerContext of rows in table CE_QUEUE can't be older than createdAt
    profiler.start("deleteCeScannerContextBefore");
    purgeMapper.deleteCeScannerContextOfCeActivityByRootUuidOrBefore(rootUuid, entityUuidToPurge, createdAt);
    session.commit();
  }

  void deleteCeQueue(String rootUuid) {
    profiler.start("deleteCeQueue (ce_scanner_context)");
    purgeMapper.deleteCeScannerContextOfCeQueueByRootUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_task_characteristics)");
    purgeMapper.deleteCeTaskCharacteristicsOfCeQueueByRootUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_task_input)");
    purgeMapper.deleteCeTaskInputOfCeQueueByRootUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_task_message)");
    purgeMapper.deleteCeTaskMessageOfCeQueueByRootUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_queue)");
    purgeMapper.deleteCeQueueByRootUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteWebhooks(String rootUuid) {
    profiler.start("deleteWebhooks (webhooks)");
    purgeMapper.deleteWebhooksByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteWebhookDeliveries(String rootUuid) {
    profiler.start("deleteWebhookDeliveries (webhook_deliveries)");
    purgeMapper.deleteWebhookDeliveriesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteApplicationProjectsByProject(String projectUuid) {
    profiler.start("deleteApplicationProjectsByProject (app_projects)");
    purgeMapper.deleteAppBranchProjectBranchesByProjectUuid(projectUuid);
    purgeMapper.deleteAppProjectsByProjectUuid(projectUuid);
    session.commit();
    profiler.stop();
  }

  void deleteApplicationProjects(String applicationUuid) {
    profiler.start("deleteApplicationProjects (app_projects)");
    purgeMapper.deleteAppBranchProjectBranchesByAppUuid(applicationUuid);
    purgeMapper.deleteAppProjectsByAppUuid(applicationUuid);
    session.commit();
    profiler.stop();
  }

  void deleteApplicationBranchProjects(String applicationBranchUuid) {
    profiler.start("deleteApplicationBranchProjects (app_branch_project_branch)");
    purgeMapper.deleteAppBranchProjectsByAppBranchUuid(applicationBranchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteProjectAlmSettings(String rootUuid) {
    profiler.start("deleteProjectAlmSettings (project_alm_settings)");
    purgeMapper.deleteProjectAlmSettingsByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteProjectBadgeToken(String rootUuid) {
    profiler.start("deleteProjectBadgeToken (project_badge_token)");
    purgeMapper.deleteProjectBadgeTokenByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteBranch(String rootUuid) {
    profiler.start("deleteBranch (project_branches)");
    purgeMapper.deleteAppBranchProjectBranchesByProjectBranchUuid(rootUuid);
    purgeMapper.deleteBranchByUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteMeasures(String rootUuid) {
    profiler.start("deleteMeasures (measures)");
    purgeMapper.deleteMeasuresByBranchUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteNewCodePeriodsForProject(String projectUuid) {
    profiler.start("deleteNewCodePeriods (new_code_periods)");
    purgeMapper.deleteNewCodePeriodsByProjectUuid(projectUuid);
    session.commit();
    profiler.stop();
  }

  void deleteNewCodePeriodsForBranch(String branchUuid) {
    profiler.start("deleteNewCodePeriods (new_code_periods)");
    purgeMapper.deleteNewCodePeriodsByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  void deleteUserDismissedMessages(String projectUuid) {
    profiler.start("deleteUserDismissedMessages (user_dismissed_messages)");
    purgeMapper.deleteUserDismissedMessagesByProjectUuid(projectUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteProjectInPortfolios(String rootUuid) {
    profiler.start("deleteProjectInPortfolios (portfolio_projects)");
    // delete selected project if it's only selecting a single branch corresponding to rootUuid
    purgeMapper.deletePortfolioProjectsByBranchUuid(rootUuid);
    // delete selected branches if branch is rootUuid or if it's part of a project that is rootUuid
    purgeMapper.deletePortfolioProjectBranchesByBranchUuid(rootUuid);
    // delete selected project if project is rootUuid
    purgeMapper.deletePortfolioProjectsByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteBranchInPortfolios(String branchUuid) {
    profiler.start("deleteBranchInPortfolios (portfolio_proj_branches)");
    purgeMapper.deletePortfolioProjectBranchesByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteScannerCache(String branchUuid) {
    profiler.start("deleteScannerCache (scanner_analysis_cache)");
    purgeMapper.deleteScannerAnalysisCacheByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteReportSchedules(String rootUuid) {
    profiler.start("deleteReportSchedules (report_schedules)");
    purgeMapper.deleteReportSchedulesByBranchUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteReportSubscriptions(String branchUuid) {
    profiler.start("deleteReportSubscriptions (report_subscriptions)");
    purgeMapper.deleteReportSubscriptionsByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteArchitectureGraphs(String branchUuid) {
    profiler.start("deleteArchitectureGraphs (architecture_graphs)");
    purgeMapper.deleteArchitectureGraphsByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteAnticipatedTransitions(String projectUuid, long createdAt) {
    profiler.start("deleteAnticipatedTransitions (anticipated_transitions)");
    purgeMapper.deleteAnticipatedTransitionsByProjectUuidAndCreationDate(projectUuid, createdAt);
    session.commit();
    profiler.stop();
  }

  public void deleteIssuesFixed(String branchUuid) {
    profiler.start("deleteIssuesFixed (issues_fixed)");
    purgeMapper.deleteIssuesFixedByBranchUuid(branchUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteScaActivity(String componentUuid) {
    // delete sca_analyses first since it sort of marks the analysis as valid/existing
    profiler.start("deleteScaAnalyses (sca_analyses)");
    purgeMapper.deleteScaAnalysesByComponentUuid(componentUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteScaDependencies (sca_dependencies)");
    purgeMapper.deleteScaDependenciesByComponentUuid(componentUuid);
    session.commit();
    profiler.stop();

    // this must be done before deleting sca_issues_releases or we won't
    // be able to find the rows
    profiler.start("deleteScaIssuesReleasesChanges (sca_issue_rels_changes)");
    purgeMapper.deleteScaIssuesReleasesChangesByComponentUuid(componentUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteScaIssuesReleases (sca_issues_releases)");
    purgeMapper.deleteScaIssuesReleasesByComponentUuid(componentUuid);
    session.commit();
    profiler.stop();

    // sca_releases MUST be deleted last because dependencies and
    // issues_releases only join to the component through sca_releases
    profiler.start("deleteScaReleases (sca_releases)");
    purgeMapper.deleteScaReleasesByComponentUuid(componentUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteScaLicenseProfiles(String projectUuid) {
    profiler.start("deleteScaLicenseProfileProjects (sca_lic_prof_projects)");
    purgeMapper.deleteScaLicenseProfileProjectsByProjectUuid(projectUuid);
    profiler.stop();
  }
}
