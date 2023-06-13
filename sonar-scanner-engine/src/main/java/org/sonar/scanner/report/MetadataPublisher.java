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
package org.sonar.scanner.report;

import java.io.File;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.scm.ScmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.ScannerPlugin;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.BranchType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ReferenceBranchSupplier;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.rule.QualityProfiles;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.scm.ScmRevision;

public class MetadataPublisher implements ReportPublisherStep {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataPublisher.class);

  private final QualityProfiles qProfiles;
  private final ProjectInfo projectInfo;
  private final InputModuleHierarchy moduleHierarchy;
  private final CpdSettings cpdSettings;
  private final ScannerPluginRepository pluginRepository;
  private final BranchConfiguration branchConfiguration;
  private final ScmRevision scmRevision;
  private final InputComponentStore componentStore;
  private final ScmConfiguration scmConfiguration;
  private final ReferenceBranchSupplier referenceBranchSupplier;

  public MetadataPublisher(ProjectInfo projectInfo, InputModuleHierarchy moduleHierarchy, QualityProfiles qProfiles,
    CpdSettings cpdSettings, ScannerPluginRepository pluginRepository, BranchConfiguration branchConfiguration,
    ScmRevision scmRevision, InputComponentStore componentStore, ScmConfiguration scmConfiguration, ReferenceBranchSupplier referenceBranchSupplier) {
    this.projectInfo = projectInfo;
    this.moduleHierarchy = moduleHierarchy;
    this.qProfiles = qProfiles;
    this.cpdSettings = cpdSettings;
    this.pluginRepository = pluginRepository;
    this.branchConfiguration = branchConfiguration;
    this.scmRevision = scmRevision;
    this.componentStore = componentStore;
    this.scmConfiguration = scmConfiguration;
    this.referenceBranchSupplier = referenceBranchSupplier;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    AbstractProjectOrModule rootProject = moduleHierarchy.root();
    ScannerReport.Metadata.Builder builder = ScannerReport.Metadata.newBuilder()
      .setAnalysisDate(projectInfo.getAnalysisDate().getTime())
      // Here we want key without branch
      .setProjectKey(rootProject.key())
      .setCrossProjectDuplicationActivated(cpdSettings.isCrossProjectDuplicationEnabled())
      .setRootComponentRef(rootProject.scannerId());
    projectInfo.getProjectVersion().ifPresent(builder::setProjectVersion);
    projectInfo.getBuildString().ifPresent(builder::setBuildString);

    if (branchConfiguration.branchName() != null) {
      addBranchInformation(builder);
    }

    String newCodeReferenceBranch = referenceBranchSupplier.getFromProperties();
    if (newCodeReferenceBranch != null) {
      builder.setNewCodeReferenceBranch(newCodeReferenceBranch);
    }

    addScmInformation(builder);
    addNotAnalyzedFileCountsByLanguage(builder);

    for (QProfile qp : qProfiles.findAll()) {
      builder.putQprofilesPerLanguage(qp.getLanguage(), ScannerReport.Metadata.QProfile.newBuilder()
        .setKey(qp.getKey())
        .setLanguage(qp.getLanguage())
        .setName(qp.getName())
        .setRulesUpdatedAt(qp.getRulesUpdatedAt().getTime()).build());
    }
    for (Entry<String, ScannerPlugin> pluginEntry : pluginRepository.getPluginsByKey().entrySet()) {
      builder.putPluginsByKey(pluginEntry.getKey(), ScannerReport.Metadata.Plugin.newBuilder()
        .setKey(pluginEntry.getKey())
        .setUpdatedAt(pluginEntry.getValue().getUpdatedAt()).build());
    }

    addRelativePathFromScmRoot(builder);

    writer.writeMetadata(builder.build());
  }

  private void addRelativePathFromScmRoot(ScannerReport.Metadata.Builder builder) {
    ScmProvider scmProvider = scmConfiguration.provider();
    if (scmProvider == null) {
      return;
    }

    Path projectBasedir = moduleHierarchy.root().getBaseDir();
    try {
      builder.setRelativePathFromScmRoot(toSonarQubePath(scmProvider.relativePathFromScmRoot(projectBasedir)));
    } catch (UnsupportedOperationException e) {
      LOG.debug(e.getMessage());
    }
  }

  private void addScmInformation(ScannerReport.Metadata.Builder builder) {
    try {
      scmRevision.get().ifPresent(revisionId -> {
        LOG.debug("SCM revision ID '{}'", revisionId);
        builder.setScmRevisionId(revisionId);
      });
    } catch (UnsupportedOperationException e) {
      LOG.debug(e.getMessage());
    }
  }

  private void addNotAnalyzedFileCountsByLanguage(ScannerReport.Metadata.Builder builder) {
    builder.putAllNotAnalyzedFilesByLanguage(componentStore.getNotAnalysedFilesByLanguage());
  }

  private void addBranchInformation(ScannerReport.Metadata.Builder builder) {
    builder.setBranchName(branchConfiguration.branchName());
    BranchType branchType = toProtobufBranchType(branchConfiguration.branchType());
    builder.setBranchType(branchType);
    String referenceBranch = branchConfiguration.referenceBranchName();
    if (referenceBranch != null) {
      builder.setReferenceBranchName(referenceBranch);
    }
    String targetBranchName = branchConfiguration.targetBranchName();
    if (targetBranchName != null) {
      builder.setTargetBranchName(targetBranchName);
    }
    if (branchType == BranchType.PULL_REQUEST) {
      builder.setPullRequestKey(branchConfiguration.pullRequestKey());
    }
  }

  private static BranchType toProtobufBranchType(org.sonar.scanner.scan.branch.BranchType branchType) {
    if (branchType == org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST) {
      return BranchType.PULL_REQUEST;
    }

    return BranchType.BRANCH;
  }

  private static String toSonarQubePath(Path path) {
    String pathAsString = path.toString();
    char sonarQubeSeparatorChar = '/';
    if (File.separatorChar != sonarQubeSeparatorChar) {
      return pathAsString.replaceAll(Pattern.quote(File.separator), String.valueOf(sonarQubeSeparatorChar));
    }
    return pathAsString;
  }

}
