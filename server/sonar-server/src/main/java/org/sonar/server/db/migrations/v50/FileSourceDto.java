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
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.CsvWriter;

import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

class FileSourceDto {

  private Iterator<String> sourceSplitter;

  private Map<Integer, String> revisions;

  private Map<Integer, String> authors;

  private Map<Integer, String> dates;

  FileSourceDto(String source, @Nullable String shortRevisions, @Nullable String longRevisions, @Nullable String shortAuthors, @Nullable String longAuthors,
    @Nullable String shortDates, @Nullable String longDates) {
    sourceSplitter = Splitter.onPattern("\r?\n|\r").split(source).iterator();
    revisions = KeyValueFormat.parseIntString(ofNullableBytes(shortRevisions, longRevisions));
    authors = KeyValueFormat.parseIntString(ofNullableBytes(shortAuthors, longAuthors));
    dates = KeyValueFormat.parseIntString(ofNullableBytes(shortDates, longDates));
  }

  String getSourceData() {
    String highlighting = "";
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int line = 0;
    String sourceLine = null;
    CsvWriter csv = CsvWriter.of(new OutputStreamWriter(output, UTF_8));
    while (sourceSplitter.hasNext()) {
      line ++;
      sourceLine = sourceSplitter.next();
      csv.values(revisions.get(line), authors.get(line), dates.get(line), highlighting, sourceLine);
    }
    csv.close();
    return new String(output.toByteArray(), UTF_8);
  }

  private static String ofNullableBytes(@Nullable String shortBytes, @Nullable String longBytes) {
    String result;
    if (shortBytes == null) {
      if (longBytes == null) {
        result = "";
      } else {
        result = longBytes;
      }
    } else {
      result = shortBytes;
    }
    return result;
  }
}
