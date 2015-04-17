/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Builder;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.util.ProgressReport;

import javax.annotation.Nullable;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class DefaultBlameOutput implements BlameOutput {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultBlameOutput.class);

  private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\x00-\\x7F]");
  private static final Pattern ACCENT_CODES = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final BatchReportWriter writer;
  private final ResourceCache componentCache;
  private final Set<InputFile> allFilesToBlame = new HashSet<InputFile>();
  private ProgressReport progressReport;
  private int count;
  private int total;

  DefaultBlameOutput(BatchReportWriter writer, ResourceCache componentCache, List<InputFile> filesToBlame) {
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
    Preconditions.checkArgument(allFilesToBlame.contains(file), "It was not expected to blame file " + file.relativePath());

    if (lines.size() != file.lines()) {
      LOG.debug("Ignoring blame result since provider returned " + lines.size() + " blame lines but file " + file.relativePath() + " has " + file.lines() + " lines");
      return;
    }

    BatchResource batchComponent = componentCache.get(file);
    Builder scmBuilder = BatchReport.Changesets.newBuilder();
    scmBuilder.setComponentRef(batchComponent.batchId());
    Map<String, Integer> changesetsIdByRevision = new HashMap<>();

    for (BlameLine line : lines) {
      if (StringUtils.isNotBlank(line.revision())) {
        Integer changesetId = changesetsIdByRevision.get(line.revision());
        if (changesetId == null) {
          addChangeset(scmBuilder, line);
          changesetId = scmBuilder.getChangesetCount() - 1;
          changesetsIdByRevision.put(line.revision(), changesetId);
        }
        scmBuilder.addChangesetIndexByLine(changesetId);
      } else {
        addChangeset(scmBuilder, line);
      }
    }
    writer.writeComponentChangesets(scmBuilder.build());
    allFilesToBlame.remove(file);
    count++;
    progressReport.message(count + "/" + total + " files analyzed, last one was " + file.absolutePath());
  }

  private void addChangeset(Builder scmBuilder, BlameLine line) {
    BatchReport.Changesets.Changeset.Builder changesetBuilder = BatchReport.Changesets.Changeset.newBuilder();
    if (StringUtils.isNotBlank(line.revision())) {
      changesetBuilder.setRevision(line.revision());
    }
    if (StringUtils.isNotBlank(line.author())) {
      changesetBuilder.setAuthor(normalizeString(line.author()));
    }
    Date date = line.date();
    if (date != null) {
      changesetBuilder.setDate(date.getTime());
    }
    scmBuilder.addChangeset(changesetBuilder.build());
  }

  private String normalizeString(@Nullable String inputString) {
    if (inputString == null) {
      return "";
    }
    String lowerCasedString = inputString.toLowerCase();
    String stringWithoutAccents = removeAccents(lowerCasedString);
    return removeNonAsciiCharacters(stringWithoutAccents);
  }

  private String removeAccents(String inputString) {
    String unicodeDecomposedString = Normalizer.normalize(inputString, Normalizer.Form.NFD);
    return ACCENT_CODES.matcher(unicodeDecomposedString).replaceAll("");
  }

  private String removeNonAsciiCharacters(String inputString) {
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
