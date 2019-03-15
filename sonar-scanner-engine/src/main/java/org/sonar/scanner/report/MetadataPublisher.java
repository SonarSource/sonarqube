/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.ScannerPlugin;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.BranchType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.rule.QualityProfiles;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scm.ScmConfiguration;

import static java.util.Optional.ofNullable;

public class MetadataPublisher implements ReportPublisherStep {

  private static final Logger LOG = Loggers.get(MetadataPublisher.class);

  private final ScanProperties properties;
  private final QualityProfiles qProfiles;
  private final ProjectInfo projectInfo;
  private final InputModuleHierarchy moduleHierarchy;
  private final CpdSettings cpdSettings;
  private final ScannerPluginRepository pluginRepository;
  private final BranchConfiguration branchConfiguration;

  @Nullable
  private final ScmConfiguration scmConfiguration;

  public MetadataPublisher(ProjectInfo projectInfo, InputModuleHierarchy moduleHierarchy, ScanProperties properties,
    QualityProfiles qProfiles, CpdSettings cpdSettings, ScannerPluginRepository pluginRepository, BranchConfiguration branchConfiguration,
    @Nullable ScmConfiguration scmConfiguration) {
    this.projectInfo = projectInfo;
    this.moduleHierarchy = moduleHierarchy;
    this.properties = properties;
    this.qProfiles = qProfiles;
    this.cpdSettings = cpdSettings;
    this.pluginRepository = pluginRepository;
    this.branchConfiguration = branchConfiguration;
    this.scmConfiguration = scmConfiguration;
  }

  public MetadataPublisher(ProjectInfo projectInfo, InputModuleHierarchy moduleHierarchy, ScanProperties properties,
    QualityProfiles qProfiles, CpdSettings cpdSettings, ScannerPluginRepository pluginRepository, BranchConfiguration branchConfiguration) {
    this(projectInfo, moduleHierarchy, properties, qProfiles, cpdSettings, pluginRepository, branchConfiguration, null);
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

    properties.organizationKey().ifPresent(builder::setOrganizationKey);

    if (branchConfiguration.branchName() != null) {
      addBranchInformation(builder);
    }

    ofNullable(rootProject.getBranch()).ifPresent(builder::setDeprecatedBranch);

    if (scmConfiguration != null) {
      addScmInformation(builder);
    }

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

    addModulesRelativePaths(builder);

    writer.writeMetadata(builder.build());
  }

  private void addModulesRelativePaths(ScannerReport.Metadata.Builder builder) {
    LinkedList<DefaultInputModule> queue = new LinkedList<>();
    queue.add(moduleHierarchy.root());

    while (!queue.isEmpty()) {
      DefaultInputModule module = queue.removeFirst();
      queue.addAll(moduleHierarchy.children(module));
      String relativePath = moduleHierarchy.relativePathToRoot(module);
      if (relativePath != null) {
        builder.putModulesProjectRelativePathByKey(module.key(), relativePath);
      }
    }
  }

  private void addScmInformation(ScannerReport.Metadata.Builder builder) {
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

  private void addBranchInformation(ScannerReport.Metadata.Builder builder) {
    builder.setBranchName(branchConfiguration.branchName());
    BranchType branchType = toProtobufBranchType(branchConfiguration.branchType());
    builder.setBranchType(branchType);
    String referenceBranch = branchConfiguration.longLivingSonarReferenceBranch();
    if (referenceBranch != null) {
      builder.setMergeBranchName(referenceBranch);
    }
    if (branchType == BranchType.PULL_REQUEST) {
      builder.setPullRequestKey(branchConfiguration.pullRequestKey());
    }
  }

  private static BranchType toProtobufBranchType(org.sonar.scanner.scan.branch.BranchType branchType) {
    if (branchType == org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST) {
      return BranchType.PULL_REQUEST;
    }
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
