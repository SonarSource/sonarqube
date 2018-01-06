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
package org.sonar.scanner.scm;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.ModuleInputComponentStore;

@InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
@ScannerSide
public final class ScmPublisher {

  private static final Logger LOG = Loggers.get(ScmPublisher.class);

  private final DefaultInputModule inputModule;
  private final ScmConfiguration configuration;
  private final ProjectRepositories projectRepositories;
  private final ModuleInputComponentStore componentStore;
  private final DefaultModuleFileSystem fs;
  private final ScannerReportWriter writer;
  private final BranchConfiguration branchConfiguration;

  public ScmPublisher(DefaultInputModule inputModule, ScmConfiguration configuration, ProjectRepositories projectRepositories,
    ModuleInputComponentStore componentStore, DefaultModuleFileSystem fs, ReportPublisher reportPublisher, BranchConfiguration branchConfiguration) {
    this.inputModule = inputModule;
    this.configuration = configuration;
    this.projectRepositories = projectRepositories;
    this.componentStore = componentStore;
    this.fs = fs;
    this.branchConfiguration = branchConfiguration;
    this.writer = reportPublisher.getWriter();
  }

  public void publish() {
    if (configuration.isDisabled()) {
      LOG.info("SCM Publisher is disabled");
      return;
    }
    if (configuration.provider() == null) {
      LOG.info("No SCM system was detected. You can use the '" + CoreProperties.SCM_PROVIDER_KEY + "' property to explicitly specify it.");
      return;
    }

    List<InputFile> filesToBlame = collectFilesToBlame(writer);
    if (!filesToBlame.isEmpty()) {
      String key = configuration.provider().key();
      LOG.info("SCM provider for this project is: " + key);
      DefaultBlameOutput output = new DefaultBlameOutput(writer, filesToBlame);
      try {
        configuration.provider().blameCommand().blame(new DefaultBlameInput(fs, filesToBlame), output);
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
    for (InputFile f : componentStore.inputFiles()) {
      DefaultInputFile inputFile = (DefaultInputFile) f;
      if (!inputFile.isPublished()) {
        continue;
      }
      if (configuration.forceReloadAll() || f.status() != Status.SAME) {
        addIfNotEmpty(filesToBlame, f);
      } else if (!branchConfiguration.isShortLivingBranch()) {
        // File status is SAME so that mean fileData exists
        FileData fileData = projectRepositories.fileData(inputModule.definition().getKeyWithBranch(), inputFile.getModuleRelativePath());
        if (StringUtils.isEmpty(fileData.revision())) {
          addIfNotEmpty(filesToBlame, f);
        } else {
          askToCopyDataFromPreviousAnalysis((DefaultInputFile) f, writer);
        }
      }
    }
    return filesToBlame;
  }

  private static void askToCopyDataFromPreviousAnalysis(DefaultInputFile f, ScannerReportWriter writer) {
    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    scmBuilder.setComponentRef(f.batchId());
    scmBuilder.setCopyFromPrevious(true);
    writer.writeComponentChangesets(scmBuilder.build());
  }

  private static void addIfNotEmpty(List<InputFile> filesToBlame, InputFile f) {
    if (!f.isEmpty()) {
      filesToBlame.add(f);
    }
  }

}
