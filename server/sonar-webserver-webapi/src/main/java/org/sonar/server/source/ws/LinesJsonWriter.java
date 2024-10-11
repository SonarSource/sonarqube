/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.source.ws;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.source.HtmlSourceDecorator;

public class LinesJsonWriter {
  private final HtmlSourceDecorator htmlSourceDecorator;

  public LinesJsonWriter(HtmlSourceDecorator htmlSourceDecorator) {
    this.htmlSourceDecorator = htmlSourceDecorator;
  }

  public void writeSource(Iterable<DbFileSources.Line> lines, JsonWriter json, Supplier<Optional<Long>> periodDateSupplier,
    boolean showAuthor) {
    Long periodDate = null;

    json.name("sources").beginArray();
    for (DbFileSources.Line line : lines) {
      json.beginObject()
        .prop("line", line.getLine())
        .prop("code", htmlSourceDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols()))
        .prop("scmRevision", line.getScmRevision());
      if (showAuthor) {
        json.prop("scmAuthor", line.getScmAuthor());
      }
      if (line.hasScmDate()) {
        json.prop("scmDate", DateUtils.formatDateTime(new Date(line.getScmDate())));
      }
      getLineHits(line).ifPresent(lh -> {
        json.prop("utLineHits", lh);
        json.prop("lineHits", lh);
      });
      getConditions(line).ifPresent(conditions -> {
        json.prop("utConditions", conditions);
        json.prop("conditions", conditions);
      });
      getCoveredConditions(line).ifPresent(coveredConditions -> {
        json.prop("utCoveredConditions", coveredConditions);
        json.prop("coveredConditions", coveredConditions);
      });
      json.prop("duplicated", line.getDuplicationCount() > 0);
      if (line.hasIsNewLine()) {
        json.prop("isNew", line.getIsNewLine());
      } else {
        if (periodDate == null) {
          periodDate = periodDateSupplier.get().orElse(Long.MAX_VALUE);
        }
        json.prop("isNew", line.getScmDate() > periodDate);
      }
      json.endObject();
    }
    json.endArray();
  }

  private static Optional<Integer> getLineHits(DbFileSources.Line line) {
    if (line.hasLineHits()) {
      return Optional.of(line.getLineHits());
    } else if (line.hasDeprecatedOverallLineHits()) {
      return Optional.of(line.getDeprecatedOverallLineHits());
    } else if (line.hasDeprecatedUtLineHits()) {
      return Optional.of(line.getDeprecatedUtLineHits());
    } else if (line.hasDeprecatedItLineHits()) {
      return Optional.of(line.getDeprecatedItLineHits());
    }
    return Optional.empty();
  }

  private static Optional<Integer> getConditions(DbFileSources.Line line) {
    if (line.hasConditions()) {
      return Optional.of(line.getConditions());
    } else if (line.hasDeprecatedOverallConditions()) {
      return Optional.of(line.getDeprecatedOverallConditions());
    } else if (line.hasDeprecatedUtConditions()) {
      return Optional.of(line.getDeprecatedUtConditions());
    } else if (line.hasDeprecatedItConditions()) {
      return Optional.of(line.getDeprecatedItConditions());
    }
    return Optional.empty();
  }

  private static Optional<Integer> getCoveredConditions(DbFileSources.Line line) {
    if (line.hasCoveredConditions()) {
      return Optional.of(line.getCoveredConditions());
    } else if (line.hasDeprecatedOverallCoveredConditions()) {
      return Optional.of(line.getDeprecatedOverallCoveredConditions());
    } else if (line.hasDeprecatedUtCoveredConditions()) {
      return Optional.of(line.getDeprecatedUtCoveredConditions());
    } else if (line.hasDeprecatedItCoveredConditions()) {
      return Optional.of(line.getDeprecatedItCoveredConditions());
    }
    return Optional.empty();
  }
}
