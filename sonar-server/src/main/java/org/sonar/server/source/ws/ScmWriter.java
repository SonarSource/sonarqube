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
package org.sonar.server.source.ws;

import com.google.common.base.Splitter;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ScmWriter implements ServerComponent {

  void write(@Nullable String authorsData, @Nullable String datesDate, int from, int to, boolean group, JsonWriter json) {
    json.name("scm").beginObject();
    if (authorsData != null) {
      List<String> authors = splitLine(authorsData);
      List<String> dates = splitLine(datesDate);

      String previousAuthor = null;
      String previousDate = null;
      boolean started = false;
      for (int i = 0; i < authors.size(); i++) {
        String[] authorWithLine = splitColumn(authors.get(i));
        Integer line = Integer.parseInt(authorWithLine[0]);
        String author = authorWithLine[1];

        String[] dateWithLine = splitColumn(dates.get(i));
        String date = dateWithLine[1];
        String formattedDate = DateUtils.formatDate(DateUtils.parseDateTime(date));
        if (line >= from && line <= to) {
          if (!started || !group || !isSameCommit(date, previousDate, author, previousAuthor)) {
            json.name(Integer.toString(line)).beginArray();
            json.value(author);
            json.value(formattedDate);
            json.endArray();
            started = true;
          }
        }
        previousAuthor = author;
        previousDate = date;
      }
    }
    json.endObject();
  }

  private List<String> splitLine(@Nullable String line) {
    if (line == null) {
      return Collections.emptyList();
    }
    return newArrayList(Splitter.on(";").omitEmptyStrings().split(line));
  }

  private String[] splitColumn(String column) {
    return column.split("=");
  }

  private boolean isSameCommit(String date, String previousDate, String author, String previousAuthor) {
    return author.equals(previousAuthor) && date.equals(previousDate);
  }
}
