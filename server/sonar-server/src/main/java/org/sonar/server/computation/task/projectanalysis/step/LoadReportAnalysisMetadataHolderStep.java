/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Joiner;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.queue.CeTask;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.Plugin;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;
import org.sonar.server.computation.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.BranchLoader;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.qualityprofile.QualityProfile;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.sonar.core.util.stream.MoreCollectors.toList;

/**
 * Feed analysis metadata holder with metadata from the analysis report.
 */
public class LoadReportAnalysisMetadataHolderStep implements ComputationStep {
  private final CeTask ceTask;
  private final BatchReportReader reportReader;
  private final MutableAnalysisMetadataHolder analysisMetadata;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final DbClient dbClient;
  private final BranchLoader branchLoader;
  private final PluginRepository pluginRepository;
  private final OrganizationFlags organizationFlags;

  public LoadReportAnalysisMetadataHolderStep(CeTask ceTask, BatchReportReader reportReader, MutableAnalysisMetadataHolder analysisMetadata,
    DefaultOrganizationProvider defaultOrganizationProvider, DbClient dbClient, BranchLoader branchLoader, PluginRepository pluginRepository,
    OrganizationFlags organizationFlags) {
    this.ceTask = ceTask;
    this.reportReader = reportReader;
    this.analysisMetadata = analysisMetadata;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.dbClient = dbClient;
    this.branchLoader = branchLoader;
    this.pluginRepository = pluginRepository;
    this.organizationFlags = organizationFlags;
  }

  @Override
  public void execute() {
    ScannerReport.Metadata reportMetadata = reportReader.readMetadata();

    loadMetadata(reportMetadata);
    Organization organization = loadOrganization(reportMetadata);
    loadProject(reportMetadata, organization);
    loadQualityProfiles(reportMetadata, organization);
    branchLoader.load(reportMetadata);
  }

  private void loadMetadata(ScannerReport.Metadata reportMetadata) {
    analysisMetadata.setAnalysisDate(reportMetadata.getAnalysisDate());
    analysisMetadata.setRootComponentRef(reportMetadata.getRootComponentRef());
    analysisMetadata.setCrossProjectDuplicationEnabled(reportMetadata.getCrossProjectDuplicationActivated());
  }

  private void loadProject(ScannerReport.Metadata reportMetadata, Organization organization) {
    String reportProjectKey = projectKeyFromReport(reportMetadata);
    checkProjectKeyConsistency(reportProjectKey);
    ComponentDto dto = toProject(reportProjectKey);
    if (!dto.getOrganizationUuid().equals(organization.getUuid())) {
      throw MessageException.of(format("Project is not in the expected organization: %s", organization.getKey()));
    }
    if (dto.getMainBranchProjectUuid() != null) {
      throw MessageException.of("Project should not reference a branch");
    }
    analysisMetadata.setProject(new Project(dto.uuid(), dto.getDbKey(), dto.name()));
  }

