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

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.util.ProgressReport;

class DefaultBlameOutput implements BlameOutput {

  private static final Logger LOG = Loggers.get(DefaultBlameOutput.class);

  private final ScannerReportWriter writer;
  private final Set<InputFile> allFilesToBlame = new LinkedHashSet<>();
  private ProgressReport progressReport;
  private int count;
  private int total;

  DefaultBlameOutput(ScannerReportWriter writer, List<InputFile> filesToBlame) {
    this.writer = writer;
    this.allFilesToBlame.addAll(filesToBlame);
    count = 0;
    total = filesToBlame.size();
    progressReport = new ProgressReport("Report about progress of SCM blame", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(total + " files to be analyzed");
  }

  @Override
  public synchronized void blameResult(InputFile file, List<BlameLine> lines) {
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(lines);
    Preconditions.checkArgument(allFilesToBlame.contains(file), "It was not expected to blame file %s", file);

    if (lines.size() != file.lines()) {
      LOG.debug("Ignoring blame result since provider returned {} blame lines but file {} has {} lines", lines.size(), file, file.lines());
      return;
    }

    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    DefaultInputFile inputFile = (DefaultInputFile) file;
    scmBuilder.setComponentRef(inputFile.batchId());
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
    progressReport.message(count + "/" + total + " files analyzed");
  }

  private static void validateLine(BlameLine line, int lineId, InputFile file) {
    Preconditions.checkArgument(StringUtils.isNotBlank(line.revision()), "Blame revision is blank for file %s at line %s", file, lineId);
    Preconditions.checkArgument(line.date() != null, "Blame date is null for file %s at line %s", file, lineId);
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

  public void finish(boolean success) {
    progressReport.stop(count + "/" + total + " files analyzed");
    if (success && !allFilesToBlame.isEmpty()) {
      LOG.warn("Missing blame information for the following files:");
      for (InputFile f : allFilesToBlame) {
        LOG.warn("  * " + f);
      }
      LOG.warn("This may lead to missing/broken features in SonarQube");
    }
  }
}
