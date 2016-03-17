/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scm;

import com.google.common.base.Preconditions;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.util.ProgressReport;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;

class DefaultBlameOutput implements BlameOutput {

  private static final Logger LOG = Loggers.get(DefaultBlameOutput.class);

  private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\x00-\\x7F]");
  private static final Pattern ACCENT_CODES = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final ScannerReportWriter writer;
  private final BatchComponentCache componentCache;
  private final Set<InputFile> allFilesToBlame = new HashSet<>();
  private ProgressReport progressReport;
  private int count;
  private int total;

  DefaultBlameOutput(ScannerReportWriter writer, BatchComponentCache componentCache, List<InputFile> filesToBlame) {
    this.writer = writer;
    this.componentCache = componentCache;
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
    Preconditions.checkArgument(allFilesToBlame.contains(file), "It was not expected to blame file %s", file.relativePath());

    if (lines.size() != file.lines()) {
      LOG.debug("Ignoring blame result since provider returned {} blame lines but file {} has {} lines", lines.size(), file.relativePath(), file.lines());
      return;
    }

    BatchComponent batchComponent = componentCache.get(file);
    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    scmBuilder.setComponentRef(batchComponent.batchId());
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
    Preconditions.checkArgument(StringUtils.isNotBlank(line.revision()), "Blame revision is blank for file %s at line %s", file.relativePath(), lineId);
    Preconditions.checkArgument(line.date() != null, "Blame date is null for file %s at line %s", file.relativePath(), lineId);
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
    String lowerCasedString = inputString.toLowerCase();
    String stringWithoutAccents = removeAccents(lowerCasedString);
    return removeNonAsciiCharacters(stringWithoutAccents);
  }

  private static String removeAccents(String inputString) {
    String unicodeDecomposedString = Normalizer.normalize(inputString, Normalizer.Form.NFD);
    return ACCENT_CODES.matcher(unicodeDecomposedString).replaceAll("");
  }

  private static String removeNonAsciiCharacters(String inputString) {
    return NON_ASCII_CHARS.matcher(inputString).replaceAll("_");
  }

  public void finish() {
    progressReport.stop(count + "/" + total + " files analyzed");
    if (!allFilesToBlame.isEmpty()) {
      LOG.warn("Missing blame information for the following files:");
      for (InputFile f : allFilesToBlame) {
        LOG.warn("  * " + f.absolutePath());
      }
      LOG.warn("This may lead to missing/broken features in SonarQube");
    }
  }
}
