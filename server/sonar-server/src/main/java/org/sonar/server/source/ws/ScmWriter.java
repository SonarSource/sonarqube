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

import org.sonar.api.ServerComponent;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;

import java.util.Map;

public class ScmWriter implements ServerComponent {

  public void write(@Nullable String authorsData, @Nullable String datesData, int from, int to, boolean showCommitsByLine, JsonWriter json) {
    json.name("scm").beginArray();
    if (authorsData != null) {
      Map<Integer, String> authors = KeyValueFormat.parseIntString(authorsData);
      Map<Integer, String> dates = KeyValueFormat.parseIntString(datesData);

      String previousAuthor = null;
      String previousDate = null;
      boolean started = false;
      for (Map.Entry<Integer, String> entry : authors.entrySet()) {
        Integer line = entry.getKey();
        String author = entry.getValue();
        String date = dates.get(line);
        String formattedDate = DateUtils.formatDate(DateUtils.parseDateTime(date));
        if (line >= from && line <= to) {
          if (!started || showCommitsByLine || !isSameCommit(date, previousDate, author, previousAuthor)) {
            json.beginArray();
            json.value(line);
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
    json.endArray();
  }

  private boolean isSameCommit(String date, String previousDate, String author, String previousAuthor) {
    return author.equals(previousAuthor) && date.equals(previousDate);
  }
}
