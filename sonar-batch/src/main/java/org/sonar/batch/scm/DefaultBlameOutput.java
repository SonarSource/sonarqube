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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.util.ProgressReport;

import javax.annotation.Nullable;

import java.text.Normalizer;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class DefaultBlameOutput implements BlameOutput {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultBlameOutput.class);

  private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\x00-\\x7F]");
  private static final Pattern ACCENT_CODES = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final SensorContext context;
  private final Set<InputFile> allFilesToBlame = new HashSet<InputFile>();
  private ProgressReport progressReport;
  private int count;
  private int total;

  DefaultBlameOutput(SensorContext context, List<InputFile> filesToBlame) {
    this.context = context;
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

    PropertiesBuilder<Integer, String> authors = propertiesBuilder(CoreMetrics.SCM_AUTHORS_BY_LINE);
    PropertiesBuilder<Integer, String> dates = propertiesBuilder(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
    PropertiesBuilder<Integer, String> revisions = propertiesBuilder(CoreMetrics.SCM_REVISIONS_BY_LINE);

    int lineNumber = 1;
    for (BlameLine line : lines) {
      authors.add(lineNumber, normalizeString(line.author()));
      Date date = line.date();
      dates.add(lineNumber, date != null ? DateUtils.formatDateTime(date) : "");
      revisions.add(lineNumber, line.revision());
      lineNumber++;
    }
    ScmSensor.saveMeasures(context, file, authors.buildData(), dates.buildData(), revisions.buildData());
    allFilesToBlame.remove(file);
    count++;
    progressReport.message(count + "/" + total + " files analyzed, last one was " + file.absolutePath());
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

  private static PropertiesBuilder<Integer, String> propertiesBuilder(Metric metric) {
    return new PropertiesBuilder<Integer, String>(metric);
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
