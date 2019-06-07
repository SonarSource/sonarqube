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
package org.sonar.scanner.scm;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositoriesSupplier;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public final class ScmPublisher {

  private static final Logger LOG = Loggers.get(ScmPublisher.class);

  private final ScmConfiguration configuration;
  private final ProjectRepositoriesSupplier projectRepositoriesSupplier;
  private final InputComponentStore componentStore;
  private final FileSystem fs;
  private final ScannerReportWriter writer;
  private final BranchConfiguration branchConfiguration;

  public ScmPublisher(ScmConfiguration configuration, ProjectRepositoriesSupplier projectRepositoriesSupplier,
    InputComponentStore componentStore, FileSystem fs, ReportPublisher reportPublisher, BranchConfiguration branchConfiguration) {
    this.configuration = configuration;
    this.projectRepositoriesSupplier = projectRepositoriesSupplier;
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

    ScmProvider provider = configuration.provider();
    if (provider == null) {
      LOG.info("SCM Publisher No SCM system was detected. You can use the '" + CoreProperties.SCM_PROVIDER_KEY + "' property to explicitly specify it.");
      return;
    }

    List<InputFile> filesToBlame = collectFilesToBlame(writer);
    if (!filesToBlame.isEmpty()) {
      String key = provider.key();
      LOG.info("SCM Publisher SCM provider for this project is: " + key);
      DefaultBlameOutput output = new DefaultBlameOutput(writer, filesToBlame);
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
      } else if (!branchConfiguration.isShortOrPullRequest()) {
        FileData fileData = projectRepositoriesSupplier.get().fileData(componentStore.findModule(f).key(), f);
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
