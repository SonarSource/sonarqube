/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.base.Joiner;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.BranchLoader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.Plugin;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile;
import org.sonar.server.project.Project;
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
  private final DbClient dbClient;
  private final BranchLoader branchLoader;
  private final PluginRepository pluginRepository;

  public LoadReportAnalysisMetadataHolderStep(CeTask ceTask, BatchReportReader reportReader, MutableAnalysisMetadataHolder analysisMetadata,
    DbClient dbClient, BranchLoader branchLoader, PluginRepository pluginRepository) {
    this.ceTask = ceTask;
    this.reportReader = reportReader;
    this.analysisMetadata = analysisMetadata;
    this.dbClient = dbClient;
    this.branchLoader = branchLoader;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    ScannerReport.Metadata reportMetadata = reportReader.readMetadata();

    loadMetadata(reportMetadata);
    Organization organization = loadOrganization(reportMetadata);
    Runnable projectValidation = loadProject(reportMetadata, organization);
    loadQualityProfiles(reportMetadata, organization);
    branchLoader.load(reportMetadata);
    projectValidation.run();
  }

  private void loadMetadata(ScannerReport.Metadata reportMetadata) {
    analysisMetadata.setAnalysisDate(reportMetadata.getAnalysisDate());
    analysisMetadata.setRootComponentRef(reportMetadata.getRootComponentRef());
    analysisMetadata.setCrossProjectDuplicationEnabled(reportMetadata.getCrossProjectDuplicationActivated());
    analysisMetadata.setScmRevision(reportMetadata.getScmRevisionId());
    analysisMetadata.setNewCodeReferenceBranch(reportMetadata.getNewCodeReferenceBranch());
  }

  /**
   * @return a {@link Runnable} to execute some checks on the project at the end of the step
   */
  private Runnable loadProject(ScannerReport.Metadata reportMetadata, Organization organization) {
    CeTask.Component mainComponent = mandatoryComponent(ceTask.getMainComponent());
    String mainComponentKey = mainComponent.getKey()
      .orElseThrow(() -> MessageException.of(format(
        "Compute Engine task main component key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        mainComponent.getUuid())));
    CeTask.Component component = mandatoryComponent(ceTask.getComponent());
    if (!component.getKey().isPresent()) {
      throw MessageException.of(format(
        "Compute Engine task component key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        component.getUuid()));
    }

    ProjectDto dto = toProject(reportMetadata.getProjectKey());
    analysisMetadata.setProject(Project.from(dto));
    return () -> {
      if (!mainComponentKey.equals(reportMetadata.getProjectKey())) {
        throw MessageException.of(format(
          "ProjectKey in report (%s) is not consistent with projectKey under which the report has been submitted (%s)",
          reportMetadata.getProjectKey(),
          mainComponentKey));
      }
      if (!dto.getOrganizationUuid().equals(organization.getUuid())) {
        throw MessageException.of(format("Project is not in the expected organization: %s", organization.getKey()));
      }
    };
  }

  private static CeTask.Component mandatoryComponent(Optional<CeTask.Component> mainComponent) {
    return mainComponent
      .orElseThrow(() -> new IllegalStateException("component missing on ce task"));
  }

  private Organization loadOrganization(ScannerReport.Metadata reportMetadata) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Organization organization = toOrganization(dbSession, ceTask.getOrganizationUuid());
      analysisMetadata.setOrganization(organization);
      return organization;
    }
  }

  private void loadQualityProfiles(ScannerReport.Metadata reportMetadata, Organization organization) {
    checkQualityProfilesConsistency(reportMetadata, organization);
    analysisMetadata.setQProfilesByLanguage(reportMetadata.getQprofilesPerLanguageMap().values().stream()
      .collect(toMap(
        QProfile::getLanguage,
        qp -> new QualityProfile(qp.getKey(), qp.getName(), qp.getLanguage(), new Date(qp.getRulesUpdatedAt())))));
    analysisMetadata.setScannerPluginsByKey(reportMetadata.getPluginsByKeyMap().values().stream()
      .collect(toMap(
        Plugin::getKey,
        p -> new ScannerPlugin(p.getKey(), getBasePluginKey(p), p.getUpdatedAt()))));
  }

  @CheckForNull
  private String getBasePluginKey(Plugin p) {
    if (!pluginRepository.hasPlugin(p.getKey())) {
      // May happen if plugin was uninstalled between start of scanner analysis and now.
      // But it doesn't matter since all active rules are removed anyway, so no issues will be reported
      return null;
    }
    return pluginRepository.getPluginInfo(p.getKey()).getBasePlugin();
  }

  /**
   * Check that the Quality profiles sent by scanner correctly relate to the project organization.
   */
  private void checkQualityProfilesConsistency(ScannerReport.Metadata metadata, Organization organization) {
    List<String> profileKeys = metadata.getQprofilesPerLanguageMap().values().stream()
            .map(QProfile::getKey)
            .collect(toList(metadata.getQprofilesPerLanguageMap().size()));
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

  private Organization toOrganization(DbSession dbSession, String organizationUuid) {
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid);
    checkState(organizationDto.isPresent(), "Organization with uuid '%s' can't be found", organizationUuid);
    return Organization.from(organizationDto.get());
  }

  private ProjectDto toProject(String projectKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ProjectDto> opt = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      checkState(opt.isPresent(), "Project with key '%s' can't be found", projectKey);
      return opt.get();
    }
  }

  @Override
  public String getDescription() {
    return "Load analysis metadata";
  }
}
