/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Date;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.component.BranchLoader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.Plugin;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile;
import org.sonar.server.project.Project;
import org.sonar.server.qualityprofile.QualityProfile;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * Feed analysis metadata holder with metadata from the analysis report.
 */
public class LoadReportAnalysisMetadataHolderStep implements ComputationStep {
  private final CeTask ceTask;
  private final ScannerReportReader reportReader;
  private final MutableAnalysisMetadataHolder analysisMetadata;
  private final DbClient dbClient;
  private final BranchLoader branchLoader;
  private final PluginRepository pluginRepository;

  public LoadReportAnalysisMetadataHolderStep(CeTask ceTask, ScannerReportReader reportReader, MutableAnalysisMetadataHolder analysisMetadata,
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
    Runnable projectValidation = loadProject(reportMetadata);
    loadQualityProfiles(reportMetadata);
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
  private Runnable loadProject(ScannerReport.Metadata reportMetadata) {
    CeTask.Component entity = mandatoryComponent(ceTask.getEntity());
    String entityKey = entity.getKey()
      .orElseThrow(() -> MessageException.of(format(
        "Compute Engine task entity key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        entity.getUuid())));
    CeTask.Component component = mandatoryComponent(ceTask.getComponent());
    if (!component.getKey().isPresent()) {
      throw MessageException.of(format(
        "Compute Engine task component key is null. Project with UUID %s must have been deleted since report was uploaded. Can not proceed.",
        component.getUuid()));
    }

    ProjectDto dto = toProject(reportMetadata.getProjectKey());
    analysisMetadata.setProject(Project.fromProjectDtoWithTags(dto));
    return () -> {
      if (!entityKey.equals(reportMetadata.getProjectKey())) {
        throw MessageException.of(format(
          "ProjectKey in report (%s) is not consistent with projectKey under which the report has been submitted (%s)",
          reportMetadata.getProjectKey(),
          entityKey));
      }
    };
  }

  private static CeTask.Component mandatoryComponent(Optional<CeTask.Component> entity) {
    return entity.orElseThrow(() -> new IllegalStateException("component missing on ce task"));
  }

  private void loadQualityProfiles(ScannerReport.Metadata reportMetadata) {
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
