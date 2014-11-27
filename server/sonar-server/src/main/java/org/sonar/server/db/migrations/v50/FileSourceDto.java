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
package org.sonar.server.db.migrations.v50;

import com.google.common.base.Splitter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.CsvWriter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

class FileSourceDto {

  private static final String SPACE_CHARS = "\t\n\r ";

  private Iterator<String> sourceSplitter;

  private final Map<Integer, String> revisions;
  private final Map<Integer, String> authors;
  private final Map<Integer, String> dates;
  private final Map<Integer, String> utHits;
  private final Map<Integer, String> utConditions;
  private final Map<Integer, String> utCoveredConditions;
  private final Map<Integer, String> itHits;
  private final Map<Integer, String> itConditions;
  private final Map<Integer, String> itCoveredConditions;
  private final Map<Integer, String> overallHits;
  private final Map<Integer, String> overallConditions;
  private final Map<Integer, String> overallCoveredConditions;

  FileSourceDto(String source, String revisions, String authors, String dates,
    String utHits, String utConditions, String utCoveredConditions,
    String itHits, String itConditions, String itCoveredConditions,
    String overallHits, String overallConditions, String overallCoveredConditions) {
    sourceSplitter = Splitter.onPattern("\r?\n|\r").split(source).iterator();
    this.revisions = KeyValueFormat.parseIntString(revisions);
    this.authors = KeyValueFormat.parseIntString(authors);
    this.dates = KeyValueFormat.parseIntString(dates);
    this.utHits = KeyValueFormat.parseIntString(utHits);
    this.utConditions = KeyValueFormat.parseIntString(utConditions);
    this.utCoveredConditions = KeyValueFormat.parseIntString(utCoveredConditions);
    this.itHits = KeyValueFormat.parseIntString(itHits);
    this.itConditions = KeyValueFormat.parseIntString(itConditions);
    this.itCoveredConditions = KeyValueFormat.parseIntString(itCoveredConditions);
    this.overallHits = KeyValueFormat.parseIntString(overallHits);
    this.overallConditions = KeyValueFormat.parseIntString(overallConditions);
    this.overallCoveredConditions = KeyValueFormat.parseIntString(overallCoveredConditions);
  }

  String[] getSourceData() {
    String highlighting = "";
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int line = 0;
    String sourceLine = null;
    CsvWriter csv = CsvWriter.of(new OutputStreamWriter(output, UTF_8));
    StringBuilder lineHashes = new StringBuilder();
    while (sourceSplitter.hasNext()) {
      line++;
      sourceLine = sourceSplitter.next();
      lineHashes.append(lineChecksum(sourceLine)).append("\n");
      csv.values(revisions.get(line), authors.get(line), dates.get(line),
        utHits.get(line), utConditions.get(line), utCoveredConditions.get(line),
        itHits.get(line), itConditions.get(line), itCoveredConditions.get(line),
        overallHits.get(line), overallConditions.get(line), overallCoveredConditions.get(line),
        highlighting, sourceLine);
    }
    csv.close();
    return new String[] {new String(output.toByteArray(), UTF_8), lineHashes.toString()};
  }

  public static String lineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
    if (line.isEmpty()) {
      return "";
    }
    return DigestUtils.md5Hex(reducedLine);
  }

}
