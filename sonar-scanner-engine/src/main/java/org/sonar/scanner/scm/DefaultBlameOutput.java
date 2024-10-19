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

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.util.ProgressReport;

import static java.lang.String.format;
import static org.sonar.api.utils.Preconditions.checkArgument;

class DefaultBlameOutput implements BlameOutput {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultBlameOutput.class);
  @VisibleForTesting
  static final String SCM_INTEGRATION_DOCUMENTATION_SUFFIX = "/analyzing-source-code/scm-integration/";

  private final ScannerReportWriter writer;
  private AnalysisWarnings analysisWarnings;
  private final DocumentationLinkGenerator documentationLinkGenerator;
  private final Set<InputFile> allFilesToBlame = new LinkedHashSet<>();
  private ProgressReport progressReport;
  private int count;
  private int total;

  DefaultBlameOutput(ScannerReportWriter writer, AnalysisWarnings analysisWarnings, List<InputFile> filesToBlame,
    DocumentationLinkGenerator documentationLinkGenerator) {
    this.writer = writer;
    this.analysisWarnings = analysisWarnings;
    this.documentationLinkGenerator = documentationLinkGenerator;
    this.allFilesToBlame.addAll(filesToBlame);
    count = 0;
    total = filesToBlame.size();
    progressReport = new ProgressReport("Report about progress of SCM blame", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("SCM Publisher " + total + " " + pluralize(total) + " to be analyzed");
  }

  @Override
  public synchronized void blameResult(InputFile file, List<BlameLine> lines) {
    checkNotNull(file);
    checkNotNull(lines);
    checkArgument(allFilesToBlame.contains(file), "It was not expected to blame file %s", file);

    if (lines.size() != file.lines()) {
      LOG.debug("Ignoring blame result since provider returned {} blame lines but file {} has {} lines", lines.size(), file, file.lines());
      return;
    }

    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    DefaultInputFile inputFile = (DefaultInputFile) file;
    scmBuilder.setComponentRef(inputFile.scannerId());
    Map<String, Integer> changesetsIdByRevision = new HashMap<>();

    int lineId = 1;
    for (BlameLine line : lines) {
      validateLine(line, lineId, file);
      Integer changesetId = changesetsIdByRevision.get(line.revision());
      if (changesetId == null) {
        addChangeset(scmBuilder, line);
        changesetId = scmBuilder.getChangesetCount() - 1;
        changesetsIdByRevision.put(line.revision(), changesetId);
      }
      scmBuilder.addChangesetIndexByLine(changesetId);
      lineId++;
    }
    writer.writeComponentChangesets(scmBuilder.build());
    allFilesToBlame.remove(file);
    count++;
    progressReport.message(count + "/" + total + " " + pluralize(count) + " have been analyzed");
  }

  private static void validateLine(BlameLine line, int lineId, InputFile file) {
    checkArgument(StringUtils.isNotBlank(line.revision()), "Blame revision is blank for file %s at line %s", file, lineId);
    checkArgument(line.date() != null, "Blame date is null for file %s at line %s", file, lineId);
  }

  private static void addChangeset(Builder scmBuilder, BlameLine line) {
    ScannerReport.Changesets.Changeset.Builder changesetBuilder = ScannerReport.Changesets.Changeset.newBuilder();
    changesetBuilder.setRevision(line.revision());
    changesetBuilder.setDate(line.date().getTime());
    if (StringUtils.isNotBlank(line.author())) {
      changesetBuilder.setAuthor(normalizeString(line.author()));
    }

    scmBuilder.addChangeset(changesetBuilder.build());
  }

  private static String normalizeString(@Nullable String inputString) {
    if (inputString == null) {
      return "";
    }
    return inputString.toLowerCase(Locale.US);
  }

  private static void checkNotNull(@Nullable Object obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
  }

  public void finish(boolean success) {
    progressReport.stopAndLogTotalTime("SCM Publisher " + count + "/" + total + " " + pluralize(count) + " have been analyzed");
    if (success && !allFilesToBlame.isEmpty()) {
      LOG.warn("Missing blame information for the following files:");
      for (InputFile f : allFilesToBlame) {
        LOG.warn("  * " + f);
      }
      LOG.warn("This may lead to missing/broken features in SonarQube");
      String docUrl = documentationLinkGenerator.getDocumentationLink(SCM_INTEGRATION_DOCUMENTATION_SUFFIX);
      analysisWarnings.addUnique(format("Missing blame information for %d %s. This may lead to some features not working correctly. " +
        "Please check the analysis logs and refer to <a href=\"%s\" rel=\"noopener noreferrer\" target=\"_blank\">the documentation</a>.",
        allFilesToBlame.size(),
        allFilesToBlame.size() > 1 ? "files" : "file",
        docUrl));
    }
  }

  private static String pluralize(long filesCount) {
    return filesCount == 1 ? "source file" : "source files";
  }
}

