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
package org.sonar.scanner.scm;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public final class ScmPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(ScmPublisher.class);

  private final ScmConfiguration configuration;
  private final ProjectRepositories projectRepositories;
  private final InputComponentStore componentStore;
  private final FileSystem fs;
  private final ScannerReportWriter writer;
  private final AnalysisWarnings analysisWarnings;
  private final BranchConfiguration branchConfiguration;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public ScmPublisher(ScmConfiguration configuration, ProjectRepositories projectRepositories, InputComponentStore componentStore, FileSystem fs,
    ReportPublisher reportPublisher, BranchConfiguration branchConfiguration, AnalysisWarnings analysisWarnings, DocumentationLinkGenerator documentationLinkGenerator) {
    this.configuration = configuration;
    this.projectRepositories = projectRepositories;
    this.componentStore = componentStore;
    this.fs = fs;
    this.branchConfiguration = branchConfiguration;
    this.writer = reportPublisher.getWriter();
    this.analysisWarnings = analysisWarnings;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  public void publish() {
    if (configuration.isDisabled()) {
      LOG.info("SCM Publisher is disabled");
      return;
    }

    ScmProvider provider = configuration.provider();
    if (provider == null) {
      LOG.info("SCM Publisher No SCM system was detected. You can use the '" + CoreProperties.SCM_PROVIDER_KEY + "' property to explicitly specify it.");
      return;
    }

    List<InputFile> filesToBlame = collectFilesToBlame(writer);
    if (!filesToBlame.isEmpty()) {
      String key = provider.key();
      LOG.info("SCM Publisher SCM provider for this project is: " + key);
      DefaultBlameOutput output = new DefaultBlameOutput(writer, analysisWarnings, filesToBlame, documentationLinkGenerator);
      try {
        provider.blameCommand().blame(new DefaultBlameInput(fs, filesToBlame), output);
      } catch (Exception e) {
        output.finish(false);
        throw e;
      }
      output.finish(true);
    }
  }

  private List<InputFile> collectFilesToBlame(ScannerReportWriter writer) {
    if (configuration.forceReloadAll()) {
      LOG.warn("Forced reloading of SCM data for all files.");
    }
    List<InputFile> filesToBlame = new LinkedList<>();
    for (DefaultInputFile f : componentStore.allFilesToPublish()) {
      if (configuration.forceReloadAll() || f.status() != Status.SAME) {
        addIfNotEmpty(filesToBlame, f);
      } else if (!branchConfiguration.isPullRequest()) {
        FileData fileData = projectRepositories.fileData(componentStore.findModule(f).key(), f);
        if (fileData == null || StringUtils.isEmpty(fileData.revision())) {
          addIfNotEmpty(filesToBlame, f);
        } else {
          askToCopyDataFromPreviousAnalysis(f, writer);
        }
      }
    }
    return filesToBlame;
  }

  private static void askToCopyDataFromPreviousAnalysis(DefaultInputFile f, ScannerReportWriter writer) {
    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    scmBuilder.setComponentRef(f.scannerId());
    scmBuilder.setCopyFromPrevious(true);
    writer.writeComponentChangesets(scmBuilder.build());
  }

  private static void addIfNotEmpty(List<InputFile> filesToBlame, InputFile f) {
    if (!f.isEmpty()) {
      filesToBlame.add(f);
    }
  }

}