  private Organization loadOrganization(ScannerReport.Metadata reportMetadata) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Organization organization = toOrganization(dbSession, ceTask.getOrganizationUuid());
      checkOrganizationKeyConsistency(reportMetadata, organization);
      analysisMetadata.setOrganization(organization);
      analysisMetadata.setOrganizationsEnabled(organizationFlags.isEnabled(dbSession));
      return organization;
    }
  }

  private void loadQualityProfiles(ScannerReport.Metadata reportMetadata, Organization organization) {
    checkQualityProfilesConsistency(reportMetadata, organization);
    analysisMetadata.setQProfilesByLanguage(reportMetadata.getQprofilesPerLanguage().values().stream()
      .collect(toMap(
        QProfile::getLanguage,
        qp -> new QualityProfile(qp.getKey(), qp.getName(), qp.getLanguage(), new Date(qp.getRulesUpdatedAt())))));
    analysisMetadata.setScannerPluginsByKey(reportMetadata.getPluginsByKey().values().stream()
      .collect(toMap(
        Plugin::getKey,
        p -> new ScannerPlugin(p.getKey(), getBasePluginKey(p), p.getUpdatedAt()))));
  }

  @CheckForNull
  private String getBasePluginKey(Plugin p) {
    PluginInfo pluginInfo = pluginRepository.getPluginInfo(p.getKey());
    if (pluginInfo == null) {
      // May happen if plugin was uninstalled between start of scanner analysis and now.
      // But it doesn't matter since all active rules are removed anyway, so no issues will be reported
      return null;
    }
    return pluginInfo.getBasePlugin();
  }

  /**
   * Check that the Quality profiles sent by scanner correctly relate to the project organization.
   */
  private void checkQualityProfilesConsistency(ScannerReport.Metadata metadata, Organization organization) {
    List<String> profileKeys = metadata.getQprofilesPerLanguage().values().stream()
      .map(QProfile::getKey)
      .collect(toList(metadata.getQprofilesPerLanguage().size()));
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<QProfileDto> profiles = dbClient.qualityProfileDao().selectByUuids(dbSession, profileKeys);
      String badKeys = profiles.stream()
        .filter(p -> !p.getOrganizationUuid().equals(organization.getUuid()))
        .map(QProfileDto::getKee)
        .collect(MoreCollectors.join(Joiner.on(", ")));
      if (!badKeys.isEmpty()) {
        throw MessageException.of(format("Quality profiles with following keys don't exist in organization [%s]: %s", organization.getKey(), badKeys));
      }
    }
  }

  private void checkProjectKeyConsistency(String reportProjectKey) {
    String componentKey = ceTask.getComponentKey();
    if (componentKey == null) {
      throw MessageException.of(format(
        "Compute Engine task component key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        ceTask.getComponentUuid()));
    }
    if (!componentKey.equals(reportProjectKey)) {
      throw MessageException.of(format(
        "ProjectKey in report (%s) is not consistent with projectKey under which the report as been submitted (%s)",
        reportProjectKey,
        componentKey));
    }
  }

  private void checkOrganizationKeyConsistency(ScannerReport.Metadata reportMetadata, Organization organization) {
    String organizationKey = reportMetadata.getOrganizationKey();
    String resolveReportOrganizationKey = resolveReportOrganizationKey(organizationKey);
    if (!resolveReportOrganizationKey.equals(organization.getKey())) {
      if (reportBelongsToDefaultOrganization(organizationKey)) {
        throw MessageException.of(format(
          "Report does not specify an OrganizationKey but it has been submitted to another organization (%s) than the default one (%s)",
          organization.getKey(),
          defaultOrganizationProvider.get().getKey()));
      } else {
        throw MessageException.of(format(
          "OrganizationKey in report (%s) is not consistent with organizationKey under which the report as been submitted (%s)",
          resolveReportOrganizationKey,
          organization.getKey()));
      }
    }
  }

  private String resolveReportOrganizationKey(@Nullable String organizationKey) {
    if (reportBelongsToDefaultOrganization(organizationKey)) {
      return defaultOrganizationProvider.get().getKey();
    }
    return organizationKey;
  }

  private static boolean reportBelongsToDefaultOrganization(@Nullable String organizationKey) {
    return organizationKey == null || organizationKey.isEmpty();
  }

  private Organization toOrganization(DbSession dbSession, String organizationUuid) {
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid);
    checkState(organizationDto.isPresent(), "Organization with uuid '%s' can't be found", organizationUuid);
    return Organization.from(organizationDto.get());
  }

  private ComponentDto toProject(String projectKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      com.google.common.base.Optional<ComponentDto> opt = dbClient.componentDao().selectByKey(dbSession, projectKey);
      checkState(opt.isPresent(), "Project with key '%s' can't be found", projectKey);
      return opt.get();
    }
  }

  private static String projectKeyFromReport(ScannerReport.Metadata reportMetadata) {
    String deprecatedBranch = reportMetadata.getDeprecatedBranch();
    if (StringUtils.isNotEmpty(deprecatedBranch)) {
      return ComponentKeys.createKey(reportMetadata.getProjectKey(), deprecatedBranch);
    }
    return reportMetadata.getProjectKey();
  }

  @Override
  public String getDescription() {
    return "Load analysis metadata";
  }
}
