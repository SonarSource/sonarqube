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
package org.sonar.xoo.scm;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;

public class XooBlameCommand extends BlameCommand {

  private static final String SCM_EXTENSION = ".scm";

  @Override
  public void blame(BlameInput input, BlameOutput result) {
    for (InputFile inputFile : input.filesToBlame()) {
      processFile(inputFile, result);
    }
  }

  @VisibleForTesting
  protected void processFile(InputFile inputFile, BlameOutput result) {
    File ioFile = inputFile.file();
    File scmDataFile = new java.io.File(ioFile.getParentFile(), ioFile.getName() + SCM_EXTENSION);
    if (!scmDataFile.exists()) {
      return;
    }

    try {
      List<String> lines = FileUtils.readLines(scmDataFile, StandardCharsets.UTF_8);
      List<BlameLine> blame = new ArrayList<>(lines.size());
      int lineNumber = 0;
      for (String line : lines) {
        lineNumber++;
        if (StringUtils.isNotBlank(line)) {
          // revision,author,dateTime
          String[] fields = StringUtils.splitPreserveAllTokens(line, ',');
          if (fields.length < 3) {
            throw new IllegalStateException("Not enough fields on line " + lineNumber);
          }
          String revision = StringUtils.trimToNull(fields[0]);
          String author = StringUtils.trimToNull(fields[1]);
          BlameLine blameLine = new BlameLine().revision(revision).author(author);
          String dateStr = StringUtils.trimToNull(fields[2]);
          if (dateStr != null) {
            Date dateTime = DateUtils.parseDateTimeQuietly(dateStr);
            if (dateTime != null) {
              blameLine.date(dateTime);
            } else {
              // Will throw an exception, when date is not in format "yyyy-MM-dd"
              blameLine.date(DateUtils.parseDate(dateStr));
            }
          }
          blame.add(blameLine);
        }
      }
      result.blameResult(inputFile, blame);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
