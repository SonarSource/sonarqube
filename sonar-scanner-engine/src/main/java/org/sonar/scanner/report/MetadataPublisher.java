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
package org.sonar.scanner.report;

import java.io.File;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.bootstrap.ScannerPlugin;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.BranchType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.rule.ModuleQProfiles;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scm.ScmConfiguration;

import static org.sonar.core.config.ScannerProperties.ORGANIZATION;

public class MetadataPublisher implements ReportPublisherStep {

  private static final Logger LOG = Loggers.get(MetadataPublisher.class);

  private final Configuration settings;
  private final ModuleQProfiles qProfiles;
  private final ProjectAnalysisInfo projectAnalysisInfo;
  private final InputModuleHierarchy moduleHierarchy;
  private final CpdSettings cpdSettings;
  private final ScannerPluginRepository pluginRepository;
  private final BranchConfiguration branchConfiguration;

  @Nullable
  private final ScmConfiguration scmConfiguration;

  public MetadataPublisher(ProjectAnalysisInfo projectAnalysisInfo, InputModuleHierarchy moduleHierarchy, Configuration settings,
    ModuleQProfiles qProfiles, CpdSettings cpdSettings, ScannerPluginRepository pluginRepository, BranchConfiguration branchConfiguration,
    @Nullable ScmConfiguration scmConfiguration) {
    this.projectAnalysisInfo = projectAnalysisInfo;
    this.moduleHierarchy = moduleHierarchy;
    this.settings = settings;
    this.qProfiles = qProfiles;
    this.cpdSettings = cpdSettings;
    this.pluginRepository = pluginRepository;
    this.branchConfiguration = branchConfiguration;
    this.scmConfiguration = scmConfiguration;
  }

  public MetadataPublisher(ProjectAnalysisInfo projectAnalysisInfo, InputModuleHierarchy moduleHierarchy, Configuration settings,
    ModuleQProfiles qProfiles, CpdSettings cpdSettings, ScannerPluginRepository pluginRepository, BranchConfiguration branchConfiguration) {
    this(projectAnalysisInfo, moduleHierarchy, settings, qProfiles, cpdSettings, pluginRepository, branchConfiguration, null);
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    DefaultInputModule rootProject = moduleHierarchy.root();
    ScannerReport.Metadata.Builder builder = ScannerReport.Metadata.newBuilder()
      .setAnalysisDate(projectAnalysisInfo.analysisDate().getTime())
      // Here we want key without branch
      .setProjectKey(rootProject.key())
      .setCrossProjectDuplicationActivated(cpdSettings.isCrossProjectDuplicationEnabled())
      .setRootComponentRef(rootProject.batchId());

    settings.get(ORGANIZATION).ifPresent(builder::setOrganizationKey);

    if (branchConfiguration.branchName() != null) {
      builder.setBranchName(branchConfiguration.branchName());
      builder.setBranchType(toProtobufBranchType(branchConfiguration.branchType()));
      String branchTarget = branchConfiguration.branchTarget();
      if (branchTarget != null) {
        builder.setMergeBranchName(branchTarget);
      }
    }
    Optional.ofNullable(rootProject.getBranch()).ifPresent(builder::setDeprecatedBranch);

    if (scmConfiguration != null) {
      ScmProvider scmProvider = scmConfiguration.provider();
      if (scmProvider != null) {
        Path projectBasedir = moduleHierarchy.root().getBaseDir();
        try {
          builder.setRelativePathFromScmRoot(toSonarQubePath(scmProvider.relativePathFromScmRoot(projectBasedir)));
        } catch (UnsupportedOperationException e) {
          LOG.debug(e.getMessage());
        }
        try {
          builder.setScmRevisionId(scmProvider.revisionId(projectBasedir));
        } catch (UnsupportedOperationException e) {
          LOG.debug(e.getMessage());
        }
      }
    }

    for (QProfile qp : qProfiles.findAll()) {
      builder.getMutableQprofilesPerLanguage().put(qp.getLanguage(), ScannerReport.Metadata.QProfile.newBuilder()
        .setKey(qp.getKey())
        .setLanguage(qp.getLanguage())
        .setName(qp.getName())
        .setRulesUpdatedAt(qp.getRulesUpdatedAt().getTime()).build());
    }
    for (Entry<String, ScannerPlugin> pluginEntry : pluginRepository.getPluginsByKey().entrySet()) {
      builder.getMutablePluginsByKey().put(pluginEntry.getKey(), ScannerReport.Metadata.Plugin.newBuilder()
        .setKey(pluginEntry.getKey())
        .setUpdatedAt(pluginEntry.getValue().getUpdatedAt()).build());
    }
    writer.writeMetadata(builder.build());
  }

  private static BranchType toProtobufBranchType(org.sonar.scanner.scan.branch.BranchType branchType) {
    if (branchType == org.sonar.scanner.scan.branch.BranchType.LONG) {
      return BranchType.LONG;
    }
    return BranchType.SHORT;
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
