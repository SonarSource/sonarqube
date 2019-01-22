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
package org.sonar.xoo.scm;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.trimToNull;

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
    File scmDataFile = new File(ioFile.getParentFile(), ioFile.getName() + SCM_EXTENSION);
    if (!scmDataFile.exists()) {
      return;
    }

    try {
      List<BlameLine> blame = readFile(scmDataFile);
      result.blameResult(inputFile, blame);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static List<BlameLine> readFile(File inputStream) throws IOException {
    try (CSVParser csvParser = CSVFormat.RFC4180
      .withIgnoreEmptyLines()
      .withIgnoreSurroundingSpaces()
      .parse(new FileReader(inputStream))) {
      List<CSVRecord> records = csvParser.getRecords();
      return records.stream()
        .map(XooBlameCommand::convertToBlameLine)
        .collect(toList());
    }
  }

  private static BlameLine convertToBlameLine(CSVRecord csvRecord) {
    checkState(csvRecord.size() == 3, "Not enough fields on line %s", csvRecord);
    String revision = trimToNull(csvRecord.get(0));
    String author = trimToNull(csvRecord.get(1));
    BlameLine blameLine = new BlameLine().revision(revision).author(author);
    String dateStr = trimToNull(csvRecord.get(2));
    if (dateStr != null) {
      Date dateTime = DateUtils.parseDateTimeQuietly(dateStr);
      if (dateTime != null) {
        blameLine.date(dateTime);
      } else {
        // Will throw an exception, when date is not in format "yyyy-MM-dd"
        blameLine.date(DateUtils.parseDate(dateStr));
      }
    }
    return blameLine;
  }
}
