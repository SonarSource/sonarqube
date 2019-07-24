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

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.impl.utils.ScannerUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scm.ScmConfiguration;

public class ChangedLinesPublisher implements ReportPublisherStep {
  private static final Logger LOG = Loggers.get(ChangedLinesPublisher.class);
  private static final String LOG_MSG = "SCM writing changed lines";

  private final ScmConfiguration scmConfiguration;
  private final DefaultInputProject project;
  private final InputComponentStore inputComponentStore;
  private final BranchConfiguration branchConfiguration;

  public ChangedLinesPublisher(ScmConfiguration scmConfiguration, DefaultInputProject project, InputComponentStore inputComponentStore,
    BranchConfiguration branchConfiguration) {
    this.scmConfiguration = scmConfiguration;
    this.project = project;
    this.inputComponentStore = inputComponentStore;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    String targetBranchName = branchConfiguration.targetBranchName();
    if (scmConfiguration.isDisabled() || !branchConfiguration.isShortOrPullRequest() || targetBranchName == null) {
      return;
    }

    ScmProvider provider = scmConfiguration.provider();
    if (provider == null) {
      return;
    }

    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    int count = writeChangedLines(provider, writer, targetBranchName);
    LOG.debug("SCM reported changed lines for {} {} in the branch", count, ScannerUtils.pluralize("file", count));
    profiler.stopInfo();
  }

  private int writeChangedLines(ScmProvider provider, ScannerReportWriter writer, String targetScmBranch) {
    Path rootBaseDir = project.getBaseDir();
    Map<Path, DefaultInputFile> changedFiles = StreamSupport.stream(inputComponentStore.allChangedFilesToPublish().spliterator(), false)
      .collect(Collectors.toMap(DefaultInputFile::path, f -> f));

    Map<Path, Set<Integer>> pathSetMap = provider.branchChangedLines(targetScmBranch, rootBaseDir, changedFiles.keySet());
    int count = 0;

    if (pathSetMap == null) {
      // no information returned by the SCM, we write nothing in the report and
      // the compute engine will use SCM dates to estimate which lines are new
      return count;
    }

    for (Map.Entry<Path, DefaultInputFile> e : changedFiles.entrySet()) {
      DefaultInputFile inputFile = e.getValue();
      Set<Integer> changedLines = pathSetMap.getOrDefault(e.getKey(), Collections.emptySet());

      // detect unchanged last empty line
      if (changedLines.size() + 1 == inputFile.lines() && inputFile.lineLength(inputFile.lines()) == 0) {
        changedLines.add(inputFile.lines());
      }

      if (changedLines.isEmpty()) {
        LOG.warn("File '{}' was detected as changed but without having changed lines", e.getKey().toAbsolutePath());
      }
      count++;
      writeChangedLines(writer, e.getValue().scannerId(), changedLines);
    }
    return count;
  }

  private static void writeChangedLines(ScannerReportWriter writer, int fileRef, Set<Integer> changedLines) {
    ScannerReport.ChangedLines.Builder builder = ScannerReport.ChangedLines.newBuilder();
    builder.addAllLine(changedLines);
    writer.writeComponentChangedLines(fileRef, builder.build());
  }
}
