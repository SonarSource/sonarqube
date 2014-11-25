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

  private Map<Integer, String> revisions;
  private Map<Integer, String> authors;
  private Map<Integer, String> dates;
  private Map<Integer, String> hits;
  private Map<Integer, String> conditions;
  private Map<Integer, String> coveredConditions;

  FileSourceDto(String source, String revisions, String authors, String dates, String hits, String conditions, String coveredConditions) {
    sourceSplitter = Splitter.onPattern("\r?\n|\r").split(source).iterator();
    this.revisions = KeyValueFormat.parseIntString(revisions);
    this.authors = KeyValueFormat.parseIntString(authors);
    this.dates = KeyValueFormat.parseIntString(dates);
    this.hits = KeyValueFormat.parseIntString(hits);
    this.conditions = KeyValueFormat.parseIntString(conditions);
    this.coveredConditions = KeyValueFormat.parseIntString(coveredConditions);
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
        hits.get(line), conditions.get(line), coveredConditions.get(line),
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
